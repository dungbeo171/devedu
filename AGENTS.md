# AGENTS.md

## Mục tiêu dự án

DevEdu là nền tảng website học lập trình dạng modular monolith, có authentication và các module học tập. Không coi các contract còn được ghi rõ là stub hoặc các màn hình minh họa là nghiệp vụ hoàn chỉnh.

## Công nghệ được phép

- Backend: Java 17, Spring Boot, Spring Web, Spring Data JPA, Spring Security
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
- `/api/admin/problems/**` chỉ cho `ADMIN`: đọc dữ liệu quản trị gồm test case ẩn, sửa toàn bộ problem/test case atomically và xóa mềm problem. Xóa mềm phải loại bài khỏi catalog nhưng giữ submission/draft lịch sử và không để dữ liệu seed tái tạo bài sau khi restart. Test case ẩn không được trả qua API public.
- Chi tiết bài tập có `sampleInput`/`sampleOutput` công khai cho chạy thử; đây là dữ liệu mẫu riêng, không lấy hoặc làm lộ test case ẩn.
- `POST /api/problems/{slug}/runs` dành cho user đã xác thực với role `STUDENT`, `TEACHER` hoặc `ADMIN`, chạy toàn bộ test case ẩn nhưng không tạo submission hoặc tiến độ. Response chỉ trả vị trí, trạng thái đạt/trượt của từng case và kết quả tổng; không trả input hay expected output ẩn. Kết quả tổng chỉ là `ACCEPTED` khi mọi test case đều đạt.
- Mỗi lượt chạy test được cộng vào `problem_run_statistics` bằng cập nhật nguyên tử; `successful_runs` chỉ tăng khi kết quả tổng là `ACCEPTED`. API danh sách trả `acceptanceRate = successful_runs / total_runs * 100`, truy vấn thống kê theo lô và không dùng submission để tính tỷ lệ này.
- Submit lấy test case qua output port và gọi `CodeJudgeUseCase`; kết quả cuối cùng là `ACCEPTED`, `WRONG_ANSWER`, `COMPILE_ERROR`, `RUNTIME_ERROR` hoặc `TIME_LIMIT`.
- Application phải kiểm tra language thuộc tập ngôn ngữ của problem trước khi lưu draft hoặc gọi Code Judge; frontend chỉ hiển thị các language được phép trong workspace.
- Mọi submission hợp lệ được lưu. `GET /api/student/problem-progress` cho phép cả ba role và trả các problem ID đã có ít nhất một submission `ACCEPTED` của chính tài khoản đó.
- Bản nháp code được lưu riêng theo cặp `user/problem`, gồm language, source code và input. `GET/PUT /api/student/problems/{slug}/draft` cho phép cả ba role nhưng chỉ đọc/ghi bản nháp của chính tài khoản đang đăng nhập. Frontend Problems ghi IndexedDB mỗi lần sửa code/input; chỉ gửi API draft sau đủ 5000ms không chỉnh sửa, kiểm tra lại hạn bằng đồng hồ đơn điệu ngay trước request kể cả sau khi chờ hàng đợi. Không flush server trên Run, focus, pagehide hoặc cleanup/unmount; rời trang sớm giữ bản dirty cục bộ. Submit chủ động là luồng ghi submission riêng, không trì hoãn nghiệp vụ nộp bài. IndexedDB tách tài khoản/khách, namespace Contest/Virtual, bài và ngôn ngữ; bản dirty cục bộ ưu tiên khôi phục, bản sạch ưu tiên dữ liệu server. Request trong cùng tab được tuần tự hóa và acknowledgement chỉ áp dụng đúng revision; online/focus retry phải tôn trọng hạn không gõ. Không gửi bản nháp tài khoản cũ bằng token tài khoản mới. Khách và Virtual chỉ lưu cục bộ theo phạm vi hiện tại; Compiler nhiều file chưa có API draft riêng.
- `NOT_JUDGED` chỉ được giữ để tương thích dữ liệu cũ. Không lưu submission mới nếu hạ tầng judge không sẵn sàng.
- Test case và expected output chỉ tồn tại ở backend; không trả qua API public.
- Frontend code của module nằm trong `src/features/programming-problems`.
- Trang `/problems` hiển thị các topic dạng bộ lọc tên ngắn ở trên, bộ lọc difficulty/language/tiến độ và danh sách bài một hàng ở dưới, phân trang cố định 20 bài mỗi trang; chọn bài mới mở workspace. Workspace ưu tiên bản nháp của sinh viên, nếu chưa có thì dùng starter code riêng của bài/ngôn ngữ. Tài khoản `TEACHER`/`ADMIN` mở trang riêng `/problems/add` để thêm bài tập, starter code và test case ẩn. Chỉ `ADMIN` thấy thao tác sửa/xóa; sửa dùng `/problems/{slug}/edit`, xóa cập nhật danh sách ngay không reload.
- Slug bài tập dùng tiếng Việt không dấu, phân tách bằng dấu gạch ngang để URL ngắn và dễ đọc.
- Topic hiển thị thành hàng lọc riêng phía trên thanh search/sort/filter. Danh sách hiển thị mỗi bài trên một hàng gọn cùng acceptance rate từ lượt chạy thử; bài đã giải có dấu tích lấy từ tiến độ `ACCEPTED` đã lưu và đồng bộ lại ngay sau Submit. Chạy thử `SUCCESS` hiển thị trạng thái và dấu tích màu xanh nhưng không được tính là đã giải.
- Danh sách không có nút sắp xếp A–Z/Z–A. Bộ lọc và phân trang lưu vào query URL `/problems` để khôi phục khi quay lại từ workspace hoặc tải lại trang; đổi bộ lọc mới đặt trang về 1. Menu lọc tự đóng sau khi dữ liệu của lựa chọn mới tải thành công, giữ mở khi lỗi; tìm kiếm giữ mở khi gõ và đóng khi nhấn Enter.

### Module đã gỡ bỏ

- Module Exam, route `/exams` và API liên quan đã gỡ bỏ. Không tái tạo bảng Exam; giữ nguyên dữ liệu cũ trong database, không tự động DROP bảng.
- Module Lớp học (Course/Lesson), route `/courses` và các API liên quan đã được gỡ bỏ. Không khôi phục khi chưa có yêu cầu của người dùng.
- Không tự động xóa bảng dữ liệu lớp học hoặc volume tài liệu cũ. Schema mới không tạo bảng lớp học; dữ liệu cũ được giữ lại để có thể khôi phục thủ công.

### Contest

- Contest là module trong cùng modular monolith: `domain/contest`, `application/contest`, `infrastructure/persistence/contest`, `presentation/rest/contest`; không tạo hệ thống Problem/Judge mới.
- Contest lưu tên, loại, thời gian UTC, thời lượng, rules và danh sách tham chiếu Problem theo thứ tự A–Z cùng điểm. Chỉ `TEACHER`/`ADMIN` tạo; từ 1 đến 26 Problem đang tồn tại, không trùng, 1–10000 điểm/bài. Chưa hỗ trợ sửa/xóa Contest hoặc rating.
- Catalog/detail là public tại `GET /api/contests`, `GET /api/contests/{id}`. Detail không trả email, UUID user nội bộ, source hay lịch sử nộp của người khác; response có thông tin cá nhân phải `Cache-Control: no-store`.
- Đăng ký idempotent theo `(contest,user)`; cả ba role có thể tham gia. Server chỉ nhận nộp trong `[startsAt, endsAt)`, yêu cầu đăng ký và Problem thuộc Contest. Timestamp ghi nhận trước Judge; lượt hợp lệ trước hạn vẫn được chấm khi Judge hoàn tất sau hạn.
- `ContestSubmissionPort` tái sử dụng `ProgrammingProblemsUseCase.submit`; adapter infrastructure đặt transaction cho submission/draft hiện có và liên kết Contest. Nộp thường hoặc chạy thử không được cộng điểm Contest. Không copy source hoặc test case vào bảng Contest.
- Leaderboard tính mỗi bài Accepted một lần; sắp xếp tổng điểm giảm dần, thời gian nhận lượt Accepted đầu tiên của bài giải cuối cùng tăng dần, sau đó public user ID. Tiến độ Problem thông thường vẫn được lưu như hiện tại.
- Frontend nằm trong `src/features/contest`, route `/contests` và `/contests/{id}`. Dùng lại `ProblemWorkspace` với `submissionPolicy` tùy chọn để nộp vào API Contest; không sửa luồng `/problems` mặc định. Countdown dùng thời gian server + đồng hồ đơn điệu phía client, cập nhật mỗi giây; server là nguồn quyết định hạn nộp.
- Không tự động seed Contest khi khởi động. Khi người dùng yêu cầu dữ liệu mẫu, có thể chạy thủ công `scripts/add-sample-contests.sql`: tham chiếu Problem sẵn có, dùng admin hiện có, không tạo người tham gia/kết quả/rating giả và không đổi lịch Contest đã tồn tại khi chạy lại. Các Problem được tham chiếu vẫn là bài công khai, không cam kết đề thi bí mật hoặc chống gian lận. Không tự ý thêm rating, queue hoặc microservice.
- Workspace dùng `/contests/{id}/problems/{problemId}`, bảng xếp hạng `/contests/{id}/leaderboard`, cùng các tab `/submissions` và `/rules`. History API giữ cùng instance Contest khi chuyển bài/tab; cache bản nháp theo problem/language chỉ dùng trong phiên Contest của tài khoản hiện tại, bên cạnh autosave Problem hiện có.
- `application/contest/scoring/ContestScoring` tính bảng điểm và công bố `scoringRules` trong detail. Quy tắc hiện tại không phạt lượt sai; trả số lượt sai trước Accepted đầu tiên, tổng attempts và điểm từng bài. Không cộng điểm lần nữa khi resubmit Accepted.
- Frontend cập nhật detail mỗi 5 giây qua `subscribeContestUpdates`, tạm ngừng khi tab ẩn và tránh request poll chồng nhau. Giữ ranh giới transport để thay polling sau này; không thêm WebSocket/SSE dependency khi chưa cần.
- Không giả số liệu bộ nhớ hoặc verdict Memory Limit: Judge hiện chỉ trả runtime và các verdict đang có. Runtime giữ nguyên định nghĩa của Judge. Backend quyết định deadline ngay cả khi UI đang chấm hoặc đồng hồ client sai.

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

Compiler và Programming Problems dùng chung `src/shared/components/SmartCodeEditor.tsx`. Editor giữ history phía client cho undo/redo, hỗ trợ thao tác bàn phím, autocomplete tĩnh/biến đã khai báo và syntax highlighting theo ngôn ngữ cho type, keyword, string, number, function và comment; không thêm editor dependency khi các hành vi hiện tại vẫn đáp ứng yêu cầu. Input của Compiler là tùy chọn và có giá trị mẫu do frontend cung cấp khi để trống. Programming Problems có thao tác chạy input tùy chỉnh riêng; nút chạy test chấm toàn bộ test case ẩn và hiển thị đạt/trượt từng case. Input khi Submit luôn lấy từ test case ẩn của backend.

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
