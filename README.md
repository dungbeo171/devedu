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
- `/courses` — Lớp học dành cho giáo viên/admin
- `/exams` — Exam
- `/interview` — Interview

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
- `GET /api/problems`, `GET /api/problems/{slug}`, `GET /api/courses`, `GET /api/courses/{slug}` và `GET /api/lessons/{id}`: public.
- Submit bài, tiến độ lesson, Exam sinh viên và Interview: chỉ `STUDENT`.
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

Schema PostgreSQL đặt constraint cho role/status, khóa ngoại, uniqueness và các giới hạn nghiệp vụ chính. Các index phục vụ list/filter/order hiện tại được tạo idempotent trong `schema.sql`, gồm catalog, topic/difficulty, course hierarchy, exam/attempt/answer và submission history.

Adapter Exam tải options theo batch khi đọc danh sách câu hỏi để tránh N+1 query; danh sách exam do giáo viên quản lý được lọc ngay tại repository. Các API list hiện trả toàn bộ dữ liệu vì dataset foundation nhỏ. Khi dữ liệu thực tế tăng, thêm pagination vào contract theo từng module thay vì thêm cache hoặc abstraction chung trước nhu cầu.

`spring.sql.init.mode=always` và `schema.sql` phù hợp cho local/foundation hiện tại. Trước khi chạy nhiều instance production, cần một quy trình migration schema duy nhất và có version; không để nhiều replica đồng thời tự thực hiện DDL. Việc đó chưa được thêm vì project hiện không cho phép tự ý bổ sung migration technology.

## Code Compiler

Editor dùng chung của Compiler và Programming Problems hỗ trợ autocomplete theo ngôn ngữ, gợi ý các biến đã khai báo trước vị trí con trỏ, syntax highlighting riêng cho type/keyword/string/number/function/comment, `Tab`, `Shift+Tab`, auto-indent, auto-pair, `Ctrl+Space`, `Ctrl+Z` và `Ctrl+Y`/`Ctrl+Shift+Z`. Input trên Compiler là tùy chọn; nếu để trống, frontend gửi input mẫu mặc định của ngôn ngữ. Khi submit Programming Problems, input và expected output luôn do test case ẩn của backend cung cấp.

Trang `/` cung cấp giao diện compiler responsive cho C++, Java, Python, HTML và MySQL:

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

Trang Bài tập có hàng topic chỉ hiển thị tên, bộ lọc độ khó `Dễ/Trung bình/Khó`, bộ lọc ngôn ngữ, tiến độ và danh sách phân trang cố định 10 bài mỗi trang ngay bên dưới. Mỗi bài chiếm một hàng đầy đủ, hiển thị difficulty cùng các ngôn ngữ được phép; bài đã có submission `ACCEPTED` của sinh viên hiện tại được đánh dấu tích xanh ngay sau khi lưu và trạng thái này vẫn còn sau khi tải lại trang. Dữ liệu khởi tạo có 40 bài (5 bài mỗi topic), mỗi bài có starter code riêng cho từng ngôn ngữ được phép. Trang chi tiết chỉ cho chọn language hợp lệ của bài; backend cũng từ chối language không được phép trước khi chấm. Workspace ưu tiên language/source/input đã autosave theo từng tài khoản/bài; nếu chưa có bản nháp thì nạp starter code của đúng bài và ngôn ngữ.

Tài khoản `TEACHER` và `ADMIN` có thể đi từ nút **Thêm bài tập** tại `/problems` sang trang riêng `/problems/add`, nhập nội dung, ngôn ngữ được phép, starter code tương ứng cho từng ngôn ngữ và một hoặc nhiều test case ẩn. Problem và test case được lưu trong cùng transaction; API chi tiết chỉ trả starter code công khai, không trả test case hoặc expected output ẩn.

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
Authorization: Bearer <student-access-token>

POST /api/teacher/problems
GET /api/admin/problems/{slug}
PUT /api/admin/problems/{slug}
DELETE /api/admin/problems/{slug}
Authorization: Bearer <teacher-or-admin-access-token>

GET /api/student/problem-progress
Authorization: Bearer <student-access-token>

GET /api/student/problems/{slug}/draft
Authorization: Bearer <student-access-token>

PUT /api/student/problems/{slug}/draft
Authorization: Bearer <student-access-token>
Content-Type: application/json

{ "language": "PYTHON", "sourceCode": "print('draft')", "input": "sample input" }

POST /api/problems/{slug}/submissions
Authorization: Bearer <student-access-token>
Content-Type: application/json

{
  "language": "PYTHON",
  "sourceCode": "print(-1)"
}
```

List, filter và detail là public. Submit chỉ chấp nhận JWT role `STUDENT` và trả `200 OK` sau khi chấm:

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

Mỗi compile/run nằm trong container tạm thời với network bị tắt, filesystem gốc read-only, non-root user, toàn bộ Linux capabilities bị drop, `no-new-privileges`, seccomp mặc định và giới hạn CPU/RAM/PID/thời gian/output. Adapter còn giới hạn số execution đồng thời (mặc định 2, cấu hình qua `JUDGE_MAX_CONCURRENT_EXECUTIONS`). Source được mount read-only; expected output chỉ được so sánh ở backend và không được mount vào sandbox. Các cờ này dựa trên [Docker run reference](https://docs.docker.com/reference/cli/docker/container/run), [seccomp guidance](https://docs.docker.com/engine/security/seccomp/) và [resource constraints](https://docs.docker.com/engine/containers/resource_constraints/).

C++/Java/Python được compile riêng và mỗi test chạy trong container mới. HTML được chấm như static output. Với MySQL, input của test case là setup SQL cho một database tạm thời, còn source sinh viên là query cần chấm.

`CodeJudgeUseCase` và `SandboxExecutionPort` tạo ranh giới độc lập trong modular monolith. Hiện adapter Docker chạy đồng bộ; khi cần scale có thể thay adapter bằng worker/queue mà không đổi API/domain. Production nên cấp một Docker daemon/worker chuyên dụng, ưu tiên rootless; quyền truy cập Docker daemon không nên dùng chung với workload tin cậy.

Frontend lấy access token từ key `devedu.accessToken` (fallback `accessToken`) trong local storage khi submit. Nếu chưa có token hoặc role không phải `STUDENT`, UI hiển thị thông báo tương ứng.

## Course/Lesson

Trang `/courses` là màn hình “Lớp học” duy nhất. Giáo viên/admin quản lý sinh viên và gán/gỡ các bài lập trình hiện có. Sinh viên chỉ thấy lớp mình đã được thêm vào, làm bài qua workspace Programming Problems và xem tiến trình `ACCEPTED` theo công thức `số bài đã giải / tổng bài được giao`; ví dụ 1/5 hiển thị 20%. `Teacher Studio` cũ đã được loại bỏ.

Thao tác sửa sinh viên trong lớp chỉ đổi tên hiển thị của enrollment; thông tin tài khoản toàn cục và email của sinh viên không bị thay đổi.

API đọc nội dung (public):

```http
GET /api/courses
GET /api/courses/{slug}
GET /api/lessons/{lessonId}
```

API giáo viên (`TEACHER` hoặc `ADMIN`):

```http
POST /api/teacher/courses
GET  /api/teacher/courses
POST /api/teacher/courses/{courseId}/topics
POST /api/teacher/topics/{topicId}/lessons
PUT  /api/teacher/lessons/{lessonId}/video
GET  /api/teacher/courses/{courseId}/students
POST /api/teacher/courses/{courseId}/students
GET  /api/teacher/courses/{courseId}/student-candidates?q={query}
POST /api/teacher/courses/{courseId}/students/bulk
DELETE /api/teacher/courses/{courseId}/students
GET  /api/teacher/courses/{courseId}/problems
POST /api/teacher/courses/{courseId}/problems
DELETE /api/teacher/courses/{courseId}/problems/{problemId}
POST /api/teacher/courses/{courseId}/students/import
POST /api/teacher/courses/{courseId}/materials
Authorization: Bearer <teacher-access-token>
```

Import sinh viên dùng file `.txt` UTF-8, mỗi dòng một mã `SV...` (cũng chấp nhận dấu cách, dấu phẩy hoặc dấu chấm phẩy), tối đa 1000 mã và 1 MB. Upload tài liệu dùng `multipart/form-data`, chỉ nhận `.pdf`, `.doc`, `.docx`, `.ppt`, `.pptx`, tối đa 20 MB.

`TEACHER` chỉ chỉnh sửa khóa học do mình tạo; `ADMIN` có thể quản lý mọi khóa học. Sinh viên đánh dấu hoàn thành bằng endpoint idempotent:

```http
POST /api/student/lessons/{lessonId}/complete
GET  /api/courses/{courseId}/materials
GET  /api/course-materials/{materialId}/content
Authorization: Bearer <student-access-token>
```

API lớp học dành cho sinh viên:

```http
GET /api/student/courses
GET /api/student/courses/{courseId}
Authorization: Bearer <student-access-token>
```

Hai endpoint tài liệu chỉ cho sinh viên đã được thêm vào lớp; giáo viên sở hữu và admin cũng có quyền truy cập.

Frontend đọc token từ `devedu.accessToken` (fallback `accessToken`) cho các thao tác được bảo vệ.

## Exam

Module Exam hỗ trợ lịch thi, thời lượng theo phút, câu hỏi Multiple Choice/Coding, một lượt thi cho mỗi sinh viên và lưu từng câu trả lời.

API giáo viên:

```http
GET  /api/teacher/exams
POST /api/teacher/exams
POST /api/teacher/exams/{examId}/questions
GET  /api/teacher/exams/{examId}/results
Authorization: Bearer <teacher-access-token>
```

API sinh viên:

```http
GET  /api/exams
GET  /api/exams/{slug}
POST /api/exams/{slug}/attempts
GET  /api/exams/attempts/{attemptId}
PUT  /api/exams/attempts/{attemptId}/answers/{questionId}
POST /api/exams/attempts/{attemptId}/submit
GET  /api/exams/attempts/{attemptId}/result
Authorization: Bearer <student-access-token>
```

Multiple Choice được chấm tự động khi nộp bài. Câu Coding chỉ lưu source code và được báo `pendingCodingQuestions`; project chưa có code judge hoặc workflow chấm tay. Đáp án đúng không được trả trong API sinh viên trước hoặc sau khi thi.

## Interview

Trang Interview dành cho sinh viên, hỗ trợ lọc đồng thời theo topic và difficulty rồi mở đáp án/giải thích của từng câu hỏi.

```http
GET /api/interview/questions
GET /api/interview/questions?topic=JAVA&difficulty=MEDIUM
GET /api/interview/questions/{questionId}
Authorization: Bearer <student-access-token>
```

Các topic: `JAVA`, `PYTHON`, `CPP`, `OOP`, `SQL`, `DATABASE`, `DATA_STRUCTURES`, `ALGORITHMS`, `WEB`. Difficulty: `EASY`, `MEDIUM`, `HARD`. Database có sẵn một câu hỏi seed cho mỗi topic.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend chạy tại `http://localhost:5173`.

## Kiểm tra build

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

Project hiện cung cấp foundation, JWT authentication, password hashing, ba role `STUDENT`, `TEACHER`, `ADMIN`, trang admin quản lý role, Compiler chạy code qua Docker sandbox, Programming Problems có Docker Code Judge, Course/Lesson, Exam, Interview và endpoint trạng thái hệ thống. Câu Coding trong Exam chưa nối với judge; chưa có upload/storage video, chống gian lận hay AI.
