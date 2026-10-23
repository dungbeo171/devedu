# DevEdu

Nền tảng website học lập trình được tổ chức dưới dạng modular monolith, gồm các module học tập và Code Judge cho Programming Problems.

Giao diện module Contest hiển thị bằng tiếng Việt với tên **Cuộc thi**. Độ khó
hiển thị **Dễ / Trung bình / Khó**, vẫn giữ enum API hiện có. Form tạo và danh sách
không còn ô Type; cuộc thi mới gửi `type=CUSTOM`, không đổi dữ liệu các cuộc thi cũ.

Form tạo có tên, mô tả, thời gian bắt đầu/kết thúc và thời gian làm (phút).
Cho phép bắt đầu ở quá khứ; kết thúc phải sau bắt đầu. **Chế độ giám sát** bật
camera trong lượt thi chính thức; không tự đổi lựa chọn của cuộc thi cũ.

Đăng ký chưa tính giờ. Bấm **Bắt đầu làm bài** gọi `POST /api/contests/{id}/attempt`,
lưu mốc bắt đầu riêng một lần ở PostgreSQL. Hạn nộp = thời điểm bắt đầu làm + thời gian
làm, nhưng không vượt giờ kết thúc cuộc thi. Ví dụ mở 8–12 giờ, thời gian làm 60 phút:
bắt đầu 9 giờ hết hạn 10 giờ; bắt đầu 11:30 hết hạn 12 giờ. Refresh/đổi thiết bị/bấm lại
không gia hạn. Backend chặn nộp trước khi bắt đầu hoặc sau hạn riêng, kể cả cuộc thi
vẫn mở; bảng điểm dùng thời gian thực làm của từng người. API detail trả `myAttempt`
của chính user, không trả mốc riêng của người khác. Kết quả lịch sử giữ mốc tính giờ cũ.
Nút “Hồ sơ thi đấu của tôi” đã bỏ khỏi danh sách; trang lịch sử vẫn giữ nguyên.

## Gỡ thể lệ và rating Contest

Đã bỏ ô/tab Thể lệ, chọn rating và trang lịch sử rating. Điểm từng bài, bảng xếp hạng,
kết quả, lịch sử thi đấu, lượt thi thử và camera vẫn giữ nguyên.
Schema nâng cấp xóa riêng `rating_history`, `user_contest_ratings`, hai cột
`contests.rated`/`contests.rules` và các trường tương ứng trong snapshot kết quả.
Không dùng DROP CASCADE, không xóa User/Problem/Submission/ContestResult.
**Sao lưu database trước khi chạy bản mới**; dữ liệu rating/thể lệ chỉ khôi phục được
bằng bản sao lưu. Bản sao lưu local nằm trong `.backups/` (được Git bỏ qua).
Không tái tạo hệ thống rating hoặc thể lệ khi chưa có yêu cầu mới.

## Camera Contest — MVP

Trong Contest có giám sát, user đã đăng ký bấm **Đồng ý và cho phép camera** trước
khi UI cho bắt đầu làm bài, cấp quyền trong trình duyệt rồi nhìn thẳng khoảng 4 giây
để hiệu chỉnh. Camera duy trì trong lượt thi; **Hiện/Ẩn hình camera** chỉ đổi hình
xem trước, không dừng stream. Không có nút tắt camera trên trang. Refresh trong cùng
tab nhớ consent theo user/contest và mở lại camera; hết hạn riêng hoặc rời trang sẽ
dừng camera/worker. Có thể thu hồi quyền ở trình duyệt; khi lỗi có nút kết nối lại,
thời gian thi vẫn chạy. Khi camera mất kết nối, workspace bị khóa đến khi kết nối lại. Nếu không thấy
khuôn mặt liên tục 2 giây, trang bị phủ mờ và khóa tối thiểu 10 giây; nếu khuôn mặt chưa trở lại thì
tiếp tục khóa. Đây là khóa UI best-effort, không tự trừ điểm và không thay đổi admission phía server.
Preview camera vẽ face mesh xanh ngay trên thiết bị; landmark không được gửi lên backend.
Sinh viên thấy số lần quay ngang của chính mình qua `/camera/me`, không thể đọc thống kê của người khác.
Giáo viên tạo Contest và admin mở **Thống kê giám sát** để xem số lần nhìn lệch/mất mặt
của từng người tham gia (phân trang 20, tự cập nhật mỗi 10 giây). Khi camera và worker còn
sẵn sàng, trình duyệt gửi heartbeat mỗi 10 giây; báo cáo nhận diện kết nối hiện tại bằng tín
hiệu trong 25 giây gần nhất, heartbeat không được tính là vi phạm.
Admin, giáo viên tạo kỳ thi và giáo viên được phân công thấy nút **Giám sát** thay cho **Vào thi**
ở kỳ thi bật chế độ giám sát. Báo cáo có các tab **Quay ngang**, **Rời khung hình**, **Chuyển tab**,
**Sao chép**, **Dán nội dung** và **Chụp màn hình**; nút **Xem thời gian** tải timeline do server ghi nhận.

- Xử lý bằng [MediaPipe Face Landmarker](https://developers.google.com/edge/mediapipe/solutions/vision/face_landmarker/web_js)
  trong Web Worker, tối đa khoảng 5 FPS. Không phải mô hình MP-HEADGAZE và chưa nhận diện liếc mắt.
- Ngưỡng thử nghiệm: yaw lệch >30° hoặc pitch >25° so với tư thế đã hiệu chỉnh sẽ
  ghi ngay một `LOOKING_AWAY`. Giữ đầu ở vùng lệch chỉ tính một lần; một frame trở lại
  rõ vùng nhìn thẳng sẽ mở khóa để lần quay tiếp theo được tính, không đếm từng frame.
- Không thấy mặt >=2 giây là `NO_FACE` riêng; nhiều mặt, tab ẩn, camera muted hoặc
  thiếu dữ liệu không được suy thành nhìn lệch. Cần kiểm chứng ngưỡng với camera,
  vị trí màn hình, ánh sáng, kính và nhu cầu tiếp cận thực tế trước khi triển khai rộng.
- Trong lượt thi, document chuyển sang hidden ghi `TAB_HIDDEN`; thao tác copy bên trong panel
  đề bài ghi `PROBLEM_COPIED`; thao tác dán trong trang ghi `CONTENT_PASTED`; phím PrintScreen mà trang nhận được ghi `SCREENSHOT_ATTEMPT`.
  Trình duyệt không thể phát hiện chắc chắn Snipping Tool, phím tắt hệ điều hành hoặc camera
  ngoài, nên đây chỉ là tín hiệu best-effort. DevEdu không chụp hay tải ảnh màn hình lên server.
- Video/ảnh/landmark/góc đầu không được upload, ghi hình hay lưu. Model 3.8 MB và
  WASM phục vụ cùng origin (không tải từ CDN lúc dùng); chỉ tải runtime khi bật camera.
  Browser cần HTTPS hoặc localhost và hỗ trợ camera, Web Worker, ImageBitmap/OffscreenCanvas.
- Java nhận `{eventId, kind}`, lấy user từ JWT, gán giờ server, kiểm tra registration
  và Contest có giám sát, live, lượt thi cá nhân còn hạn; PostgreSQL lưu sự kiện,
  dedupe theo `(contest,user,eventId)`. Chuẩn bị camera trước khi bắt đầu không ghi sự kiện.
  Giới hạn 500ms giữa sự kiện mới và 20000 sự kiện/user/contest. Không thay đổi Judge,
  Submission, scoring hoặc Virtual Contest.
- Sự kiện chỉ hiển thị trong 30 ngày gần nhất và được dọn mỗi giờ. Chỉ giáo viên tạo
  Contest/admin xem báo cáo; giáo viên khác và sinh viên không xem được. API no-store.
- Đây là tín hiệu client tự báo, có thể giả mạo/bỏ sót; **không phải bằng chứng gian lận**.
  Không có dữ liệu không chứng minh camera đang bật hay người dùng luôn nhìn thẳng.
  Queue retry giới hạn 30 sự kiện trong RAM, không giữ qua refresh/rời trang; lỗi đồng bộ
  được báo trên UI. Không suy ra tổng thời gian nhìn lệch từ số sự kiện.

API bổ sung (đều cần JWT):

```text
GET  /api/contests/{id}/camera/me
POST /api/contests/{id}/camera/events
GET  /api/teacher/contests/{id}/camera?page=1
GET  /api/teacher/contests/{id}/camera/events?userId=1&kind=LOOKING_AWAY&page=1
```

Sau khi mở Docker Desktop (Linux engine), áp dụng code/schema mới:

```powershell
docker compose up -d --build backend frontend
```

Kiểm tra thủ công camera trên Chrome/Edge: đồng ý/từ chối quyền, hiệu chỉnh, quay
đầu ngắn, giữ lệch lâu, quay lại nhìn thẳng rồi lệch tiếp, che camera, chuyển tab,
ẩn/hiện hình xem trước, đổi bài, refresh, đăng xuất và hết hạn cá nhân. Ẩn hình/đổi bài
không tắt camera; đèn camera phải tắt khi rời Contest/hết giờ. Thu hồi quyền/rút camera
phải hiện lỗi kết nối lại; count không nhân đôi khi retry; sinh viên/giáo viên khác
không xem được báo cáo. Cuộc thi không giám sát và Virtual không mở camera.
Kiểm thử tự động chỉ xác nhận state machine, retry và ranh giới API; không thay thế
việc đo độ chính xác với camera thật.

## Công nghệ

- Backend: Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA, Spring Security, Spring Mail (SMTP xác nhận email)
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
- `/documents` — thư viện tài liệu và đề xuất học tập; `/documents/{slug}` — trang đọc từng tài liệu
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

Biến môi trường có thể cấu hình: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `JWT_SECRET`, `JWT_EXPIRATION`, `CORS_ALLOWED_ORIGINS`, `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `GEMINI_TIMEOUT`, `JUDGE_DOCKER_COMMAND`, `JUDGE_WORKSPACE_ROOT`, `JUDGE_WORKSPACE_VOLUME`, các biến `JUDGE_*_IMAGE` và `JUDGE_*` limit trong `application.yml`.

## Thống kê quản trị

Đăng nhập bằng tài khoản `ADMIN` và mở `/admin/statistics`. Dashboard đọc số liệu thật từ PostgreSQL về người dùng, bài tập, lượt chạy/nộp, kỳ thi, thi thử và tín hiệu giám sát.

```http
GET /api/admin/analytics
Authorization: Bearer <admin-token>
```

API chỉ dành cho `ADMIN`, trả `Cache-Control: no-store` và không trả email, UUID nội bộ, source code, test case hay dữ liệu camera thô. Lượt sử dụng Compiler công khai hiện không được thống kê vì hệ thống không persistence hoạt động đó.

## Authentication API

Frontend cung cấp `/login` và `/register` bằng tên, email và password, đồng thời hỗ trợ Google và GitHub OAuth. Đăng ký bằng email dùng mã xác nhận 6 chữ số; Google/GitHub dùng email đã được provider xác minh. Provider chỉ được bật khi cả Client ID và Client Secret tương ứng có trong `.env`; xem trạng thái public tại `GET /api/auth/oauth/providers`.

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

Request trên gửi mã xác nhận và trả `202 Accepted` cùng thời gian hết hạn/gửi lại. Tài khoản chưa được tạo ở bước này. Mã được sinh bằng bộ tạo số ngẫu nhiên an toàn, chỉ lưu BCrypt hash, có hiệu lực mặc định 10 phút, được gửi lại sau 60 giây và bị hủy sau 5 lần nhập sai.

```http
POST /api/auth/register/verify
Content-Type: application/json

{
  "email": "student@example.com",
  "code": "123456"
}
```

Email xác nhận được gửi dưới dạng HTML responsive có plain-text fallback. Ở trang **Quản lý người dùng**, ADMIN có thể tạm bật/tắt gửi mã; cấu hình được lưu trong PostgreSQL và không làm lộ hoặc thay đổi SMTP credential. Khi công tắc tắt, đăng ký email trả `503`, không tạo tài khoản hoặc pending registration; đăng nhập Google/GitHub không bị ảnh hưởng.

```text
GET /api/admin/settings/email-verification
PUT /api/admin/settings/email-verification
Authorization: Bearer <admin-jwt>

{ "enabled": true }
```

Response gồm `enabled` và `smtpConfigured`. Muốn gửi được email thì cả công tắc quản trị và `MAIL_ENABLED` trong môi trường đều phải bật.

Khi mã đúng, response `201 Created` mới chứa access token, thời hạn token và thông tin user. Đăng ký công khai và tài khoản OAuth mới luôn tạo user với role `STUDENT`; đăng nhập giữ nguyên role hiện có trong database. Tên được chuẩn hóa khoảng trắng và giới hạn 100 ký tự.

### Cấu hình gửi email

Điền SMTP trong `.env`. Ví dụ Gmail cần bật xác minh 2 bước và dùng **App Password**, không dùng mật khẩu đăng nhập Gmail:

```dotenv
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-account@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-account@gmail.com
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLED=true
```

Sau khi đổi cấu hình, chạy `docker compose up -d --build backend frontend`. Nếu `MAIL_ENABLED=false` hoặc SMTP lỗi, API trả `503` và không tạo tài khoản. Không commit `MAIL_PASSWORD`.

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

- `/api/auth/register`, `/api/auth/register/verify`, `/api/auth/login`, `/api/system/status`: public.
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

Editor dùng chung của Compiler và Programming Problems hỗ trợ autocomplete theo ngôn ngữ, gợi ý các biến đã khai báo trước vị trí con trỏ, syntax highlighting riêng cho type/keyword/string/number/function/comment, `Tab`, `Shift+Tab`, auto-indent, auto-pair, `Ctrl+Space`, `Ctrl+Z` và `Ctrl+Y`/`Ctrl+Shift+Z`. Với tài khoản đã đăng nhập, đặt con trỏ tại vị trí cần hoàn thành rồi bấm **AI gợi ý** để gọi `POST /api/code/suggestions`; AI không tự gọi khi dừng gõ. Chữ mờ xuất hiện tại con trỏ, `Tab` để nhận và `Esc` để bỏ. Trong workspace bài tập/Contest, Gemini nhận thêm đề bài, yêu cầu input/output và ví dụ mẫu để tạo gợi ý sát bài hơn. Gemini chỉ được gọi qua backend bằng `GEMINI_API_KEY`; mã nguồn và gợi ý không được lưu vào database. Input trên Compiler là tùy chọn; nếu để trống, frontend gửi input mẫu mặc định của ngôn ngữ. Khi submit Programming Problems, input và expected output luôn do test case ẩn của backend cung cấp.

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

Trang Bài tập có hàng topic riêng, popup tìm kiếm/lọc độ khó/ngôn ngữ/tiến độ và danh sách phân trang cố định 20 bài mỗi trang. `GET /api/problems` vẫn trả cùng contract; khi có JWT, backend trộn thứ tự content-based trực tiếp vào danh sách thay vì tạo khu vực “Dành cho bạn”. Xếp hạng dùng metadata submission, lượt chạy test, topic/độ khó/ngôn ngữ đã lọc và ngôn ngữ chạy ở Trình biên dịch. Bảng `problem_recommendation_signals` chỉ giữ trọng số tổng hợp, không sao chép source code, input hoặc lịch sử event chi tiết. Mỗi bài chiếm một hàng gọn, hiển thị acceptance rate dựa trên tỷ lệ lượt chạy thử đạt toàn bộ test case; thống kê được cập nhật nguyên tử và tải theo lô. Bài đã có submission `ACCEPTED` của tài khoản hiện tại được đánh dấu tích xanh ngay sau khi lưu và trạng thái này vẫn còn sau khi tải lại trang. Dữ liệu khởi tạo có 350 bài được phân bổ theo các topic, mỗi bài có starter code riêng cho từng ngôn ngữ được phép và tối thiểu ba test case. Phần **Đề bài** chỉ nêu mục tiêu; định dạng và giới hạn nằm rõ trong **Yêu cầu Input**/**Yêu cầu Output**, tách khỏi **Input mẫu**/**Output mẫu**. Hai khối ví dụ được hiển thị rộng để dễ đối chiếu. Trang chi tiết chỉ cho chọn language hợp lệ của bài; backend cũng từ chối language không được phép trước khi chấm. Workspace ưu tiên language/source/input đã autosave theo từng tài khoản/bài; nếu chưa có bản nháp thì nạp starter code của đúng bài và ngôn ngữ.

Tài khoản `TEACHER` và `ADMIN` có thể đi từ nút **Thêm bài tập** tại `/problems` sang trang riêng `/problems/add`, nhập nội dung, yêu cầu Input/Output, ví dụ Input/Output, ngôn ngữ được phép, starter code tương ứng cho từng ngôn ngữ và từ 3 đến 50 test case ẩn. Trang này hỗ trợ tạo thủ công, tạo bản nháp bằng Gemini hoặc nhập tối đa 100 bài từ nội dung/file JSON. Với AI, người dùng mô tả bài bằng tiếng Việt; backend gọi Gemini bằng structured output, kiểm tra draft có 3–10 test case rồi trả về form để người dùng sửa và chủ động lưu. Gemini không tự ghi Problem vào database. JSON dùng các khóa `inputDescription`, `outputDescription`, `sampleInput`, `sampleOutput`; JSON được kiểm tra cấu trúc ở frontend rồi sử dụng tuần tự API tạo bài hiện có, không mở thêm bulk API. Problem và test case của từng request được lưu trong cùng transaction; API chi tiết chỉ trả starter code công khai, không trả test case hoặc expected output ẩn.

Để bật tạo bài bằng AI, tạo API key Gemini và điền vào `.env` (không commit key), sau đó rebuild backend:

```dotenv
GEMINI_API_KEY=your-gemini-api-key
GEMINI_MODEL=gemini-2.5-flash
GEMINI_TIMEOUT=PT45S
```

```bash
docker compose up -d --build backend frontend
```

Nếu thiếu key, endpoint trả `503 Service Unavailable` với thông báo cấu hình rõ ràng.

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
POST /api/teacher/problems/ai-draft
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

## Tài liệu và đề xuất học tập

Trang `/documents` cung cấp 10 tài liệu nền tảng về Java, Python, C++, OOP, cấu trúc dữ liệu, thuật toán, SQL, Web, phương pháp học và debug. Mỗi tài liệu có URL riêng `/documents/{slug}` và được lấy từ PostgreSQL, không hard-code trong frontend.

API public gồm `GET /api/documents`, `GET /api/documents/{slug}` và `GET /api/documents/recommendations`. Khi có JWT, endpoint gợi ý xếp hạng nội dung dựa trên ngôn ngữ/chủ đề trong submission của chính người dùng và lịch sử đọc; khách nhận danh sách biên tập mặc định. `POST /api/documents/{slug}/views` ghi nhận lượt đọc của tài khoản hiện tại. Hệ thống không gửi source code, hành vi hay dữ liệu cá nhân sang dịch vụ AI bên ngoài và không cần thêm API key.

Trang đọc có mục lục tự động ở bên trái từ heading `##`, `###`, `####`; trên mobile mục lục chuyển thành khối thu gọn. Admin luôn có nút **Chỉnh sửa tài liệu**. Admin cấp hoặc thu hồi quyền **Sửa tài liệu** cho từng tài khoản Giáo viên trong `/admin/users`; Giáo viên chưa được cấp quyền không thể cập nhật qua API dù tự mở URL chỉnh sửa.

API quản trị tài liệu:

- `GET /api/teacher/documents/access` — quyền sửa của tài khoản hiện tại.
- `PUT /api/teacher/documents/{slug}` — cập nhật tiêu đề, mô tả, nội dung, tag, thời gian đọc và độ ưu tiên.
- `GET /api/admin/documents/editors` — danh sách Giáo viên và trạng thái quyền.
- `PUT /api/admin/documents/editors/{publicUserId}` — cấp hoặc thu hồi quyền bằng public user ID.

## Lớp học đã được gỡ bỏ

Module Course/Lesson đã được gỡ khỏi frontend, backend và cấu hình Docker. URL `/courses` hiển thị trang không tìm thấy; các API lớp học, thành viên, tài liệu và lesson không còn được cung cấp. Các bảng legacy Course/Lesson được schema xóa theo thứ tự khóa ngoại, không dùng `CASCADE`. Volume tài liệu cũ không bị xóa trong thao tác này.

## Contest

Module Contest sử dụng User, Problem, editor và Docker Judge hiện có; dữ liệu lưu thật trong PostgreSQL, không có Contest/leaderboard minh họa. Teacher/Admin tạo thông tin cơ bản tại `/contests/create`; hệ thống lưu bản nháp rồi chuyển tới `/contests/{id}/edit`. Teacher chỉ quản lý Contest do mình tạo, còn Admin quản lý toàn bộ Contest. Khi kỳ thi kết thúc, hệ thống tự lưu `visibility=PRIVATE`. Màn hình chỉnh sửa có ba tab **Chi tiết**, **Bài tập**, **Cài đặt** để chọn bài từ catalog, sắp thứ tự A–Z, đặt tổng đúng 10 điểm, chọn công khai/riêng tư bằng dropdown, quyền tham gia và giáo viên giám sát. Student/Teacher/Admin đều có thể đăng ký và nộp bài khi đáp ứng quyền tham gia.

Trong tab **Bài tập**, giáo viên/admin cũng có thể tạo bài chỉ dành cho kỳ thi bằng form hoặc dùng Gemini tạo bản nháp rồi kiểm tra trước khi lưu. Các bài này nằm trong bảng Contest riêng, không xuất hiện ở kho `/problems`, nhưng vẫn chạy bằng editor và Docker Judge hiện tại. Có thể xóa mềm kỳ thi từ menu quản lý hoặc trang chỉnh sửa; submission và kết quả lịch sử vẫn được giữ trong PostgreSQL.

- Contest mới luôn là bản nháp riêng tư và chưa xuất hiện trong catalog. Chỉ người tạo hoặc admin được sửa; chọn **Công khai** rồi lưu tab Cài đặt sẽ publish nguyên tử, không có nút công khai riêng. Người tạo hoặc admin có thể chỉnh sửa cấu hình ở cả trạng thái sắp diễn ra, đang diễn ra và đã kết thúc.
- Nút **Quản lý kỳ thi** trên `/contests` mở `/admin`, hiển thị các kỳ thi do tài khoản hiện tại tạo, kể cả bản nháp và kỳ thi riêng tư. Menu dấu ba chấm cạnh từng kỳ thi mở thao tác **Chỉnh sửa** và **Giám sát**. Bản nháp đã được lưu trong PostgreSQL nên có thể thoát khỏi `/edit` và quay lại chỉnh sửa.
- Danh sách kỳ thi dùng layout một hàng toàn chiều ngang cho mỗi kỳ thi, hiển thị riêng ngày tạo, thời gian bắt đầu/kết thúc, thời lượng, số bài và số người tham gia; các nhóm thông tin có màu riêng và tự xếp dọc trên màn hình nhỏ.
- Chế độ tham gia **Tự do** cho mọi tài khoản đăng nhập đăng ký. Chế độ **Giới hạn** kiểm tra email của tài khoản ở backend trước khi tạo registration.
- Giáo viên giám sát được chọn thấy kỳ thi trong mục **Kỳ thi được phân công giám sát** tại `/admin`, có thể mở báo cáo camera nhưng không thể chỉnh sửa Contest. Báo cáo luôn hiện số lượt nhìn lệch/mất mặt; người mới đăng ký nhưng chưa bắt đầu được ghi rõ **Chưa bắt đầu làm bài**.

- Enter Contest mở bài đầu tiên trong workspace `/contests/{id}/problems/{problemId}`; sidebar chuyển bài nhanh và hiển thị Solved/Attempted/Not attempted. Code được giữ trong cache phiên theo bài/ngôn ngữ khi chuyển tab/bài, bên cạnh autosave vào draft Problem hiện có.
- Timer cố định khi cuộn trang; hết giờ khóa Submit ở UI và API. Không chuyển khỏi editor khi Accepted. Kết quả Judge hiển thị riêng, cập nhật điểm và trạng thái bài ngay sau nộp.
- `/contests/{id}/leaderboard` hiển thị từng bài, điểm, lượt sai trước Accepted đầu tiên và Your Rank. Dữ liệu cập nhật mỗi 5 giây khi tab đang mở; transport polling tách khỏi UI, có cleanup và không gửi poll chồng nhau.
- `/contests/{id}/submissions` có filter All/Accepted/Wrong Answer/Other, điểm từng lượt, runtime, thời gian. `/rules` lấy scoring/penalty/attempts/resubmission/ranking từ backend. Quy tắc hiện tại **không phạt lượt sai**; Time là thời gian giải bài cuối cùng, không phải penalty ICPC.
- Bộ tính điểm riêng tại `application/contest/scoring/ContestScoring`. Không thay Judge: chưa có số liệu bộ nhớ hay verdict Memory Limit riêng; UI thông báo chưa hỗ trợ thay vì hiển thị số giả.

- `/contests`: dropdown trạng thái mặc định **Tất cả**, các kỳ thi đang diễn ra được xếp trước, tiếp theo là sắp diễn ra và đã kết thúc; không có bộ lọc độ khó hoặc sắp xếp thủ công. Mỗi kỳ thi là một hàng ngang gọn gồm icon, tên/badge, ngày tạo, lịch thi, thời lượng, số bài, số người tham gia, trạng thái và CTA; mobile chuyển sang lưới hai cột. Kỳ thi giới hạn hiển thị badge **Giới hạn email** từ `participationMode` của API.
- `/contests/{id}`: Problems, Leaderboard, My Submissions, Rules. Nút Solve mở lại `ProblemWorkspace` ngay trong trang chi tiết; không tạo editor mới. URL có `?tab=problems&problem={problemId}` có thể tải lại/đi Back.
- Countdown đồng bộ với `serverTime`, cập nhật mỗi giây và tự khóa Submit khi hết giờ. API vẫn kiểm tra hạn nộp độc lập, không tin đồng hồ hay trạng thái trên browser.
- Một lượt nộp được nhận hợp lệ trong `[startsAt, endsAt)` vẫn được tính nếu Judge trả kết quả sau hạn. Đăng ký được phép trước và trong Contest; gọi lại không tạo bản ghi trùng.
- Mỗi bài Accepted cộng điểm một lần. Xếp hạng theo điểm giảm dần, thời điểm giải bài cuối cùng tăng dần (lấy Accepted đầu tiên mỗi bài), sau cùng theo public ID. My Submissions chỉ trả các lượt nộp của tài khoản hiện tại.
- Chỉ Submit từ Contest được tính điểm Contest. Nộp bài qua `/problems`, chạy thử và tiến độ cũ không tính; lượt nộp Contest vẫn cập nhật tiến độ Problem cá nhân hiện có.
- Contest đã gỡ rating và thể lệ tùy chỉnh; vẫn tính điểm và thứ hạng trong từng cuộc thi. Chưa hỗ trợ sửa/xóa Contest. Các bài vẫn thuộc catalog công khai; thay đổi bài gốc sẽ được phản ánh trong Contest. Không dùng mô hình này để cam kết đề thi bí mật.

API:

```http
GET  /api/contests?status=UPCOMING|ONGOING|FINISHED
GET  /api/contests/{id}
POST /api/teacher/contests
POST /api/contests/{id}/registration
POST /api/contests/{id}/problems/{problemId}/submissions
```

Tạo Contest nhận thông tin thời gian/mô tả/giám sát và có thể chưa chọn bài ở bước đầu. Tab Bài tập cho tối đa 10 bài duy nhất; bài mới mặc định 1 điểm, nút tăng/giảm thay đổi đúng 1 điểm và tổng danh sách có bài phải bằng 10. Thời gian làm giới hạn 1–10080 phút; thời gian bắt đầu có thể ở quá khứ nhưng thời gian kết thúc phải sau thời gian bắt đầu và còn hiệu lực lúc công khai. Submission dùng `{language,sourceCode}` như Problem hiện có. Các bảng Contest không thay schema hoặc contract submission của Problem/Judge.

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

Exam (Kỳ thi) đã được gỡ khỏi frontend, backend và schema. Các bảng legacy Exam được xóa theo thứ tự khóa ngoại, không dùng `CASCADE`. Contest là module độc lập và vẫn hoạt động. Trước lần nâng cấp này, database đã được sao lưu trong `.backups/` (thư mục bị Git bỏ qua).

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

Project hiện cung cấp foundation, JWT authentication, password hashing, ba role `STUDENT`, `TEACHER`, `ADMIN`, trang admin quản lý role, Compiler chạy code qua Docker sandbox, Programming Problems có Docker Code Judge, Contest có đăng ký/nộp bài/xếp hạng và endpoint trạng thái hệ thống. Chưa có upload/storage video, cơ chế tự kết luận gian lận.
