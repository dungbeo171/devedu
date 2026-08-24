CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS interview_questions (
    id UUID PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    explanation TEXT NOT NULL,
    difficulty VARCHAR(20) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    topic VARCHAR(30) NOT NULL CHECK (
        topic IN ('JAVA', 'PYTHON', 'CPP', 'OOP', 'SQL', 'DATABASE', 'DATA_STRUCTURES', 'ALGORITHMS', 'WEB')
    ),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS exams (
    id UUID PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    teacher_id UUID NOT NULL REFERENCES users(id),
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 1 AND 1440),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS exam_questions (
    id UUID PRIMARY KEY,
    exam_id UUID NOT NULL REFERENCES exams(id),
    type VARCHAR(30) NOT NULL CHECK (type IN ('MULTIPLE_CHOICE', 'CODING')),
    prompt TEXT NOT NULL,
    correct_option_index INTEGER,
    coding_language VARCHAR(20) CHECK (coding_language IN ('CPP', 'JAVA', 'PYTHON', 'HTML', 'MYSQL')),
    points INTEGER NOT NULL CHECK (points > 0),
    position INTEGER NOT NULL CHECK (position > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (exam_id, position)
);

CREATE TABLE IF NOT EXISTS exam_question_options (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES exam_questions(id),
    option_index INTEGER NOT NULL CHECK (option_index >= 0),
    value VARCHAR(1000) NOT NULL,
    UNIQUE (question_id, option_index)
);

CREATE TABLE IF NOT EXISTS exam_attempts (
    id UUID PRIMARY KEY,
    exam_id UUID NOT NULL REFERENCES exams(id),
    student_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'SUBMITTED')),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    automatic_score INTEGER NOT NULL DEFAULT 0 CHECK (automatic_score >= 0),
    automatic_max_score INTEGER NOT NULL DEFAULT 0 CHECK (automatic_max_score >= automatic_score),
    pending_coding_questions INTEGER NOT NULL DEFAULT 0 CHECK (pending_coding_questions >= 0),
    UNIQUE (exam_id, student_id)
);

CREATE TABLE IF NOT EXISTS exam_answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES exam_attempts(id),
    question_id UUID NOT NULL REFERENCES exam_questions(id),
    selected_option_index INTEGER,
    source_code TEXT,
    answered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (attempt_id, question_id)
);

CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    teacher_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS course_topics (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    title VARCHAR(180) NOT NULL,
    position INTEGER NOT NULL CHECK (position > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS lessons (
    id UUID PRIMARY KEY,
    topic_id UUID NOT NULL REFERENCES course_topics(id),
    title VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    video_url VARCHAR(2048),
    position INTEGER NOT NULL CHECK (position > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS lesson_progress (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id),
    lesson_id UUID NOT NULL REFERENCES lessons(id),
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (student_id, lesson_id)
);

CREATE TABLE IF NOT EXISTS programming_problems (
    id UUID PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    topic VARCHAR(30) NOT NULL CHECK (
        topic IN ('INTRODUCTION', 'CPP', 'JAVA', 'PYTHON', 'OOP', 'DATA_STRUCTURES', 'ALGORITHMS', 'SQL')
    ),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS problem_submissions (
    id UUID PRIMARY KEY,
    problem_id UUID NOT NULL REFERENCES programming_problems(id),
    student_id UUID NOT NULL REFERENCES users(id),
    language VARCHAR(20) NOT NULL CHECK (language IN ('CPP', 'JAVA', 'PYTHON', 'HTML', 'MYSQL')),
    source_code TEXT NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (
        status IN ('NOT_JUDGED', 'ACCEPTED', 'WRONG_ANSWER', 'COMPILE_ERROR', 'RUNTIME_ERROR', 'TIME_LIMIT')
    ),
    diagnostic TEXT NOT NULL DEFAULT '',
    passed_tests INTEGER NOT NULL DEFAULT 0 CHECK (passed_tests >= 0),
    total_tests INTEGER NOT NULL DEFAULT 0 CHECK (total_tests >= passed_tests),
    execution_time_ms BIGINT NOT NULL DEFAULT 0 CHECK (execution_time_ms >= 0),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE problem_submissions ADD COLUMN IF NOT EXISTS diagnostic TEXT NOT NULL DEFAULT '';
ALTER TABLE problem_submissions ADD COLUMN IF NOT EXISTS passed_tests INTEGER NOT NULL DEFAULT 0;
ALTER TABLE problem_submissions ADD COLUMN IF NOT EXISTS total_tests INTEGER NOT NULL DEFAULT 0;
ALTER TABLE problem_submissions ADD COLUMN IF NOT EXISTS execution_time_ms BIGINT NOT NULL DEFAULT 0;
ALTER TABLE problem_submissions DROP CONSTRAINT IF EXISTS problem_submissions_status_check;
ALTER TABLE problem_submissions ADD CONSTRAINT problem_submissions_status_check CHECK (
    status IN ('NOT_JUDGED', 'ACCEPTED', 'WRONG_ANSWER', 'COMPILE_ERROR', 'RUNTIME_ERROR', 'TIME_LIMIT')
);

CREATE TABLE IF NOT EXISTS problem_test_cases (
    id UUID PRIMARY KEY,
    problem_id UUID NOT NULL REFERENCES programming_problems(id),
    input TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    time_limit_ms INTEGER NOT NULL CHECK (time_limit_ms BETWEEN 100 AND 30000),
    position INTEGER NOT NULL CHECK (position > 0),
    UNIQUE (problem_id, position)
);

CREATE INDEX IF NOT EXISTS idx_interview_questions_topic_difficulty_question
    ON interview_questions (topic, difficulty, question);
CREATE INDEX IF NOT EXISTS idx_interview_questions_difficulty_question
    ON interview_questions (difficulty, question);
CREATE INDEX IF NOT EXISTS idx_exams_scheduled_at ON exams (scheduled_at);
CREATE INDEX IF NOT EXISTS idx_exams_teacher_scheduled_at ON exams (teacher_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_started_at ON exam_attempts (exam_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_exam_answers_attempt_answered_at ON exam_answers (attempt_id, answered_at);
CREATE INDEX IF NOT EXISTS idx_courses_title ON courses (title);
CREATE INDEX IF NOT EXISTS idx_course_topics_course_position ON course_topics (course_id, position);
CREATE INDEX IF NOT EXISTS idx_lessons_topic_position ON lessons (topic_id, position);
CREATE INDEX IF NOT EXISTS idx_programming_problems_title ON programming_problems (title);
CREATE INDEX IF NOT EXISTS idx_programming_problems_topic_title ON programming_problems (topic, title);
CREATE INDEX IF NOT EXISTS idx_problem_submissions_student_submitted_at
    ON problem_submissions (student_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_problem_submissions_problem_submitted_at
    ON problem_submissions (problem_id, submitted_at DESC);

INSERT INTO programming_problems (id, slug, title, summary, description, topic, created_at) VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'hello-devedu',
        'Hello DevEdu',
        'Đọc một tên và in ra lời chào đầu tiên của bạn.',
        'Cho một chuỗi name không chứa khoảng trắng. Hãy in ra Hello, name! trên một dòng. Ví dụ input: An. Output tương ứng: Hello, An!',
        'INTRODUCTION',
        '2026-01-01T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'cpp-sum-two-numbers',
        'C++: Tổng hai số',
        'Luyện nhập xuất cơ bản và phép cộng với C++.',
        'Cho hai số nguyên a và b trên cùng một dòng. Hãy in ra tổng a + b. Giới hạn: trị tuyệt đối của mỗi số không vượt quá 10^9.',
        'CPP',
        '2026-01-02T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'java-array-maximum',
        'Java: Phần tử lớn nhất',
        'Tìm giá trị lớn nhất trong một mảng số nguyên.',
        'Dòng đầu chứa số nguyên n. Dòng tiếp theo chứa n số nguyên. Hãy in ra phần tử lớn nhất trong mảng. Giới hạn: 1 <= n <= 100000.',
        'JAVA',
        '2026-01-03T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        'python-word-count',
        'Python: Đếm số từ',
        'Xử lý chuỗi và đếm số từ trong một câu.',
        'Cho một dòng văn bản không rỗng. Các từ được phân tách bởi một hoặc nhiều khoảng trắng. Hãy in ra số từ xuất hiện trong dòng.',
        'PYTHON',
        '2026-01-04T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000005',
        'oop-bank-account',
        'OOP: Tài khoản ngân hàng',
        'Thiết kế class và đóng gói trạng thái của một tài khoản.',
        'Cài đặt lớp BankAccount có số dư ban đầu, phương thức deposit và withdraw. Chương trình đọc các thao tác rồi in số dư cuối cùng. Không cho phép rút quá số dư hiện có.',
        'OOP',
        '2026-01-05T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000006',
        'stack-operations',
        'Mô phỏng Stack',
        'Cài đặt các thao tác push, pop và top trên ngăn xếp.',
        'Xử lý q truy vấn trên một stack số nguyên. Với push x, thêm x. Với pop, xóa phần tử trên cùng nếu có. Với top, in phần tử trên cùng hoặc EMPTY nếu stack rỗng.',
        'DATA_STRUCTURES',
        '2026-01-06T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000007',
        'binary-search',
        'Tìm kiếm nhị phân',
        'Tìm vị trí của một giá trị trong mảng đã sắp xếp.',
        'Cho mảng tăng dần gồm n số nguyên và giá trị x. In chỉ số đầu tiên của x theo hệ zero-based, hoặc -1 nếu x không xuất hiện. Yêu cầu độ phức tạp O(log n).',
        'ALGORITHMS',
        '2026-01-07T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000008',
        'sql-top-students',
        'SQL: Sinh viên có điểm cao',
        'Viết truy vấn lọc và sắp xếp kết quả học tập.',
        'Bảng students gồm id, name và score. Viết truy vấn trả về name và score của các sinh viên có score từ 8 trở lên, sắp xếp score giảm dần rồi name tăng dần.',
        'SQL',
        '2026-01-08T00:00:00Z'
    )
ON CONFLICT (id) DO UPDATE SET
    slug = EXCLUDED.slug,
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    description = EXCLUDED.description,
    topic = EXCLUDED.topic;

INSERT INTO problem_test_cases (id, problem_id, input, expected_output, time_limit_ms, position) VALUES
    ('50000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', E'An\n', E'Hello, An!\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', E'DevEdu\n', E'Hello, DevEdu!\n', 2000, 2),
    ('50000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', E'2 3\n', E'5\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', E'-10 4\n', E'-6\n', 2000, 2),
    ('50000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000003', E'5\n1 9 -2 7 3\n', E'9\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000004', E'  hoc   lap trinh  moi ngay \n', E'6\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000005', E'100\n3\ndeposit 50\nwithdraw 30\nwithdraw 150\n', E'120\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000006', E'7\npush 4\npush 8\ntop\npop\ntop\npop\ntop\n', E'8\n4\nEMPTY\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000007', E'6\n1 3 3 7 9 11\n3\n', E'1\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000007', E'4\n2 4 6 8\n5\n', E'-1\n', 2000, 2),
    ('50000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000008', E'CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100), score INT);\nINSERT INTO students VALUES (1, ''An'', 9), (2, ''Binh'', 8), (3, ''Chi'', 7);\n', E'An\t9\nBinh\t8\n', 3000, 1)
ON CONFLICT DO NOTHING;

INSERT INTO interview_questions (id, question, answer, explanation, difficulty, topic, created_at) VALUES
    ('40000000-0000-0000-0000-000000000001', 'Sự khác nhau giữa == và equals() trong Java là gì?', '== so sánh tham chiếu đối với object, còn equals() so sánh tính tương đương logic theo implementation của class.', 'Object.equals mặc định cũng so sánh tham chiếu, nhưng các class như String override phương thức này để so sánh nội dung.', 'EASY', 'JAVA', '2026-08-22T00:00:00Z'),
    ('40000000-0000-0000-0000-000000000002', 'List và tuple trong Python khác nhau thế nào?', 'List có thể thay đổi, còn tuple là immutable.', 'Tính immutable giúp tuple phù hợp cho dữ liệu cố định và có thể dùng làm dictionary key nếu các phần tử bên trong cũng hashable.', 'EASY', 'PYTHON', '2026-08-22T00:00:00Z'),
    ('40000000-0000-0000-0000-000000000003', 'RAII trong C++ là gì?', 'RAII gắn vòng đời tài nguyên với vòng đời của object: cấp phát trong constructor và giải phóng trong destructor.', 'Cách này bảo đảm tài nguyên được giải phóng khi object ra khỏi scope, kể cả khi có exception.', 'MEDIUM', 'CPP', '2026-08-22T00:00:00Z'),
    ('40000000-0000-0000-0000-000000000004', 'Bốn tính chất chính của OOP là gì?', 'Encapsulation, abstraction, inheritance và polymorphism.', 'Các tính chất này lần lượt giúp che giấu trạng thái, mô hình hóa phần thiết yếu, tái sử dụng quan hệ và thay thế implementation qua cùng contract.', 'EASY', 'OOP', '2026-08-22T00:00:00Z'),
    ('40000000-0000-0000-0000-000000000005', 'INNER JOIN và LEFT JOIN khác nhau thế nào?', 'INNER JOIN chỉ trả hàng khớp ở hai phía; LEFT JOIN giữ mọi hàng bên trái và điền NULL khi bên phải không khớp.', 'Chọn loại JOIN dựa trên việc các bản ghi không có quan hệ ở bảng bên phải có cần xuất hiện trong kết quả hay không.', 'EASY', 'SQL', '2026-08-22T00:00:00Z'),
    ('40000000-0000-0000-0000-000000000006', 'Database index cải thiện và đánh đổi điều gì?', 'Index tăng tốc truy vấn đọc phù hợp nhưng tốn dung lượng và làm thao tác ghi chậm hơn.', 'Database phải duy trì cấu trúc index khi INSERT, UPDATE hoặc DELETE; index không phù hợp còn có thể không được query planner sử dụng.', 'MEDIUM', 'DATABASE', '2026-08-22T00:00:00Z'),
    ('40000000-0000-0000-0000-000000000007', 'Stack và queue khác nhau ở thứ tự lấy phần tử nào?', 'Stack theo LIFO, queue theo FIFO.', 'Stack lấy phần tử thêm gần nhất; queue lấy phần tử đã chờ lâu nhất. Hai cấu trúc phù hợp với các luồng xử lý khác nhau.', 'EASY', 'DATA_STRUCTURES', '2026-08-22T00:00:00Z'),
    ('40000000-0000-0000-0000-000000000008', 'Độ phức tạp của binary search là bao nhiêu và cần điều kiện gì?', 'Thời gian O(log n) và dữ liệu phải được sắp xếp theo thứ tự mà thuật toán sử dụng.', 'Mỗi bước loại bỏ một nửa không gian tìm kiếm. Nếu dữ liệu chưa sắp xếp, quyết định bỏ nửa nào không còn đúng.', 'MEDIUM', 'ALGORITHMS', '2026-08-22T00:00:00Z'),
    ('40000000-0000-0000-0000-000000000009', 'CORS giải quyết vấn đề gì trên web?', 'CORS cho phép server khai báo origin nào được trình duyệt cho phép đọc response cross-origin.', 'Đây là cơ chế được trình duyệt thực thi dựa trên HTTP header; nó không thay thế authentication hay authorization phía server.', 'MEDIUM', 'WEB', '2026-08-22T00:00:00Z')
ON CONFLICT DO NOTHING;
