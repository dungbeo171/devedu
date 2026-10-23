package com.devedu.learningplatform.infrastructure.persistence.contest;

import com.devedu.learningplatform.application.contest.*;
import com.devedu.learningplatform.application.port.in.ProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.port.in.CodeJudgeUseCase;
import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.domain.model.ProblemSubmission;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.contest.ContestProblemDefinition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class ContestSubmissionAdapter implements ContestSubmissionPort {
    private final ProgrammingProblemsUseCase problems;
    private final ContestRepository contests;
    private final EntityManager em;
    private final ProblemSubmissionRepository saved;
    private final Clock clock;
    private final CodeJudgeUseCase judge;
    public ContestSubmissionAdapter(ProgrammingProblemsUseCase problems, ContestRepository contests, EntityManager em,
                                    ProblemSubmissionRepository saved, CodeJudgeUseCase judge, Clock clock) {
        this.problems = problems; this.contests = contests; this.em=em; this.saved=saved; this.judge=judge; this.clock=clock;
    }
    @Override @Transactional
    public ProblemSubmission submit(UUID contestId, SubmitProblemCommand command, Instant receivedAt) {
        return submit(contestId,command,receivedAt,null,null);
    }
    @Override @Transactional
    public ProblemSubmission submit(UUID contestId, SubmitProblemCommand command, Instant receivedAt, UUID virtualId, String requestKey) {
        if(requestKey==null) requestKey=UUID.randomUUID().toString();
        if(!requestKey.matches("[A-Za-z0-9_-]{1,100}")) throw new IllegalArgumentException("Invalid Idempotency-Key");
        UUID scope=virtualId==null?contestId:virtualId;
        // Shared admission lock lets different submissions run in parallel, but excludes finalization.
        em.createNativeQuery("select 1 from pg_advisory_xact_lock_shared(hashtextextended(:key,0))").setParameter("key","contest:"+contestId).getSingleResult();
        em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:key,0))").setParameter("key","request:"+scope+":"+command.studentId()+":"+requestKey).getSingleResult();
        String hash=digest(command.problemSlug()+"\n"+command.language()+"\n"+command.sourceCode());
        var existing=em.createNativeQuery("select request_hash,submission_id from contest_submission_requests where scope_id=:scope and user_id=:user and request_key=:key")
                .setParameter("scope",scope).setParameter("user",command.studentId()).setParameter("key",requestKey).getResultList();
        if(!existing.isEmpty()) {
            var row=(Object[])existing.get(0);
            if(!hash.equals(row[0])) throw new ContestException(ContestException.Kind.CONFLICT,"Idempotency-Key was already used for different code");
            return saved.findById((UUID)row[1]).orElseThrow();
        }
        String admission=virtualId==null
                ? "select count(*) from contests c join contest_registrations r on r.contest_id=c.id and r.user_id=:user where c.id=:id and c.deleted_at is null and c.starts_at <= clock_timestamp() and r.attempt_started_at <= clock_timestamp() and least(c.ends_at,r.attempt_started_at+c.duration_minutes*interval '1 minute') > clock_timestamp() and not exists(select 1 from contest_finalizations f where f.contest_id=c.id)"
                : "select count(*) from virtual_contests where id=:id and user_id=:user and contest_id=:contest and starts_at <= clock_timestamp() and ends_at > clock_timestamp()";
        var query=em.createNativeQuery(admission).setParameter("id",scope).setParameter("user",command.studentId());
        if(virtualId!=null) query.setParameter("contest",contestId);
        if(((Number)query.getSingleResult()).longValue()==0) throw new ContestException(ContestException.Kind.CONFLICT,"Contest is not accepting submissions");
        receivedAt=clock.instant();
        var submission = problems.submit(command);
        if(virtualId==null) contests.linkSubmission(contestId, submission.id(), receivedAt);
        else em.createNativeQuery("insert into contest_submissions(submission_id,contest_id,received_at,virtual_id,mode) values(:submission,:contest,:at,:virtual,'VIRTUAL')")
                .setParameter("submission",submission.id()).setParameter("contest",contestId).setParameter("at",receivedAt).setParameter("virtual",virtualId).executeUpdate();
        em.createNativeQuery("insert into contest_submission_requests(scope_id,user_id,request_key,request_hash,submission_id) values(:scope,:user,:key,:hash,:submission)")
                .setParameter("scope",scope).setParameter("user",command.studentId()).setParameter("key",requestKey).setParameter("hash",hash).setParameter("submission",submission.id()).executeUpdate();
        return submission;
    }
    private String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    @Override @Transactional
    public ProblemSubmission submitLocal(UUID contestId, ContestProblemDefinition definition, UUID userId,
                                         CodeLanguage language, String sourceCode, Instant receivedAt,
                                         UUID virtualId, String requestKey) {
        if (requestKey == null) requestKey = UUID.randomUUID().toString();
        if (!requestKey.matches("[A-Za-z0-9_-]{1,100}")) throw new IllegalArgumentException("Invalid Idempotency-Key");
        UUID scope = virtualId == null ? contestId : virtualId;
        em.createNativeQuery("select 1 from pg_advisory_xact_lock_shared(hashtextextended(:key,0))")
                .setParameter("key", "contest:" + contestId).getSingleResult();
        em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "local-request:" + scope + ":" + userId + ":" + requestKey).getSingleResult();
        String hash = digest(definition.problem().id() + "\n" + language + "\n" + sourceCode);
        var existing = em.createNativeQuery("select request_hash,submission_id from contest_local_submission_requests where scope_id=:scope and user_id=:user and request_key=:key")
                .setParameter("scope", scope).setParameter("user", userId).setParameter("key", requestKey).getResultList();
        if (!existing.isEmpty()) {
            var row = (Object[]) existing.get(0);
            if (!hash.equals(row[0])) throw new ContestException(ContestException.Kind.CONFLICT, "Idempotency-Key was already used for different code");
            return localSubmission((UUID) row[1]);
        }
        requireAdmission(contestId, scope, userId, virtualId);
        receivedAt = clock.instant();
        var id = UUID.randomUUID();
        var result = judge.judge(new JudgeSubmissionCommand(id, language, sourceCode, definition.testCases()));
        var submission = new ProblemSubmission(id, definition.problem().id(), userId, language, sourceCode,
                result.status(), result.diagnostic(), result.passedTests(), result.totalTests(), result.executionTimeMillis(), receivedAt);
        em.createNativeQuery("""
                insert into contest_local_submissions
                (id,contest_id,virtual_id,mode,problem_id,user_id,language,source_code,status,diagnostic,passed_tests,total_tests,execution_time_ms,received_at)
                values(:id,:contest,:virtual,:mode,:problem,:user,:language,:source,:status,:diagnostic,:passed,:total,:runtime,:received)
                """).setParameter("id", id).setParameter("contest", contestId).setParameter("virtual", virtualId)
                .setParameter("mode", virtualId == null ? "OFFICIAL" : "VIRTUAL").setParameter("problem", definition.problem().id())
                .setParameter("user", userId).setParameter("language", language.name()).setParameter("source", sourceCode)
                .setParameter("status", result.status().name()).setParameter("diagnostic", result.diagnostic())
                .setParameter("passed", result.passedTests()).setParameter("total", result.totalTests())
                .setParameter("runtime", result.executionTimeMillis()).setParameter("received", receivedAt).executeUpdate();
        em.createNativeQuery("insert into contest_local_submission_requests(scope_id,user_id,request_key,request_hash,submission_id) values(:scope,:user,:key,:hash,:submission)")
                .setParameter("scope", scope).setParameter("user", userId).setParameter("key", requestKey)
                .setParameter("hash", hash).setParameter("submission", id).executeUpdate();
        return submission;
    }

    private void requireAdmission(UUID contestId, UUID scope, UUID userId, UUID virtualId) {
        String admission = virtualId == null
                ? "select count(*) from contests c join contest_registrations r on r.contest_id=c.id and r.user_id=:user where c.id=:id and c.deleted_at is null and c.starts_at <= clock_timestamp() and r.attempt_started_at <= clock_timestamp() and least(c.ends_at,r.attempt_started_at+c.duration_minutes*interval '1 minute') > clock_timestamp() and not exists(select 1 from contest_finalizations f where f.contest_id=c.id)"
                : "select count(*) from virtual_contests where id=:id and user_id=:user and contest_id=:contest and starts_at <= clock_timestamp() and ends_at > clock_timestamp()";
        var query = em.createNativeQuery(admission).setParameter("id", scope).setParameter("user", userId);
        if (virtualId != null) query.setParameter("contest", contestId);
        if (((Number) query.getSingleResult()).longValue() == 0) throw new ContestException(ContestException.Kind.CONFLICT, "Contest is not accepting submissions");
    }

    private ProblemSubmission localSubmission(UUID id) {
        var rows = em.createNativeQuery("select id,problem_id,user_id,language,source_code,status,diagnostic,passed_tests,total_tests,execution_time_ms,received_at from contest_local_submissions where id=:id")
                .setParameter("id", id).getResultList();
        if (rows.isEmpty()) throw new ContestException(ContestException.Kind.NOT_FOUND, "Submission is unavailable");
        var row = (Object[]) rows.get(0);
        Object time = row[10];
        Instant at = time instanceof Instant instant ? instant : time instanceof java.time.OffsetDateTime date ? date.toInstant() : ((java.sql.Timestamp) time).toInstant();
        return new ProblemSubmission((UUID) row[0], (UUID) row[1], (UUID) row[2], CodeLanguage.valueOf((String) row[3]),
                (String) row[4], com.devedu.learningplatform.domain.model.SubmissionStatus.valueOf((String) row[5]),
                (String) row[6], ((Number) row[7]).intValue(), ((Number) row[8]).intValue(), ((Number) row[9]).longValue(), at);
    }
}
