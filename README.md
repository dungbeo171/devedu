# DevEdu

Nền tảng website học lập trình được tổ chức dưới dạng modular monolith, gồm các module học tập và Code Judge cho Programming Problems.

## Công nghệ

- Backend: Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA, Spring Security
- Database: PostgreSQL 17
- Frontend: React 19, TypeScript, Vite, Tailwind CSS 4
- API: REST
- Hạ tầng local: Docker Compose cho frontend, backend, PostgreSQL và Docker sandbox cho Code Judge

## Cấu trúc

```text
.
├── backend
│   └── src
│       ├── main
│       │   ├── java/com/devedu/learningplatform
│       │   │   ├── domain
│       │   │   ├── application
│       │   │   ├── infrastructure
│       │   │   └── presentation
│       │   └── resources
│       └── test
├── frontend
│   └── src
│       ├── app
│       ├── features
│       └── styles
├── compose.yaml
└── AGENTS.md
```

Backend tuân theo hướng phụ thuộc của Clean Architecture:

```text
presentation ──> application ──> domain
                       ^
                       │
                infrastructure
```

Khi có nghiệp vụ mới, mỗi module nghiệp vụ vẫn tuân theo bốn lớp này và giao tiếp qua contract rõ ràng trong cùng một ứng dụng triển khai. Frontend đặt code nghiệp vụ trong `src/features/<feature-name>`; `src/app` chỉ dùng để khởi tạo và kết nối ứng dụng.

## Yêu cầu

- Java 17
- Node.js 20.19+ hoặc 22.12+
- Docker với Docker Compose

Không cần cài Maven toàn cục vì project có Maven Wrapper.

## Chạy toàn bộ bằng Docker Compose

Chuẩn bị trước các runner image của Code Judge (judge dùng `--pull=never` để request của người dùng không thể tự tải image):

```bash
docker pull gcc:14.4
docker pull eclipse-temurin:17-jdk-alpine-3.23
docker pull python:3.13-alpine
docker pull alpine:3.23
docker pull mysql:8.4
```

Project có file `.env` local (đã được `.gitignore`) chứa cấu hình chạy ngay và JWT secret riêng. Khi chia sẻ/deploy project, dùng `.env.example` làm mẫu và tạo secret mới tối thiểu 32 ký tự; không commit `.env`.

PowerShell:

```powershell
docker compose up --build
```

macOS/Linux:

```bash
# Cập nhật DOCKER_GID trong .env bằng kết quả lệnh sau trên Linux:
stat -c '%g' /var/run/docker.sock
docker compose up --build
```

Sau khi các healthcheck pass:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`
- PostgreSQL: `localhost:5433`

Các trang frontend:

- `/` — Code Compiler
- `/problems` — Programming Problems
- `/contests` — danh sách Contest; `/contests/{id}` — chi tiết và workspace Contest

Đổi cổng host bằng `FRONTEND_PORT`, `BACKEND_PORT` hoặc `POSTGRES_PORT`. Dừng stack bằng `docker compose down`. `docker compose down -v` còn xóa toàn bộ database và workspace volume, vì vậy chỉ dùng khi chủ động muốn xóa dữ liệu local.

Backend container chạy non-root và chỉ gọi Docker daemon để tạo sandbox Code Judge. Compose mount Docker socket vào backend và dùng named volume `devedu_judge_workspaces`; mỗi sandbox chỉ được mount đúng subdirectory của submission bằng `volume-subpath`. Đây là cấu hình development/local. Docker socket có quyền rất cao; production phải thay bằng daemon hoặc worker chuyên dụng, ưu tiên rootless, không dùng chung Docker host với workload tin cậy. Cơ chế subpath tuân theo [Docker volume documentation](https://docs.docker.com/engine/storage/volumes/), còn thứ tự khởi động dùng healthcheck và `service_healthy` theo [Docker Compose documentation](https://docs.docker.com/compose/how-tos/startup-order/).

Frontend container build static assets rồi phục vụ bằng `vite preview`, phù hợp để chạy stack hiện tại. Khi có hạ tầng production thực tế, lớp reverse proxy/TLS nên được quyết định ở deployment thay vì tự ý thêm vào project này.

## Chạy local

### 1. PostgreSQL

```bash
docker compose up -d postgres
```

PostgreSQL chạy ở `localhost:5433` với giá trị local mặc định:

- Database: `devedu`
- Username: `devedu`
- Password: `devedu`

Có thể đổi cổng host bằng biến `POSTGRES_PORT`; khi chạy backend trực tiếp ngoài Compose, cập nhật `DB_URL` tương ứng. Backend chạy trong Compose vẫn kết nối PostgreSQL qua cổng nội bộ `5432`.

### 2. Backend

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
cd backend
./mvnw spring-boot:run
```

Backend chạy tại `http://localhost:8080`. Kiểm tra foundation API:

```http
GET http://localhost:8080/api/system/status
```

Biến môi trường có thể cấu hình: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `JWT_SECRET`, `JWT_EXPIRATION`, `CORS_ALLOWED_ORIGINS`, `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `JUDGE_DOCKER_COMMAND`, `JUDGE_WORKSPACE_ROOT`, `JUDGE_WORKSPACE_VOLUME`, các biến `JUDGE_*_IMAGE` và `JUDGE_*` limit trong `application.yml`.

## Authentication API

Frontend cung cấp `/login` và `/register` bằng tên, email và password, đồng thời hỗ trợ Google và GitHub OAuth. Provider chỉ được bật khi cả Client ID và Client Secret tương ứng có trong `.env`; xem trạng thái public tại `GET /api/auth/oauth/providers`.

Google và GitHub luôn được gửi `prompt=select_account` khi bắt đầu đăng nhập để hiển thị bước chọn tài khoản, kể cả khi trình duyệt đã có phiên đăng nhập tại nhà cung cấp. Không cần đổi Client ID, Client Secret hoặc callback URL.

Đăng ký OAuth app với các redirect URI local sau:

```text
http://localhost:5173/login/oauth2/code/google
http://localhost:5173/login/oauth2/code/github
```

Sau đó điền `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` trong `.env` và chạy lại `docker compose up -d --build backend frontend`. Không đưa client secret vào frontend hoặc commit credential thật.

### Đăng ký

```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "Nguyễn Văn An",
  "email": "student@example.com",
  "password": "password123"
}
```

Đăng ký công khai và tài khoản OAuth mới luôn tạo user với role `STUDENT`; đăng nhập giữ nguyên role hiện có trong database. Tên được chuẩn hóa khoảng trắng và giới hạn 100 ký tự. Response `201 Created` chứa access token, thời hạn token và thông tin user.

### Đăng nhập

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "student@example.com",
  "password": "password123"
}
```

Gửi token cho endpoint được bảo vệ:

```http
Authorization: Bearer <access-token>
```

Quy tắc quyền:

- `/api/auth/register`, `/api/auth/login`, `/api/system/status`: public.
- `GET /api/problems` và `GET /api/problems/{slug}`: public.
- Submit bài và tham gia Contest: `STUDENT`, `TEACHER`, `ADMIN`; dữ liệu học tập luôn gắn với chính tài khoản thực hiện.
- `/api/teacher/**`: `TEACHER` hoặc `ADMIN`.
- `/api/admin/**`: chỉ `ADMIN`.

### Tài khoản admin và quản lý role

Đặt ba biến sau trong `.env`; backend sẽ tạo tài khoản admin một lần khi khởi động nếu email chưa tồn tại:

```dotenv
ADMIN_NAME=DevEdu Admin
ADMIN_EMAIL=admin@devedu.local
ADMIN_PASSWORD=your-strong-password
```

Ba biến phải được cấu hình cùng nhau. Password phải dài 8-72 UTF-8 byte. Nếu email đã thuộc một tài khoản không phải admin, backend dừng khởi động thay vì tự ý chiếm quyền tài khoản đó.

Đăng nhập bằng tài khoản admin rồi mở `/admin/users`. API tương ứng:

```http
GET /api/admin/users
Authorization: Bearer <admin-token>

PATCH /api/admin/users/{userId}/role
Authorization: Bearer <admin-token>
Content-Type: application/json

{ "role": "TEACHER" }
```

Role hợp lệ gồm `STUDENT`, `TEACHER`, `ADMIN`. Admin không thể tự đổi role của chính mình; người được đổi role cần đăng nhập lại để JWT mới mang quyền vừa được cấp.

UUID chỉ còn là khóa kỹ thuật nội bộ cho JWT và khóa ngoại, không được hiển thị làm ID tài khoản. API trả `id`/`publicId` dạng số; dãy ID học viên/giáo viên tự tăng từ `1` và tài khoản admin bootstrap không chiếm dãy này. Mã được suy ra từ ID: sinh viên dùng `SV000001...`, giáo viên dùng `GV000001...`.
- Endpoint không khớp rule cụ thể: cần JWT hợp lệ.

`JWT_SECRET` cần có ít nhất 32 byte UTF-8 trong môi trường triển khai. Production bắt buộc cấu hình secret ổn định, ngẫu nhiên và không ghi vào source/log. Khi chạy local mà không cấu hình, ứng dụng tạo secret ngẫu nhiên; token local sẽ hết hiệu lực sau mỗi lần restart. Backend standalone mặc định một giờ; Compose local dùng `P7D` trong `.env` để phiên đăng nhập còn hiệu lực qua các lần khởi động máy. Có thể đổi bằng duration ISO-8601 qua `JWT_EXPIRATION`. Frontend kiểm tra claim `exp` trước khi hiển thị user và tự xóa trạng thái đăng nhập đã hết hạn. Response register/login có `Cache-Control: no-store`; JWT xác minh HS256, `typ`, thời điểm phát hành/hết hạn và chữ ký constant-time. Password giới hạn tối đa 72 UTF-8 byte theo BCrypt; login email không tồn tại vẫn chạy một dummy BCrypt check để giảm timing signal cho account enumeration.

Frontend hiện đọc token từ local storage để phục vụ các feature đã có; đây là cơ chế local hiện tại, không phải mô hình session hoàn chỉnh cho production. Không đưa token vào URL hoặc log. Nếu chuyển sang cookie HttpOnly trong tương lai phải thiết kế lại CSRF và contract đăng nhập một cách riêng biệt.

## Database và hiệu năng

Schema PostgreSQL đặt constraint cho role/status, khóa ngoại, uniqueness và các giới hạn nghiệp vụ chính. Các index phục vụ list/filter/order hiện tại được tạo idempotent trong `schema.sql`, gồm catalog, topic/difficulty, Contest và submission history.

Các API list hiện trả toàn bộ dữ liệu vì dataset foundation nhỏ. Khi dữ liệu thực tế tăng, thêm pagination vào contract theo từng module thay vì thêm cache hoặc abstraction chung trước nhu cầu.

`spring.sql.init.mode=always` và `schema.sql` phù hợp cho local/foundation hiện tại. Trước khi chạy nhiều instance production, cần một quy trình migration schema duy nhất và có version; không để nhiều replica đồng thời tự thực hiện DDL. Việc đó chưa được thêm vì project hiện không cho phép tự ý bổ sung migration technology.

## Code Compiler

Editor dùng chung của Compiler và Programming Problems hỗ trợ autocomplete theo ngôn ngữ, gợi ý các biến đã khai báo trước vị trí con trỏ, syntax highlighting riêng cho type/keyword/string/number/function/comment, `Tab`, `Shift+Tab`, auto-indent, auto-pair, `Ctrl+Space`, `Ctrl+Z` và `Ctrl+Y`/`Ctrl+Shift+Z`. Input trên Compiler là tùy chọn; nếu để trống, frontend gửi input mẫu mặc định của ngôn ngữ. Khi submit Programming Problems, input và expected output luôn do test case ẩn của backend cung cấp.

Trang `/` cung cấp giao diện compiler responsive cho C++, Java, Python, Web và MySQL:

- Java: danh sách file ở bên trái editor, thu gọn/mở lại bằng nút **File** trên thanh công cụ; thêm/xóa file `.java` phụ. Điểm vào luôn là `Main.java` (không dùng package). Tối đa 20 file, tổng mã nguồn 100.000 ký tự. API `/api/code/execute` nhận thêm `files` tùy chọn, ví dụ `{"Helper.java":"public class Helper {}"}`; `code` vẫn chứa nội dung `Main.java`. Request cũ không có `files` vẫn hoạt động.
- Web: có sẵn `index.html`, `style.css`, `script.js`. Giữ các thẻ liên kết `href="style.css"`, `src="script.js"` để ghép CSS/JavaScript khi chạy. Mã API vẫn là `HTML`. Preview chạy script trong iframe cách ly, chặn fetch/tài nguyên ngoài, truy cập trang cha và gửi form; không hỗ trợ CDN hoặc thư viện bên ngoài.
- Chuyển file/ngôn ngữ giữ nội dung và undo trong phiên mở trang; chưa lưu project compiler xuống database. Bài tập và submission tiếp tục dùng contract một file hiện có.

Kiểm tra phần ghép file Web bằng Node 22.18+ hoặc Node 24: `node --test frontend/src/features/compiler/webProject.test.mjs`.

- Chọn ngôn ngữ và starter code tương ứng.
- Code editor có số dòng, autocomplete theo ngôn ngữ, `Tab` để nhận gợi ý/thụt lề, `Shift+Tab` để bỏ thụt lề, `Enter` tự giữ indent, tự đóng ngoặc/nháy và `Ctrl+Space` để mở gợi ý.
- Khu vực input và output.
- Nút Run gọi REST contract qua Vite development proxy.

Contract hiện tại:

```http
POST /api/code/execute
Content-Type: application/json

{
  "language": "PYTHON",
  "code": "print('Hello')",
  "input": ""
}
```

API trả `200 OK` với output thật khi chạy thành công:

```json
{
  "language": "PYTHON",
  "status": "SUCCESS",
  "output": "Hello\n"
}
```

Các mã ngôn ngữ hợp lệ: `CPP`, `JAVA`, `PYTHON`, `HTML`, `MYSQL`. Endpoint trả một trong các status `SUCCESS`, `COMPILE_ERROR`, `RUNTIME_ERROR`, `TIME_LIMIT`. Source code chỉ được compile/chạy trong container tạm thời không có network, filesystem gốc read-only, non-root và có giới hạn CPU, RAM, PID, thời gian, output. Runner image phải được pull trước theo hướng dẫn Docker ở trên.

## Programming Problems

Trang Bài tập có hàng topic riêng, sắp xếp, popup tìm kiếm/lọc độ khó/ngôn ngữ/tiến độ và danh sách phân trang cố định 20 bài mỗi trang. Mỗi bài chiếm một hàng gọn, hiển thị acceptance rate dựa trên tỷ lệ lượt chạy thử đạt toàn bộ test case; thống kê được cập nhật nguyên tử và tải theo lô. Bài đã có submission `ACCEPTED` của tài khoản hiện tại được đánh dấu tích xanh ngay sau khi lưu và trạng thái này vẫn còn sau khi tải lại trang. Dữ liệu khởi tạo có 350 bài được phân bổ theo các topic, mỗi bài có starter code riêng cho từng ngôn ngữ được phép và tối thiểu ba test case. Phần **Đề bài** chỉ nêu mục tiêu; định dạng và giới hạn nằm rõ trong **Yêu cầu Input**/**Yêu cầu Output**, tách khỏi **Input mẫu**/**Output mẫu**. Hai khối ví dụ được hiển thị rộng để dễ đối chiếu. Trang chi tiết chỉ cho chọn language hợp lệ của bài; backend cũng từ chối language không được phép trước khi chấm. Workspace ưu tiên language/source/input đã autosave theo từng tài khoản/bài; nếu chưa có bản nháp thì nạp starter code của đúng bài và ngôn ngữ.

Tài khoản `TEACHER` và `ADMIN` có thể đi từ nút **Thêm bài tập** tại `/problems` sang trang riêng `/problems/add`, nhập nội dung, yêu cầu Input/Output, ví dụ Input/Output, ngôn ngữ được phép, starter code tương ứng cho từng ngôn ngữ và từ 3 đến 50 test case ẩn. Trang này hỗ trợ tạo thủ công hoặc nhập tối đa 100 bài từ nội dung/file JSON (dùng các khóa `inputDescription`, `outputDescription`, `sampleInput`, `sampleOutput`); JSON được kiểm tra cấu trúc ở frontend rồi sử dụng tuần tự API tạo bài hiện có, không mở thêm bulk API. Problem và test case của từng request được lưu trong cùng transaction; API chi tiết chỉ trả starter code công khai, không trả test case hoặc expected output ẩn.

Tài khoản `ADMIN` có thêm nút **Sửa** và **Xóa** trên từng dòng bài tập. Trang `/problems/{slug}/edit` tải nội dung cùng test case ẩn qua API quản trị; cập nhật thay thế problem/test case trong một transaction. Xóa là xóa mềm để giữ lịch sử submission và bản nháp, nhưng bài biến mất ngay khỏi catalog và không xuất hiện lại sau khi restart.

Home có thêm feature Programming Problems:

- Xem danh sách và chi tiết bài.
- Lọc theo 8 topic ban đầu: Nhập môn lập trình, C++, Java, Python, OOP, Data Structures, Algorithms và SQL.
- Chọn ngôn ngữ, viết source code và submit.
- Submit được chấm bằng test case ẩn trong Docker sandbox và trả verdict cuối cùng.
- Nút **Chạy test** chạy toàn bộ test case ẩn, hiển thị một ô cho từng case với dấu tích hoặc dấu X và chỉ báo `SUCCESS` khi tất cả đều đúng; thao tác này không tạo submission hay đánh dấu đã giải. Nút **Chạy input** vẫn dùng input tùy chỉnh để xem output riêng.

API:

```http
GET /api/problems
GET /api/problems?topic=ALGORITHMS&difficulty=HARD&language=PYTHON
GET /api/problems/{slug}
POST /api/problems/{slug}/runs
Authorization: Bearer <access-token>

POST /api/teacher/problems
GET /api/admin/problems/{slug}
PUT /api/admin/problems/{slug}
DELETE /api/admin/problems/{slug}
Authorization: Bearer <teacher-or-admin-access-token>

GET /api/student/problem-progress
Authorization: Bearer <access-token>

GET /api/student/problems/{slug}/draft
Authorization: Bearer <access-token>

PUT /api/student/problems/{slug}/draft
Authorization: Bearer <access-token>
Content-Type: application/json

{ "language": "PYTHON", "sourceCode": "print('draft')", "input": "sample input" }

POST /api/problems/{slug}/submissions
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "language": "PYTHON",
  "sourceCode": "print(-1)"
}
```

List, filter và detail là public. Chạy test (`/runs`) yêu cầu JWT của `STUDENT`, `TEACHER` hoặc `ADMIN` và không tạo submission/tiến độ. Submit chấp nhận cả ba role, lưu kết quả theo chính tài khoản thực hiện và trả `200 OK` sau khi chấm:

```json
{
  "id": "submission-uuid",
  "problemId": "problem-uuid",
  "language": "PYTHON",
  "status": "ACCEPTED",
  "diagnostic": "All test cases passed",
  "passedTests": 3,
  "totalTests": 3,
  "executionTimeMillis": 148,
  "submittedAt": "2026-08-22T10:00:00Z"
}
```

Các verdict: `ACCEPTED`, `WRONG_ANSWER`, `COMPILE_ERROR`, `RUNTIME_ERROR`, `TIME_LIMIT`. Nếu Docker hoặc image không sẵn sàng, API trả `503 Service Unavailable` và không lưu kết quả giả.

### Chuẩn bị Code Judge

Code Judge không thêm compiler/interpreter vào process Spring Boot. Hãy pull trước các image đã pin; request dùng `--pull=never`:

```bash
docker pull gcc:14.4
docker pull eclipse-temurin:17-jdk-alpine-3.23
docker pull python:3.13-alpine
docker pull alpine:3.23
docker pull mysql:8.4
```

Mỗi compile/run nằm trong container tạm thời với network bị tắt, filesystem gốc read-only, non-root user, toàn bộ Linux capabilities bị drop, `no-new-privileges`, seccomp mặc định và giới hạn CPU/RAM/PID/thời gian/output. Sandbox mặc định được cấp tối đa một CPU; có thể cấu hình qua `JUDGE_CPUS`. Adapter còn giới hạn số execution đồng thời (mặc định 2, cấu hình qua `JUDGE_MAX_CONCURRENT_EXECUTIONS`). Các test case độc lập chạy qua pool giới hạn này; bài chỉ có một test gộp compile và run trong cùng container để bỏ một lần khởi động container, còn bài nhiều test compile một lần rồi chạy các test song song. Mỗi test vẫn được cô lập và không chia sẻ filesystem ghi được. `executionTimeMillis` đo pha test; ở fast path nó bao gồm cả compile thực hiện trong cùng container. Source được mount read-only; expected output chỉ được so sánh ở backend và không được mount vào sandbox. Các cờ này dựa trên [Docker run reference](https://docs.docker.com/reference/cli/docker/container/run), [seccomp guidance](https://docs.docker.com/engine/security/seccomp/) và [resource constraints](https://docs.docker.com/engine/containers/resource_constraints/).

C++/Java/Python được compile riêng và mỗi test chạy trong container mới. HTML được chấm như static output. Với MySQL, input của test case là setup SQL cho một database tạm thời, còn source sinh viên là query cần chấm.

`CodeJudgeUseCase` và `SandboxExecutionPort` tạo ranh giới độc lập trong modular monolith. Hiện adapter Docker chạy đồng bộ; khi cần scale có thể thay adapter bằng worker/queue mà không đổi API/domain. Production nên cấp một Docker daemon/worker chuyên dụng, ưu tiên rootless; quyền truy cập Docker daemon không nên dùng chung với workload tin cậy.

Frontend lấy access token từ key `devedu.accessToken` (fallback `accessToken`) trong local storage khi chạy test hoặc submit. Chạy test, lưu bản nháp, submit và tiến độ cá nhân dùng được với cả `STUDENT`, `TEACHER` và `ADMIN`.

## Lớp học đã được gỡ bỏ

Module Course/Lesson đã được gỡ khỏi frontend, backend và cấu hình Docker. URL `/courses` hiển thị trang không tìm thấy; các API lớp học, thành viên, tài liệu và lesson không còn được cung cấp.

Bản cài mới không tạo bảng lớp học. Với database đã có, các bảng lớp học và volume `devedu_course_materials` cũ được giữ nguyên, không còn được ứng dụng sử dụng. Không chạy `docker compose down -v` để cập nhật vì có thể mất dữ liệu PostgreSQL. Mã nguồn đã gỡ có thể khôi phục từ Git nếu cần.

## Contest

Module Contest sử dụng User, Problem, editor và Docker Judge hiện có; dữ liệu lưu thật trong PostgreSQL, không có Contest/leaderboard minh họa. Teacher/Admin vào **Contests → Create Contest**, chọn các bài từ catalog, sắp thứ tự A–Z, đặt điểm, thời gian bắt đầu và thời lượng. Student/Teacher/Admin đều có thể đăng ký và nộp bài.

- Enter Contest mở bài đầu tiên trong workspace `/contests/{id}/problems/{problemId}`; sidebar chuyển bài nhanh và hiển thị Solved/Attempted/Not attempted. Code được giữ trong cache phiên theo bài/ngôn ngữ khi chuyển tab/bài, bên cạnh autosave vào draft Problem hiện có.
- Timer cố định khi cuộn trang; hết giờ khóa Submit ở UI và API. Không chuyển khỏi editor khi Accepted. Kết quả Judge hiển thị riêng, cập nhật điểm và trạng thái bài ngay sau nộp.
- `/contests/{id}/leaderboard` hiển thị từng bài, điểm, lượt sai trước Accepted đầu tiên và Your Rank. Dữ liệu cập nhật mỗi 5 giây khi tab đang mở; transport polling tách khỏi UI, có cleanup và không gửi poll chồng nhau.
- `/contests/{id}/submissions` có filter All/Accepted/Wrong Answer/Other, điểm từng lượt, runtime, thời gian. `/rules` lấy scoring/penalty/attempts/resubmission/ranking từ backend. Quy tắc hiện tại **không phạt lượt sai**; Time là thời gian giải bài cuối cùng, không phải penalty ICPC.
- Bộ tính điểm riêng tại `application/contest/scoring/ContestScoring`. Không thay Judge: chưa có số liệu bộ nhớ hay verdict Memory Limit riêng; UI thông báo chưa hỗ trợ thay vì hiển thị số giả.

- `/contests`: Upcoming/Ongoing/Finished, thời gian, số bài, số người tham gia và CTA theo trạng thái.
- `/contests/{id}`: Problems, Leaderboard, My Submissions, Rules. Nút Solve mở lại `ProblemWorkspace` ngay trong trang chi tiết; không tạo editor mới. URL có `?tab=problems&problem={problemId}` có thể tải lại/đi Back.
- Countdown đồng bộ với `serverTime`, cập nhật mỗi giây và tự khóa Submit khi hết giờ. API vẫn kiểm tra hạn nộp độc lập, không tin đồng hồ hay trạng thái trên browser.
- Một lượt nộp được nhận hợp lệ trong `[startsAt, endsAt)` vẫn được tính nếu Judge trả kết quả sau hạn. Đăng ký được phép trước và trong Contest; gọi lại không tạo bản ghi trùng.
- Mỗi bài Accepted cộng điểm một lần. Xếp hạng theo điểm giảm dần, thời điểm giải bài cuối cùng tăng dần (lấy Accepted đầu tiên mỗi bài), sau cùng theo public ID. My Submissions chỉ trả các lượt nộp của tài khoản hiện tại.
- Chỉ Submit từ Contest được tính điểm Contest. Nộp bài qua `/problems`, chạy thử và tiến độ cũ không tính; lượt nộp Contest vẫn cập nhật tiến độ Problem cá nhân hiện có.
- Contest hiện **Unrated**, chưa có công thức rating, sửa/xóa Contest hoặc chống gian lận. Các bài vẫn thuộc catalog công khai; thay đổi bài gốc sẽ được phản ánh trong Contest. Không dùng mô hình này để cam kết đề thi bí mật.

API:

```http
GET  /api/contests?status=UPCOMING|ONGOING|FINISHED
GET  /api/contests/{id}
POST /api/teacher/contests
POST /api/contests/{id}/registration
POST /api/contests/{id}/problems/{problemId}/submissions
```

Tạo Contest nhận `{name,type,startsAt,durationMinutes,rules,problems:[{problemId,points}]}`; `type` là `WEEKLY`, `PRACTICE` hoặc `CUSTOM`. Giới hạn 1–26 bài duy nhất, 1–10000 điểm/bài, 1–10080 phút và thời gian bắt đầu trong tương lai. Submission dùng `{language,sourceCode}` như Problem hiện có. Bảng mới: `contests`, `contest_problems`, `contest_registrations`, `contest_submissions`; không thay schema hoặc contract submission cũ.

Kiểm thử:

```powershell
cd backend
.\mvnw.cmd clean verify
cd ..
npm.cmd run build --prefix frontend
node --test frontend/src/features/contest/contestTime.test.mjs frontend/src/features/contest/contestPresentation.test.mjs frontend/src/features/contest/api/contestUpdates.test.mjs frontend/src/features/compiler/webProject.test.mjs
# Sau khi build/start Compose và chuẩn bị judge images:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-contest.ps1
```

Smoke test tạo schema PostgreSQL và backend container tạm riêng, kiểm tra tạo/đăng ký/giới hạn thời gian/nộp bằng Judge thật/điểm/lịch sử riêng tư, rồi chỉ xóa schema và container kiểm thử vừa tạo. Không sửa dữ liệu ứng dụng đang sử dụng.

### Contest mẫu (chạy thủ công khi cần)

`scripts/add-sample-contests.sql` thêm 3 Contest Unrated, tham chiếu bài tập đang có và admin hiện có: Warm-up bắt đầu sau 24 giờ (3 bài/120 phút), Algorithm Sprint bắt đầu trước thời điểm chạy script 15 phút (4 bài/180 phút), Practice Archive đã kết thúc (4 bài/120 phút). Không tạo user, lượt nộp hoặc thứ hạng giả.

Chạy từ thư mục gốc với Compose đang hoạt động:

```powershell
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Get-Content scripts/add-sample-contests.sql -Raw -Encoding utf8 | docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Script có transaction và kiểm tra bài tập trước khi thêm. Chạy lại không tạo trùng, không đổi thời gian hay dữ liệu các Contest đã có. Trạng thái thay đổi tự nhiên theo thời gian; restart Docker không đặt lại đồng hồ. Script không nằm trong luồng khởi động ứng dụng.

## Module đã gỡ bỏ

Exam (Kỳ thi) đã được gỡ khỏi frontend, backend và schema khởi tạo mới. Các bảng/dữ liệu Exam cũ được giữ nguyên, không tự động xóa. Contest là module độc lập và vẫn hoạt động.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend chạy tại `http://localhost:5173`.

## Kiểm tra build

### Lưu bản nháp bài tập

Editor bài tập ghi code, ngôn ngữ và input vào IndexedDB ngay khi chỉnh sửa; chỉ sau **đủ 5 giây không chỉnh sửa**, frontend mới gọi API draft Spring Boot để lưu PostgreSQL. Mỗi lần gõ đặt lại hạn gửi; thời gian được kiểm tra lần nữa sau khi chờ IndexedDB/hàng đợi request. Online/focus, chạy test và rời workspace không được bỏ qua hạn này. Rời trang sớm chỉ giữ bản cục bộ để khôi phục lần sau. Bấm Lưu/Nộp bài là thao tác submission chủ động, độc lập với autosave và vẫn ghi dữ liệu theo luồng nộp hiện tại.

- Bản chưa đồng bộ trên máy được ưu tiên khi mở lại bài; bản đã đồng bộ không che bản mới hơn trên server. Khi kết nối trở lại hoặc quay lại cửa sổ, editor thử gửi lại bản đang chờ.
- Dữ liệu tách theo tài khoản, bài và phiên Contest/Virtual. Khách và Virtual chỉ lưu cục bộ; không tự chuyển draft của khách sang tài khoản sau đăng nhập. Trình biên dịch nhiều file ở trang chính chưa có API draft, không nằm trong thay đổi này.
- Không lưu token vào IndexedDB. Nếu IndexedDB bị chặn/hết dung lượng hoặc API lỗi, editor hiện cảnh báo thay vì báo lưu thành công.
- Đây là cơ chế phục hồi bản nháp, không phải toàn bộ website offline. Đóng tab đột ngột không bảo đảm request mạng hoàn tất; bản cục bộ được dùng khi mở lại. Xóa dữ liệu trình duyệt sẽ xóa bản chưa đồng bộ. Nhiều tab/thiết bị chưa có merge xung đột: PostgreSQL vẫn dùng bản ghi cuối cùng được server nhận theo API hiện có.

Test autosave: `node --test frontend/src/features/programming-problems/draftPersistence.test.mjs`.

### Lệnh build

```powershell
cd backend
.\mvnw.cmd clean verify
```

```bash
cd frontend
npm ci
npm run build
```

Kiểm tra dependency frontend:

```bash
cd frontend
npm audit
```

## Phạm vi hiện tại

Project hiện cung cấp foundation, JWT authentication, password hashing, ba role `STUDENT`, `TEACHER`, `ADMIN`, trang admin quản lý role, Compiler chạy code qua Docker sandbox, Programming Problems có Docker Code Judge, Contest có đăng ký/nộp bài/xếp hạng và endpoint trạng thái hệ thống. Chưa có upload/storage video, rating Contest, chống gian lận hay AI.
