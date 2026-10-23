# Integration smoke test against an isolated, temporary PostgreSQL schema.
# Requires the existing Docker Compose stack, freshly built backend image and judge images.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(60)
$schema = 'contest_smoke_' + [Guid]::NewGuid().ToString('N')
$containerName = 'devedu-contest-smoke-' + [Guid]::NewGuid().ToString('N')
$containerId = $null
$schemaCreated = $false

function Docker-Checked([string[]] $Arguments) {
    $output = & docker @Arguments
    if ($LASTEXITCODE -ne 0) { throw 'Docker test command failed' }
    return $output
}
function Test-Api([string] $Method, [string] $Path, $Body, [string] $Token = '', [int] $Expected = 200) {
    $request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::new($Method), ($script:baseUrl + $Path))
    if ($Token) { $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token) }
    if ($null -ne $Body) { $request.Content = [System.Net.Http.StringContent]::new(($Body | ConvertTo-Json -Depth 8 -Compress), [Text.Encoding]::UTF8, 'application/json') }
    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if ([int]$response.StatusCode -ne $Expected) { throw "$Method $Path expected $Expected, got $([int]$response.StatusCode): $content" }
        if ($content) { return $content | ConvertFrom-Json }
    } finally { $request.Dispose() }
}
function Assert-True([bool] $Condition, [string] $Message) { if (-not $Condition) { throw $Message } }

try {
    $pgId = Docker-Checked @('compose', 'ps', '-q', 'postgres')
    $backendId = Docker-Checked @('compose', 'ps', '-q', 'backend')
    $pg = (Docker-Checked @('inspect', $pgId) | ConvertFrom-Json)[0]
    $backend = (Docker-Checked @('inspect', $backendId) | ConvertFrom-Json)[0]
    $pgEnv = @{}
    foreach ($entry in $pg.Config.Env) { $parts = $entry -split '=', 2; $pgEnv[$parts[0]] = $parts[1] }
    $backendEnv = @{}
    foreach ($entry in $backend.Config.Env) { $parts = $entry -split '=', 2; $backendEnv[$parts[0]] = $parts[1] }
    $network = @($backend.NetworkSettings.Networks.PSObject.Properties.Name)[0]
    $dbUser = $pgEnv['POSTGRES_USER']; $dbName = $pgEnv['POSTGRES_DB']
    Docker-Checked @('exec', $pgId, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $dbUser, '-d', $dbName, '-c', "CREATE SCHEMA $schema") | Out-Null
    $schemaCreated = $true
    $password = [Guid]::NewGuid().ToString('N')
    $jwt = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    $dbUrl = ($backendEnv['DB_URL'] -split '\?')[0] + '?currentSchema=' + $schema
    $containerId = Docker-Checked @('run', '-d', '--name', $containerName, '--network', $network,
        '-p', '127.0.0.1::8080', '--group-add', '0',
        '-v', '/var/run/docker.sock:/var/run/docker.sock', '-v', 'devedu_judge_workspaces:/judge-workspaces',
        '-e', "DB_URL=$dbUrl", '-e', "DB_USERNAME=$dbUser", '-e', "DB_PASSWORD=$($pgEnv['POSTGRES_PASSWORD'])",
        '-e', "JWT_SECRET=$jwt", '-e', 'ADMIN_EMAIL=contest-smoke@example.com', '-e', "ADMIN_PASSWORD=$password", '-e', 'ADMIN_NAME=Contest Smoke',
        '-e', 'JUDGE_WORKSPACE_ROOT=/judge-workspaces', '-e', 'JUDGE_WORKSPACE_VOLUME=devedu_judge_workspaces', $backend.Config.Image)
    $binding = Docker-Checked @('port', $containerId, '8080')
    $script:baseUrl = 'http://' + $binding.Trim()
    $ready = $false
    for ($i = 0; $i -lt 45; $i++) {
        try { Test-Api 'GET' '/api/system/status' $null | Out-Null; $ready = $true; break } catch { Start-Sleep -Seconds 1 }
    }
    Assert-True $ready 'Temporary backend did not start'
    Write-Output 'Isolated Contest backend ready.'
    $admin = Test-Api 'POST' '/api/auth/login' @{ email='contest-smoke@example.com'; password=$password }
    $student = Test-Api 'POST' '/api/auth/register' @{ name='Contest Student'; email='contest-student@example.com'; password=$password } '' 201
    $teacher = Test-Api 'POST' '/api/auth/register' @{ name='Contest Teacher'; email='contest-teacher@example.com'; password=$password } '' 201
    Test-Api 'PATCH' "/api/admin/users/$($teacher.user.id)/role" @{ role='TEACHER' } $admin.accessToken | Out-Null
    $teacher = Test-Api 'POST' '/api/auth/login' @{ email='contest-teacher@example.com'; password=$password }
    $problem = Test-Api 'GET' '/api/problems/tong-hai-so' $null
    $body = @{ name='Contest integration smoke'; type='CUSTOM'; startsAt=[DateTime]::UtcNow.AddHours(1).ToString('o'); durationMinutes=1; problems=@(@{ problemId=$problem.id; points=100 }) }
    Test-Api 'POST' '/api/teacher/contests' $body $student.accessToken 403 | Out-Null
    $created = Test-Api 'POST' '/api/teacher/contests' $body $teacher.accessToken 201
    $id = $created.id
    Assert-True ([Guid]::TryParse($id, [ref]([Guid]::Empty))) 'Invalid Contest ID'
    $path = "/api/contests/$id"
    $submitPath = "$path/problems/$($problem.id)/submissions"
    $code = @{ language='CPP'; sourceCode='#include <iostream>
int main() { long long a, b; std::cin >> a >> b; std::cout << a + b; }' }
    Test-Api 'POST' "$path/registration" $null '' 401 | Out-Null
    Test-Api 'POST' "$path/registration" $null $student.accessToken 204
    Test-Api 'POST' "$path/registration" $null $student.accessToken 204
    $detail = Test-Api 'GET' $path $null $student.accessToken
    Assert-True ($detail.contest.participants -eq 1 -and $detail.registered -and $detail.contest.status -eq 'UPCOMING') 'Registration is not idempotent'
    Assert-True ($detail.PSObject.Properties.Name -notcontains 'rules' -and $detail.contest.PSObject.Properties.Name -notcontains 'rated') 'Removed fields still exposed'
    Test-Api 'POST' $submitPath $code $student.accessToken 409 | Out-Null
    Docker-Checked @('exec', $pgId, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $dbUser, '-d', $dbName, '-c', "UPDATE $schema.contests SET starts_at=now()-interval '5 seconds', ends_at=now()+interval '55 seconds' WHERE id='$id'::uuid") | Out-Null
    Test-Api 'POST' $submitPath $code $teacher.accessToken 403 | Out-Null
    Test-Api 'POST' $submitPath $code $student.accessToken 409 | Out-Null
    $attempt = Test-Api 'POST' "$path/attempt" $null $student.accessToken
    $again = Test-Api 'POST' "$path/attempt" $null $student.accessToken
    Assert-True ($attempt.startsAt -eq $again.startsAt -and $attempt.endsAt -eq $again.endsAt) 'Personal clock reset on repeated start'
    $refresh = Test-Api 'GET' $path $null $student.accessToken
    Assert-True ($refresh.myAttempt.startsAt -eq $attempt.startsAt) 'Personal clock was lost on refresh'
    Test-Api 'POST' "$path/problems/$([Guid]::NewGuid())/submissions" $code $student.accessToken 404 | Out-Null
    Test-Api 'POST' $submitPath @{ language='HTML'; sourceCode='<p>invalid language</p>' } $student.accessToken 400 | Out-Null
    $wrong = Test-Api 'POST' $submitPath @{ language='CPP'; sourceCode='#include <iostream>
int main() { std::cout << -999999999; }' } $student.accessToken
    Assert-True ($wrong.status -eq 'WRONG_ANSWER') 'Wrong answer was not judged correctly'
    foreach ($attempt in 1..2) {
        $accepted = Test-Api 'POST' $submitPath $code $student.accessToken
        Assert-True ($accepted.status -eq 'ACCEPTED') 'Existing Judge did not accept the sum solution'
    }
    $detail = Test-Api 'GET' $path $null $student.accessToken
    Assert-True ($detail.myResult.score -eq 100 -and $detail.mySubmissions.Count -eq 3 -and $detail.problems[0].solved) 'Scores/history did not persist correctly'
    Assert-True ($detail.myResult.problems[0].wrongAttempts -eq 1 -and $detail.myResult.problems[0].attempts -eq 3) 'Leaderboard problem attempts are incorrect'
    Assert-True ($detail.scoringRules.wrongAttemptPenaltySeconds -eq 0 -and $detail.scoringRules.resubmissionAllowed) 'Scoring rules are incorrect'
    Assert-True ($detail.mySubmissions[0].score -eq 100 -and $detail.mySubmissions[0].maxScore -eq 100) 'Submission scores are missing'
    $anonymous = Test-Api 'GET' $path $null
    Assert-True ($anonymous.mySubmissions.Count -eq 0 -and -not $anonymous.registered -and -not $anonymous.problems[0].solved) 'Private history leaked'
    Docker-Checked @('exec', $pgId, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $dbUser, '-d', $dbName, '-c', "UPDATE $schema.contests SET starts_at=now()-interval '61 seconds', ends_at=now()-interval '1 second' WHERE id='$id'::uuid") | Out-Null
    Test-Api 'POST' $submitPath $code $student.accessToken 409 | Out-Null
    Test-Api 'POST' "$path/registration" $null $teacher.accessToken 409 | Out-Null
    $finished = Test-Api 'GET' '/api/contests?status=FINISHED' $null
    Assert-True ($finished.contests.Count -eq 1 -and $finished.contests[0].participants -eq 1) 'Finished catalog is incorrect'
    for ($i = 0; $i -lt 15; $i++) {
        $result = Test-Api 'GET' "$path/results" $null $student.accessToken
        if ($result.finalized) { break }
        Start-Sleep -Seconds 1
    }
    Assert-True ($result.finalized -and $result.detail.myResult.score -eq 100 -and $result.PSObject.Properties.Name -notcontains 'rating') 'Finalization or result without rating failed'
    $profile = Test-Api 'GET' "/api/profiles/user-$($student.user.publicId)/contests" $null $student.accessToken
    Assert-True ($profile.history.Count -eq 1 -and $profile.history[0].score -eq 100 -and $profile.PSObject.Properties.Name -notcontains 'currentRating') 'Contest history without rating failed'
    $virtual = Test-Api 'POST' "$path/virtual" $null $student.accessToken
    Assert-True ($virtual.detail.contest.status -eq 'ONGOING' -and $virtual.detail.myResult.score -eq 0) 'Virtual contest no longer works'
    $pastBody = @{ name='Past opening'; description='Description persists'; monitoringEnabled=$true; type='CUSTOM'; startsAt=[DateTime]::UtcNow.AddHours(-1).ToString('o'); endsAt=[DateTime]::UtcNow.AddHours(2).ToString('o'); durationMinutes=1; problems=@(@{ problemId=$problem.id; points=100 }) }
    $past = Test-Api 'POST' '/api/teacher/contests' $pastBody $teacher.accessToken 201
    $pastPath = "/api/contests/$($past.id)"
    Test-Api 'POST' "$pastPath/registration" $null $student.accessToken 204 | Out-Null
    $personal = Test-Api 'POST' "$pastPath/attempt" $null $student.accessToken
    Assert-True (([datetime]$personal.endsAt - [datetime]$personal.startsAt).TotalSeconds -eq 60) 'Personal duration ignored'
    $pastDetail = Test-Api 'GET' $pastPath $null $student.accessToken
    Assert-True ($pastDetail.contest.description -eq 'Description persists' -and $pastDetail.contest.monitoringEnabled -and $pastDetail.contest.status -eq 'ONGOING') 'New fields did not persist'
    $cameraMine = Test-Api 'GET' "$pastPath/camera/me" $null $student.accessToken
    Assert-True ($cameraMine.canMonitor -and -not $cameraMine.canReview -and $cameraMine.PSObject.Properties.Name -notcontains 'summary') 'Student camera endpoint exposed statistics or incorrect capabilities'
    $observation = @{ eventId=[Guid]::NewGuid().ToString(); kind='LOOKING_AWAY' }
    Test-Api 'POST' "$pastPath/camera/events" $observation $student.accessToken 204 | Out-Null
    Test-Api 'POST' "$pastPath/camera/events" $observation $student.accessToken 204 | Out-Null
    $reportPath = "/api/teacher/contests/$($past.id)/camera"
    Test-Api 'GET' $reportPath $null $student.accessToken 403 | Out-Null
    $teacherReport = Test-Api 'GET' $reportPath $null $teacher.accessToken
    $adminReport = Test-Api 'GET' $reportPath $null $admin.accessToken
    Assert-True ($teacherReport.participants[0].lookingAwayCount -eq 1 -and $adminReport.participants[0].lookingAwayCount -eq 1) 'Authorized camera report or deduplication failed'
    $otherTeacher = Test-Api 'POST' '/api/auth/register' @{ name='Other Teacher'; email='other-teacher@example.com'; password=$password } '' 201
    Test-Api 'PATCH' "/api/admin/users/$($otherTeacher.user.id)/role" @{ role='TEACHER' } $admin.accessToken | Out-Null
    $otherTeacher = Test-Api 'POST' '/api/auth/login' @{ email='other-teacher@example.com'; password=$password }
    Test-Api 'GET' $reportPath $null $otherTeacher.accessToken 403 | Out-Null
    Docker-Checked @('exec', $pgId, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $dbUser, '-d', $dbName, '-c', "UPDATE $schema.contest_registrations SET attempt_started_at=now()-interval '61 seconds' WHERE contest_id='$($past.id)'::uuid") | Out-Null
    Test-Api 'POST' "$pastPath/problems/$($problem.id)/submissions" $code $student.accessToken 409 | Out-Null
    Test-Api 'POST' "$pastPath/camera/events" @{ eventId=[Guid]::NewGuid().ToString(); kind='LOOKING_AWAY' } $student.accessToken 409 | Out-Null
    $endedCamera = Test-Api 'GET' "$pastPath/camera/me" $null $student.accessToken
    Assert-True (-not $endedCamera.canMonitor) 'Camera remained allowed after personal deadline'
    $expired = Test-Api 'POST' "$pastPath/attempt" $null $student.accessToken
    Assert-True ([datetimeoffset]$expired.endsAt -lt [datetimeoffset]::UtcNow) 'Expired attempt was restarted'
    Write-Output 'PASS: create, personal deadline, real Judge, camera privacy, supervisor authorization, event deduplication and camera expiry.'
} catch {
    if ($containerId -and $containerId -match '^[a-f0-9]{64}$') { & docker logs --tail 250 $containerId 2>&1 | Select-String -Pattern 'ERROR|Caused by:|Exception:|com.devedu.learningplatform.application|com.devedu.learningplatform.infrastructure.persistence' -Context 0,3 }
    throw
} finally {
    $client.Dispose()
    if ($containerId -and $containerId -match '^[a-f0-9]{64}$') { & docker rm -f $containerId | Out-Null }
    if ($schemaCreated -and $schema -match '^contest_smoke_[a-f0-9]{32}$') {
        & docker exec $pgId psql -v ON_ERROR_STOP=1 -U $dbUser -d $dbName -c "SET client_min_messages=warning; DROP SCHEMA $schema CASCADE" | Out-Null
        Write-Output 'Removed only the temporary test schema/container; application data was untouched.'
    }
}
