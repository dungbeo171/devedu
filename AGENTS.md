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
- Register/login được truy cập qua application input port; repository, password hasher và token provider là output port.
- JPA adapter, BCrypt và JWT implementation nằm trong `infrastructure`.
- REST request/response DTO và exception mapping nằm trong `presentation`.
- Đăng ký công khai chỉ tạo `STUDENT`; không cho client tự chọn role.
- `TEACHER` và `ADMIN` chỉ được cấp qua workflow quản trị khi task tương lai yêu cầu.
- JWT dùng HMAC-SHA256, stateless, secret từ `JWT_SECRET`; không commit secret cố định.
- JWT phải xác minh chữ ký constant-time, `alg`, `typ`, `iat`, `exp` và các claim định danh; response register/login phải có `Cache-Control: no-store`.
- Đăng nhập Google, GitHub và Microsoft dùng Spring Security OAuth2 Client. Client ID/Secret chỉ lấy từ biến môi trường; provider chưa cấu hình phải được báo disabled qua API, không dùng credential giả.
- OAuth callback chỉ chấp nhận email đã được provider xác thực theo contract của từng provider. GitHub phải lấy email verified qua API `user:email`; Google yêu cầu claim `email_verified`; Microsoft dùng email/preferred username từ OIDC.
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

### Code Judge

- `CodeJudgeUseCase` là input port độc lập; `SandboxExecutionPort` là output port tách application khỏi cách thực thi. `DockerSandboxExecutionAdapter` nằm trong `infrastructure/judge`.
- Code người dùng không được compile, load hoặc chạy trong JVM/Spring Boot. Chỉ adapter judge được phép khởi tạo Docker CLI bằng danh sách argument cố định; không ghép source code, input hay test case vào host shell command.
- Mỗi lần compile/run dùng container tạm thời, không network, root filesystem read-only, non-root user, drop toàn bộ capability, `no-new-privileges`, seccomp mặc định và giới hạn CPU, RAM, PID, thời gian, output. Tổng số execution đồng thời cũng phải bị giới hạn. Không dùng `--privileged`, host network hoặc mount Docker socket vào sandbox.
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

- Domain gồm `ProgrammingProblem`, `ProblemTopic`, `ProblemTestCase` và `ProblemSubmission`.
- Application input port hỗ trợ list, filter theo topic, lấy detail và submit; persistence chỉ đi qua output port.
- JPA entities/adapters nằm trong `infrastructure/persistence/problem`; REST DTO/controller nằm trong `presentation`.
- Danh sách và chi tiết bài tập là public. Submit yêu cầu JWT có role `STUDENT`.
- Submit lấy test case qua output port và gọi `CodeJudgeUseCase`; kết quả cuối cùng là `ACCEPTED`, `WRONG_ANSWER`, `COMPILE_ERROR`, `RUNTIME_ERROR` hoặc `TIME_LIMIT`.
- `NOT_JUDGED` chỉ được giữ để tương thích dữ liệu cũ. Không lưu submission mới nếu hạ tầng judge không sẵn sàng.
- Test case và expected output chỉ tồn tại ở backend; không trả qua API public.
- Frontend code của module nằm trong `src/features/programming-problems`.

### Course/Lesson

- Domain gồm `Course`, `CourseTopic`, `Lesson` và `LessonProgress`; không phụ thuộc Spring/JPA/web.
- Quản lý nội dung đi qua `CourseLearningUseCase`; persistence đi qua output port và adapter trong `infrastructure/persistence/course`.
- Giáo viên chỉ được quản lý khóa học do chính mình tạo; `ADMIN` có thể quản lý mọi khóa học.
- Danh sách môn học, chi tiết khóa học và lesson là public. Đánh dấu lesson hoàn thành chỉ chấp nhận JWT role `STUDENT`.
- Tiến độ hoàn thành là idempotent theo cặp student/lesson; không tạo bản ghi trùng khi gọi lại.
- Video chỉ là URL HTTP/HTTPS được lưu cùng lesson. Không tự ý thêm upload, object storage, transcoding hoặc streaming infrastructure.
- Frontend code của module nằm trong `src/features/course-learning`.

### Exam

- Domain gồm `Exam`, `ExamQuestion`, `ExamAttempt`, `ExamAnswer` và các enum trạng thái/loại câu hỏi.
- `ExamUseCase` điều phối tạo đề, bắt đầu lượt thi, lưu đáp án, nộp bài và đọc kết quả; persistence nằm trong `infrastructure/persistence/exam`.
- `/api/teacher/exams/**` dành cho `TEACHER`/`ADMIN`; giáo viên chỉ quản lý và xem kết quả kỳ thi của mình, admin có thể quản lý tất cả.
- `/api/exams/**` chỉ dành cho `STUDENT`. Mỗi sinh viên có tối đa một attempt cho mỗi kỳ thi.
- Đề chỉ được bắt đầu từ `scheduledAt`; hạn làm bài của attempt được tính bằng `durationMinutes` kể từ lúc bắt đầu.
- Không trả đáp án đúng qua API sinh viên. Không cho thay đổi bộ câu hỏi sau khi đã có attempt.
- Multiple Choice được chấm tự động. Coding chỉ lưu source code và luôn được báo chờ chấm; không giả lập judge hoặc điểm coding.
- Frontend code của module nằm trong `src/features/exam`.

### Interview

- Domain gồm `InterviewQuestion`, `InterviewTopic` và `InterviewDifficulty`.
- `InterviewQuestionsUseCase` hỗ trợ list, filter đồng thời theo topic/difficulty và lấy chi tiết; persistence nằm trong `infrastructure/persistence/interview`.
- `/api/interview/**` chỉ dành cho JWT role `STUDENT`.
- API danh sách không trả `answer` hoặc `explanation`; hai trường này chỉ xuất hiện ở API chi tiết khi sinh viên mở đáp án.
- Topic cố định gồm Java, Python, C++, OOP, SQL, Database, Data Structures, Algorithms và Web; difficulty gồm `EASY`, `MEDIUM`, `HARD`.
- Frontend code của module nằm trong `src/features/interview`.

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

Compiler và Programming Problems dùng chung `src/shared/components/SmartCodeEditor.tsx`. Editor giữ history phía client cho undo/redo, hỗ trợ thao tác bàn phím và autocomplete tĩnh/biến đã khai báo; không thêm editor dependency khi các hành vi hiện tại vẫn đáp ứng yêu cầu. Input của Compiler là tùy chọn và có giá trị mẫu do frontend cung cấp khi để trống; input chấm Programming Problems luôn lấy từ test case ẩn của backend.

Các module frontend là các trang độc lập: `/` mở trực tiếp Compiler; các trang còn lại là `/problems`, `/courses`, `/exams`, `/interview`. `src/app/App.tsx` chỉ chịu trách nhiệm page composition, navigation và chọn page theo URL; không đưa logic nghiệp vụ của feature vào app shell. Không thêm router dependency khi các route tĩnh hiện tại vẫn được xử lý rõ ràng bằng browser pathname.

Authentication frontend nằm trong `src/features/auth`, gồm `/login`, `/register` và `/auth/callback`. Header chỉ đọc trạng thái đăng nhập tối thiểu để hiển thị login/logout; request và lưu/xóa token thuộc feature auth.

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
- Với Course/Lesson, không tự ý thêm upload/storage, streaming, enrollment, payment, quiz hoặc certificate.
- Với Exam, không tự ý thêm proctoring/chống gian lận, webcam, browser lockdown, code judge hoặc chấm điểm coding giả.
- Với Compiler code execution, luôn đi qua `CodeExecutionPort` và Docker sandbox; không chạy source trong JVM/Spring Boot hoặc nối Compiler vào logic chấm test case của Programming Problems.
- Không nới lỏng giới hạn sandbox, đưa source/input vào shell command hoặc cho sandbox truy cập network/host filesystem.
- Tôn trọng thay đổi đang có của người dùng; không xóa hoặc ghi đè thay đổi không liên quan.
- Nếu yêu cầu mới xung đột với kiến trúc hoặc mở rộng đáng kể phạm vi, cần nêu rõ trade-off trước khi thực hiện.
