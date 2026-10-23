# AGENTS.md

## Mục tiêu dự án

DevEdu là nền tảng website học lập trình dạng modular monolith, có authentication và các module học tập. Không coi các contract còn được ghi rõ là stub hoặc các màn hình minh họa là nghiệp vụ hoàn chỉnh.

## Công nghệ được phép

- Backend: Java 17, Spring Boot, Spring Web, Spring Data JPA, Spring Security; Spring Mail chỉ dùng cho SMTP xác nhận email
- Database: PostgreSQL
- Frontend: React, TypeScript, Vite, Tailwind CSS
- API: REST
- Local infrastructure: Docker Compose cho frontend, backend, PostgreSQL và Docker sandbox của Code Judge
- Build: Maven Wrapper và npm

Không tự ý thêm framework, thư viện, database, message broker, cache, công cụ build hoặc nền tảng cloud. Chỉ thêm dependency khi task yêu cầu trực tiếp và có lý do rõ ràng.

## Kiến trúc backend

Backend là một **Modular Monolith**, không phải Microservices. Tất cả module cùng nằm trong một ứng dụng Spring Boot, cùng quy trình build và cùng deployment.

Bốn lớp:

- `domain`: model và quy tắc nghiệp vụ thuần Java; không phụ thuộc Spring, JPA hay web.
- `application`: use case, input/output port và orchestration; chỉ phụ thuộc `domain`.
- `infrastructure`: cấu hình Spring, adapter persistence/JPA và tích hợp kỹ thuật; triển khai output port của `application`.
- `presentation`: REST controller và DTO/API mapping; gọi application input port, không truy cập repository trực tiếp.

Hướng phụ thuộc bắt buộc:

```text
presentation -> application -> domain
infrastructure -> application/domain
```

Không để `domain` phụ thuộc lớp ngoài. Không đặt business logic trong controller, configuration hoặc JPA entity mapping.

Khi thêm một module nghiệp vụ, giữ ranh giới module rõ ràng và áp dụng bốn lớp trên trong module đó. Module chỉ giao tiếp qua public contract cần thiết; không truy cập implementation nội bộ của module khác.

### Authentication

- `User` và `UserRole` là domain model thuần Java.
- `User.name` là tên hiển thị bắt buộc, tối đa 100 ký tự; tài khoản OAuth lấy tên provider và dữ liệu cũ dùng phần local của email làm giá trị migration.
- UUID của user chỉ là khóa nội bộ cho JWT/foreign key và không được trả làm `id` công khai. API user trả `id`/`publicId` dạng số; dãy ID học viên/giáo viên tăng từ 1 và không bị tài khoản bootstrap admin chiếm số. Mã vai trò được suy ra từ ID này: `STUDENT` có `studentCode` dạng `SV000001`, `TEACHER` có `teacherCode` dạng `GV000001`. Không hiển thị UUID trên frontend.
- Register/login được truy cập qua application input port; repository, password hasher và token provider là output port.
- JPA adapter, BCrypt và JWT implementation nằm trong `infrastructure`.
- REST request/response DTO và exception mapping nằm trong `presentation`.
- Đăng ký công khai chỉ tạo `STUDENT`; không cho client tự chọn role.
- Tài khoản mới từ email hoặc OAuth luôn là `STUDENT`; login không thay đổi role đã lưu.
- Admin bootstrap lấy `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` từ môi trường và chỉ tạo tài khoản khi email chưa tồn tại; không commit credential.
- `AdminUserManagementUseCase` hỗ trợ liệt kê user và cấp role `STUDENT`, `TEACHER`, `ADMIN`; chỉ `ADMIN` được gọi và admin không được tự đổi role của chính mình.
- JWT dùng HMAC-SHA256, stateless, secret từ `JWT_SECRET`; không commit secret cố định.
- JWT phải xác minh chữ ký constant-time, `alg`, `typ`, `iat`, `exp` và các claim định danh; response register/login phải có `Cache-Control: no-store`.
- Đăng nhập Google và GitHub dùng Spring Security OAuth2 Client. Client ID/Secret chỉ lấy từ biến môi trường; provider chưa cấu hình phải được báo disabled qua API, không dùng credential giả.
- Mỗi lần bắt đầu OAuth Google/GitHub, authorization request phải có `prompt=select_account` để nhà cung cấp hiển thị chọn tài khoản. Dùng customizer của resolver Spring; không tự ghép URL hoặc thay đổi `state`, nonce, scope và callback.
- OAuth callback chỉ chấp nhận email đã được provider xác thực theo contract của từng provider. GitHub phải lấy email verified qua API `user:email`; Google yêu cầu claim `email_verified`.
- OAuth login tìm tài khoản theo email chuẩn hóa, tái sử dụng tài khoản email hiện có hoặc tạo `STUDENT` mới với password hash ngẫu nhiên không dùng để đăng nhập. Sau OAuth, backend phát JWT DevEdu và xóa session handshake; API tiếp tục xác thực stateless bằng Bearer JWT.
- OAuth token chuyển về `/auth/callback` qua URL fragment, frontend phải lưu token rồi xóa fragment ngay bằng `history.replaceState`. Không đưa Client Secret hoặc access token của provider xuống frontend.
- Password dùng BCrypt và tối đa 72 UTF-8 byte ở cả register/login. Login email không tồn tại vẫn thực hiện dummy BCrypt check để giảm timing signal cho account enumeration.
- Đăng ký email là luồng hai bước: `POST /api/auth/register` gửi mã 6 chữ số qua SMTP và lưu pending registration riêng; chỉ `POST /api/auth/register/verify` mới tạo `User` và phát JWT. Không lưu mã rõ, không trả mã qua API/log, mã mặc định hết hạn sau 10 phút, giới hạn 5 lần nhập sai và gửi lại sau 60 giây. SMTP credential chỉ lấy từ biến môi trường; khi gửi thất bại không được tạo user. Email có cả plain-text fallback và HTML cùng-origin, không tải asset ngoài và phải escape dữ liệu người nhận. OAuth không dùng bước này vì chỉ chấp nhận email đã được provider xác minh.
- Trạng thái gửi email xác nhận do `ADMIN` bật/tắt tại `/api/admin/settings/email-verification` và được lưu trong `system_settings`; đây là công tắc nghiệp vụ, không thay thế `MAIL_ENABLED` hay SMTP credential trong môi trường. Khi tắt, đăng ký email tạm dừng với lỗi rõ ràng, không bỏ qua bước xác minh và không tạo pending registration; OAuth vẫn hoạt động. UI nhập mã dùng sáu ô số, hỗ trợ gõ, dán, Backspace và phím mũi tên.
- Namespace `/api/teacher/**` cho `TEACHER`/`ADMIN`, `/api/admin/**` chỉ cho `ADMIN`.
- Chỉ permit public đúng endpoint hiện có. Không dùng wildcard nhiều cấp cho catalog public vì endpoint con được thêm sau có thể chứa dữ liệu riêng tư.

### Code execution contract

- `CodeLanguage` là domain enum gồm `CPP`, `JAVA`, `PYTHON`, `HTML`, `MYSQL`.
- `ExecuteCodeUseCase` là input port; `CodeExecutionService` gọi `CodeExecutionPort` để thực thi qua Docker sandbox.
- REST contract `POST /api/code/execute` là public và trả `SUCCESS`, `COMPILE_ERROR`, `RUNTIME_ERROR` hoặc `TIME_LIMIT` cùng output thực tế.
- Compiler và Code Judge dùng chung adapter sandbox nhưng giữ input port/application service độc lập; Compiler chạy một lần với input tùy chọn, còn Programming Problems chấm theo test case ẩn.
- Compiler Java hỗ trợ `files` tùy chọn trong `/api/code/execute`: map tên file phụ sang nội dung; `code` vẫn là `Main.java`. Tối đa 20 file và tổng 100.000 ký tự, tên file class Java phẳng, không đường dẫn/package, không trùng tên (kể cả hoa/thường). Compile toàn bộ `.java` trong Docker rồi chạy `Main`; giữ tương thích request một file.
- Compiler hiển thị `HTML` thành “Web” nhưng giữ mã API `HTML`. Frontend có `index.html`, `style.css`, `script.js`; HTML vẫn qua CodeExecutionPort, sau đó frontend ghép tài nguyên local vào preview. JavaScript chỉ chạy trong iframe `allow-scripts` không `allow-same-origin`, CSP chặn fetch/tài nguyên ngoài/frame/form/base; không chạy trong trang cha hoặc JVM. Không thay đổi contract draft/submission của Problems thành nhiều file.

### Code Judge

- `CodeJudgeUseCase` là input port độc lập; `SandboxExecutionPort` là output port tách application khỏi cách thực thi. `DockerSandboxExecutionAdapter` nằm trong `infrastructure/judge`.
- Code người dùng không được compile, load hoặc chạy trong JVM/Spring Boot. Chỉ adapter judge được phép khởi tạo Docker CLI bằng danh sách argument cố định; không ghép source code, input hay test case vào host shell command.
- Mỗi lần compile/run dùng container tạm thời, không network, root filesystem read-only, non-root user, drop toàn bộ capability, `no-new-privileges`, seccomp mặc định và giới hạn CPU, RAM, PID, thời gian, output. Tổng số execution đồng thời cũng phải bị giới hạn. Không dùng `--privileged`, host network hoặc mount Docker socket vào sandbox.
- Các test case độc lập có thể chạy song song bằng pool giới hạn toàn cục theo `max-concurrent-executions`; mỗi test vẫn dùng container riêng và không chia sẻ filesystem ghi được. Với bài chỉ có một test, bước compile và run được gộp trong cùng container để tránh một lần khởi tạo container; bài có nhiều test vẫn compile một lần rồi chạy test song song. `executionTimeMillis` của kết quả judge đo pha chạy test; với fast path này thời gian bao gồm cả compile trong cùng container.
- Source chỉ được mount read-only. Expected output không bao giờ được đưa vào sandbox. Image phải được chuẩn bị trước; request không được tự pull image (`--pull=never`).
- C++/Java/Python compile riêng rồi chạy từng test case trong container mới. HTML được so sánh như static output. MySQL dùng database tạm thời trong container riêng cho từng test case; input test là setup SQL.
- Adapter hiện chạy đồng bộ trong modular monolith. Giữ contract application độc lập để sau này có thể thay bằng worker/queue mà không đổi domain hoặc presentation; không tự ý tách thành Microservice.
- Docker daemon là quyền nhạy cảm. Production phải dùng daemon/worker chuyên dụng (ưu tiên rootless), không cho Spring Boot truy cập Docker host dùng chung với workload tin cậy.
- Khi backend chạy trực tiếp trên host, workspace judge dùng bind mount dưới `JUDGE_WORKSPACE_ROOT`. Khi chạy bằng Compose, backend và sandbox dùng named volume `devedu_judge_workspaces`; sandbox chỉ mount subdirectory của submission qua `volume-subpath`, không mount toàn bộ volume.

### Containerization

- `compose.yaml` là stack local duy nhất gồm `postgres`, `backend` và `frontend`; đây vẫn là một Modular Monolith, không phải ba Microservices.
- Backend và frontend dùng Dockerfile riêng với build stage. Backend runtime chạy non-root; frontend build static assets rồi chạy Vite preview cho môi trường local/container hiện tại.
- Compose phải chờ PostgreSQL và backend healthy trước khi khởi động dependency kế tiếp. Frontend proxy `/api` tới backend trong Docker network; browser không gọi hostname nội bộ `backend` trực tiếp.
- `JWT_SECRET` chỉ được forward từ môi trường host, không đặt secret cố định trong Compose hoặc Dockerfile.
- Chỉ backend judge adapter được mount Docker socket. Không mount socket vào frontend, PostgreSQL hoặc sandbox; không thêm `--privileged`, host network hay host filesystem mount cho sandbox.
- Docker socket trong Compose chỉ phục vụ development/local. Production phải tách daemon/worker chuyên dụng và kiểm soát quyền theo nguyên tắc Code Judge ở trên.

### Programming Problems

- Domain gồm `ProgrammingProblem`, `ProblemTopic`, `ProblemDifficulty`, `ProblemTestCase`, `ProblemSubmission` và `ProblemDraft`. Mỗi problem khai báo tập `CodeLanguage` được phép.
- `ProgrammingProblem` lưu riêng `inputDescription`/`outputDescription` (yêu cầu định dạng) với `sampleInput`/`sampleOutput` (ví dụ minh họa); không dùng dữ liệu mẫu thay cho yêu cầu bài.
- Mỗi problem lưu `starterCodes` theo từng `CodeLanguage` được phép. API chi tiết trả starter code công khai; mỗi ngôn ngữ được phép phải có template không rỗng và form `/problems/add` cho phép giáo viên/admin chỉnh từng template.
- Application input port hỗ trợ list, filter đồng thời theo topic/difficulty/language, lấy detail, tạo bài, submit và đọc danh sách bài đã giải của người dùng; persistence chỉ đi qua output port.
- JPA entities/adapters nằm trong `infrastructure/persistence/problem`; REST DTO/controller nằm trong `presentation`.
- Danh sách và chi tiết bài tập là public. Submit yêu cầu JWT hợp lệ và cho phép `STUDENT`, `TEACHER`, `ADMIN`; tiến độ được lưu riêng theo chính tài khoản thực hiện.
- `POST /api/teacher/problems` cho phép `TEACHER` và `ADMIN` tạo bài cùng ít nhất ba test case ẩn. Problem và test case phải được lưu atomically; `STUDENT` không được gọi endpoint này.
- `POST /api/teacher/problems/ai-draft` cho phép `TEACHER`/`ADMIN` gửi mô tả 20–4000 ký tự để Gemini tạo bản nháp có cấu trúc. Frontend không gọi Gemini trực tiếp; `GEMINI_API_KEY` chỉ tồn tại ở backend, model/timeout cấu hình bằng môi trường. Application phải kiểm tra topic, language, starter code và 3–10 test case trước khi trả draft. Draft AI chỉ điền vào form `/problems/add`, không tự lưu Problem; người dùng phải xem, sửa và chủ động bấm lưu qua API tạo bài hiện có.
- `/api/admin/problems/**` chỉ cho `ADMIN`: đọc dữ liệu quản trị gồm test case ẩn, sửa toàn bộ problem/test case atomically và xóa mềm problem. Xóa mềm phải loại bài khỏi catalog nhưng giữ submission/draft lịch sử và không để dữ liệu seed tái tạo bài sau khi restart. Test case ẩn không được trả qua API public.
- Chi tiết bài tập có `sampleInput`/`sampleOutput` công khai cho chạy thử; đây là dữ liệu mẫu riêng, không lấy hoặc làm lộ test case ẩn.
- `POST /api/problems/{slug}/runs` dành cho user đã xác thực với role `STUDENT`, `TEACHER` hoặc `ADMIN`, chạy toàn bộ test case ẩn nhưng không tạo submission hoặc tiến độ. Response chỉ trả vị trí, trạng thái đạt/trượt của từng case và kết quả tổng; không trả input hay expected output ẩn. Kết quả tổng chỉ là `ACCEPTED` khi mọi test case đều đạt.
- Mỗi lượt chạy test được cộng vào `problem_run_statistics` bằng cập nhật nguyên tử; `successful_runs` chỉ tăng khi kết quả tổng là `ACCEPTED`. API danh sách trả `acceptanceRate = successful_runs / total_runs * 100`, truy vấn thống kê theo lô và không dùng submission để tính tỷ lệ này.
- `GET /api/problems` giữ nguyên response contract nhưng dùng JWT tùy chọn: khách nhận thứ tự catalog, tài khoản đăng nhập nhận toàn bộ kết quả lọc theo thứ tự content-based ngay trong danh sách hiện tại. Xếp hạng dùng metadata submission, lượt chạy test của Problem, bộ lọc topic/difficulty/language đã chọn và ngôn ngữ chạy tại Compiler; bài đã giải bị loại khỏi ưu tiên, bài đang làm dở được ưu tiên, user mới nhận fallback bài dễ/nhập môn. `problem_recommendation_signals` chỉ lưu trọng số tổng hợp theo `(user, signal_type, signal_value)` và thời gian gần nhất, không lưu từng event, source code hay input. Không gọi AI/provider ngoài và frontend không hiển thị nhãn/lý do cá nhân hóa.
- Submit lấy test case qua output port và gọi `CodeJudgeUseCase`; kết quả cuối cùng là `ACCEPTED`, `WRONG_ANSWER`, `COMPILE_ERROR`, `RUNTIME_ERROR` hoặc `TIME_LIMIT`.
- Application phải kiểm tra language thuộc tập ngôn ngữ của problem trước khi lưu draft hoặc gọi Code Judge; frontend chỉ hiển thị các language được phép trong workspace.
- Mọi submission hợp lệ được lưu. `GET /api/student/problem-progress` cho phép cả ba role và trả các problem ID đã có ít nhất một submission `ACCEPTED` của chính tài khoản đó.
- Bản nháp code được lưu riêng theo cặp `user/problem`, gồm language, source code và input. `GET/PUT /api/student/problems/{slug}/draft` cho phép cả ba role nhưng chỉ đọc/ghi bản nháp của chính tài khoản đang đăng nhập. Frontend Problems ghi IndexedDB mỗi lần sửa code/input; chỉ gửi API draft sau đủ 5000ms không chỉnh sửa, kiểm tra lại hạn bằng đồng hồ đơn điệu ngay trước request kể cả sau khi chờ hàng đợi. Không flush server trên Run, focus, pagehide hoặc cleanup/unmount; rời trang sớm giữ bản dirty cục bộ. Submit chủ động là luồng ghi submission riêng, không trì hoãn nghiệp vụ nộp bài. IndexedDB tách tài khoản/khách, namespace Contest/Virtual, bài và ngôn ngữ; bản dirty cục bộ ưu tiên khôi phục, bản sạch ưu tiên dữ liệu server. Request trong cùng tab được tuần tự hóa và acknowledgement chỉ áp dụng đúng revision; online/focus retry phải tôn trọng hạn không gõ. Không gửi bản nháp tài khoản cũ bằng token tài khoản mới. Khách và Virtual chỉ lưu cục bộ theo phạm vi hiện tại; Compiler nhiều file chưa có API draft riêng.
- `NOT_JUDGED` chỉ được giữ để tương thích dữ liệu cũ. Không lưu submission mới nếu hạ tầng judge không sẵn sàng.
- Test case và expected output chỉ tồn tại ở backend; không trả qua API public.
- Frontend code của module nằm trong `src/features/programming-problems`.
- Bảng xếp hạng bài tập dùng lần `ACCEPTED` đầu tiên trong toàn bộ lịch sử của mỗi cặp user/problem; không cộng lại điểm khi resubmit. Kỳ tuần bắt đầu thứ Hai, tháng/năm theo lịch tại múi giờ `Asia/Ho_Chi_Minh`. Điểm cố định: `EASY` 100, `MEDIUM` 200, `HARD` 300. API public `GET /api/leaderboards/problems?period=WEEK|MONTH|YEAR`; frontend nằm trong `src/features/leaderboard` tại `/leaderboard`.
- Trang `/problems` hiển thị các topic dạng bộ lọc tên ngắn ở trên, bộ lọc difficulty/language/tiến độ và danh sách bài một hàng ở dưới, phân trang cố định 20 bài mỗi trang; chọn bài mới mở workspace. Workspace ưu tiên bản nháp của sinh viên, nếu chưa có thì dùng starter code riêng của bài/ngôn ngữ. Tài khoản `TEACHER`/`ADMIN` mở trang riêng `/problems/add` để thêm bài tập, starter code và test case ẩn. Chỉ `ADMIN` thấy thao tác sửa/xóa; sửa dùng `/problems/{slug}/edit`, xóa cập nhật danh sách ngay không reload.
- Slug bài tập dùng tiếng Việt không dấu, phân tách bằng dấu gạch ngang để URL ngắn và dễ đọc.
- Topic hiển thị thành hàng lọc riêng phía trên thanh search/sort/filter. Danh sách hiển thị mỗi bài trên một hàng gọn cùng acceptance rate từ lượt chạy thử; bài đã giải có dấu tích lấy từ tiến độ `ACCEPTED` đã lưu và đồng bộ lại ngay sau Submit. Chạy thử `SUCCESS` hiển thị trạng thái và dấu tích màu xanh nhưng không được tính là đã giải.
- Danh sách không có nút sắp xếp A–Z/Z–A. Bộ lọc và phân trang lưu vào query URL `/problems` để khôi phục khi quay lại từ workspace hoặc tải lại trang; đổi bộ lọc mới đặt trang về 1. Menu lọc tự đóng sau khi dữ liệu của lựa chọn mới tải thành công, giữ mở khi lỗi; tìm kiếm giữ mở khi gõ và đóng khi nhấn Enter.

### Learning Documents

- Module tài liệu nằm tại `domain/document`, `application/document`, `infrastructure/persistence/document`, `presentation/rest`; frontend nằm trong `src/features/documents`.
- Catalog và chi tiết tài liệu public qua `GET /api/documents` và `GET /api/documents/{slug}`. Mỗi tài liệu có route riêng `/documents/{slug}`; không cần thư viện Markdown, frontend chỉ render định dạng nội dung giới hạn do hệ thống quản lý.
- `GET /api/documents/recommendations` dùng JWT tùy chọn. Khi có tài khoản, backend xếp hạng nội dung từ ngôn ngữ/chủ đề trong submission của chính user và lịch sử tài liệu đã xem; khách nhận thứ tự biên tập mặc định. Không gửi hành vi, source code hay dữ liệu cá nhân tới AI/provider bên ngoài.
- `POST /api/documents/{slug}/views` yêu cầu một trong ba role và chỉ tăng bộ đếm theo cặp user/document. Recommendation là content-based trong modular monolith, không thêm vector database, tracking bên thứ ba, model AI, queue hoặc microservice.
- Trang đọc tài liệu dựng mục lục từ các heading `##`/`###`/`####`, mục lục sticky trên desktop và thu gọn trên mobile. Nội dung hỗ trợ đoạn văn, danh sách, chữ đậm và inline code bằng renderer giới hạn; không render HTML tùy ý hoặc dùng `dangerouslySetInnerHTML`.
- `ADMIN` luôn được sửa tài liệu. `TEACHER` chỉ được gọi `PUT /api/teacher/documents/{slug}` khi có quyền trong `learning_document_editor_permissions`; role đơn thuần không đủ. `GET /api/teacher/documents/access` trả quyền của chính tài khoản. Chỉ `ADMIN` được xem/cập nhật danh sách quyền qua `/api/admin/documents/editors`; API dùng public user ID và không trả UUID.
- Cập nhật tài liệu giữ nguyên ID/slug/createdAt, kiểm tra giới hạn nội dung/tag/thời gian đọc/priority và không bị dữ liệu seed ghi đè sau restart. Frontend chỉnh sửa tại `/documents/{slug}/edit`; Admin cấp/thu hồi quyền ngay trong bảng quản lý người dùng.

### Module đã gỡ bỏ

- Module Exam, route `/exams`, API và các bảng `exams`, `exam_questions`, `exam_question_options`, `exam_attempts`, `exam_answers` đã gỡ bỏ. Schema drop đúng các bảng legacy này theo thứ tự khóa ngoại, không dùng CASCADE.
- Module Lớp học (Course/Lesson), route `/courses`, API và các bảng `courses`, `course_topics`, `lessons`, `lesson_progress`, `course_enrollments`, `course_materials`, `course_problem_assignments` đã gỡ bỏ. Schema drop đúng các bảng legacy này theo thứ tự khóa ngoại, không dùng CASCADE.
- Việc xóa bảng legacy đã được người dùng yêu cầu rõ ràng. Phải sao lưu PostgreSQL trước khi áp dụng; không xóa volume tài liệu cũ hoặc dữ liệu User/Problem/Submission/Contest trong cùng thao tác. Không khôi phục module/bảng khi chưa có yêu cầu mới.

### Contest

- Contest là module trong cùng modular monolith: `domain/contest`, `application/contest`, `infrastructure/persistence/contest`, `presentation/rest/contest`; không tạo hệ thống Problem/Judge mới.
- Contest lưu tên, loại, thời gian UTC, thời lượng và danh sách bài theo thứ tự A–Z cùng điểm. Chỉ `TEACHER`/`ADMIN` tạo bản nháp tại `/contests/create`; sau đó cấu hình tại `/contests/{id}/edit` qua ba tab Chi tiết, Bài tập, Cài đặt. Bản nháp cho phép chưa có bài; mỗi bài mới mặc định 1 điểm, điều chỉnh theo bước 1 và danh sách có bài phải có tổng đúng 10 điểm (tối đa 10 bài, không trùng) trước khi lưu/công khai. Trạng thái Công khai/Riêng tư là dropdown trong tab Cài đặt; lưu với Công khai sẽ publish nguyên tử, không có nút publish riêng. Thời gian kết thúc phải còn hiệu lực khi công khai. Người tạo hoặc `ADMIN` được chỉnh sửa Contest ở cả trạng thái sắp diễn ra, đang diễn ra và đã kết thúc; giáo viên chỉ được phân công giám sát vẫn không có quyền sửa. Rating và thể lệ tùy chỉnh đã gỡ bỏ.
- Tab Bài tập cho phép chọn Problem công khai hoặc tạo đề riêng bằng form/Gemini. Đề riêng lưu trong `contest_problem_definitions` và `contest_problem_test_cases`, không ghi vào `programming_problems` và không xuất hiện trong `/problems`; AI chỉ tạo bản nháp để người dùng duyệt. Submission của đề riêng lưu trong `contest_local_submissions`, nhưng Run/Submit vẫn gọi `CodeJudgeUseCase` và Docker sandbox hiện có, không nhân bản Judge.
- Người tạo hoặc `ADMIN` được xóa mềm Contest qua `DELETE /api/teacher/contests/{id}`. Xóa đặt `deleted_at`, chuyển Contest về riêng tư và loại khỏi catalog/quản lý; không xóa vật lý registration, submission, camera event, kết quả hoặc dữ liệu lịch sử.
- Cài đặt Contest tách `visibility` (`PUBLIC`/`PRIVATE`) khỏi `participationMode` (`OPEN`/`RESTRICTED`). Private không xuất hiện trong catalog; Restricted chỉ cho email đã chuẩn hóa trong danh sách đăng ký. Danh sách email có thể chứa người chưa có tài khoản. Chỉ người tạo hoặc `ADMIN` được sửa/công khai; giáo viên giám sát không có quyền sửa.
- `/api/teacher/contests/mine` và trang `/admin` trả các Contest do chính `TEACHER` hiện tại tạo; `ADMIN` xem và quản lý toàn bộ Contest. `/api/teacher/contests/supervised` trả riêng các Contest đã công khai mà giáo viên được phân công giám sát; chúng hiển thị trong khu vực chỉ đọc với thao tác xem thống kê, không cấp quyền chỉnh sửa. Nút **Quản lý kỳ thi** trên catalog mở trang này; menu dấu ba chấm của từng kỳ thi có thao tác chỉnh sửa và mở báo cáo giám sát. Bản nháp được ghi vào PostgreSQL ngay sau bước `/contests/create`, luôn được xem là riêng tư dù cấu hình công khai đã được chọn cho thời điểm publish; rời `/edit` không làm mất bản nháp. Khi `endsAt` đã tới, application chuyển `visibility` của Contest sang `PRIVATE` và lưu vào PostgreSQL trước khi trả catalog/quản lý.
- Contest lưu `createdAt` theo thời gian server; public summary trả thêm `participationMode` để người xem biết kỳ thi có giới hạn email hay không. Các danh sách Contest hiển thị mỗi kỳ thi thành một hàng ngang gọn: icon, tên/badge, thời gian bắt đầu-kết thúc, thời lượng, số bài, số người tham gia, trạng thái và CTA. Màu ngữ nghĩa phải nhất quán; mobile chia thành lưới hai cột và không tràn ngang.
- Catalog/detail là public tại `GET /api/contests`, `GET /api/contests/{id}`. Detail không trả email, UUID user nội bộ, source hay lịch sử nộp của người khác; response có thông tin cá nhân phải `Cache-Control: no-store`.
- Đăng ký idempotent theo `(contest,user)`; cả ba role có thể tham gia. Server chỉ nhận nộp trong `[startsAt, endsAt)`, yêu cầu đăng ký và Problem thuộc Contest. Timestamp ghi nhận trước Judge; lượt hợp lệ trước hạn vẫn được chấm khi Judge hoàn tất sau hạn.
- Với Problem công khai, `ContestSubmissionPort` tái sử dụng `ProgrammingProblemsUseCase.submit`; với đề riêng, port gọi cùng `CodeJudgeUseCase` rồi lưu submission riêng của Contest. Adapter infrastructure giữ transaction và idempotency cho cả hai luồng. Nộp thường hoặc chạy thử ngoài Contest không được cộng điểm Contest.
- Leaderboard tính mỗi bài Accepted một lần; sắp xếp tổng điểm giảm dần, thời gian nhận lượt Accepted đầu tiên của bài giải cuối cùng tăng dần, sau đó public user ID. Tiến độ Problem thông thường vẫn được lưu như hiện tại.
- Frontend nằm trong `src/features/contest`, gồm `/contests`, `/admin`, `/contests/create`, `/contests/{id}/edit` và `/contests/{id}`. Catalog dùng dropdown trạng thái mặc định **Tất cả**, hợp nhất ba catalog status và luôn xếp Contest đang diễn ra lên đầu; không hiển thị bộ lọc độ khó hoặc sắp xếp thủ công. Dùng lại `ProblemWorkspace` với `submissionPolicy` tùy chọn để nộp vào API Contest; không sửa luồng `/problems` mặc định. Countdown dùng thời gian server + đồng hồ đơn điệu phía client, cập nhật mỗi giây; server là nguồn quyết định hạn nộp.
- Không tự động seed Contest khi khởi động. Khi người dùng yêu cầu dữ liệu mẫu, có thể chạy thủ công `scripts/add-sample-contests.sql`: tham chiếu Problem sẵn có, dùng admin hiện có, không tạo người tham gia/kết quả/rating giả và không đổi lịch Contest đã tồn tại khi chạy lại. Các Problem được tham chiếu vẫn là bài công khai, không cam kết đề thi bí mật hoặc chống gian lận. Không tự ý thêm rating, queue hoặc microservice.
- Workspace dùng `/contests/{id}/problems/{problemId}`, bảng xếp hạng `/contests/{id}/leaderboard`, cùng các tab `/submissions` và `/rules`. History API giữ cùng instance Contest khi chuyển bài/tab; cache bản nháp theo problem/language chỉ dùng trong phiên Contest của tài khoản hiện tại, bên cạnh autosave Problem hiện có.
- `application/contest/scoring/ContestScoring` tính bảng điểm và công bố `scoringRules` trong detail. Quy tắc hiện tại không phạt lượt sai; trả số lượt sai trước Accepted đầu tiên, tổng attempts và điểm từng bài. Không cộng điểm lần nữa khi resubmit Accepted.
- Frontend cập nhật detail mỗi 5 giây qua `subscribeContestUpdates`, tạm ngừng khi tab ẩn và tránh request poll chồng nhau. Giữ ranh giới transport để thay polling sau này; không thêm WebSocket/SSE dependency khi chưa cần.
- Không giả số liệu bộ nhớ hoặc verdict Memory Limit: Judge hiện chỉ trả runtime và các verdict đang có. Runtime giữ nguyên định nghĩa của Judge. Backend quyết định deadline ngay cả khi UI đang chấm hoặc đồng hồ client sai.

### Gỡ thể lệ và rating Contest

- Không còn `Contest.rules`, `Contest.rated`, RatingCalculator, API field/filter rating, tab Thể lệ hoặc route lịch sử rating. Giữ scoring/leaderboard/kết quả/lịch sử/Virtual; scoringRules là mô tả cách tính điểm hiện có, không phải thể lệ tùy chỉnh lưu database.
- Theo yêu cầu xóa database, schema chỉ DROP `rating_history`, `user_contest_ratings`, `contests.rules` và `contests.rated`; loại hai trường này khỏi snapshot kết quả cũ. Không CASCADE và không xóa kết quả hay submission. Sao lưu trước nâng cấp; `.backups/` chỉ dùng local, không commit dữ liệu người dùng. Không khôi phục rating/thể lệ khi chưa có yêu cầu.

### Thời gian làm bài Contest

- Form tạo lưu riêng `description` (tối đa 5000 ký tự), `startsAt`, `endsAt`, `durationMinutes`, `monitoringEnabled`. Cho phép ngày bắt đầu quá khứ; ngày kết thúc phải sau ngày bắt đầu, thời gian làm 1–10080 phút. Request cũ thiếu `endsAt` dùng startsAt + duration để tương thích.
- Đăng ký không bắt đầu đồng hồ. `POST /api/contests/{id}/attempt` yêu cầu JWT, đã đăng ký và Contest đang mở; ghi `contest_registrations.attempt_started_at` một lần, cập nhật nguyên tử/idempotent qua row lock. Không reset khi refresh, đổi thiết bị, mở nhiều tab hay bấm lại. `Detail.myAttempt` chỉ trả lượt của chính user.
- Hạn riêng = min(thời điểm bắt đầu làm + durationMinutes, endsAt). Application và admission trước Judge đều chặn nộp khi chưa bắt đầu/hết hạn riêng. Giữ timestamp nhận bài trước Judge; không sửa Judge hoặc lưu code lần nữa. Leaderboard tính thời gian giải từ lúc từng người bắt đầu; submission lịch sử được backfill mốc startsAt cũ, không dùng submission Virtual để backfill.
- `monitoringEnabled` bật camera giám sát cho lượt thi chính thức; cuộc thi không bật cờ và Virtual không mở camera/gửi sự kiện. Không tự bật cờ cho cuộc thi cũ.
- Không còn nút “Hồ sơ thi đấu của tôi” ở danh sách. Giữ route hồ sơ/lịch sử và link tên user trong bảng xếp hạng.

### Camera Contest MVP

- Contest có giám sát yêu cầu đồng ý thông báo, cấp quyền và camera ở trạng thái sẵn sàng trước khi UI cho bắt đầu làm bài. Camera được duy trì suốt lượt thi, không có nút tắt trong trang; chỉ hiện/ẩn hình xem trước bằng CSS, không dừng stream/worker. Consent lưu sessionStorage theo user/contest để mở lại sau refresh trong tab; không lưu sự kiện. Người dùng vẫn có thể thu hồi quyền ở trình duyệt hoặc rời trang. Khi camera ngắt, workspace bị khóa và yêu cầu kết nối lại; không sửa Judge/scoring hoặc tự trừ điểm.
- Dependency được người dùng yêu cầu cho MVP: `@mediapipe/tasks-vision` pin version. Model Face Landmarker và WASM phục vụ cùng origin, xử lý trong Web Worker khoảng 5 FPS, chỉ một frame đang xử lý. Không thêm Python/service AI bên ngoài.
- Frontend `features/contest/camera` hiệu chỉnh nhìn thẳng trước khi đếm; khi pose chuyển vào vùng lệch đầu thì ghi ngay một episode, giữ nguyên tư thế chỉ tính một lần, và một frame trở lại rõ vùng nhìn thẳng sẽ mở khóa lần đếm tiếp theo. Mất mặt/nhiều mặt/tab ẩn không phải LOOKING_AWAY. Không thấy mặt liên tục 2 giây tạo một `NO_FACE`: workspace phủ mờ và khóa tối thiểu 10 giây, tiếp tục khóa nếu khuôn mặt chưa trở lại; đồng hồ thi vẫn chạy. Đây là khóa UI best-effort và head-pose heuristic, không nhận diện liếc mắt, cảm xúc, danh tính hay ý định gian lận.
- Chỉ gửi `eventId` và enum sự kiện qua JWT; Java gán user và thời gian server. Không upload/lưu video, ảnh, landmark, góc đầu hoặc embedding. UI thông báo rõ, có consent trước khi xin quyền; camera/worker dừng khi rời trang, đổi tài khoản hoặc hết hạn cá nhân/toàn cuộc thi. Camera bị gián đoạn phải có thông báo và nút kết nối lại, không đặt lại thời gian thi.
- API camera đi qua application input/output port và domain `CameraEvent`, không tác động Judge/scoring. Server kiểm tra monitoringEnabled, registration, live window và hạn lượt thi cá nhân; chưa bắt đầu chỉ chuẩn bị camera, không ghi sự kiện. Khóa theo registration, dedupe event ID, giới hạn 2 sự kiện/giây và 20000 sự kiện/user/contest. Retry cùng ID không tăng count.
- `/camera/me` trả khả năng sử dụng/xem báo cáo và số lượt `LOOKING_AWAY` của chính tài khoản trong thời hạn lưu; sinh viên không thể đọc thống kê của người khác. Preview vẽ face mesh xanh từ landmark xử lý cục bộ và không gửi landmark lên backend. Báo cáo tổng chỉ admin, giáo viên tạo Contest hoặc giáo viên được chọn làm giám sát; giáo viên khác và sinh viên bị từ chối ở backend. Giáo viên giám sát chỉ có quyền xem báo cáo, không có quyền sửa Contest. API no-store, báo cáo phân trang 20; không trả email/UUID user. Dữ liệu chỉ giữ 30 ngày, lọc theo hạn khi đọc và dọn mỗi giờ. Xóa registration cascade chỉ các sự kiện camera của registration đó.
- Event là dữ liệu client tự báo, có thể giả mạo/bỏ sót; không dùng để tự trừ điểm, thay đổi admission phía server hay kết luận gian lận. `NO_FACE` chỉ khóa tương tác UI như mô tả trên và không kéo dài thời gian thi. Không có sự kiện không chứng minh camera luôn bật. Retry queue tối đa 30 trong bộ nhớ, không lưu telemetry vào browser storage; rời trang có thể mất sự kiện chưa đồng bộ và UI phải báo lỗi gửi.
- Trong lượt thi đang giám sát, frontend gửi `CAMERA_ACTIVE` mỗi 10 giây khi stream/worker còn sẵn sàng. Báo cáo dùng tín hiệu trong 25 giây gần nhất để hiển thị trạng thái kết nối; heartbeat không được tính là một lần bắt đầu hoặc một lần vi phạm.
- Trong lượt thi chính thức, frontend còn gửi `TAB_HIDDEN` khi document chuyển sang hidden, `PROBLEM_COPIED` khi copy trong panel đề bài, `CONTENT_PASTED` khi dán trong trang và `SCREENSHOT_ATTEMPT` khi trang nhận được phím PrintScreen. Đây chỉ là tín hiệu best-effort: trình duyệt không thể bảo đảm phát hiện Snipping Tool, phím tắt/hành vi ở cấp hệ điều hành hoặc thiết bị ngoài; tuyệt đối không tuyên bố đã phát hiện mọi ảnh chụp và không upload ảnh màn hình.
- Catalog thay CTA **Vào thi** bằng **Giám sát** đối với `ADMIN`, giáo viên tạo Contest hoặc giáo viên được phân công khi Contest bật monitoring. Báo cáo mở bằng `?monitoring=1` và chia tab Quay ngang, Rời khung hình, Chuyển tab, Sao chép, Dán nội dung, Chụp màn hình. Reviewer có thể tải timeline phân trang theo public user ID/loại; thời gian do Java gán và API no-store. Quyền xem vẫn do backend kiểm tra độc lập.

### Thống kê quản trị

- Thống kê hệ thống là module read-only theo bốn lớp: `application/analytics`, `infrastructure/persistence/analytics`, `presentation/rest` và frontend `features/admin-analytics`; không đặt truy vấn tổng hợp trong controller.
- `GET /api/admin/analytics` và `/admin/statistics` chỉ dành cho `ADMIN`, trả `Cache-Control: no-store` và lấy số liệu thật từ PostgreSQL.
- Dashboard tổng hợp User, Programming Problems, run/submission, Contest/Virtual và tín hiệu camera gồm cả sao chép/dán. Không hiển thị số liệu giả cho hoạt động chưa được persistence, ví dụ lượt dùng Compiler công khai.
- Truy vấn báo cáo không được trả email, UUID nội bộ, source code, test case hoặc dữ liệu camera thô. Đây là dashboard vận hành, không thay đổi scoring, tiến độ hay kết luận gian lận.

## Kiến trúc frontend

Frontend chia theo feature:

```text
src/
├── app/                  # bootstrap và composition
├── features/
│   └── <feature-name>/   # components, hooks, services, types của feature
├── shared/               # chỉ tạo khi có code thực sự dùng chung
└── styles/               # global styles
```

Không gom toàn bộ component, hook hoặc service của nhiều feature vào các thư mục chung. Chỉ chuyển code sang `shared` sau khi có nhu cầu tái sử dụng thực tế.

Compiler và Programming Problems dùng chung `src/shared/components/SmartCodeEditor.tsx`. Editor giữ history phía client cho undo/redo, hỗ trợ thao tác bàn phím, autocomplete tĩnh/biến đã khai báo và syntax highlighting theo ngôn ngữ cho type, keyword, string, number, function và comment; không thêm editor dependency khi các hành vi hiện tại vẫn đáp ứng yêu cầu. Gợi ý dòng tiếp theo bằng AI dùng `POST /api/code/suggestions`, chỉ dành cho user đã đăng nhập thuộc cả ba role và gọi Gemini qua backend; API key không xuống frontend. AI không tự chạy khi dừng gõ: chỉ nút **AI gợi ý** mới gửi request. Workspace bài tập/Contest gửi kèm nội dung đề, yêu cầu input/output và ví dụ để tăng độ chính xác; kết quả hiển thị một dòng ghost text và dùng `Tab` để nhận/`Esc` để bỏ. Nguồn gửi lên tối đa 100.000 ký tự, ngữ cảnh bài tối đa 20.000 ký tự, adapter chỉ gửi cửa sổ code quanh con trỏ và không lưu mã nguồn/gợi ý vào database. Input của Compiler là tùy chọn và có giá trị mẫu do frontend cung cấp khi để trống. Programming Problems có thao tác chạy input tùy chỉnh riêng; nút chạy test chấm toàn bộ test case ẩn và hiển thị đạt/trượt từng case. Input khi Submit luôn lấy từ test case ẩn của backend.

Các module frontend là các trang độc lập: `/` mở trực tiếp Compiler; các trang còn lại là `/problems`, `/problems/{slug}`, `/contests`, `/contests/{id}`. `src/app/App.tsx` chỉ chịu trách nhiệm page composition, navigation và chọn page theo URL; không đưa logic nghiệp vụ của feature vào app shell. Không thêm router dependency khi các route hiện tại vẫn được xử lý rõ ràng bằng browser pathname và History API.

Authentication frontend nằm trong `src/features/auth`, gồm `/login`, `/register` và `/auth/callback`. Header chỉ đọc trạng thái đăng nhập tối thiểu để hiển thị avatar, tên và menu logout; không hiển thị email trên navbar. Request và lưu/xóa token thuộc feature auth.

Quản trị user frontend nằm trong `src/features/admin-users` tại `/admin/users`. Link quản trị chỉ hiển thị cho `ADMIN`; API vẫn bắt buộc bảo vệ độc lập dưới `/api/admin/**`.

## Quy tắc code

- Ưu tiên code đơn giản, dễ đọc, tên thể hiện đúng ý nghĩa.
- Java dùng constructor injection; không dùng field injection.
- Domain là Java thuần và bất biến khi hợp lý.
- REST endpoint đặt dưới `/api`; controller trả DTO/domain projection phù hợp, không làm việc trực tiếp với persistence.
- Lỗi API dùng cùng shape `timestamp/status/error/message/path`; lỗi authentication/authorization từ filter cũng phải theo shape này.
- Validation nghiệp vụ phải diễn ra trước persistence và giới hạn độ dài phải khớp hoặc chặt hơn schema database; không dựa vào lỗi cắt ngắn/constraint của database để validate request.
- Cấu hình nhạy cảm lấy từ biến môi trường; không commit secret.
- TypeScript bật strict; tránh `any`.
- React dùng function component; state đặt gần nơi sử dụng nhất.
- Tailwind dùng trực tiếp cho UI; không tạo abstraction styling khi chưa có nhu cầu.
- Viết test tập trung vào hành vi quan trọng, không test implementation detail.
- Tránh N+1 query; đường list/filter phải lọc tại repository và có index phù hợp với khóa lọc/sắp xếp thực tế. Không thêm cache khi chưa đo được nhu cầu.
- Sau thay đổi, chạy kiểm tra liên quan tối thiểu: backend `mvnw clean verify`, frontend `npm run build`.

## Nguyên tắc bắt buộc

- Không tự ý thêm công nghệ hoặc dependency.
- Không chuyển sang Microservices và không tạo service triển khai độc lập.
- Không over-engineering: không thêm abstraction, generic framework, event bus, CQRS, DDD pattern hay infrastructure khi chưa có yêu cầu thực tế.
- Không sửa file hoặc module ngoài phạm vi task.
- Không mở rộng authentication hoặc triển khai compiler, exam hay AI nếu task không yêu cầu rõ ràng.
- Với Compiler code execution, luôn đi qua `CodeExecutionPort` và Docker sandbox; không chạy source trong JVM/Spring Boot hoặc nối Compiler vào logic chấm test case của Programming Problems.
- Không nới lỏng giới hạn sandbox, đưa source/input vào shell command hoặc cho sandbox truy cập network/host filesystem.
- Tôn trọng thay đổi đang có của người dùng; không xóa hoặc ghi đè thay đổi không liên quan.
- Nếu yêu cầu mới xung đột với kiến trúc hoặc mở rộng đáng kể phạm vi, cần nêu rõ trade-off trước khi thực hiện.
