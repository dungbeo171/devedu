CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(100) NOT NULL DEFAULT '';
UPDATE users SET name = split_part(email, '@', 1) WHERE name = '';

CREATE SEQUENCE IF NOT EXISTS user_public_id_seq START WITH 1;
ALTER TABLE users ADD COLUMN IF NOT EXISTS public_id BIGINT;
UPDATE users SET public_id = nextval('user_public_id_seq') WHERE public_id IS NULL;
SELECT setval('user_public_id_seq', COALESCE((SELECT MAX(public_id) FROM users), 0) + 1, false);
ALTER TABLE users ALTER COLUMN public_id SET DEFAULT nextval('user_public_id_seq');
ALTER TABLE users ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_public_id ON users (public_id);
DROP INDEX IF EXISTS uk_users_student_code;
DROP INDEX IF EXISTS uk_users_teacher_code;
ALTER TABLE users DROP COLUMN IF EXISTS student_code;
ALTER TABLE users DROP COLUMN IF EXISTS teacher_code;
DROP SEQUENCE IF EXISTS student_code_seq;
DROP SEQUENCE IF EXISTS teacher_code_seq;

-- Remove the retired Interview module and its legacy data.
DROP TABLE IF EXISTS interview_questions;

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
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE courses ADD COLUMN IF NOT EXISTS start_date DATE;
UPDATE courses SET start_date = (created_at AT TIME ZONE 'UTC')::date WHERE start_date IS NULL;
ALTER TABLE courses ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS end_date DATE;
ALTER TABLE courses DROP CONSTRAINT IF EXISTS courses_date_range_check;
ALTER TABLE courses ADD CONSTRAINT courses_date_range_check CHECK (end_date IS NULL OR end_date >= start_date);

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

CREATE TABLE IF NOT EXISTS course_enrollments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    student_id UUID NOT NULL REFERENCES users(id),
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (course_id, student_id)
);
ALTER TABLE course_enrollments ADD COLUMN IF NOT EXISTS display_name VARCHAR(100);

CREATE TABLE IF NOT EXISTS course_materials (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    title VARCHAR(180) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(100) NOT NULL UNIQUE,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_course_enrollments_course ON course_enrollments (course_id, enrolled_at);
CREATE INDEX IF NOT EXISTS idx_course_enrollments_student ON course_enrollments (student_id, enrolled_at);
CREATE INDEX IF NOT EXISTS idx_course_materials_course ON course_materials (course_id, uploaded_at DESC);

CREATE TABLE IF NOT EXISTS programming_problems (
    id UUID PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    input_description TEXT NOT NULL DEFAULT '',
    output_description TEXT NOT NULL DEFAULT '',
    sample_input TEXT NOT NULL DEFAULT '',
    sample_output TEXT NOT NULL DEFAULT '',
    topic VARCHAR(30) NOT NULL CHECK (
        topic IN ('INTRODUCTION', 'CPP', 'JAVA', 'PYTHON', 'OOP', 'DATA_STRUCTURES', 'ALGORITHMS', 'SQL')
    ),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS sample_input TEXT NOT NULL DEFAULT '';
ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS sample_output TEXT NOT NULL DEFAULT '';
ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS input_description TEXT NOT NULL DEFAULT '';
ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS output_description TEXT NOT NULL DEFAULT '';
ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS difficulty VARCHAR(20) NOT NULL DEFAULT 'EASY';
ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS allowed_languages VARCHAR(100) NOT NULL DEFAULT 'CPP,JAVA,PYTHON';
ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS starter_codes TEXT NOT NULL DEFAULT '{}';
ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE programming_problems DROP CONSTRAINT IF EXISTS programming_problems_difficulty_check;
ALTER TABLE programming_problems ADD CONSTRAINT programming_problems_difficulty_check
    CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'));

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

CREATE TABLE IF NOT EXISTS problem_run_statistics (
    problem_id UUID PRIMARY KEY REFERENCES programming_problems(id),
    total_runs BIGINT NOT NULL DEFAULT 0 CHECK (total_runs >= 0),
    successful_runs BIGINT NOT NULL DEFAULT 0 CHECK (successful_runs >= 0 AND successful_runs <= total_runs)
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

CREATE TABLE IF NOT EXISTS problem_drafts (
    id UUID PRIMARY KEY,
    problem_id UUID NOT NULL REFERENCES programming_problems(id),
    student_id UUID NOT NULL REFERENCES users(id),
    language VARCHAR(20) NOT NULL CHECK (language IN ('CPP', 'JAVA', 'PYTHON', 'HTML', 'MYSQL')),
    source_code TEXT NOT NULL DEFAULT '',
    input TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_problem_drafts_student_problem UNIQUE (student_id, problem_id)
);

CREATE TABLE IF NOT EXISTS course_problem_assignments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    problem_id UUID NOT NULL REFERENCES programming_problems(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (course_id, problem_id)
);

CREATE INDEX IF NOT EXISTS idx_course_problem_assignments_course
    ON course_problem_assignments (course_id, assigned_at);

CREATE INDEX IF NOT EXISTS idx_exams_scheduled_at ON exams (scheduled_at);
CREATE INDEX IF NOT EXISTS idx_exams_teacher_scheduled_at ON exams (teacher_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_started_at ON exam_attempts (exam_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_exam_answers_attempt_answered_at ON exam_answers (attempt_id, answered_at);
CREATE INDEX IF NOT EXISTS idx_courses_title ON courses (title);
CREATE INDEX IF NOT EXISTS idx_course_topics_course_position ON course_topics (course_id, position);
CREATE INDEX IF NOT EXISTS idx_lessons_topic_position ON lessons (topic_id, position);
CREATE INDEX IF NOT EXISTS idx_programming_problems_title ON programming_problems (title);
CREATE INDEX IF NOT EXISTS idx_programming_problems_topic_title ON programming_problems (topic, title);
CREATE INDEX IF NOT EXISTS idx_programming_problems_filters
    ON programming_problems (topic, difficulty, title);
CREATE INDEX IF NOT EXISTS idx_problem_submissions_student_submitted_at
    ON problem_submissions (student_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_problem_submissions_problem_submitted_at
    ON problem_submissions (problem_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_problem_submissions_student_accepted_problem
    ON problem_submissions (student_id, problem_id) WHERE status = 'ACCEPTED';
CREATE INDEX IF NOT EXISTS idx_problem_drafts_student_updated_at
    ON problem_drafts (student_id, updated_at DESC);

INSERT INTO programming_problems (id, slug, title, summary, description, sample_input, sample_output, topic, created_at) VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'xin-chao-devedu',
        'Hello DevEdu',
        'Đọc một tên và in ra lời chào đầu tiên của bạn.',
        'Cho một chuỗi name không chứa khoảng trắng. Hãy in ra Hello, name! trên một dòng. Ví dụ input: An. Output tương ứng: Hello, An!',
        E'An\n',
        E'Hello, An!\n',
        'INTRODUCTION',
        '2026-01-01T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'tong-hai-so',
        'C++: Tổng hai số',
        'Luyện nhập xuất cơ bản và phép cộng với C++.',
        'Cho hai số nguyên a và b trên cùng một dòng. Hãy in ra tổng a + b. Giới hạn: trị tuyệt đối của mỗi số không vượt quá 10^9.',
        E'2 3\n',
        E'5\n',
        'CPP',
        '2026-01-02T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'phan-tu-lon-nhat',
        'Java: Phần tử lớn nhất',
        'Tìm giá trị lớn nhất trong một mảng số nguyên.',
        'Dòng đầu chứa số nguyên n. Dòng tiếp theo chứa n số nguyên. Hãy in ra phần tử lớn nhất trong mảng. Giới hạn: 1 <= n <= 100000.',
        E'5\n1 9 -2 7 3\n',
        E'9\n',
        'JAVA',
        '2026-01-03T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        'dem-so-tu',
        'Python: Đếm số từ',
        'Xử lý chuỗi và đếm số từ trong một câu.',
        'Cho một dòng văn bản không rỗng. Các từ được phân tách bởi một hoặc nhiều khoảng trắng. Hãy in ra số từ xuất hiện trong dòng.',
        E'  hoc   lap trinh  moi ngay \n',
        E'6\n',
        'PYTHON',
        '2026-01-04T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000005',
        'tai-khoan-ngan-hang',
        'OOP: Tài khoản ngân hàng',
        'Thiết kế class và đóng gói trạng thái của một tài khoản.',
        'Cài đặt lớp BankAccount có số dư ban đầu, phương thức deposit và withdraw. Chương trình đọc các thao tác rồi in số dư cuối cùng. Không cho phép rút quá số dư hiện có.',
        E'100\n3\ndeposit 50\nwithdraw 30\nwithdraw 150\n',
        E'120\n',
        'OOP',
        '2026-01-05T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000006',
        'mo-phong-ngan-xep',
        'Mô phỏng Stack',
        'Cài đặt các thao tác push, pop và top trên ngăn xếp.',
        'Xử lý q truy vấn trên một stack số nguyên. Với push x, thêm x. Với pop, xóa phần tử trên cùng nếu có. Với top, in phần tử trên cùng hoặc EMPTY nếu stack rỗng.',
        E'7\npush 4\npush 8\ntop\npop\ntop\npop\ntop\n',
        E'8\n4\nEMPTY\n',
        'DATA_STRUCTURES',
        '2026-01-06T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000007',
        'tim-kiem-nhi-phan',
        'Tìm kiếm nhị phân',
        'Tìm vị trí của một giá trị trong mảng đã sắp xếp.',
        'Cho mảng tăng dần gồm n số nguyên và giá trị x. In chỉ số đầu tiên của x theo hệ zero-based, hoặc -1 nếu x không xuất hiện. Yêu cầu độ phức tạp O(log n).',
        E'6\n1 3 3 7 9 11\n3\n',
        E'1\n',
        'ALGORITHMS',
        '2026-01-07T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000008',
        'sinh-vien-diem-cao',
        'SQL: Sinh viên có điểm cao',
        'Viết truy vấn lọc và sắp xếp kết quả học tập.',
        'Bảng students gồm id, name và score. Viết truy vấn trả về name và score của các sinh viên có score từ 8 trở lên, sắp xếp score giảm dần rồi name tăng dần.',
        E'CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100), score INT);\nINSERT INTO students VALUES (1, ''An'', 9), (2, ''Binh'', 8), (3, ''Chi'', 7);\n',
        E'An\t9\nBinh\t8\n',
        'SQL',
        '2026-01-08T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000009', 'kiem-tra-chan-le', 'Kiểm tra chẵn lẻ',
        'Luyện câu lệnh điều kiện với một số nguyên.',
        'Cho số nguyên n. In EVEN nếu n chẵn, ngược lại in ODD.',
        E'7\n', E'ODD\n', 'INTRODUCTION', '2026-01-09T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000010', 'dien-tich-hinh-chu-nhat', 'Diện tích hình chữ nhật',
        'Đọc hai số nguyên và tính diện tích.',
        'Cho chiều rộng w và chiều cao h là hai số nguyên dương. In ra diện tích w * h.',
        E'4 6\n', E'24\n', 'INTRODUCTION', '2026-01-10T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000011', 'dao-nguoc-mang', 'C++: Đảo ngược mảng',
        'Luyện vector và duyệt mảng theo chiều ngược lại.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên. In các phần tử theo thứ tự đảo ngược, cách nhau bởi một khoảng trắng.',
        E'5\n1 2 3 4 5\n', E'5 4 3 2 1\n', 'CPP', '2026-01-11T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000012', 'dem-so-duong', 'C++: Đếm số dương',
        'Đếm các phần tử lớn hơn 0 trong mảng.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên. In số lượng phần tử dương.',
        E'6\n-2 0 3 5 -1 4\n', E'3\n', 'CPP', '2026-01-12T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000013', 'tong-phan-tu-mang', 'Java: Tổng phần tử mảng',
        'Luyện Scanner, mảng và kiểu long trong Java.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên. In tổng tất cả phần tử.',
        E'5\n2 -1 4 3 2\n', E'10\n', 'JAVA', '2026-01-13T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000014', 'chuoi-doi-xung', 'Java: Chuỗi đối xứng',
        'Kiểm tra một chuỗi có đọc xuôi và ngược giống nhau.',
        'Cho một chuỗi không chứa khoảng trắng. In YES nếu chuỗi là palindrome, ngược lại in NO.',
        E'level\n', E'YES\n', 'JAVA', '2026-01-14T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000015', 'dem-nguyen-am', 'Python: Đếm nguyên âm',
        'Xử lý chuỗi và đếm ký tự bằng Python.',
        'Cho một dòng chỉ gồm chữ cái Latin. Đếm các nguyên âm a, e, i, o, u, không phân biệt hoa thường.',
        E'DevEdu\n', E'3\n', 'PYTHON', '2026-01-15T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000016', 'so-fibonacci', 'Python: Số Fibonacci',
        'Tính số Fibonacci thứ n bằng vòng lặp.',
        'Cho n với 0 <= n <= 90. Biết F0 = 0, F1 = 1. In Fn.',
        E'10\n', E'55\n', 'PYTHON', '2026-01-16T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000017', 'diem-trung-binh-sinh-vien', 'OOP: Điểm trung bình sinh viên',
        'Mô hình hóa sinh viên và tính điểm trung bình.',
        'Tạo class Student lưu ba điểm nguyên. Đọc ba điểm, tạo object và in phần nguyên của điểm trung bình.',
        E'8 9 7\n', E'8\n', 'OOP', '2026-01-17T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000018', 'lop-hinh-chu-nhat', 'OOP: Lớp Rectangle',
        'Đóng gói chiều rộng, chiều cao và hành vi tính diện tích.',
        'Tạo class Rectangle nhận width và height. Đọc hai số nguyên, tạo object và in diện tích qua một phương thức của class.',
        E'4 5\n', E'20\n', 'OOP', '2026-01-18T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000019', 'mo-phong-hang-doi', 'Mô phỏng Queue',
        'Cài đặt các thao tác push, pop và front trên hàng đợi.',
        'Xử lý q truy vấn. push x thêm x vào cuối; pop xóa đầu nếu có; front in phần tử đầu hoặc EMPTY nếu rỗng.',
        E'7\npush 4\npush 8\nfront\npop\nfront\npop\nfront\n', E'4\n8\nEMPTY\n', 'DATA_STRUCTURES', '2026-01-19T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000020', 'dau-ngoac-hop-le', 'Dấu ngoặc hợp lệ',
        'Dùng stack để kiểm tra thứ tự các cặp dấu ngoặc.',
        'Cho chuỗi chỉ gồm (), [] và {}. In YES nếu mọi dấu ngoặc đóng mở đúng thứ tự, ngược lại in NO.',
        E'([]{})\n', E'YES\n', 'DATA_STRUCTURES', '2026-01-20T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000021', 'uoc-chung-lon-nhat', 'Ước chung lớn nhất',
        'Áp dụng thuật toán Euclid.',
        'Cho hai số nguyên dương a và b. In ước chung lớn nhất của chúng.',
        E'48 18\n', E'6\n', 'ALGORITHMS', '2026-01-21T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000022', 'sap-xep-tang-dan', 'Sắp xếp tăng dần',
        'Sắp xếp một dãy số nguyên theo thứ tự tăng dần.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên. In dãy đã sắp xếp tăng dần, cách nhau bởi một khoảng trắng.',
        E'5\n5 1 4 2 3\n', E'1 2 3 4 5\n', 'ALGORITHMS', '2026-01-22T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000023', 'dem-sinh-vien', 'SQL: Đếm sinh viên',
        'Sử dụng hàm tổng hợp COUNT.',
        'Bảng students gồm id và name. Viết truy vấn trả về số lượng sinh viên trong bảng.',
        E'CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100));\nINSERT INTO students VALUES (1, ''An''), (2, ''Binh''), (3, ''Chi'');\n', E'3\n', 'SQL', '2026-01-23T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000024', 'diem-cao-nhat', 'SQL: Điểm cao nhất',
        'Kết hợp sắp xếp và giới hạn kết quả.',
        'Bảng students gồm id, name và score. Trả về name và score của sinh viên có điểm cao nhất. Nếu bằng điểm, ưu tiên name tăng dần.',
        E'CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100), score INT);\nINSERT INTO students VALUES (1, ''An'', 9), (2, ''Binh'', 8), (3, ''Chi'', 9);\n', E'An\t9\n', 'SQL', '2026-01-24T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000025', 'tieu-de-devedu', 'HTML: Tiêu đề DevEdu',
        'Tạo một tiêu đề cấp một đơn giản bằng HTML.',
        'Xuất chính xác thẻ h1 có nội dung DevEdu: <h1>DevEdu</h1>. Bài này chỉ sử dụng HTML.',
        '', E'<h1>DevEdu</h1>\n', 'INTRODUCTION', '2026-01-25T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000026', 'tong-tu-mot-den-n', 'Tổng từ 1 đến n',
        'Luyện vòng lặp và công thức tổng cơ bản.',
        'Cho số nguyên dương n không vượt quá 10^9. In tổng 1 + 2 + ... + n.',
        E'10\n', E'55\n', 'INTRODUCTION', '2026-01-26T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000027', 'phan-tu-phan-biet', 'C++: Phần tử phân biệt',
        'Đếm số giá trị khác nhau bằng STL.',
        'Dòng đầu chứa n, dòng sau chứa n số nguyên. In số lượng giá trị phân biệt.',
        E'6\n1 2 2 3 1 4\n', E'4\n', 'CPP', '2026-01-27T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000028', 'tong-doan', 'C++: Tổng đoạn',
        'Dùng prefix sum để trả lời nhiều truy vấn.',
        'Cho n số nguyên và q truy vấn l r theo chỉ số 1-based. Với mỗi truy vấn, in tổng đoạn từ l đến r.',
        E'5 2\n1 2 3 4 5\n1 3\n2 5\n', E'6\n14\n', 'CPP', '2026-01-28T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000029', 'tan-suat-phan-tu', 'Java: Tần suất phần tử',
        'Đếm tần suất bằng HashMap.',
        'Cho n số nguyên và một giá trị x. In số lần x xuất hiện trong dãy.',
        E'7\n1 2 3 2 4 2 5\n2\n', E'3\n', 'JAVA', '2026-01-29T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000030', 'tong-duong-cheo', 'Java: Tổng đường chéo',
        'Duyệt ma trận vuông trong Java.',
        'Cho ma trận vuông n x n. In tổng các phần tử trên đường chéo chính.',
        E'3\n1 2 3\n4 5 6\n7 8 9\n', E'15\n', 'JAVA', '2026-01-30T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000031', 'hai-chuoi-hoan-vi', 'Python: Hai chuỗi hoán vị',
        'So sánh tần suất ký tự của hai chuỗi.',
        'Cho hai chuỗi chữ thường không có khoảng trắng. In YES nếu chúng là hoán vị của nhau, ngược lại in NO.',
        E'listen\nsilent\n', E'YES\n', 'PYTHON', '2026-01-31T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000032', 'so-lon-thu-hai', 'Python: Số lớn thứ hai',
        'Tìm giá trị phân biệt lớn thứ hai.',
        'Cho n số nguyên có ít nhất hai giá trị phân biệt. In giá trị lớn thứ hai.',
        E'6\n4 9 2 9 7 4\n', E'7\n', 'PYTHON', '2026-02-01T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000033', 'luong-nhan-vien', 'OOP: Lương nhân viên',
        'Đóng gói lương cơ bản và phần thưởng.',
        'Tạo lớp Employee lưu baseSalary và bonus. Đọc hai số nguyên rồi in tổng lương qua phương thức của object.',
        E'1000 250\n', E'1250\n', 'OOP', '2026-02-02T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000034', 'da-hinh-dien-tich', 'OOP: Đa hình diện tích',
        'Áp dụng abstraction và polymorphism cho các hình.',
        'Đọc loại RECTANGLE hoặc SQUARE cùng kích thước nguyên. Tạo object hình phù hợp và in diện tích.',
        E'RECTANGLE 4 5\n', E'20\n', 'OOP', '2026-02-03T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000035', 'mo-phong-hang-doi-hai-dau', 'Mô phỏng Deque',
        'Thao tác thêm và lấy ở hai đầu hàng đợi.',
        'Xử lý push_front x, push_back x, pop_front, pop_back và front. Lệnh front in phần tử đầu hoặc EMPTY.',
        E'6\npush_back 2\npush_front 1\nfront\npop_front\nfront\npop_back\n', E'1\n2\n', 'DATA_STRUCTURES', '2026-02-04T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000036', 'phan-tu-lon-hon-ke-tiep', 'Phần tử lớn hơn kế tiếp',
        'Dùng monotonic stack để xử lý dãy.',
        'Với mỗi phần tử, in phần tử đầu tiên lớn hơn nó ở bên phải hoặc -1 nếu không có.',
        E'5\n2 1 5 3 4\n', E'5 5 -1 4 -1\n', 'DATA_STRUCTURES', '2026-02-05T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000037', 'hai-so-co-tong-bang-muc-tieu', 'Hai số có tổng bằng target',
        'Tìm một cặp chỉ số bằng hash map.',
        'Cho n số nguyên và target. In hai chỉ số 0-based đầu tiên i < j có tổng bằng target, hoặc -1 nếu không có.',
        E'4\n2 7 11 15\n9\n', E'0 1\n', 'ALGORITHMS', '2026-02-06T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000038', 'duong-di-ngan-nhat-theo-chieu-rong', 'Đường đi ngắn nhất BFS',
        'Tìm khoảng cách trên đồ thị vô hướng không trọng số.',
        'Cho n, m, s, t và m cạnh. In số cạnh ít nhất từ s đến t hoặc -1 nếu không thể tới.',
        E'5 5 1 5\n1 2\n2 5\n1 3\n3 4\n4 5\n', E'2\n', 'ALGORITHMS', '2026-02-07T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000039', 'diem-trung-binh-theo-lop', 'SQL: Điểm trung bình theo lớp',
        'Nhóm dữ liệu bằng GROUP BY và AVG.',
        'Bảng students có class_name và score. Trả class_name cùng điểm trung bình, sắp xếp class_name tăng dần.',
        E'CREATE TABLE students (id INT, class_name VARCHAR(20), score INT);\nINSERT INTO students VALUES (1, ''A'', 8), (2, ''A'', 10), (3, ''B'', 7);\n', E'A\t9.0000\nB\t7.0000\n', 'SQL', '2026-02-08T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000040', 'khach-hang-va-don-hang', 'SQL: Khách hàng và đơn hàng',
        'Kết hợp JOIN và SUM trên hai bảng.',
        'Bảng customers và orders. Trả tên khách hàng cùng tổng giá trị đơn hàng, chỉ lấy tổng từ 100 trở lên và sắp xếp tên.',
        E'CREATE TABLE customers (id INT, name VARCHAR(50));\nCREATE TABLE orders (id INT, customer_id INT, amount INT);\nINSERT INTO customers VALUES (1, ''An''), (2, ''Binh'');\nINSERT INTO orders VALUES (1, 1, 60), (2, 1, 50), (3, 2, 40);\n', E'An\t110\n', 'SQL', '2026-02-09T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000041', 'so-nguyen-to', 'Số nguyên tố',
        'Kiểm tra một số có phải số nguyên tố hay không.',
        'Cho số nguyên n. In YES nếu n là số nguyên tố, ngược lại in NO.',
        E'17\n', E'YES\n', 'INTRODUCTION', '2026-03-01T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000042', 'dem-boi-so-cua-ba', 'Đếm bội số của ba',
        'Đếm các phần tử chia hết cho 3 trong một dãy số.',
        'Dòng đầu chứa n, dòng sau chứa n số nguyên. In số phần tử chia hết cho 3.',
        E'7\n1 3 5 6 8 9 10\n', E'3\n', 'INTRODUCTION', '2026-03-02T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000043', 'ucln-cua-hai-so', 'Ước chung lớn nhất',
        'Tính ước chung lớn nhất bằng thuật toán Euclid.',
        'Cho hai số nguyên dương a và b. In ước chung lớn nhất của chúng.',
        E'84 30\n', E'6\n', 'INTRODUCTION', '2026-03-03T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000044', 'dao-nguoc-so-nguyen', 'Đảo ngược số nguyên',
        'Luyện vòng lặp và phép chia lấy dư.',
        'Cho số nguyên dương n. In số nhận được khi viết các chữ số của n theo thứ tự ngược lại.',
        E'12040\n', E'4021\n', 'INTRODUCTION', '2026-03-04T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000045', 'tong-chu-so', 'Tổng chữ số',
        'Tính tổng các chữ số của một số nguyên.',
        'Cho số nguyên không âm n. In tổng tất cả chữ số của n.',
        E'2026\n', E'10\n', 'INTRODUCTION', '2026-03-05T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000046', 'so-doi-xung', 'Số đối xứng',
        'Nhận biết số đọc xuôi và ngược giống nhau.',
        'Cho số nguyên dương n. In YES nếu n là số đối xứng, ngược lại in NO.',
        E'1221\n', E'YES\n', 'INTRODUCTION', '2026-03-06T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000047', 'sap-xep-chon', 'Sắp xếp chọn',
        'Cài đặt thuật toán selection sort.',
        'Cho n số nguyên. Sắp xếp dãy theo thứ tự tăng dần và in trên một dòng.',
        E'6\n5 2 8 1 4 3\n', E'1 2 3 4 5 8\n', 'CPP', '2026-03-07T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000048', 'doan-con-lien-tiep-lon-nhat', 'Đoạn con liên tiếp lớn nhất',
        'Tìm tổng lớn nhất của một đoạn con liên tiếp.',
        'Cho n số nguyên. In tổng lớn nhất của một đoạn con liên tiếp không rỗng.',
        E'8\n-2 3 -1 5 -6 4 2 -1\n', E'7\n', 'CPP', '2026-03-08T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000049', 'ma-tran-xoan-oc', 'Ma trận xoắn ốc',
        'Duyệt ma trận theo đường xoắn ốc.',
        'Cho ma trận n x m. In các phần tử theo thứ tự xoắn ốc, bắt đầu từ góc trên bên trái.',
        E'3 3\n1 2 3\n4 5 6\n7 8 9\n', E'1 2 3 6 9 8 7 4 5\n', 'CPP', '2026-03-09T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000050', 'tron-hai-day', 'Trộn hai dãy đã sắp xếp',
        'Kết hợp hai dãy tăng dần bằng kỹ thuật hai con trỏ.',
        'Cho hai dãy đã sắp xếp tăng dần. Trộn chúng thành một dãy tăng dần và in kết quả.',
        E'4 3\n1 4 7 10\n2 3 9\n', E'1 2 3 4 7 9 10\n', 'CPP', '2026-03-10T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000051', 'chuoi-dai-nhat-java', 'Chuỗi dài nhất',
        'Tìm chuỗi có độ dài lớn nhất trong danh sách.',
        'Dòng đầu chứa n, tiếp theo là n chuỗi không có khoảng trắng. In chuỗi dài nhất đầu tiên.',
        E'4\ncat\nprogramming\njava\ncode\n', E'programming\n', 'JAVA', '2026-03-11T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000052', 'sap-xep-tan-suat-java', 'Sắp xếp theo tần suất',
        'Đếm tần suất và sắp xếp dữ liệu trong Java.',
        'Cho n số nguyên. In các số theo tần suất giảm dần; nếu bằng nhau, số nhỏ hơn đứng trước.',
        E'7\n4 5 6 5 4 4 6\n', E'4 4 4 5 5 6 6\n', 'JAVA', '2026-03-12T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000053', 'loai-bo-ky-tu-trung-java', 'Loại bỏ ký tự trùng',
        'Xây dựng chuỗi mới bằng cách giữ lần xuất hiện đầu tiên.',
        'Cho một chuỗi chữ thường không có khoảng trắng. In chuỗi sau khi loại bỏ các ký tự trùng lặp.',
        E'programming\n', E'progamin\n', 'JAVA', '2026-03-13T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000054', 'chuyen-doi-nhiet-do-java', 'Chuyển đổi nhiệt độ',
        'Thực hành đọc số thực và định dạng kết quả.',
        'Cho nhiệt độ Celsius c. In nhiệt độ Fahrenheit theo công thức F = c * 9 / 5 + 32 với đúng hai chữ số thập phân.',
        E'25\n', E'77.00\n', 'JAVA', '2026-03-14T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000055', 'dem-tu-java', 'Đếm từ trong câu',
        'Tách và đếm các từ trong một câu.',
        'Cho một dòng văn bản có thể có nhiều khoảng trắng ở đầu, cuối và giữa các từ. In số từ trong câu.',
        E'  hoc   lap trinh Java  \n', E'4\n', 'JAVA', '2026-03-15T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000056', 'tan-suat-ky-tu-python', 'Tần suất ký tự',
        'Đếm ký tự xuất hiện nhiều nhất trong chuỗi.',
        'Cho chuỗi chữ thường không có khoảng trắng. In ký tự xuất hiện nhiều nhất; nếu hòa, chọn ký tự nhỏ hơn.',
        E'banana\n', E'a\n', 'PYTHON', '2026-03-16T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000057', 'tu-dai-nhat-python', 'Từ dài nhất',
        'Tìm từ đầu tiên có độ dài lớn nhất trong câu.',
        'Cho một dòng gồm các từ cách nhau bởi khoảng trắng. In từ dài nhất đầu tiên.',
        E' hoc lap trinh python \n', E'python\n', 'PYTHON', '2026-03-17T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000058', 'xoay-mang-python', 'Xoay mảng sang phải',
        'Thực hành slicing và chỉ số mảng trong Python.',
        'Cho n, d và dãy số. Xoay dãy sang phải d vị trí rồi in kết quả.',
        E'5 2\n1 2 3 4 5\n', E'4 5 1 2 3\n', 'PYTHON', '2026-03-18T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000059', 'kiem-tra-ngoac-python', 'Kiểm tra ngoặc',
        'Dùng stack để kiểm tra chuỗi ngoặc hợp lệ.',
        'Cho chuỗi chỉ gồm các ký tự ngoặc (), [], {}. In YES nếu chuỗi hợp lệ, ngược lại in NO.',
        E'{[()]}\n', E'YES\n', 'PYTHON', '2026-03-19T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000060', 'day-con-tang-dai-nhat-python', 'Dãy con tăng dài nhất',
        'Tìm độ dài dãy con tăng nghiêm ngặt dài nhất.',
        'Cho n số nguyên. In độ dài dãy con tăng nghiêm ngặt dài nhất.',
        E'6\n10 9 2 5 3 7\n', E'3\n', 'PYTHON', '2026-03-20T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000061', 'mo-phong-tai-khoan', 'Mô phỏng tài khoản',
        'Áp dụng đóng gói qua các thao tác trên tài khoản.',
        'Tài khoản có số dư ban đầu. Xử lý deposit x và withdraw x; giao dịch rút vượt số dư bị bỏ qua. In số dư cuối.',
        E'100\n5\ndeposit 50\nwithdraw 30\nwithdraw 150\ndeposit 20\nwithdraw 10\n', E'130\n', 'OOP', '2026-03-21T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000062', 'hinh-tron-oop', 'Diện tích hình tròn',
        'Mô hình hóa hình tròn bằng một lớp đơn giản.',
        'Cho bán kính r là số thực. In diện tích hình tròn theo công thức pi * r * r với hai chữ số thập phân.',
        E'2\n', E'12.57\n', 'OOP', '2026-03-22T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000063', 'hang-doi-uu-tien', 'Hàng đợi ưu tiên',
        'Sắp xếp các phần tử theo độ ưu tiên.',
        'Cho n cặp priority value. In value theo priority giảm dần; nếu bằng nhau, value nhỏ hơn trước.',
        E'4\n2 40\n5 10\n5 8\n1 99\n', E'8 10 40 99\n', 'DATA_STRUCTURES', '2026-03-23T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000064', 'danh-sach-lien-ket', 'Danh sách liên kết',
        'Mô phỏng thao tác thêm và xóa trong danh sách.',
        'Xử lý append x, remove x và print trên danh sách liên kết. Lệnh print in các phần tử hoặc EMPTY.',
        E'6\nappend 3\nappend 5\nremove 3\nappend 7\nprint\nremove 9\n', E'5 7\n', 'DATA_STRUCTURES', '2026-03-24T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000065', 'tim-kiem-nhi-phan-dau-tien', 'Vị trí xuất hiện đầu tiên',
        'Tìm vị trí đầu tiên của giá trị trong dãy đã sắp xếp.',
        'Cho dãy tăng dần và giá trị x. In chỉ số 0-based đầu tiên của x hoặc -1 nếu không tồn tại.',
        E'7\n1 2 2 2 5 8 9\n2\n', E'1\n', 'ALGORITHMS', '2026-03-25T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000066', 'giao-nhau-doan', 'Giao của các đoạn',
        'Tìm phần giao chung của nhiều đoạn trên trục số.',
        'Cho n đoạn đóng [l, r]. In đoạn giao chung lớn nhất hoặc -1 nếu các đoạn không giao nhau.',
        E'3\n1 10\n3 8\n5 12\n', E'5 8\n', 'ALGORITHMS', '2026-03-26T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000067', 'so-cach-leo-cau-thang', 'Số cách leo cầu thang',
        'Giải bài toán quy hoạch động cơ bản.',
        'Có n bậc thang. Mỗi bước được đi 1 hoặc 2 bậc. In số cách đi lên bậc thứ n, với n không quá 30.',
        E'5\n', E'8\n', 'ALGORITHMS', '2026-03-27T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000068', 'dem-sinh-vien-theo-lop', 'Đếm sinh viên theo lớp',
        'Nhóm dữ liệu và đếm số bản ghi trong SQL.',
        'Bảng students gồm id và class_name. Trả class_name cùng số sinh viên, sắp xếp class_name tăng dần.',
        E'CREATE TABLE students (id INT, class_name VARCHAR(20));\nINSERT INTO students VALUES (1, ''A''), (2, ''B''), (3, ''A''), (4, ''C''), (5, ''B'');\n', E'A\t2\nB\t2\nC\t1\n', 'SQL', '2026-03-28T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000069', 'tong-luong-phong-ban', 'Tổng lương phòng ban',
        'Tính tổng và sắp xếp kết quả theo nhóm.',
        'Bảng employees gồm id, department và salary. Trả department cùng tổng salary, sắp xếp tổng giảm dần.',
        E'CREATE TABLE employees (id INT, department VARCHAR(20), salary INT);\nINSERT INTO employees VALUES (1, ''IT'', 100), (2, ''HR'', 80), (3, ''IT'', 120);\n', E'IT\t220\nHR\t80\n', 'SQL', '2026-03-29T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000070', 'san-pham-chua-ban', 'Sản phẩm chưa bán',
        'Tìm bản ghi không có quan hệ bằng LEFT JOIN.',
        'Bảng products và order_items. In tên sản phẩm chưa từng xuất hiện trong order_items theo thứ tự alphabet.',
        E'CREATE TABLE products (id INT, name VARCHAR(50));\nCREATE TABLE order_items (product_id INT);\nINSERT INTO products VALUES (1, ''Keyboard''), (2, ''Mouse''), (3, ''Monitor'');\nINSERT INTO order_items VALUES (1), (3);\n', E'Mouse\n', 'SQL', '2026-03-30T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000071', 'so-chinh-phuong', 'Số chính phương',
        'Kiểm tra một số có phải bình phương của số nguyên hay không.',
        'Cho số nguyên không âm n. In YES nếu n là số chính phương, ngược lại in NO.',
        E'49\n', E'YES\n', 'INTRODUCTION', '2026-04-01T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000072', 'dem-chu-so-le', 'Đếm chữ số lẻ',
        'Đếm số chữ số lẻ trong một số nguyên dương.',
        'Cho số nguyên dương n. In số lượng chữ số lẻ xuất hiện trong n.',
        E'12345\n', E'3\n', 'INTRODUCTION', '2026-04-02T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000073', 'boi-chung-nho-nhat', 'Bội chung nhỏ nhất',
        'Tính bội chung nhỏ nhất từ ước chung lớn nhất.',
        'Cho hai số nguyên dương a và b. In bội chung nhỏ nhất của chúng.',
        E'12 18\n', E'36\n', 'INTRODUCTION', '2026-04-03T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000074', 'phan-tich-thua-so', 'Phân tích thừa số nguyên tố',
        'Tách một số thành các thừa số nguyên tố tăng dần.',
        'Cho số nguyên n lớn hơn 1. In các thừa số nguyên tố của n theo thứ tự không giảm, cách nhau bởi dấu cách.',
        E'60\n', E'2 2 3 5\n', 'INTRODUCTION', '2026-04-04T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000075', 'kiem-tra-tam-giac', 'Kiểm tra tam giác',
        'Kiểm tra điều kiện tạo thành tam giác.',
        'Cho ba cạnh a, b, c. In YES nếu ba cạnh tạo thành một tam giác không suy biến, ngược lại in NO.',
        E'3 4 5\n', E'YES\n', 'INTRODUCTION', '2026-04-05T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000076', 'tong-ma-tran', 'Tổng hai ma trận',
        'Cộng từng phần tử tương ứng của hai ma trận.',
        'Cho hai ma trận cùng kích thước n x m. In ma trận tổng theo từng dòng.',
        E'2 2\n1 2\n3 4\n5 6\n7 8\n', E'6 8\n10 12\n', 'CPP', '2026-04-06T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000077', 'day-con-dai-nhat-khong-giam', 'Dãy con không giảm dài nhất',
        'Tìm độ dài dãy con không giảm dài nhất.',
        'Cho n số nguyên. In độ dài dãy con không giảm dài nhất của dãy.',
        E'8\n1 3 2 2 4 3 5 6\n', E'6\n', 'CPP', '2026-04-07T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000078', 'dem-cap-nghich-the', 'Đếm cặp nghịch thế',
        'Đếm số cặp i nhỏ hơn j nhưng a[i] lớn hơn a[j].',
        'Cho n số nguyên. In số cặp nghịch thế trong dãy.',
        E'5\n2 4 1 3 5\n', E'3\n', 'CPP', '2026-04-08T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000079', 'mat-do-ky-tu', 'Mật độ ký tự',
        'Đếm số chữ cái trong một dòng văn bản.',
        'Cho một dòng văn bản gồm chữ cái, chữ số và khoảng trắng. In số lượng ký tự alphabet trong dòng.',
        E'DevEdu 2026\n', E'6\n', 'CPP', '2026-04-09T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000080', 'ma-hoa-dich-chuyen', 'Mã hóa dịch chuyển',
        'Dịch các chữ cái thường trong chuỗi theo một số bước.',
        'Cho chuỗi chữ thường và số k. Dịch mỗi chữ cái sang phải k vị trí trong bảng alphabet, có quay vòng.',
        E'abcxyz 2\n', E'cdezab\n', 'CPP', '2026-04-10T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000081', 'tong-cac-so-le-java', 'Tổng các số lẻ',
        'Tính tổng các phần tử lẻ trong dãy bằng Java.',
        'Cho n số nguyên. In tổng các số lẻ trong dãy; nếu không có số lẻ, in 0.',
        E'6\n2 5 8 7 4 1\n', E'13\n', 'JAVA', '2026-04-11T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000082', 'kiem-tra-so-armstrong', 'Kiểm tra số Armstrong',
        'Kiểm tra tổng lũy thừa các chữ số có bằng chính số đó.',
        'Cho số nguyên dương n có ba chữ số. In YES nếu n là số Armstrong, ngược lại in NO.',
        E'153\n', E'YES\n', 'JAVA', '2026-04-12T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000083', 'gop-chuoi-java', 'Gộp chuỗi',
        'Nối các từ theo đúng thứ tự xuất hiện.',
        'Dòng đầu chứa n, tiếp theo là n từ không có khoảng trắng. In một chuỗi gồm các từ nối liền nhau.',
        E'4\nDev\nEdu\nCode\n2026\n', E'DevEduCode2026\n', 'JAVA', '2026-04-13T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000084', 'ma-tran-chuyen-vi-java', 'Ma trận chuyển vị',
        'Đổi hàng thành cột của ma trận.',
        'Cho ma trận n x m. In ma trận chuyển vị kích thước m x n.',
        E'2 3\n1 2 3\n4 5 6\n', E'1 4\n2 5\n3 6\n', 'JAVA', '2026-04-14T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000085', 'ngay-trong-thang', 'Số ngày trong tháng',
        'Xác định số ngày của một tháng theo năm nhuận.',
        'Cho năm y và tháng m. In số ngày của tháng m trong năm y.',
        E'2024 2\n', E'29\n', 'JAVA', '2026-04-15T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000086', 'day-con-tong-muc-tieu', 'Dãy con có tổng mục tiêu',
        'Tìm đoạn con liên tiếp có tổng bằng target.',
        'Cho n, target và dãy số nguyên dương. In chỉ số 1-based l r của đoạn con đầu tiên có tổng bằng target, hoặc -1.',
        E'5 6\n2 3 1 2 4\n', E'2 4\n', 'PYTHON', '2026-04-16T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000087', 'dem-doan-lien-tiep', 'Đếm đoạn liên tiếp',
        'Đếm số đoạn toàn số dương liên tiếp.',
        'Cho n số nguyên. In số đoạn con liên tiếp chỉ gồm các số dương.',
        E'6\n1 2 -1 3 4 5\n', E'9\n', 'PYTHON', '2026-04-17T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000088', 'sap-xep-tu', 'Sắp xếp từ',
        'Sắp xếp các từ theo thứ tự từ điển.',
        'Cho một dòng gồm các từ cách nhau bởi khoảng trắng. In các từ theo thứ tự alphabet, cách nhau bởi một dấu cách.',
        E'code learn python java\n', E'code java learn python\n', 'PYTHON', '2026-04-18T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000089', 'tach-so-am', 'Tách số âm',
        'Đếm và tính tổng các số âm trong dãy.',
        'Cho n số nguyên. In số lượng số âm và tổng các số âm trên cùng một dòng.',
        E'6\n-2 5 -7 0 -1 4\n', E'3 -10\n', 'PYTHON', '2026-04-19T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000090', 'ma-tran-chuyen-vi-python', 'Ma trận chuyển vị Python',
        'Dùng list comprehension để chuyển vị ma trận.',
        'Cho ma trận n x m. In ma trận chuyển vị kích thước m x n.',
        E'2 2\n1 2\n3 4\n', E'1 3\n2 4\n', 'PYTHON', '2026-04-20T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000091', 'hinh-chu-nhat-oop', 'Hình chữ nhật OOP',
        'Tính chu vi và diện tích qua lớp hình chữ nhật.',
        'Cho chiều dài và chiều rộng. In diện tích và chu vi của hình chữ nhật trên cùng một dòng.',
        E'4 6\n', E'24 20\n', 'OOP', '2026-04-21T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000092', 'nhan-vien-thuong', 'Nhân viên và thưởng',
        'Tính thu nhập bằng phương thức của đối tượng.',
        'Cho lương cơ bản và phần trăm thưởng. In tổng thu nhập, làm tròn xuống đến số nguyên gần nhất.',
        E'1000 10\n', E'1100\n', 'OOP', '2026-04-22T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000093', 'kiem-tra-palindrome-stack', 'Palindrome bằng stack',
        'Kiểm tra chuỗi đối xứng bằng cấu trúc stack.',
        'Cho chuỗi chữ thường không có khoảng trắng. In YES nếu chuỗi là palindrome, ngược lại in NO.',
        E'abccba\n', E'YES\n', 'DATA_STRUCTURES', '2026-04-23T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000094', 'vung-lien-thong', 'Số thành phần liên thông',
        'Đếm thành phần liên thông trong đồ thị vô hướng.',
        'Cho đồ thị vô hướng gồm n đỉnh và m cạnh. In số thành phần liên thông.',
        E'5 3\n1 2\n2 3\n4 5\n', E'2\n', 'DATA_STRUCTURES', '2026-04-24T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000095', 'truy-van-hang-doi', 'Truy vấn hàng đợi',
        'Mô phỏng hàng đợi với nhiều thao tác.',
        'Xử lý enqueue x, dequeue và size. Lệnh dequeue bỏ phần tử đầu nếu có; size in kích thước hiện tại.',
        E'6\nenqueue 4\nenqueue 7\nsize\ndequeue\nsize\n', E'2\n1\n', 'DATA_STRUCTURES', '2026-04-25T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000096', 'tron-sort', 'Trộn sort',
        'Sắp xếp dãy bằng thuật toán merge sort.',
        'Cho n số nguyên. Sắp xếp dãy tăng dần bằng merge sort và in kết quả.',
        E'7\n9 1 5 3 8 2 4\n', E'1 2 3 4 5 8 9\n', 'ALGORITHMS', '2026-04-26T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000097', 'quy-hoach-duong-di', 'Quy hoạch đường đi',
        'Tìm số đường đi trong lưới bằng quy hoạch động.',
        'Trên lưới n x m chỉ được đi sang phải hoặc xuống dưới. In số đường đi từ ô đầu đến ô cuối.',
        E'3 4\n', E'10\n', 'ALGORITHMS', '2026-04-27T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000098', 'doi-tien-it-xu-nhat', 'Đổi tiền ít xu nhất',
        'Tìm số đồng xu ít nhất để đạt một giá trị tiền.',
        'Cho n loại xu và số tiền target. In số đồng xu ít nhất cần dùng hoặc -1 nếu không thể đổi.',
        E'3 11\n1 2 5\n', E'3\n', 'ALGORITHMS', '2026-04-28T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000099', 'sinh-vien-dat-diem-cao', 'Sinh viên điểm cao',
        'Lọc sinh viên đạt điểm từ ngưỡng cho trước.',
        'Bảng students gồm name và score. Trả tên sinh viên có score từ 8 trở lên theo thứ tự name.',
        E'CREATE TABLE students (id INT, name VARCHAR(50), score INT);\nINSERT INTO students VALUES (1, ''An'', 9), (2, ''Binh'', 7), (3, ''Chi'', 8);\n', E'An\nChi\n', 'SQL', '2026-04-29T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000100', 'doanh-thu-theo-thang', 'Doanh thu theo tháng',
        'Tổng hợp doanh thu theo tháng trong SQL.',
        'Bảng orders gồm id, order_month và amount. Trả order_month cùng tổng amount, sắp xếp tháng tăng dần.',
        E'CREATE TABLE orders (id INT, order_month INT, amount INT);\nINSERT INTO orders VALUES (1, 1, 100), (2, 2, 80), (3, 1, 50);\n', E'1\t150\n2\t80\n', 'SQL', '2026-04-30T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000101', 'tich-cac-chu-so', 'Tích các chữ số',
        'Tính tích tất cả chữ số của một số nguyên.',
        'Cho số nguyên dương n. In tích các chữ số của n.',
        E'123\n', E'6\n', 'INTRODUCTION', '2026-05-01T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000102', 'so-lon-nhat-ba-so', 'Số lớn nhất trong ba số',
        'So sánh ba số nguyên bằng cấu trúc điều kiện.',
        'Cho ba số nguyên a, b, c. In số lớn nhất trong ba số.',
        E'8 3 10\n', E'10\n', 'INTRODUCTION', '2026-05-02T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000103', 'dem-uoc-so', 'Đếm ước số',
        'Đếm số lượng ước dương của một số.',
        'Cho số nguyên dương n. In số lượng ước dương của n.',
        E'12\n', E'6\n', 'INTRODUCTION', '2026-05-03T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000104', 'doi-co-so-nhi-phan', 'Đổi sang cơ số nhị phân',
        'Chuyển một số thập phân sang biểu diễn nhị phân.',
        'Cho số nguyên không âm n. In biểu diễn nhị phân của n, không có số 0 ở đầu trừ trường hợp n bằng 0.',
        E'13\n', E'1101\n', 'INTRODUCTION', '2026-05-04T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000105', 'tinh-giai-thua', 'Tính giai thừa',
        'Tính n giai thừa bằng vòng lặp.',
        'Cho số nguyên n từ 0 đến 12. In n! .',
        E'5\n', E'120\n', 'INTRODUCTION', '2026-05-05T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000106', 'day-con-tang-dai-nhat-cpp', 'Dãy con tăng dài nhất C++',
        'Tìm độ dài dãy con tăng nghiêm ngặt bằng quy hoạch động.',
        'Cho n số nguyên. In độ dài dãy con tăng nghiêm ngặt dài nhất.',
        E'7\n1 2 2 3 1 4 5\n', E'5\n', 'CPP', '2026-05-06T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000107', 'kiem-tra-ma-tran-doi-xung', 'Ma trận đối xứng',
        'Kiểm tra ma trận vuông có đối xứng qua đường chéo chính.',
        'Cho ma trận vuông n x n. In YES nếu ma trận đối xứng, ngược lại in NO.',
        E'3\n1 2 3\n2 4 5\n3 5 6\n', E'YES\n', 'CPP', '2026-05-07T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000108', 'sap-xep-chen', 'Sắp xếp chèn',
        'Cài đặt thuật toán insertion sort.',
        'Cho n số nguyên. Sắp xếp dãy tăng dần bằng insertion sort và in kết quả.',
        E'4\n5 2 4 1\n', E'1 2 4 5\n', 'CPP', '2026-05-08T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000109', 'tim-phan-tu-xuat-hien-nhieu-nhat', 'Phần tử xuất hiện nhiều nhất',
        'Tìm phần tử có tần suất lớn nhất trong dãy.',
        'Cho n số nguyên. In phần tử xuất hiện nhiều nhất; nếu hòa, chọn phần tử nhỏ hơn.',
        E'8\n3 1 3 2 3 2 1 3\n', E'3\n', 'CPP', '2026-05-09T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000110', 'chuyen-co-so-16', 'Chuyển sang cơ số 16',
        'Đổi số thập phân sang hệ thập lục phân.',
        'Cho số nguyên không âm n. In biểu diễn cơ số 16 bằng chữ in hoa.',
        E'255\n', E'FF\n', 'CPP', '2026-05-10T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000111', 'tinh-giai-thua-java', 'Tính giai thừa Java',
        'Luyện vòng lặp và kiểu số nguyên trong Java.',
        'Cho số nguyên n từ 0 đến 12. In n! .',
        E'6\n', E'720\n', 'JAVA', '2026-05-11T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000112', 'loc-so-nguyen-to', 'Lọc số nguyên tố',
        'Lọc các số nguyên tố trong một dãy.',
        'Cho n số nguyên dương. In các số nguyên tố theo đúng thứ tự xuất hiện.',
        E'7\n2 3 4 5 6 7 8\n', E'2 3 5 7\n', 'JAVA', '2026-05-12T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000113', 'dem-cap-ky-tu', 'Đếm cặp ký tự',
        'Đếm ký tự xuất hiện nhiều nhất trong chuỗi Java.',
        'Cho chuỗi chữ thường không có khoảng trắng. In ký tự xuất hiện nhiều nhất và số lần xuất hiện.',
        E'abracadabra\n', E'a 5\n', 'JAVA', '2026-05-13T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000114', 'ma-tran-tich-java', 'Tích hai ma trận',
        'Nhân hai ma trận tương thích kích thước.',
        'Cho ma trận A kích thước n x p và B kích thước p x m. In ma trận tích A nhân B.',
        E'2 2 2\n1 2\n3 4\n5 6\n7 8\n', E'19 22\n43 50\n', 'JAVA', '2026-05-14T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000115', 'ngay-thu-trong-nam', 'Ngày thứ bao nhiêu trong năm',
        'Tính thứ tự của một ngày trong năm.',
        'Cho ngày theo định dạng yyyy mm dd. In ngày đó là ngày thứ bao nhiêu trong năm, xét cả năm nhuận.',
        E'2024 3 1\n', E'61\n', 'JAVA', '2026-05-15T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000116', 'tong-prefix', 'Mảng tổng tiền tố',
        'Tạo mảng tổng tiền tố từ một dãy số.',
        'Cho n số nguyên. In tổng của các phần tử từ đầu dãy đến từng vị trí.',
        E'5\n1 2 3 4 5\n', E'1 3 6 10 15\n', 'PYTHON', '2026-05-16T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000117', 'doan-tang-lien-tiep-dai-nhat', 'Đoạn tăng liên tiếp dài nhất',
        'Tìm đoạn tăng liên tiếp dài nhất trong dãy.',
        'Cho n số nguyên. In độ dài đoạn liên tiếp tăng nghiêm ngặt dài nhất.',
        E'7\n1 2 3 2 4 5 6\n', E'3\n', 'PYTHON', '2026-05-17T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000118', 'dem-so-lan-xuat-hien', 'Đếm số lần xuất hiện',
        'Đếm số lần một giá trị xuất hiện trong dãy.',
        'Cho n, dãy số và x. In số lần x xuất hiện trong dãy.',
        E'7\n1 2 2 3 2 4 5\n2\n', E'3\n', 'PYTHON', '2026-05-18T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000119', 'kiem-tra-so-hoan-hao', 'Kiểm tra số hoàn hảo',
        'Tính tổng các ước thực sự của một số.',
        'Cho số nguyên dương n. In YES nếu tổng các ước dương nhỏ hơn n bằng n, ngược lại in NO.',
        E'28\n', E'YES\n', 'PYTHON', '2026-05-19T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000120', 'so-nghiem-phuong-trinh-bac-hai', 'Số nghiệm phương trình bậc hai',
        'Xác định số nghiệm thực của phương trình bậc hai.',
        'Cho a, b, c với a khác 0. In số nghiệm thực phân biệt của ax^2 + bx + c = 0.',
        E'1 -3 2\n', E'2\n', 'PYTHON', '2026-05-20T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000121', 'san-pham-giam-gia', 'Sản phẩm giảm giá',
        'Tính giá cuối cùng của sản phẩm qua một lớp.',
        'Cho giá gốc, số lượng và phần trăm giảm giá. In tổng tiền sau giảm giá, làm tròn xuống.',
        E'100 3 10\n', E'270\n', 'OOP', '2026-05-21T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000122', 'the-tich-hinh-tru', 'Thể tích hình trụ',
        'Tính thể tích hình trụ bằng phương thức của lớp.',
        'Cho bán kính r và chiều cao h là số thực. In thể tích pi * r * r * h với hai chữ số thập phân.',
        E'2 3\n', E'37.70\n', 'OOP', '2026-05-22T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000123', 'nhan-vien-da-hinh', 'Nhân viên đa hình',
        'Tính lương qua các loại nhân viên khác nhau.',
        'Dòng đầu chứa n. Mỗi dòng là FULL salary hoặc PART hours rate. In tổng lương của tất cả nhân viên.',
        E'2\nFULL 1000\nPART 8 100\n', E'1800\n', 'OOP', '2026-05-23T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000124', 'ngan-xep-min', 'Ngăn xếp lấy phần tử nhỏ nhất',
        'Mô phỏng ngăn xếp hỗ trợ truy vấn min.',
        'Xử lý push x, pop và min. Lệnh min in phần tử nhỏ nhất hiện có hoặc EMPTY.',
        E'6\npush 5\npush 2\nmin\npop\nmin\n', E'2\n5\n', 'DATA_STRUCTURES', '2026-05-24T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000125', 'duyet-cay-nhi-phan', 'Duyệt cây nhị phân tìm kiếm',
        'Xây cây tìm kiếm nhị phân và duyệt giữa.',
        'Cho n giá trị khác nhau. Chèn lần lượt vào cây tìm kiếm nhị phân, sau đó in phép duyệt inorder.',
        E'3\n2 1 3\n', E'1 2 3\n', 'DATA_STRUCTURES', '2026-05-25T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000126', 'bieu-thuc-hau-to', 'Tính biểu thức hậu tố',
        'Tính giá trị biểu thức postfix bằng ngăn xếp.',
        'Cho biểu thức hậu tố gồm số nguyên và toán tử + - *. In giá trị biểu thức.',
        E'5 1 2 + 4 * + 3 -\n', E'14\n', 'DATA_STRUCTURES', '2026-05-26T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000127', 'sap-xep-nhanh', 'Sắp xếp nhanh',
        'Cài đặt thuật toán quicksort.',
        'Cho n số nguyên. Sắp xếp dãy tăng dần bằng quicksort và in kết quả.',
        E'6\n10 7 8 9 1 5\n', E'1 5 7 8 9 10\n', 'ALGORITHMS', '2026-05-27T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000128', 'day-con-chung-dai-nhat', 'Dãy con chung dài nhất',
        'Tìm độ dài dãy con chung dài nhất của hai chuỗi.',
        'Cho hai chuỗi chữ thường. In độ dài dãy con chung dài nhất của chúng.',
        E'abcde\nace\n', E'3\n', 'ALGORITHMS', '2026-05-28T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000129', 'luong-trung-binh-phong-ban', 'Lương trung bình phòng ban',
        'Tính lương trung bình theo từng phòng ban.',
        'Bảng employees gồm id, department và salary. Trả department cùng lương trung bình, sắp xếp department tăng dần.',
        E'CREATE TABLE employees (id INT, department VARCHAR(20), salary INT);\nINSERT INTO employees VALUES (1, ''IT'', 100), (2, ''HR'', 80), (3, ''IT'', 120);\n', E'HR\t80.0000\nIT\t110.0000\n', 'SQL', '2026-05-29T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000130', 'don-hang-lon-nhat', 'Đơn hàng lớn nhất',
        'Tìm đơn hàng có giá trị lớn nhất trong bảng.',
        'Bảng orders gồm id và amount. Trả id cùng amount của đơn hàng lớn nhất; nếu hòa, chọn id nhỏ hơn.',
        E'CREATE TABLE orders (id INT, amount INT);\nINSERT INTO orders VALUES (1, 100), (2, 250), (3, 180);\n', E'2\t250\n', 'SQL', '2026-05-30T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000131', 'kiem-tra-nam-nhuan', 'Kiểm tra năm nhuận',
        'Xác định một năm có phải năm nhuận hay không.',
        'Cho số nguyên y. In YES nếu y chia hết cho 400 hoặc chia hết cho 4 nhưng không chia hết cho 100, ngược lại in NO.',
        E'2024\n', E'YES\n', 'INTRODUCTION', '2026-06-01T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000132', 'tong-chu-so-chan', 'Tổng các chữ số chẵn',
        'Tính tổng những chữ số chẵn của một số nguyên.',
        'Cho số nguyên không âm n. In tổng các chữ số chẵn xuất hiện trong n.',
        E'123456\n', E'12\n', 'INTRODUCTION', '2026-06-02T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000133', 'kiem-tra-so-chinh-phuong', 'Kiểm tra số chính phương',
        'Kiểm tra một số có phải là số chính phương.',
        'Cho số nguyên không âm n. In YES nếu tồn tại số nguyên k sao cho k nhân k bằng n, ngược lại in NO.',
        E'49\n', E'YES\n', 'INTRODUCTION', '2026-06-03T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000134', 'dem-so-nguyen-to', 'Đếm số nguyên tố trong dãy',
        'Đếm các phần tử là số nguyên tố trong một dãy số.',
        'Dòng đầu chứa n, dòng sau chứa n số nguyên dương. In số lượng số nguyên tố trong dãy.',
        E'8\n2 4 5 9 11 12 13 15\n', E'4\n', 'INTRODUCTION', '2026-06-04T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000135', 'tong-cap-so-cong', 'Tổng cấp số cộng',
        'Tính tổng n số hạng đầu của một cấp số cộng.',
        'Cho số hạng đầu a, công sai d và số lượng n. In tổng n số hạng đầu của cấp số cộng.',
        E'1 2 5\n', E'25\n', 'INTRODUCTION', '2026-06-05T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000136', 'sap-xep-noi-bot-cpp', 'Sắp xếp nổi bọt',
        'Cài đặt thuật toán bubble sort bằng C++.',
        'Cho n số nguyên. Sắp xếp dãy tăng dần bằng bubble sort và in kết quả trên một dòng.',
        E'5\n5 1 4 2 8\n', E'1 2 4 5 8\n', 'CPP', '2026-06-06T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000137', 'tong-duong-cheo-phu-cpp', 'Tổng đường chéo phụ',
        'Tính tổng các phần tử trên đường chéo phụ của ma trận vuông.',
        'Cho ma trận vuông n x n. In tổng các phần tử có chỉ số hàng cộng chỉ số cột bằng n - 1.',
        E'3\n1 2 3\n4 5 6\n7 8 9\n', E'15\n', 'CPP', '2026-06-07T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000138', 'tong-lon-nhat-doan-cpp', 'Tổng lớn nhất của đoạn con',
        'Tìm tổng lớn nhất của một đoạn con liên tiếp.',
        'Cho n số nguyên. In tổng lớn nhất của một đoạn con liên tiếp không rỗng.',
        E'7\n-2 3 -1 5 -6 4 2\n', E'7\n', 'CPP', '2026-06-08T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000139', 'kiem-tra-day-tang-cpp', 'Kiểm tra dãy tăng',
        'Kiểm tra các phần tử trong dãy có tăng nghiêm ngặt hay không.',
        'Cho n số nguyên. In YES nếu mọi phần tử sau lớn hơn phần tử đứng trước, ngược lại in NO.',
        E'5\n1 2 3 4 5\n', E'YES\n', 'CPP', '2026-06-09T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000140', 'dem-ky-tu-hoa-cpp', 'Đếm ký tự viết hoa',
        'Đếm số chữ cái viết hoa trong một chuỗi.',
        'Cho một dòng văn bản. In số lượng ký tự từ A đến Z xuất hiện trong dòng.',
        E'DevEdu2026\n', E'2\n', 'CPP', '2026-06-10T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000141', 'tim-vi-tri-phan-tu-java', 'Tìm vị trí phần tử',
        'Tìm vị trí xuất hiện đầu tiên của một giá trị trong mảng.',
        'Cho n, một mảng số nguyên và giá trị x. In chỉ số zero-based đầu tiên của x hoặc -1 nếu không tìm thấy.',
        E'6\n4 9 2 7 9 1\n7\n', E'3\n', 'JAVA', '2026-06-11T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000142', 'dao-nguoc-chuoi-java', 'Đảo ngược chuỗi',
        'Đảo thứ tự ký tự của một chuỗi trong Java.',
        'Cho một chuỗi không chứa khoảng trắng. In chuỗi sau khi đảo ngược.',
        E'hello\n', E'olleh\n', 'JAVA', '2026-06-12T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000143', 'tong-so-le-java', 'Tổng các số lẻ',
        'Tính tổng các phần tử lẻ trong dãy bằng Java.',
        'Cho n số nguyên. In tổng những phần tử lẻ trong dãy.',
        E'6\n1 2 3 4 5 6\n', E'9\n', 'JAVA', '2026-06-13T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000144', 'kiem-tra-so-nguyen-to-java', 'Kiểm tra số nguyên tố Java',
        'Kiểm tra tính nguyên tố của một số bằng Java.',
        'Cho số nguyên dương n. In YES nếu n là số nguyên tố, ngược lại in NO.',
        E'29\n', E'YES\n', 'JAVA', '2026-06-14T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000145', 'dem-tu-dai-nhat-java', 'Độ dài từ dài nhất',
        'Tìm độ dài của từ dài nhất trong một câu.',
        'Cho một dòng gồm các từ phân tách bởi khoảng trắng. In độ dài từ dài nhất.',
        E'hoc lap trinh java\n', E'4\n', 'JAVA', '2026-06-15T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000146', 'dem-ky-tu-khac-nhau-python', 'Đếm ký tự khác nhau',
        'Đếm số ký tự khác nhau trong chuỗi bằng Python.',
        'Cho một chuỗi chữ thường không có khoảng trắng. In số lượng ký tự khác nhau xuất hiện trong chuỗi.',
        E'banana\n', E'3\n', 'PYTHON', '2026-06-16T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000147', 'dem-so-0-python', 'Đếm số 0 trong dãy',
        'Đếm số phần tử bằng 0 trong một dãy số.',
        'Cho n số nguyên. In số lượng phần tử có giá trị bằng 0.',
        E'8\n0 1 0 2 3 0 4 0\n', E'4\n', 'PYTHON', '2026-06-17T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000148', 'kiem-tra-day-doi-xung-python', 'Dãy đối xứng',
        'Kiểm tra một dãy số có đối xứng hay không.',
        'Cho n số nguyên. In YES nếu dãy đọc từ trái sang phải giống đọc từ phải sang trái, ngược lại in NO.',
        E'5\n1 2 3 2 1\n', E'YES\n', 'PYTHON', '2026-06-18T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000149', 'tinh-trung-vi-python', 'Tính trung vị',
        'Tìm trung vị của dãy số có số phần tử lẻ.',
        'Cho n lẻ và n số nguyên. Sắp xếp dãy rồi in phần tử đứng giữa.',
        E'5\n7 1 9 3 5\n', E'5\n', 'PYTHON', '2026-06-19T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000150', 'tong-tung-hang-python', 'Tổng từng hàng ma trận',
        'Tính tổng các phần tử trên từng hàng của ma trận.',
        'Cho ma trận n x m. In tổng của từng hàng, mỗi tổng trên một dòng.',
        E'2 3\n1 2 3\n4 5 6\n', E'6\n15\n', 'PYTHON', '2026-06-20T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000151', 'dien-tich-hinh-tron-oop', 'Diện tích hình tròn OOP',
        'Tính diện tích hình tròn qua một lớp hình học.',
        'Cho bán kính r là số thực. Tạo đối tượng hình tròn và in diện tích pi nhân r nhân r với hai chữ số thập phân.',
        E'2\n', E'12.57\n', 'OOP', '2026-06-21T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000152', 'lai-suat-tiet-kiem-oop', 'Lãi suất tiết kiệm',
        'Tính số tiền sau một kỳ áp dụng lãi suất qua lớp tài khoản.',
        'Cho số tiền gốc p và lãi suất r theo phần trăm. In số tiền sau kỳ hạn, làm tròn xuống đến số nguyên.',
        E'1000 5\n', E'1050\n', 'OOP', '2026-06-22T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000153', 'tinh-luong-tang-ca-oop', 'Tính lương tăng ca',
        'Tính tổng lương khi có thêm giờ làm qua lớp nhân viên.',
        'Cho lương cơ bản, số giờ tăng ca và tiền công mỗi giờ. In tổng lương sau khi cộng tiền tăng ca.',
        E'1000 5 50\n', E'1250\n', 'OOP', '2026-06-23T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000154', 'hang-doi-vong', 'Hàng đợi vòng',
        'Mô phỏng hàng đợi vòng với các thao tác cơ bản.',
        'Xử lý push x, pop, front và size. Lệnh front in phần tử đầu hoặc EMPTY nếu hàng đợi rỗng.',
        E'5\npush 1\npush 2\npop\nfront\nsize\n', E'2\n1\n', 'DATA_STRUCTURES', '2026-06-24T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000155', 'chieu-cao-cay-nhi-phan', 'Chiều cao cây nhị phân',
        'Xây cây tìm kiếm nhị phân và tính chiều cao của cây.',
        'Cho n giá trị khác nhau, chèn lần lượt vào cây tìm kiếm nhị phân. In chiều cao theo số mức của cây.',
        E'7\n4 2 6 1 3 5 7\n', E'3\n', 'DATA_STRUCTURES', '2026-06-25T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000156', 'duyet-rong-do-thi', 'Duyệt rộng đồ thị',
        'Duyệt đồ thị vô hướng theo chiều rộng BFS.',
        'Cho đồ thị vô hướng, đỉnh bắt đầu s và các cạnh. In thứ tự các đỉnh được thăm bằng BFS, ưu tiên đỉnh nhỏ hơn.',
        E'5 4 1\n1 2\n1 3\n2 4\n3 5\n', E'1 2 3 4 5\n', 'DATA_STRUCTURES', '2026-06-26T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000157', 'luy-thua-nhanh', 'Lũy thừa nhanh',
        'Tính lũy thừa bằng phương pháp bình phương nhanh.',
        'Cho hai số nguyên không âm a và b. In a mũ b. Kết quả trong phạm vi số nguyên 64 bit.',
        E'2 10\n', E'1024\n', 'ALGORITHMS', '2026-06-27T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000158', 'sap-xep-top-k', 'Sắp xếp top K',
        'Tìm và sắp xếp K phần tử lớn nhất trong dãy.',
        'Cho n, k và n số nguyên. In k phần tử lớn nhất theo thứ tự giảm dần.',
        E'6 3\n4 9 1 7 3 8\n', E'9 8 7\n', 'ALGORITHMS', '2026-06-28T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000159', 'tong-doanh-thu-theo-danh-muc', 'Tổng doanh thu theo danh mục',
        'Nhóm và cộng doanh thu theo danh mục trong SQL.',
        'Bảng sales gồm category và amount. Trả category cùng tổng amount, sắp xếp category tăng dần.',
        E'CREATE TABLE sales (id INT, category VARCHAR(20), amount INT);\nINSERT INTO sales VALUES (1, ''A'', 100), (2, ''B'', 80), (3, ''A'', 50);\n', E'A\t150\nB\t80\n', 'SQL', '2026-06-29T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000160', 'khach-hang-chua-co-don', 'Khách hàng chưa có đơn',
        'Tìm khách hàng chưa từng phát sinh đơn hàng.',
        'Bảng customers gồm id và name, bảng orders gồm customer_id. Trả tên khách hàng không có đơn nào theo alphabet.',
        E'CREATE TABLE customers (id INT, name VARCHAR(50));\nCREATE TABLE orders (id INT, customer_id INT);\nINSERT INTO customers VALUES (1, ''An''), (2, ''Binh''), (3, ''Chi'');\nINSERT INTO orders VALUES (1, 1), (2, 3);\n', E'Binh\n', 'SQL', '2026-06-30T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000161', 'tong-so-chia-het-cho-5', 'Đếm số chia hết cho 5',
        'Đếm các phần tử chia hết cho 5 trong dãy số.',
        'Cho n số nguyên. In số lượng phần tử trong dãy chia hết cho 5.',
        E'6\n5 7 10 12 15 20\n', E'4\n', 'INTRODUCTION', '2026-07-01T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000162', 'so-nho-nhat-ba-so', 'Số nhỏ nhất trong ba số',
        'So sánh ba số nguyên bằng cấu trúc điều kiện.',
        'Cho ba số nguyên a, b, c. In số nhỏ nhất trong ba số.',
        E'8 3 10\n', E'3\n', 'INTRODUCTION', '2026-07-02T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000163', 'nhi-phan-sang-thap-phan', 'Đổi nhị phân sang thập phân',
        'Chuyển một chuỗi nhị phân thành số thập phân.',
        'Cho chuỗi chỉ gồm ký tự 0 và 1. In giá trị thập phân tương ứng.',
        E'10101\n', E'21\n', 'INTRODUCTION', '2026-07-03T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000164', 'dem-chu-so-0', 'Đếm chữ số 0',
        'Đếm số lần chữ số 0 xuất hiện trong một số nguyên.',
        'Cho số nguyên không âm n. In số lượng chữ số 0 trong biểu diễn thập phân của n.',
        E'100200\n', E'3\n', 'INTRODUCTION', '2026-07-04T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000165', 'phan-so-toi-gian', 'Rút gọn phân số',
        'Rút gọn một phân số bằng ước chung lớn nhất.',
        'Cho tử số a và mẫu số b dương. In phân số tối giản theo dạng a/b.',
        E'8 12\n', E'2/3\n', 'INTRODUCTION', '2026-07-05T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000166', 'tong-cac-so-am-cpp', 'Tổng các số âm',
        'Tính tổng những phần tử âm trong dãy bằng C++.',
        'Cho n số nguyên. In tổng tất cả phần tử nhỏ hơn 0 trong dãy.',
        E'6\n-2 5 -7 0 -1 4\n', E'-10\n', 'CPP', '2026-07-06T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000167', 'sap-xep-giam-dan-cpp', 'Sắp xếp giảm dần',
        'Sắp xếp một dãy số nguyên theo thứ tự giảm dần.',
        'Cho n số nguyên. In dãy đã sắp xếp giảm dần, các phần tử cách nhau bởi một khoảng trắng.',
        E'5\n3 9 1 7 5\n', E'9 7 5 3 1\n', 'CPP', '2026-07-07T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000168', 'tong-hang-lon-nhat-cpp', 'Tổng hàng lớn nhất',
        'Tìm tổng lớn nhất trong các hàng của ma trận.',
        'Cho ma trận n x m. In tổng các phần tử của hàng có tổng lớn nhất.',
        E'2 3\n1 2 3\n4 5 6\n', E'15\n', 'CPP', '2026-07-08T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000169', 'dem-cap-bang-nhau-cpp', 'Đếm cặp phần tử bằng nhau',
        'Đếm số cặp chỉ số có cùng giá trị trong dãy.',
        'Cho n số nguyên. In số cặp i nhỏ hơn j sao cho a[i] bằng a[j].',
        E'6\n1 2 1 2 1 3\n', E'4\n', 'CPP', '2026-07-09T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000170', 'dem-nguyen-am-cpp', 'Đếm nguyên âm C++',
        'Đếm các nguyên âm trong một chuỗi bằng C++.',
        'Cho một dòng văn bản. Đếm a, e, i, o, u không phân biệt chữ hoa chữ thường.',
        E'DevEdu\n', E'3\n', 'CPP', '2026-07-10T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000171', 'tong-binh-phuong-java', 'Tổng bình phương',
        'Tính tổng bình phương các phần tử trong dãy bằng Java.',
        'Cho n số nguyên. In tổng a[i] nhân a[i] của mọi phần tử.',
        E'5\n1 2 3 4 5\n', E'55\n', 'JAVA', '2026-07-11T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000172', 'xoa-phan-tu-trung-java', 'Loại phần tử trùng',
        'Giữ lại lần xuất hiện đầu tiên của mỗi phần tử.',
        'Cho n số nguyên. In dãy sau khi loại bỏ các giá trị trùng nhưng giữ nguyên thứ tự xuất hiện.',
        E'7\n1 2 1 3 2 3 1\n', E'1 2 3\n', 'JAVA', '2026-07-12T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000173', 'tong-cot-ma-tran-java', 'Tổng từng cột ma trận',
        'Tính tổng các phần tử trên từng cột của ma trận.',
        'Cho ma trận n x m. In tổng từng cột trên một dòng, cách nhau bởi khoảng trắng.',
        E'2 3\n1 2 3\n4 5 6\n', E'5 7 9\n', 'JAVA', '2026-07-13T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000174', 'dem-ky-tu-so-java', 'Đếm chữ số trong chuỗi',
        'Đếm số ký tự là chữ số trong một chuỗi Java.',
        'Cho một dòng văn bản. In số lượng ký tự từ 0 đến 9 xuất hiện trong dòng.',
        E'DevEdu2026\n', E'4\n', 'JAVA', '2026-07-14T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000175', 'liet-ke-uoc-java', 'Liệt kê các ước số',
        'Tìm và in các ước dương của một số theo thứ tự tăng dần.',
        'Cho số nguyên dương n. In tất cả ước dương của n trên một dòng.',
        E'24\n', E'1 2 3 4 6 8 12 24\n', 'JAVA', '2026-07-15T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000176', 'dao-nguoc-day-python', 'Đảo ngược dãy',
        'Đảo ngược thứ tự các phần tử của dãy bằng Python.',
        'Cho n số nguyên. In dãy từ phần tử cuối về phần tử đầu.',
        E'4\n1 2 3 4\n', E'4 3 2 1\n', 'PYTHON', '2026-07-16T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000177', 'tong-so-duong-python', 'Tổng số dương',
        'Tính tổng các phần tử dương trong dãy bằng Python.',
        'Cho n số nguyên. In tổng các phần tử lớn hơn 0 trong dãy.',
        E'6\n-2 5 -7 0 -1 4\n', E'9\n', 'PYTHON', '2026-07-17T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000178', 'sap-xep-tu-theo-do-dai-python', 'Sắp xếp từ theo độ dài',
        'Sắp xếp các từ theo độ dài tăng dần.',
        'Cho n từ không có khoảng trắng. In các từ theo độ dài tăng dần; nếu bằng nhau giữ thứ tự ban đầu.',
        E'4\ncode learn ai dev\n', E'ai dev code learn\n', 'PYTHON', '2026-07-18T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000179', 'dem-nguyen-am-python', 'Đếm nguyên âm Python',
        'Đếm số nguyên âm trong chuỗi bằng Python.',
        'Cho chuỗi chữ thường không có khoảng trắng. In số lượng a, e, i, o, u trong chuỗi.',
        E'education\n', E'5\n', 'PYTHON', '2026-07-19T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000180', 'so-lon-thu-ba-python', 'Số lớn thứ ba',
        'Tìm giá trị phân biệt lớn thứ ba trong dãy.',
        'Cho dãy có ít nhất ba giá trị phân biệt. In giá trị phân biệt lớn thứ ba.',
        E'7\n4 9 2 9 7 4 8\n', E'7\n', 'PYTHON', '2026-07-20T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000181', 'dien-tich-tam-giac-oop', 'Diện tích tam giác OOP',
        'Tính diện tích tam giác qua một lớp hình học.',
        'Cho đáy b và chiều cao h là số thực. Tạo đối tượng tam giác và in diện tích với hai chữ số thập phân.',
        E'3 4\n', E'6.00\n', 'OOP', '2026-07-21T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000182', 'hoa-hong-nhan-vien-oop', 'Hoa hồng nhân viên',
        'Tính thu nhập sau khi cộng phần trăm hoa hồng.',
        'Cho lương cơ bản salary và phần trăm hoa hồng rate. In salary cộng salary nhân rate chia 100, làm tròn xuống.',
        E'2000 15\n', E'2300\n', 'OOP', '2026-07-22T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000183', 'tong-gia-san-pham-oop', 'Tổng giá sản phẩm',
        'Tính tổng giá của danh sách sản phẩm qua lớp quản lý.',
        'Cho n mức giá nguyên. In tổng giá của tất cả sản phẩm.',
        E'3\n100 200 50\n', E'350\n', 'OOP', '2026-07-23T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000184', 'kiem-tra-ngoac-stack', 'Kiểm tra ngoặc bằng stack',
        'Kiểm tra chuỗi ngoặc bằng cấu trúc ngăn xếp.',
        'Cho chuỗi chỉ gồm (), [] và {}. In YES nếu ngoặc hợp lệ, ngược lại in NO.',
        E'([{}])\n', E'YES\n', 'DATA_STRUCTURES', '2026-07-24T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000185', 'dao-nguoc-danh-sach-lien-ket', 'Đảo danh sách liên kết',
        'Đảo thứ tự các nút của danh sách liên kết đơn.',
        'Cho n phần tử của danh sách. In danh sách sau khi đảo ngược.',
        E'5\n1 2 3 4 5\n', E'5 4 3 2 1\n', 'DATA_STRUCTURES', '2026-07-25T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000186', 'duyet-cay-preorder', 'Duyệt cây theo thứ tự trước',
        'Xây cây tìm kiếm nhị phân và duyệt preorder.',
        'Cho n giá trị khác nhau, chèn lần lượt vào cây tìm kiếm nhị phân. In thứ tự duyệt trước.',
        E'5\n4 2 1 3 5\n', E'4 2 1 3 5\n', 'DATA_STRUCTURES', '2026-07-26T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000187', 'day-con-tang-dai-nhat-java', 'Dãy con tăng dài nhất Java',
        'Tìm độ dài dãy con tăng nghiêm ngặt bằng Java.',
        'Cho n số nguyên. In độ dài dãy con tăng nghiêm ngặt dài nhất.',
        E'6\n10 9 2 5 3 7\n', E'3\n', 'ALGORITHMS', '2026-07-27T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000188', 'tim-so-bi-thieu', 'Tìm số bị thiếu',
        'Tìm số còn thiếu trong dãy từ 0 đến n.',
        'Cho n và n số khác nhau thuộc đoạn 0 đến n. In số chưa xuất hiện trong dãy.',
        E'5\n0 1 3 4 5\n', E'2\n', 'ALGORITHMS', '2026-07-28T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000189', 'so-don-theo-khach-hang', 'Số đơn theo khách hàng',
        'Đếm số đơn hàng của từng khách hàng trong SQL.',
        'Bảng customers gồm id và name, bảng orders gồm customer_id. Trả tên cùng số đơn, kể cả khách hàng chưa có đơn, theo id tăng dần.',
        E'CREATE TABLE customers (id INT, name VARCHAR(50));\nCREATE TABLE orders (id INT, customer_id INT);\nINSERT INTO customers VALUES (1, ''An''), (2, ''Binh''), (3, ''Chi'');\nINSERT INTO orders VALUES (1, 1), (2, 1), (3, 3);\n', E'An\t2\nBinh\t0\nChi\t1\n', 'SQL', '2026-07-29T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000190', 'nhan-vien-luong-cao-hon-trung-binh', 'Nhân viên lương cao hơn trung bình',
        'Lọc nhân viên có lương cao hơn mức trung bình trong SQL.',
        'Bảng employees gồm name và salary. Trả tên cùng lương của nhân viên có salary lớn hơn lương trung bình, sắp xếp salary giảm dần.',
        E'CREATE TABLE employees (id INT, name VARCHAR(50), salary INT);\nINSERT INTO employees VALUES (1, ''An'', 100), (2, ''Binh'', 80), (3, ''Chi'', 120);\n', E'Chi\t120\n', 'SQL', '2026-07-30T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000191', 'tong-day-so-le', 'Tổng dãy số lẻ',
        'Tính tổng các phần tử lẻ trong một dãy số.',
        'Cho n số nguyên. In tổng những phần tử lẻ trong dãy.',
        E'5\n1 2 3 4 5\n', E'9\n', 'INTRODUCTION', '2026-08-01T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000192', 'dem-chu-so-chan', 'Đếm chữ số chẵn',
        'Đếm số chữ số chẵn trong một số nguyên.',
        'Cho số nguyên không âm n. In số lượng chữ số chẵn xuất hiện trong n.',
        E'123456\n', E'3\n', 'INTRODUCTION', '2026-08-02T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000193', 'so-gan-0-nhat', 'Số gần 0 nhất',
        'Tìm phần tử có giá trị tuyệt đối nhỏ nhất trong dãy.',
        'Cho n số nguyên. In phần tử gần 0 nhất; nếu hòa, chọn số dương.',
        E'5\n-7 -2 3 9 -1\n', E'-1\n', 'INTRODUCTION', '2026-08-03T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000194', 'chuyen-doi-phut-sang-gio', 'Đổi phút sang giờ',
        'Đổi tổng số phút thành số giờ và phút còn lại.',
        'Cho m số phút không âm. In số giờ và số phút dư theo dạng h m.',
        E'135\n', E'2 15\n', 'INTRODUCTION', '2026-08-04T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000195', 'tinh-bieu-thuc-a-cong-b-nhan-c', 'Tính biểu thức a + b nhân c',
        'Tính biểu thức số học theo đúng thứ tự ưu tiên.',
        'Cho ba số nguyên a, b, c. In giá trị của a + b * c.',
        E'2 3 4\n', E'14\n', 'INTRODUCTION', '2026-08-05T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000196', 'dem-so-am-cpp', 'Đếm số âm C++',
        'Đếm số phần tử âm trong dãy bằng C++.',
        'Cho n số nguyên. In số lượng phần tử nhỏ hơn 0 trong dãy.',
        E'6\n-2 5 -7 0 -1 4\n', E'3\n', 'CPP', '2026-08-06T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000197', 'tim-phan-tu-nho-thu-hai-cpp', 'Phần tử nhỏ thứ hai',
        'Tìm giá trị phân biệt nhỏ thứ hai trong dãy.',
        'Cho dãy có ít nhất hai giá trị phân biệt. In giá trị phân biệt nhỏ thứ hai.',
        E'6\n4 1 9 1 7 4\n', E'4\n', 'CPP', '2026-08-07T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000198', 'hieu-hai-ma-tran-cpp', 'Hiệu hai ma trận',
        'Trừ từng phần tử tương ứng của hai ma trận.',
        'Cho hai ma trận cùng kích thước n x m. In ma trận A trừ B.',
        E'2 2\n5 6\n7 8\n1 2\n3 4\n', E'4 4\n4 4\n', 'CPP', '2026-08-08T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000199', 'dem-doan-tang-cpp', 'Đếm đoạn tăng liên tiếp',
        'Đếm số đoạn tăng nghiêm ngặt tối đa trong dãy.',
        'Một đoạn tăng kết thúc trước phần tử không lớn hơn phần tử trước đó. In số đoạn tăng liên tiếp tối đa.',
        E'5\n1 2 3 2 4\n', E'2\n', 'CPP', '2026-08-09T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000200', 'hex-sang-thap-phan-cpp', 'Đổi hệ 16 sang thập phân',
        'Chuyển một số thập lục phân thành số thập phân.',
        'Cho chuỗi hệ 16 gồm chữ số 0-9 và A-F. In giá trị thập phân tương ứng.',
        E'1A\n', E'26\n', 'CPP', '2026-08-10T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000201', 'tong-so-chan-java', 'Tổng số chẵn Java',
        'Tính tổng các phần tử chẵn trong dãy bằng Java.',
        'Cho n số nguyên. In tổng những phần tử chia hết cho 2.',
        E'6\n1 2 3 4 5 6\n', E'12\n', 'JAVA', '2026-08-11T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000202', 'tim-phan-tu-nho-nhat-java', 'Tìm phần tử nhỏ nhất Java',
        'Tìm giá trị nhỏ nhất trong mảng bằng Java.',
        'Cho n số nguyên. In phần tử nhỏ nhất của mảng.',
        E'5\n4 9 2 7 1\n', E'1\n', 'JAVA', '2026-08-12T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000203', 'viet-hoa-chuoi-java', 'Viết hoa chuỗi',
        'Chuyển các chữ cái trong chuỗi thành chữ hoa.',
        'Cho một chuỗi chỉ gồm chữ cái Latin và khoảng trắng. In chuỗi sau khi chuyển sang chữ hoa.',
        E'devedu\n', E'DEVEDU\n', 'JAVA', '2026-08-13T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000204', 'ma-tran-doi-xung-java', 'Ma trận đối xứng Java',
        'Kiểm tra ma trận vuông có đối xứng qua đường chéo chính.',
        'Cho ma trận vuông n x n. In YES nếu ma trận đối xứng, ngược lại in NO.',
        E'3\n1 2 3\n2 4 5\n3 5 6\n', E'YES\n', 'JAVA', '2026-08-14T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000205', 'sap-xep-chen-java', 'Sắp xếp chèn Java',
        'Cài đặt insertion sort trong Java.',
        'Cho n số nguyên. Sắp xếp dãy tăng dần bằng insertion sort và in kết quả.',
        E'5\n5 2 4 1 3\n', E'1 2 3 4 5\n', 'JAVA', '2026-08-15T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000206', 'tong-binh-phuong-python', 'Tổng bình phương Python',
        'Tính tổng bình phương các phần tử bằng Python.',
        'Cho n số nguyên. In tổng bình phương của mọi phần tử trong dãy.',
        E'3\n-2 3 4\n', E'29\n', 'PYTHON', '2026-08-16T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000207', 'tim-phan-tu-lon-nhat-python', 'Tìm phần tử lớn nhất Python',
        'Tìm giá trị lớn nhất trong dãy bằng Python.',
        'Cho n số nguyên. In phần tử lớn nhất của dãy.',
        E'5\n-3 7 2 9 1\n', E'9\n', 'PYTHON', '2026-08-17T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000208', 'loc-phan-tu-am-python', 'Lọc phần tử âm',
        'Lọc và in các phần tử âm trong dãy bằng Python.',
        'Cho n số nguyên. In các phần tử nhỏ hơn 0 theo đúng thứ tự xuất hiện.',
        E'6\n-2 5 -7 0 -1 4\n', E'-2 -7 -1\n', 'PYTHON', '2026-08-18T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000209', 'chuoi-xoay-python', 'Kiểm tra chuỗi xoay',
        'Kiểm tra hai chuỗi có phải là phép xoay vòng của nhau.',
        'Cho hai chuỗi không chứa khoảng trắng. In YES nếu chuỗi thứ hai là một phép xoay của chuỗi thứ nhất.',
        E'abc\nbca\n', E'YES\n', 'PYTHON', '2026-08-19T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000210', 'gop-hai-day-python', 'Gộp hai dãy đã sắp xếp',
        'Gộp hai dãy tăng dần bằng Python.',
        'Cho hai dãy đã sắp xếp tăng dần. In dãy kết quả cũng theo thứ tự tăng dần.',
        E'3 2\n1 4 7\n2 5\n', E'1 2 4 5 7\n', 'PYTHON', '2026-08-20T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000211', 'tinh-chu-vi-hinh-tron-oop', 'Chu vi hình tròn OOP',
        'Tính chu vi hình tròn qua một lớp hình học.',
        'Cho bán kính r là số thực. Tạo đối tượng hình tròn và in chu vi 2 * pi * r với hai chữ số thập phân.',
        E'2\n', E'12.57\n', 'OOP', '2026-08-21T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000212', 'phuong-tien-da-hinh-oop', 'Phương tiện đa hình',
        'Tính chi phí di chuyển qua các loại phương tiện.',
        'Cho loại phương tiện CAR hoặc BUS, số km và đơn giá mỗi km. In chi phí di chuyển.',
        E'CAR 120 2\n', E'240\n', 'OOP', '2026-08-22T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000213', 'don-hang-co-thue-oop', 'Đơn hàng có thuế',
        'Tính tổng tiền đơn hàng sau thuế qua một lớp nghiệp vụ.',
        'Cho đơn giá, số lượng và phần trăm thuế. In tổng tiền bằng đơn giá nhân số lượng rồi cộng thuế, làm tròn xuống.',
        E'100 2 10\n', E'220\n', 'OOP', '2026-08-23T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000214', 'dem-tan-suat-bang-map', 'Đếm tần suất bằng Map',
        'Tìm phần tử xuất hiện nhiều nhất bằng bảng băm.',
        'Cho n số nguyên. In phần tử có tần suất lớn nhất; nếu hòa, chọn phần tử nhỏ hơn.',
        E'7\n3 1 3 2 3 2 1\n', E'3\n', 'DATA_STRUCTURES', '2026-08-24T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000215', 'duyet-cay-postorder', 'Duyệt cây hậu thứ tự',
        'Xây cây tìm kiếm nhị phân và duyệt postorder.',
        'Cho n giá trị khác nhau, chèn lần lượt vào cây tìm kiếm nhị phân. In thứ tự duyệt trái phải gốc.',
        E'5\n4 2 1 3 5\n', E'1 3 2 5 4\n', 'DATA_STRUCTURES', '2026-08-25T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000216', 'duyet-do-thi-theo-chieu-sau', 'Duyệt sâu đồ thị',
        'Duyệt đồ thị vô hướng theo chiều sâu DFS.',
        'Cho đồ thị vô hướng, đỉnh bắt đầu s và các cạnh. In thứ tự DFS, luôn thăm đỉnh nhỏ hơn trước.',
        E'5 4 1\n1 2\n1 3\n2 4\n3 5\n', E'1 2 4 3 5\n', 'DATA_STRUCTURES', '2026-08-26T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000217', 'doan-con-tong-k', 'Đoạn con có tổng K',
        'Tìm đoạn con liên tiếp có tổng bằng K.',
        'Cho n, K và dãy số nguyên dương. In hai chỉ số 1-based của đoạn con đầu tiên có tổng K hoặc -1 nếu không có.',
        E'5 9\n2 3 4 1 5\n', E'1 3\n', 'ALGORITHMS', '2026-08-27T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000218', 'sap-xep-dem', 'Sắp xếp bằng đếm',
        'Sắp xếp các số nguyên nhỏ bằng counting sort.',
        'Cho n số nguyên trong đoạn 0 đến 9. In dãy theo thứ tự tăng dần.',
        E'7\n3 1 4 1 5 9 2\n', E'1 1 2 3 4 5 9\n', 'ALGORITHMS', '2026-08-28T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000219', 'san-pham-gia-trung-binh', 'Giá trung bình sản phẩm',
        'Tính giá trung bình theo danh mục trong SQL.',
        'Bảng products gồm category và price. Trả category cùng giá trung bình, sắp xếp category tăng dần.',
        E'CREATE TABLE products (id INT, category VARCHAR(20), price INT);\nINSERT INTO products VALUES (1, ''A'', 100), (2, ''B'', 80), (3, ''A'', 200);\n', E'A\t150.0000\nB\t80.0000\n', 'SQL', '2026-08-29T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000220', 'don-hang-theo-thang', 'Đếm đơn hàng theo tháng',
        'Đếm số đơn hàng trong từng tháng bằng SQL.',
        'Bảng orders gồm id và order_month. Trả tháng cùng số đơn, sắp xếp tháng tăng dần.',
        E'CREATE TABLE orders (id INT, order_month INT);\nINSERT INTO orders VALUES (1, 1), (2, 2), (3, 1), (4, 3);\n', E'1\t2\n2\t1\n3\t1\n', 'SQL', '2026-08-30T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000221', 'dem-so-chia-het-cho-5', 'Đếm số chia hết cho 5',
        'Đếm các phần tử trong dãy chia hết cho 5.',
        'Cho một dãy số nguyên. Hãy đếm có bao nhiêu phần tử chia hết cho 5.',
        E'6\n5 10 -5 4 15 0\n', E'5\n', 'INTRODUCTION', '2026-09-01T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000222', 'chuyen-doi-giay', 'Chuyển đổi giây',
        'Đổi tổng số giây thành giờ, phút và giây.',
        'Cho một số giây không âm. Hãy biểu diễn thời gian theo định dạng HH:MM:SS.',
        E'3661\n', E'01:01:01\n', 'INTRODUCTION', '2026-09-02T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000223', 'dem-ky-tu-in-hoa', 'Đếm ký tự in hoa',
        'Đếm số chữ cái in hoa trong một dòng văn bản.',
        'Cho một dòng văn bản có thể chứa chữ cái, chữ số và dấu cách. Hãy đếm các chữ cái tiếng Anh viết hoa từ A đến Z.',
        E'DevEdu2026\n', E'2\n', 'INTRODUCTION', '2026-09-03T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000224', 'khoang-cach-hai-diem', 'Khoảng cách hai điểm',
        'Tính khoảng cách Euclid giữa hai điểm trên mặt phẳng.',
        'Cho tọa độ (x1, y1) và (x2, y2). Hãy tính khoảng cách giữa hai điểm và in với đúng hai chữ số sau dấu thập phân.',
        E'0 0 3 4\n', E'5.00\n', 'INTRODUCTION', '2026-09-04T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000225', 'giao-hai-tap-hop-cpp', 'Giao hai tập hợp',
        'Tìm các giá trị xuất hiện trong cả hai dãy số.',
        'Cho hai dãy số nguyên. Hãy in mỗi giá trị phân biệt xuất hiện ở cả hai dãy theo thứ tự tăng dần. Nếu giao rỗng, in EMPTY.',
        E'5 4\n1 2 2 4 6\n2 4 4 8\n', E'2 4\n', 'CPP', '2026-09-05T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000226', 'tong-duong-cheo-phu-ma-tran-cpp', 'Tổng đường chéo phụ',
        'Tính tổng các phần tử trên đường chéo phụ của ma trận vuông.',
        'Cho ma trận vuông kích thước n x n. Hãy tính tổng các phần tử có chỉ số hàng và cột cộng lại bằng n - 1.',
        E'3\n1 2 3\n4 5 6\n7 8 9\n', E'15\n', 'CPP', '2026-09-06T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000227', 'kiem-tra-so-chinh-phuong-cpp', 'Kiểm tra số chính phương',
        'Xác định một số nguyên không âm có phải số chính phương hay không.',
        'Cho số nguyên không âm n. In YES nếu tồn tại số nguyên không âm k sao cho k * k = n, ngược lại in NO.',
        E'49\n', E'YES\n', 'CPP', '2026-09-07T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000228', 'ma-hoa-dich-vong-cpp', 'Mã hóa dịch vòng',
        'Mã hóa chuỗi chữ thường bằng phép dịch Caesar.',
        'Cho chuỗi chỉ gồm chữ cái thường tiếng Anh và số bước dịch k từ 0 đến 25. Mỗi chữ cái được thay bằng chữ cách nó k vị trí trong bảng chữ cái, có quay vòng từ z về a.',
        E'abc 2\n', E'cde\n', 'CPP', '2026-09-08T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000229', 'dem-ky-tu-java', 'Đếm ký tự trong chuỗi',
        'Đếm số lần một ký tự xuất hiện trong chuỗi.',
        'Dòng đầu là chuỗi cần xét, dòng thứ hai là một ký tự. Phân biệt chữ hoa và chữ thường khi đếm.',
        E'banana\na\n', E'3\n', 'JAVA', '2026-09-09T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000230', 'sap-xep-tu-java', 'Sắp xếp từ',
        'Sắp xếp danh sách từ theo thứ tự từ điển tăng dần.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n từ không có khoảng trắng. Hãy in các từ đã sắp xếp, cách nhau bởi một dấu cách.',
        E'3\npear apple kiwi\n', E'apple kiwi pear\n', 'JAVA', '2026-09-10T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000231', 'chuyen-vi-ma-tran-java', 'Chuyển vị ma trận',
        'Tạo ma trận chuyển vị từ ma trận đầu vào.',
        'Cho ma trận có n hàng và m cột. In ma trận chuyển vị gồm m hàng, mỗi hàng có n phần tử; các phần tử trên một hàng cách nhau bởi dấu cách.',
        E'2 3\n1 2 3\n4 5 6\n', E'1 4\n2 5\n3 6\n', 'JAVA', '2026-09-11T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000232', 'dem-nguyen-am-java', 'Đếm nguyên âm',
        'Đếm số nguyên âm trong một dòng văn bản.',
        'Cho một dòng văn bản tiếng Anh. Đếm các ký tự a, e, i, o, u không phân biệt chữ hoa chữ thường.',
        E'DevEdu\n', E'3\n', 'JAVA', '2026-09-12T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000233', 'rut-gon-khoang-trang-python', 'Rút gọn khoảng trắng',
        'Chuẩn hóa khoảng trắng giữa các từ trong một dòng.',
        'Cho một dòng gồm các từ phân tách bởi một hoặc nhiều dấu cách. Hãy bỏ khoảng trắng đầu/cuối và chỉ giữ một dấu cách giữa hai từ.',
        E'hoc   python   vui\n', E'hoc python vui\n', 'PYTHON', '2026-09-13T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000234', 'dem-tu-khong-trung-python', 'Đếm từ không trùng',
        'Đếm số từ khác nhau trong một dòng.',
        'Cho một dòng gồm các từ phân tách bởi dấu cách. Hai từ chỉ được xem là giống nhau khi cùng chính xác chuỗi ký tự, kể cả phân biệt hoa thường.',
        E'code learn code devedu\n', E'3\n', 'PYTHON', '2026-09-14T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000235', 'xoay-mang-phai-python', 'Xoay mảng sang phải',
        'Xoay các phần tử của mảng sang phải k vị trí.',
        'Dòng đầu chứa n và k. Dòng tiếp theo chứa n số nguyên. Xoay vòng mảng sang phải k vị trí và in kết quả cách nhau bởi dấu cách.',
        E'5 2\n1 2 3 4 5\n', E'4 5 1 2 3\n', 'PYTHON', '2026-09-15T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000236', 'phan-tu-xuat-hien-nhieu-nhat-python', 'Phần tử xuất hiện nhiều nhất',
        'Tìm giá trị có tần suất xuất hiện cao nhất trong mảng.',
        'Cho n số nguyên. In giá trị xuất hiện nhiều nhất; nếu có nhiều giá trị cùng tần suất lớn nhất, in giá trị nhỏ nhất.',
        E'7\n1 3 2 3 1 3 2\n', E'3\n', 'PYTHON', '2026-09-16T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000237', 'lop-hinh-vuong-oop', 'Lớp hình vuông',
        'Mô hình hóa hình vuông bằng lớp và tính chu vi.',
        'Tạo lớp Square lưu độ dài cạnh và có phương thức tính chu vi. Đọc cạnh, tạo object và in chu vi của hình vuông.',
        E'5\n', E'20\n', 'OOP', '2026-09-17T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000238', 'lop-hinh-tron-oop', 'Lớp hình tròn',
        'Mô hình hóa hình tròn bằng lớp và tính diện tích.',
        'Tạo lớp Circle lưu bán kính và có phương thức tính diện tích theo công thức 3.14 * r * r. In kết quả với đúng hai chữ số thập phân.',
        E'2\n', E'12.56\n', 'OOP', '2026-09-18T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000239', 'mo-phong-tai-khoan-ngan-hang-oop', 'Tài khoản ngân hàng',
        'Mô phỏng số dư tài khoản bằng một lớp ngân hàng.',
        'Tạo lớp BankAccount quản lý số dư với các thao tác nạp tiền DEPOSIT x và rút tiền WITHDRAW x. Mọi thao tác rút đều hợp lệ. In số dư sau tất cả thao tác.',
        E'100 3\nDEPOSIT 50\nWITHDRAW 20\nDEPOSIT 10\n', E'140\n', 'OOP', '2026-09-19T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000240', 'lop-sinh-vien-diem-trung-binh-oop', 'Điểm trung bình sinh viên',
        'Tính điểm trung bình thông qua một lớp Student.',
        'Tạo lớp Student lưu danh sách điểm nguyên và có phương thức tính trung bình. Dòng đầu chứa n, dòng sau chứa n điểm. In trung bình với đúng hai chữ số thập phân.',
        E'3\n70 80 90\n', E'80.00\n', 'OOP', '2026-09-20T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000241', 'mo-phong-hang-doi-them', 'Mô phỏng hàng đợi',
        'Thực hiện các thao tác cơ bản trên hàng đợi số nguyên.',
        'Dòng đầu chứa q lệnh. Các lệnh là PUSH x, POP hoặc FRONT. Với mỗi lệnh FRONT, in phần tử đầu hàng đợi hoặc EMPTY nếu hàng đợi rỗng. Dữ liệu POP luôn hợp lệ.',
        E'6\nPUSH 4\nPUSH 7\nFRONT\nPOP\nFRONT\nPOP\n', E'4\n7\n', 'DATA_STRUCTURES', '2026-09-21T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000242', 'ngan-xep-min-stack', 'Ngăn xếp lấy giá trị nhỏ nhất',
        'Hỗ trợ lấy phần tử nhỏ nhất hiện tại trong ngăn xếp.',
        'Dòng đầu chứa q lệnh PUSH x, POP hoặc MIN. Với mỗi MIN, in phần tử nhỏ nhất đang có hoặc EMPTY nếu ngăn xếp rỗng. Dữ liệu POP luôn hợp lệ.',
        E'5\nPUSH 3\nPUSH 1\nMIN\nPOP\nMIN\n', E'1\n3\n', 'DATA_STRUCTURES', '2026-09-22T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000243', 'phan-tu-lon-hon-ben-phai', 'Phần tử lớn hơn bên phải',
        'Tìm phần tử đầu tiên lớn hơn ở bên phải mỗi vị trí.',
        'Với mỗi phần tử trong mảng, tìm giá trị đầu tiên lớn hơn nó ở phía bên phải. Nếu không tồn tại, ghi -1. In n kết quả trên một dòng.',
        E'4\n2 1 2 4\n', E'4 2 4 -1\n', 'DATA_STRUCTURES', '2026-09-23T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000244', 'loai-bo-phan-tu-trung-giu-thu-tu', 'Loại bỏ phần tử trùng',
        'Loại bỏ các giá trị lặp lại nhưng giữ thứ tự xuất hiện đầu tiên.',
        'Cho n số nguyên. In mỗi giá trị đúng một lần theo thứ tự xuất hiện đầu tiên, các giá trị cách nhau bởi dấu cách.',
        E'7\n3 1 3 2 1 4 2\n', E'3 1 2 4\n', 'DATA_STRUCTURES', '2026-09-24T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000245', 'tong-doan-con-lien-tiep-lon-nhat-2', 'Tổng đoạn con lớn nhất',
        'Tìm tổng lớn nhất của một đoạn con liên tiếp không rỗng.',
        'Cho mảng gồm n số nguyên. Hãy tìm tổng lớn nhất có thể của một đoạn con liên tiếp không rỗng.',
        E'9\n-2 1 -3 4 -1 2 1 -5 4\n', E'6\n', 'ALGORITHMS', '2026-09-25T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000246', 'hai-so-co-tong-bang-muc-tieu-2', 'Hai số có tổng bằng mục tiêu',
        'Tìm hai chỉ số có phần tử cộng lại bằng giá trị mục tiêu.',
        'Dòng đầu chứa n và target, dòng sau chứa n số nguyên. Tìm cặp chỉ số i < j đầu tiên theo thứ tự i rồi j sao cho a[i] + a[j] = target. In hai chỉ số zero-based; nếu không có cặp, in -1 -1.',
        E'4 9\n2 7 11 15\n', E'0 1\n', 'ALGORITHMS', '2026-09-26T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000247', 'tron-cac-doan-giao-nhau', 'Trộn các đoạn giao nhau',
        'Gộp những đoạn số có phần giao nhau hoặc tiếp giáp.',
        'Cho n đoạn đóng [left, right]. Gộp các đoạn giao nhau hoặc có điểm cuối bằng điểm đầu đoạn kế tiếp. In các đoạn kết quả theo thứ tự tăng dần, mỗi dòng gồm left và right.',
        E'4\n1 3\n2 6\n8 10\n15 18\n', E'1 6\n8 10\n15 18\n', 'ALGORITHMS', '2026-09-27T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000248', 'dem-nhan-vien-theo-phong', 'Đếm nhân viên theo phòng ban',
        'Thống kê số nhân viên trong từng phòng ban bằng SQL.',
        'Bảng employees có các cột id và department. Trả về mỗi phòng ban cùng số nhân viên, sắp xếp tên phòng ban tăng dần.',
        E'CREATE TABLE employees (id INT, department VARCHAR(20));\nINSERT INTO employees VALUES (1, ''IT''), (2, ''HR''), (3, ''IT'');\n', E'HR\t1\nIT\t2\n', 'SQL', '2026-09-28T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000249', 'khach-hang-chua-dat-hang', 'Khách hàng chưa đặt hàng',
        'Tìm khách hàng chưa có đơn hàng nào bằng SQL.',
        'Có bảng customers(id, name) và orders(id, customer_id). Trả về tên khách hàng không xuất hiện trong orders, sắp xếp tên tăng dần.',
        E'CREATE TABLE customers (id INT, name VARCHAR(30));\nCREATE TABLE orders (id INT, customer_id INT);\nINSERT INTO customers VALUES (1, ''Nam''), (2, ''An''), (3, ''Mai'');\nINSERT INTO orders VALUES (1, 1);\n', E'An\nMai\n', 'SQL', '2026-09-29T00:00:00Z'
    ),
    (
        '10000000-0000-0000-0000-000000000250', 'doanh-thu-theo-san-pham', 'Doanh thu theo sản phẩm',
        'Tính doanh thu từng sản phẩm từ các dòng chi tiết đơn hàng.',
        'Có bảng products(id, name) và order_items(product_id, quantity, unit_price). Trả tên sản phẩm cùng tổng quantity * unit_price, sắp xếp doanh thu giảm dần rồi tên tăng dần.',
        E'CREATE TABLE products (id INT, name VARCHAR(30));\nCREATE TABLE order_items (product_id INT, quantity INT, unit_price INT);\nINSERT INTO products VALUES (1, ''Mouse''), (2, ''Keyboard'');\nINSERT INTO order_items VALUES (1, 2, 10), (2, 3, 15), (1, 2, 10);\n', E'Keyboard\t45\nMouse\t40\n', 'SQL', '2026-09-30T00:00:00Z'
    )
ON CONFLICT (id) DO UPDATE SET
    slug = EXCLUDED.slug,
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    description = EXCLUDED.description,
    sample_input = EXCLUDED.sample_input,
    sample_output = EXCLUDED.sample_output,
    topic = EXCLUDED.topic;

-- Additional bundled catalog (251-350). Keeping this as compact seed data
-- makes startup idempotent while preserving separate requirements and samples.
WITH new_problems(seed_number, slug, title, goal, input_description, output_description, sample_input, sample_output, topic) AS (VALUES
    (251, 'tong-chu-so-chan-cua-so', 'Tổng các chữ số chẵn',
        'Tính tổng những chữ số chẵn xuất hiện trong một số nguyên không âm.',
        'Một số nguyên không âm n có không quá 18 chữ số.',
        'In tổng các chữ số chẵn của n.', E'123456\n', E'12\n', 'INTRODUCTION'),
    (252, 'dem-so-nho-hon-trung-binh', 'Đếm số nhỏ hơn trung bình',
        'Đếm các phần tử nhỏ hơn giá trị trung bình cộng của cả dãy.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên; 1 <= n <= 100000.',
        'In số phần tử nhỏ hơn trung bình cộng của dãy.', E'5\n1 2 3 4 10\n', E'3\n', 'INTRODUCTION'),
    (253, 'doi-phut-sang-gio-phut', 'Đổi phút sang giờ và phút',
        'Chuyển tổng số phút thành số giờ và số phút còn lại.',
        'Một số nguyên không âm m là tổng số phút.',
        'In hai số nguyên hours và minutes, cách nhau bởi một dấu cách.', E'135\n', E'2 15\n', 'INTRODUCTION'),
    (254, 'kiem-tra-tam-giac-can', 'Kiểm tra tam giác cân',
        'Xác định ba cạnh có tạo thành một tam giác cân hay không.',
        'Ba số nguyên dương a, b, c là độ dài ba cạnh.',
        'In YES nếu ba cạnh tạo thành tam giác cân, ngược lại in NO.', E'5 5 8\n', E'YES\n', 'INTRODUCTION'),
    (255, 'tong-binh-phuong-tu-1-den-n', 'Tổng bình phương từ 1 đến n',
        'Tính tổng bình phương của các số nguyên từ 1 đến n.',
        'Một số nguyên n; 1 <= n <= 100000.',
        'In giá trị 1^2 + 2^2 + ... + n^2.', E'3\n', E'14\n', 'INTRODUCTION'),
    (256, 'dem-chu-so-le-cua-so', 'Đếm chữ số lẻ',
        'Đếm số chữ số lẻ trong biểu diễn thập phân của một số.',
        'Một số nguyên không âm n có không quá 18 chữ số.',
        'In số lượng chữ số thuộc tập 1, 3, 5, 7, 9.', E'102345\n', E'3\n', 'INTRODUCTION'),
    (257, 'dao-nguoc-so-bo-so-khong-dau', 'Đảo ngược số',
        'Đảo ngược thứ tự chữ số và loại bỏ các số 0 ở đầu kết quả.',
        'Một số nguyên không âm n có không quá 18 chữ số.',
        'In số thu được sau khi đảo ngược các chữ số của n.', E'12030\n', E'3021\n', 'INTRODUCTION'),
    (258, 'tong-boi-so-trong-doan', 'Tổng bội số trong đoạn',
        'Tính tổng các số chia hết cho k trong một đoạn số nguyên.',
        'Ba số nguyên a, b, k với a <= b và k > 0.',
        'In tổng các số x trong đoạn [a, b] thỏa x chia hết cho k.', E'1 10 3\n', E'18\n', 'INTRODUCTION'),
    (259, 'phan-loai-diem-so', 'Phân loại điểm số',
        'Xếp loại một điểm số theo các mức A, B, C, D và F.',
        'Một số nguyên score trong đoạn từ 0 đến 100.',
        'In A nếu score >= 90, B nếu >= 80, C nếu >= 70, D nếu >= 60, còn lại in F.', E'85\n', E'B\n', 'INTRODUCTION'),
    (260, 'so-ngay-trong-thang', 'Số ngày trong tháng',
        'Xác định số ngày của một tháng, có xét năm nhuận.',
        'Hai số nguyên month và year; month từ 1 đến 12, year dương.',
        'In số ngày của tháng đã cho.', E'2 2024\n', E'29\n', 'INTRODUCTION'),
    (261, 'dem-cap-ke-nhau-bang-nhau', 'Đếm cặp kề nhau bằng nhau',
        'Đếm số cặp phần tử kề nhau có cùng giá trị.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.',
        'In số chỉ số i thỏa a[i] = a[i + 1].', E'6\n1 1 2 2 2 3\n', E'3\n', 'INTRODUCTION'),
    (262, 'tinh-giai-thua-kep', 'Tính giai thừa kép',
        'Tính giai thừa kép n!! của một số nguyên không âm.',
        'Một số nguyên n; 0 <= n <= 20.',
        'In n * (n - 2) * (n - 4) * ...; quy ước 0!! = 1 và 1!! = 1.', E'7\n', E'105\n', 'INTRODUCTION'),
    (263, 'kiem-tra-cap-so-cong-ba-so', 'Kiểm tra cấp số cộng',
        'Kiểm tra ba số theo thứ tự đã cho có tạo thành cấp số cộng hay không.',
        'Ba số nguyên a, b, c.',
        'In YES nếu b - a = c - b, ngược lại in NO.', E'3 5 7\n', E'YES\n', 'INTRODUCTION'),
    (264, 'gop-hai-day-da-sap-xep-cpp', 'Gộp hai dãy đã sắp xếp',
        'Gộp hai dãy tăng dần thành một dãy tăng dần, giữ nguyên phần tử trùng.',
        'Dòng đầu chứa n và m. Hai dòng tiếp theo chứa hai dãy đã sắp xếp tăng dần.',
        'In dãy sau khi gộp trên một dòng, các phần tử cách nhau bởi dấu cách.', E'3 3\n1 3 5\n2 4 6\n', E'1 2 3 4 5 6\n', 'CPP'),
    (265, 'dem-tu-trong-dong-cpp', 'Đếm từ trong dòng',
        'Đếm số từ trong một dòng có thể chứa nhiều khoảng trắng liên tiếp.',
        'Một dòng văn bản; từ là chuỗi ký tự không phải khoảng trắng.',
        'In số từ trong dòng.', E'  hoc   lap trinh C++  \n', E'4\n', 'CPP'),
    (266, 'hang-co-tong-lon-nhat-cpp', 'Hàng có tổng lớn nhất',
        'Tìm hàng có tổng phần tử lớn nhất trong ma trận.',
        'Dòng đầu chứa n và m. Tiếp theo là n hàng, mỗi hàng có m số nguyên.',
        'In chỉ số zero-based của hàng có tổng lớn nhất; nếu hòa, chọn hàng có chỉ số nhỏ hơn.', E'3 3\n1 2 3\n4 0 1\n2 2 5\n', E'2\n', 'CPP'),
    (267, 'cot-co-tong-nho-nhat-cpp', 'Cột có tổng nhỏ nhất',
        'Tìm cột có tổng phần tử nhỏ nhất trong ma trận.',
        'Dòng đầu chứa n và m. Tiếp theo là n hàng, mỗi hàng có m số nguyên.',
        'In chỉ số zero-based của cột có tổng nhỏ nhất; nếu hòa, chọn cột có chỉ số nhỏ hơn.', E'2 3\n1 5 2\n3 0 4\n', E'0\n', 'CPP'),
    (268, 'kiem-tra-ma-tran-don-vi-cpp', 'Kiểm tra ma trận đơn vị',
        'Kiểm tra một ma trận vuông có phải ma trận đơn vị hay không.',
        'Dòng đầu chứa n. Tiếp theo là n hàng, mỗi hàng có n số nguyên.',
        'In YES nếu đường chéo chính toàn 1 và các vị trí khác toàn 0, ngược lại in NO.', E'3\n1 0 0\n0 1 0\n0 0 1\n', E'YES\n', 'CPP'),
    (269, 'xoa-phan-tu-tai-vi-tri-cpp', 'Xóa phần tử tại vị trí',
        'Xóa một phần tử khỏi mảng theo chỉ số cho trước.',
        'Dòng đầu chứa n. Dòng thứ hai chứa n số nguyên. Dòng cuối chứa chỉ số zero-based k hợp lệ.',
        'In mảng sau khi xóa phần tử tại k, các phần tử cách nhau bởi dấu cách.', E'5\n1 2 3 4 5\n2\n', E'1 2 4 5\n', 'CPP'),
    (270, 'chen-phan-tu-vao-day-tang-cpp', 'Chèn vào dãy tăng',
        'Chèn một giá trị vào dãy tăng dần và giữ nguyên thứ tự tăng.',
        'Dòng đầu chứa n. Dòng thứ hai chứa n số nguyên tăng dần. Dòng cuối chứa x.',
        'In dãy sau khi chèn x; x được đặt sau các phần tử bằng nó.', E'4\n1 3 5 7\n4\n', E'1 3 4 5 7\n', 'CPP'),
    (271, 'do-dai-tu-dai-nhat-cpp', 'Độ dài từ dài nhất',
        'Tìm độ dài lớn nhất của một từ trong dòng văn bản.',
        'Một dòng gồm các từ phân tách bởi một hoặc nhiều khoảng trắng.',
        'In độ dài của từ dài nhất.', E'hoc lap trinh moi ngay\n', E'5\n', 'CPP'),
    (272, 'dem-ky-tu-so-cpp', 'Đếm ký tự số',
        'Đếm số ký tự chữ số trong một dòng văn bản.',
        'Một dòng có độ dài không quá 100000 ký tự.',
        'In số ký tự từ 0 đến 9 trong dòng.', E'DevEdu 2026!\n', E'4\n', 'CPP'),
    (273, 'ucln-cua-day-cpp', 'Ước chung lớn nhất của dãy',
        'Tìm ước chung lớn nhất của toàn bộ phần tử trong dãy.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên dương.',
        'In ước chung lớn nhất của n số.', E'4\n24 36 60 48\n', E'12\n', 'CPP'),
    (274, 'dao-nguoc-tung-tu-cpp', 'Đảo ngược từng từ',
        'Đảo ngược ký tự của từng từ nhưng giữ nguyên thứ tự các từ.',
        'Một dòng gồm các từ phân tách bởi đúng một dấu cách.',
        'In dòng mới với từng từ được đảo ngược.', E'hoc lap trinh\n', E'coh pal hnirt\n', 'CPP'),
    (275, 'kiem-tra-day-tang-chat-cpp', 'Kiểm tra dãy tăng chặt',
        'Kiểm tra mọi phần tử có lớn hơn phần tử đứng ngay trước nó hay không.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.',
        'In YES nếu dãy tăng chặt, ngược lại in NO.', E'5\n1 3 4 8 10\n', E'YES\n', 'CPP')
)
INSERT INTO programming_problems (
    id, slug, title, summary, description, input_description, output_description,
    sample_input, sample_output, topic, created_at
)
SELECT ('10000000-0000-0000-0000-' || LPAD(seed_number::text, 12, '0'))::uuid,
       slug, title, goal, goal, input_description, output_description,
       sample_input, sample_output, topic, '2026-09-17T00:00:00Z'
FROM new_problems
ON CONFLICT (id) DO UPDATE SET
    slug = EXCLUDED.slug,
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    description = EXCLUDED.description,
    input_description = EXCLUDED.input_description,
    output_description = EXCLUDED.output_description,
    sample_input = EXCLUDED.sample_input,
    sample_output = EXCLUDED.sample_output,
    topic = EXCLUDED.topic;

WITH new_problems(seed_number, slug, title, goal, input_description, output_description, sample_input, sample_output, topic) AS (VALUES
    (276, 'trung-vi-day-le-cpp', 'Trung vị của dãy lẻ',
        'Tìm trung vị của một dãy có số lượng phần tử lẻ.',
        'Dòng đầu chứa n lẻ. Dòng tiếp theo chứa n số nguyên.',
        'Sắp xếp dãy và in phần tử đứng chính giữa.', E'5\n7 1 9 3 5\n', E'5\n', 'CPP'),
    (277, 'tong-chu-so-java', 'Tổng chữ số Java',
        'Tính tổng các chữ số của một số nguyên không âm bằng Java.',
        'Một số nguyên không âm n có không quá 18 chữ số.',
        'In tổng các chữ số của n.', E'12345\n', E'15\n', 'JAVA'),
    (278, 'ky-tu-khong-lap-dau-tien-java', 'Ký tự không lặp đầu tiên',
        'Tìm ký tự đầu tiên chỉ xuất hiện đúng một lần trong chuỗi.',
        'Một chuỗi không chứa khoảng trắng.',
        'In ký tự không lặp đầu tiên, hoặc NONE nếu không tồn tại.', E'swiss\n', E'w\n', 'JAVA'),
    (279, 'tong-bien-ma-tran-java', 'Tổng biên ma trận',
        'Tính tổng các phần tử nằm trên đường biên của ma trận.',
        'Dòng đầu chứa n và m. Tiếp theo là n hàng, mỗi hàng có m số nguyên.',
        'In tổng các phần tử thuộc hàng đầu, hàng cuối, cột đầu hoặc cột cuối; không đếm lặp.', E'3 3\n1 2 3\n4 5 6\n7 8 9\n', E'40\n', 'JAVA'),
    (280, 'chuan-hoa-ho-ten-java', 'Chuẩn hóa họ tên',
        'Chuẩn hóa cách viết hoa và khoảng trắng của một họ tên.',
        'Một dòng họ tên gồm các chữ cái tiếng Anh và dấu cách.',
        'Bỏ khoảng trắng thừa; viết hoa chữ đầu và viết thường các chữ còn lại của mỗi từ.', E'  nGUYEN   van AN  \n', E'Nguyen Van An\n', 'JAVA'),
    (281, 'tien-to-chung-dai-nhat-java', 'Tiền tố chung dài nhất',
        'Tìm tiền tố chung dài nhất của danh sách chuỗi.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n chuỗi không có khoảng trắng.',
        'In tiền tố chung dài nhất, hoặc EMPTY nếu không có.', E'3\nflower flow flight\n', E'fl\n', 'JAVA'),
    (282, 'so-lon-thu-hai-phan-biet-java', 'Số lớn thứ hai phân biệt',
        'Tìm giá trị lớn thứ hai khác với giá trị lớn nhất.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên và luôn có ít nhất hai giá trị khác nhau.',
        'In giá trị lớn thứ hai phân biệt.', E'6\n5 1 5 3 4 4\n', E'4\n', 'JAVA'),
    (283, 'thap-phan-sang-nhi-phan-java', 'Thập phân sang nhị phân',
        'Chuyển một số nguyên không âm từ hệ thập phân sang hệ nhị phân.',
        'Một số nguyên không âm n không vượt quá 10^9.',
        'In biểu diễn nhị phân của n, không có số 0 thừa ở đầu.', E'13\n', E'1101\n', 'JAVA'),
    (284, 'nhi-phan-sang-thap-phan-java', 'Nhị phân sang thập phân',
        'Chuyển một chuỗi nhị phân sang giá trị thập phân.',
        'Một chuỗi gồm các ký tự 0 và 1, độ dài không quá 30.',
        'In giá trị thập phân tương ứng.', E'10110\n', E'22\n', 'JAVA'),
    (285, 'tong-so-le-trong-day-java', 'Tổng các số lẻ Java',
        'Tính tổng các phần tử lẻ trong một dãy số nguyên.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.',
        'In tổng các phần tử không chia hết cho 2.', E'5\n1 2 3 4 5\n', E'9\n', 'JAVA'),
    (286, 'xoay-mang-trai-java', 'Xoay mảng sang trái',
        'Xoay vòng một mảng sang trái k vị trí.',
        'Dòng đầu chứa n và k. Dòng tiếp theo chứa n số nguyên.',
        'In mảng sau khi xoay trái k vị trí.', E'5 2\n1 2 3 4 5\n', E'3 4 5 1 2\n', 'JAVA'),
    (287, 'kiem-tra-anagram-java', 'Kiểm tra Anagram Java',
        'Kiểm tra hai chuỗi có phải hoán vị ký tự của nhau hay không.',
        'Hai dòng, mỗi dòng chứa một chuỗi chữ thường không có khoảng trắng.',
        'In YES nếu hai chuỗi có cùng đa tập ký tự, ngược lại in NO.', E'listen\nsilent\n', E'YES\n', 'JAVA'),
    (288, 'dem-tu-bat-dau-nguyen-am-java', 'Đếm từ bắt đầu bằng nguyên âm',
        'Đếm các từ có chữ cái đầu là nguyên âm.',
        'Một dòng văn bản tiếng Anh, các từ phân tách bởi khoảng trắng.',
        'In số từ bắt đầu bằng a, e, i, o hoặc u, không phân biệt hoa thường.', E'An em hoc online moi ngay\n', E'3\n', 'JAVA'),
    (289, 'giao-hai-mang-java', 'Giao hai mảng Java',
        'Tìm các giá trị phân biệt xuất hiện trong cả hai mảng.',
        'Dòng đầu chứa n và m. Hai dòng tiếp theo chứa hai mảng số nguyên.',
        'In các giá trị chung phân biệt theo thứ tự tăng dần, hoặc EMPTY.', E'5 4\n1 2 2 5 7\n2 3 5 5\n', E'2 5\n', 'JAVA'),
    (290, 'tong-so-am-python', 'Tổng các số âm Python',
        'Tính tổng các phần tử âm trong một dãy.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.',
        'In tổng các phần tử nhỏ hơn 0; nếu không có, in 0.', E'6\n-2 5 -3 0 7 -1\n', E'-6\n', 'PYTHON'),
    (291, 'tu-dai-nhat-python-theo-thu-tu', 'Từ dài nhất Python',
        'Tìm từ dài nhất xuất hiện đầu tiên trong một dòng.',
        'Một dòng gồm các từ phân tách bởi khoảng trắng.',
        'In từ có độ dài lớn nhất; nếu hòa, chọn từ xuất hiện trước.', E'hoc laptrinh Python moi ngay\n', E'laptrinh\n', 'PYTHON'),
    (292, 'tan-suat-ky-tu-python-nang-cao', 'Tần suất ký tự Python',
        'Thống kê tần suất của từng ký tự trong chuỗi.',
        'Một chuỗi chữ thường không chứa khoảng trắng.',
        'In mỗi ký tự và số lần xuất hiện trên một dòng, theo thứ tự ký tự tăng dần.', E'banana\n', E'a 3\nb 1\nn 2\n', 'PYTHON'),
    (293, 'tong-tung-hang-ma-tran-python', 'Tổng từng hàng Python',
        'Tính tổng phần tử của từng hàng trong ma trận.',
        'Dòng đầu chứa n và m. Tiếp theo là n hàng, mỗi hàng có m số nguyên.',
        'In n tổng trên một dòng, theo thứ tự các hàng.', E'2 3\n1 2 3\n4 5 6\n', E'6 15\n', 'PYTHON'),
    (294, 'trai-phang-ma-tran-python', 'Trải phẳng ma trận',
        'Chuyển ma trận thành một dãy theo thứ tự duyệt từng hàng.',
        'Dòng đầu chứa n và m. Tiếp theo là n hàng, mỗi hàng có m số nguyên.',
        'In các phần tử theo thứ tự từ trái sang phải, từ hàng trên xuống hàng dưới.', E'2 2\n1 2\n3 4\n', E'1 2 3 4\n', 'PYTHON'),
    (295, 'so-chan-truoc-so-le-python', 'Số chẵn trước số lẻ',
        'Sắp lại dãy để số chẵn đứng trước số lẻ và giữ thứ tự tương đối.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.',
        'In các số chẵn theo thứ tự cũ, sau đó các số lẻ theo thứ tự cũ.', E'6\n1 2 3 4 5 6\n', E'2 4 6 1 3 5\n', 'PYTHON'),
    (296, 'doi-xung-bo-khoang-trang-python', 'Đối xứng bỏ khoảng trắng',
        'Kiểm tra một dòng có đối xứng khi bỏ khoảng trắng và không phân biệt hoa thường.',
        'Một dòng chỉ gồm chữ cái tiếng Anh và dấu cách.',
        'In YES nếu chuỗi chuẩn hóa là palindrome, ngược lại in NO.', E'A man a plan a canal Panama\n', E'YES\n', 'PYTHON'),
    (297, 'dem-cap-gia-tri-bang-nhau-python', 'Đếm cặp giá trị bằng nhau',
        'Đếm số cặp chỉ số khác nhau có cùng giá trị.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.',
        'In số cặp i < j thỏa a[i] = a[j].', E'5\n1 2 1 2 1\n', E'4\n', 'PYTHON'),
    (298, 'doan-so-khong-dai-nhat-python', 'Đoạn số 0 dài nhất',
        'Tìm độ dài lớn nhất của một đoạn liên tiếp chỉ gồm số 0.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n giá trị 0 hoặc 1.',
        'In độ dài đoạn số 0 liên tiếp dài nhất.', E'8\n1 0 0 1 0 0 0 1\n', E'3\n', 'PYTHON'),
    (299, 'xoay-trai-day-python', 'Xoay trái dãy Python',
        'Xoay vòng dãy sang trái k vị trí.',
        'Dòng đầu chứa n và k. Dòng tiếp theo chứa n số nguyên.',
        'In dãy sau khi xoay trái k modulo n vị trí.', E'5 7\n1 2 3 4 5\n', E'3 4 5 1 2\n', 'PYTHON'),
    (300, 'giao-day-sap-xep-python', 'Giao hai dãy Python',
        'Tìm giao phân biệt của hai dãy và sắp xếp tăng dần.',
        'Dòng đầu chứa n và m. Hai dòng tiếp theo chứa hai dãy số nguyên.',
        'In các giá trị chung phân biệt tăng dần, hoặc EMPTY.', E'4 5\n4 1 2 2\n2 3 4 4 5\n', E'2 4\n', 'PYTHON')
)
INSERT INTO programming_problems (
    id, slug, title, summary, description, input_description, output_description,
    sample_input, sample_output, topic, created_at
)
SELECT ('10000000-0000-0000-0000-' || LPAD(seed_number::text, 12, '0'))::uuid,
       slug, title, goal, goal, input_description, output_description,
       sample_input, sample_output, topic, '2026-09-17T00:00:00Z'
FROM new_problems
ON CONFLICT (id) DO UPDATE SET
    slug = EXCLUDED.slug,
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    description = EXCLUDED.description,
    input_description = EXCLUDED.input_description,
    output_description = EXCLUDED.output_description,
    sample_input = EXCLUDED.sample_input,
    sample_output = EXCLUDED.sample_output,
    topic = EXCLUDED.topic;

WITH new_problems(seed_number, slug, title, goal, input_description, output_description, sample_input, sample_output, topic) AS (VALUES
    (301, 'sap-xep-theo-tri-tuyet-do-python', 'Sắp xếp theo trị tuyệt đối',
        'Sắp xếp dãy theo trị tuyệt đối tăng dần rồi theo giá trị tăng dần.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.',
        'In dãy theo khóa (abs(x), x) tăng dần.', E'6\n-5 2 -2 3 0 -1\n', E'0 -1 -2 2 3 -5\n', 'PYTHON'),
    (302, 'nen-chuoi-rle-python', 'Nén chuỗi RLE',
        'Nén các nhóm ký tự giống nhau liên tiếp bằng độ dài nhóm.',
        'Một chuỗi chữ thường không rỗng và không chứa khoảng trắng.',
        'Với mỗi nhóm liên tiếp, in ký tự rồi số lần lặp; nối các nhóm không có dấu cách.', E'aaabbc\n', E'a3b2c1\n', 'PYTHON'),
    (303, 'chu-vi-hinh-chu-nhat-oop', 'Chu vi hình chữ nhật OOP',
        'Mô hình hóa hình chữ nhật và tính chu vi qua phương thức của object.',
        'Hai số nguyên dương width và height.',
        'Tạo lớp Rectangle và in chu vi 2 * (width + height).', E'4 6\n', E'20\n', 'OOP'),
    (304, 'the-tich-hinh-tru-oop', 'Thể tích hình trụ OOP',
        'Mô hình hóa hình trụ và tính thể tích với pi bằng 3.14.',
        'Hai số thực dương radius và height.',
        'Tạo lớp Cylinder và in 3.14 * radius^2 * height với đúng hai chữ số thập phân.', E'2 5\n', E'62.80\n', 'OOP'),
    (305, 'san-pham-giam-gia-oop', 'Sản phẩm giảm giá',
        'Mô hình hóa sản phẩm và tính giá sau khi áp dụng phần trăm giảm.',
        'Hai số thực price và discountPercent; 0 <= discountPercent <= 100.',
        'Tạo lớp Product và in giá sau giảm với đúng hai chữ số thập phân.', E'200 15\n', E'170.00\n', 'OOP'),
    (306, 'luong-lam-them-oop', 'Lương làm thêm',
        'Tính lương nhân viên với giờ làm thêm được nhân hệ số 1.5.',
        'Hai số thực hours và hourlyRate. Tối đa 40 giờ tính hệ số 1, phần còn lại tính hệ số 1.5.',
        'Tạo lớp Employee và in tổng lương với đúng hai chữ số thập phân.', E'45 20\n', E'950.00\n', 'OOP'),
    (307, 'bo-chuyen-doi-nhiet-do-oop', 'Bộ chuyển đổi nhiệt độ',
        'Đóng gói phép chuyển đổi từ độ C sang độ F trong một lớp.',
        'Một số thực celsius.',
        'Tạo lớp TemperatureConverter và in celsius * 9 / 5 + 32 với đúng hai chữ số thập phân.', E'25\n', E'77.00\n', 'OOP'),
    (308, 'phan-so-toi-gian-oop', 'Phân số tối giản',
        'Mô hình hóa phân số và rút gọn bằng ước chung lớn nhất.',
        'Hai số nguyên numerator và denominator, denominator khác 0.',
        'Tạo lớp Fraction và in phân số tối giản dạng numerator/denominator với mẫu số dương.', E'8 12\n', E'2/3\n', 'OOP'),
    (309, 'cong-so-phuc-oop', 'Cộng số phức',
        'Mô hình hóa số phức và cộng hai object số phức.',
        'Bốn số nguyên a, b, c, d biểu diễn a + bi và c + di.',
        'In phần thực và phần ảo của tổng, cách nhau bởi một dấu cách.', E'2 3 4 -1\n', E'6 2\n', 'OOP'),
    (310, 'tich-vo-huong-vector-oop', 'Tích vô hướng vector',
        'Mô hình hóa hai vector hai chiều và tính tích vô hướng.',
        'Bốn số nguyên x1, y1, x2, y2.',
        'Tạo lớp Vector2D và in x1 * x2 + y1 * y2.', E'2 3 4 5\n', E'23\n', 'OOP'),
    (311, 'tien-phat-sach-qua-han-oop', 'Tiền phạt sách quá hạn',
        'Mô hình hóa lượt mượn sách và tính tiền phạt theo số ngày quá hạn.',
        'Một số nguyên không âm overdueDays. Mỗi ngày quá hạn bị phạt 2000.',
        'Tạo lớp BookLoan và in tổng tiền phạt.', E'7\n', E'14000\n', 'OOP'),
    (312, 'cuoc-taxi-oop', 'Tính cước taxi',
        'Mô hình hóa chuyến taxi và tính cước theo quãng đường nguyên.',
        'Một số nguyên km >= 1. Kilomet đầu giá 10000, mỗi kilomet tiếp theo giá 8000.',
        'Tạo lớp TaxiTrip và in tổng cước.', E'5\n', E'42000\n', 'OOP'),
    (313, 'tiet-kiem-lai-don-oop', 'Tiết kiệm lãi đơn',
        'Mô hình hóa khoản tiết kiệm và tính số tiền cuối kỳ theo lãi đơn.',
        'Ba số thực principal, annualRatePercent và years.',
        'Tạo lớp SavingsAccount và in principal * (1 + rate / 100 * years) với hai chữ số thập phân.', E'1000 5 2\n', E'1100.00\n', 'OOP'),
    (314, 'xep-loai-sinh-vien-oop', 'Xếp loại sinh viên OOP',
        'Mô hình hóa sinh viên, tính điểm trung bình và xác định đạt hay chưa đạt.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n điểm thực.',
        'Tạo lớp Student; in trung bình với hai chữ số thập phân và PASS nếu trung bình >= 5, ngược lại FAIL.', E'3\n6 7 5\n', E'6.00 PASS\n', 'OOP'),
    (315, 'kiem-tra-ngoac-ba-loai', 'Kiểm tra ngoặc ba loại',
        'Kiểm tra tính hợp lệ của chuỗi gồm ngoặc tròn, vuông và nhọn.',
        'Một chuỗi chỉ gồm các ký tự (), [] và {}.',
        'In YES nếu mọi ngoặc được đóng đúng loại và đúng thứ tự, ngược lại in NO.', E'{[()]}\n', E'YES\n', 'DATA_STRUCTURES'),
    (316, 'tinh-bieu-thuc-hau-to', 'Tính biểu thức hậu tố',
        'Tính giá trị biểu thức hậu tố bằng ngăn xếp.',
        'Một dòng gồm số nguyên và các toán tử +, -, *, / phân tách bởi dấu cách; phép chia nguyên và biểu thức hợp lệ.',
        'In giá trị nguyên của biểu thức.', E'2 3 4 * +\n', E'14\n', 'DATA_STRUCTURES'),
    (317, 'dao-nguoc-hang-doi', 'Đảo ngược hàng đợi',
        'Đảo ngược thứ tự các phần tử của một hàng đợi.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên theo thứ tự từ đầu đến cuối hàng đợi.',
        'In các phần tử từ cuối về đầu.', E'5\n1 2 3 4 5\n', E'5 4 3 2 1\n', 'DATA_STRUCTURES'),
    (318, 'mo-phong-undo-van-ban', 'Mô phỏng Undo văn bản',
        'Dùng ngăn xếp để hoàn tác thao tác nối ký tự vào văn bản.',
        'Dòng đầu chứa q. Mỗi dòng sau là TYPE c, UNDO hoặc PRINT; UNDO khi rỗng không làm gì.',
        'Với mỗi PRINT, in văn bản hiện tại hoặc EMPTY nếu rỗng.', E'6\nTYPE a\nTYPE b\nPRINT\nUNDO\nPRINT\nUNDO\n', E'ab\na\n', 'DATA_STRUCTURES'),
    (319, 'bang-tan-suat-so-nguyen', 'Bảng tần suất số nguyên',
        'Dùng cấu trúc ánh xạ để đếm tần suất các giá trị.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.',
        'In mỗi giá trị phân biệt và tần suất trên một dòng, theo giá trị tăng dần.', E'6\n3 1 3 2 1 3\n', E'1 2\n2 1\n3 3\n', 'DATA_STRUCTURES'),
    (320, 'ky-tu-khong-lap-dau-tien-queue', 'Ký tự không lặp đầu tiên',
        'Tìm ký tự đầu tiên có tần suất bằng một bằng hàng đợi và bảng đếm.',
        'Một chuỗi chữ thường không chứa khoảng trắng.',
        'In ký tự đầu tiên xuất hiện đúng một lần, hoặc NONE.', E'aabbcdde\n', E'c\n', 'DATA_STRUCTURES'),
    (321, 'cuc-dai-cua-so-truot', 'Cực đại cửa sổ trượt',
        'Tìm giá trị lớn nhất của mọi cửa sổ liên tiếp kích thước k.',
        'Dòng đầu chứa n và k. Dòng tiếp theo chứa n số nguyên.',
        'In n - k + 1 giá trị lớn nhất, cách nhau bởi dấu cách.', E'8 3\n1 3 -1 -3 5 3 6 7\n', E'3 3 5 5 6 7\n', 'DATA_STRUCTURES'),
    (322, 'tong-theo-tang-cay-nhi-phan', 'Tổng theo tầng cây nhị phân',
        'Tính tổng giá trị ở từng tầng của cây nhị phân đầy theo biểu diễn mức.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n giá trị node theo thứ tự level-order, không có node rỗng.',
        'In tổng từng tầng từ gốc xuống, cách nhau bởi dấu cách.', E'7\n1 2 3 4 5 6 7\n', E'1 5 22\n', 'DATA_STRUCTURES'),
    (323, 'danh-sach-lien-ket-them-dau-cuoi', 'Thêm đầu cuối danh sách liên kết',
        'Mô phỏng thêm phần tử vào đầu hoặc cuối danh sách liên kết.',
        'Dòng đầu chứa q. Mỗi dòng sau là HEAD x hoặc TAIL x.',
        'In danh sách cuối cùng từ đầu đến cuối.', E'4\nTAIL 2\nHEAD 1\nTAIL 3\nHEAD 0\n', E'0 1 2 3\n', 'DATA_STRUCTURES'),
    (324, 'phan-tu-lon-thu-k-priority-queue', 'Phần tử lớn thứ k',
        'Tìm phần tử lớn thứ k bằng hàng đợi ưu tiên.',
        'Dòng đầu chứa n và k. Dòng tiếp theo chứa n số nguyên; phần tử trùng được tính riêng.',
        'In phần tử đứng thứ k trong dãy sắp xếp giảm dần.', E'6 3\n7 4 6 3 9 1\n', E'6\n', 'DATA_STRUCTURES'),
    (325, 'kiem-tra-doi-xung-deque', 'Kiểm tra đối xứng bằng Deque',
        'Kiểm tra chuỗi đối xứng bằng cách so sánh hai đầu deque.',
        'Một chuỗi không chứa khoảng trắng.',
        'In YES nếu chuỗi đọc xuôi và ngược giống nhau, ngược lại in NO.', E'racecar\n', E'YES\n', 'DATA_STRUCTURES')
)
INSERT INTO programming_problems (
    id, slug, title, summary, description, input_description, output_description,
    sample_input, sample_output, topic, created_at
)
SELECT ('10000000-0000-0000-0000-' || LPAD(seed_number::text, 12, '0'))::uuid,
       slug, title, goal, goal, input_description, output_description,
       sample_input, sample_output, topic, '2026-09-17T00:00:00Z'
FROM new_problems
ON CONFLICT (id) DO UPDATE SET
    slug = EXCLUDED.slug,
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    description = EXCLUDED.description,
    input_description = EXCLUDED.input_description,
    output_description = EXCLUDED.output_description,
    sample_input = EXCLUDED.sample_input,
    sample_output = EXCLUDED.sample_output,
    topic = EXCLUDED.topic;

WITH new_problems(seed_number, slug, title, goal, input_description, output_description, sample_input, sample_output, topic) AS (VALUES
    (326, 'hop-nhat-tap-hop-dsu', 'Hợp nhất tập hợp DSU',
        'Xử lý thao tác hợp nhất và kiểm tra liên thông bằng Disjoint Set Union.',
        'Dòng đầu chứa n và q. Mỗi dòng sau là UNION u v hoặc SAME u v, các đỉnh đánh số từ 1.',
        'Với mỗi SAME, in YES nếu hai đỉnh cùng tập hợp, ngược lại in NO.', E'5 5\nUNION 1 2\nSAME 1 2\nSAME 1 3\nUNION 2 3\nSAME 1 3\n', E'YES\nNO\nYES\n', 'DATA_STRUCTURES'),
    (327, 'vi-tri-chen-lower-bound', 'Vị trí chèn Lower Bound',
        'Tìm vị trí đầu tiên có thể chèn x mà vẫn giữ dãy tăng dần.',
        'Dòng đầu chứa n và x. Dòng tiếp theo chứa n số nguyên tăng dần.',
        'In chỉ số zero-based đầu tiên i sao cho a[i] >= x, hoặc n nếu không có.', E'5 4\n1 3 4 4 7\n', E'2\n', 'ALGORITHMS'),
    (328, 'dem-cap-nghich-the-nang-cao', 'Đếm cặp nghịch thế',
        'Đếm số cặp chỉ số i < j nhưng a[i] > a[j].',
        'Dòng đầu chứa n; dòng sau chứa n số nguyên. 1 <= n <= 200000.',
        'In số cặp nghịch thế; dùng kiểu số nguyên 64 bit.', E'5\n2 4 1 3 5\n', E'3\n', 'ALGORITHMS'),
    (329, 'do-dai-day-con-tang-dai-nhat', 'Độ dài dãy con tăng dài nhất',
        'Tìm độ dài dãy con tăng chặt dài nhất, không yêu cầu liên tiếp.',
        'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.',
        'In độ dài LIS tăng chặt.', E'8\n10 9 2 5 3 7 101 18\n', E'4\n', 'ALGORITHMS'),
    (330, 'do-dai-day-con-chung-dai-nhat', 'Độ dài dãy con chung dài nhất',
        'Tìm độ dài dãy con chung dài nhất của hai chuỗi.',
        'Hai dòng, mỗi dòng chứa một chuỗi không có khoảng trắng; độ dài mỗi chuỗi không quá 1000.',
        'In độ dài LCS của hai chuỗi.', E'abcde\nace\n', E'3\n', 'ALGORITHMS'),
    (331, 'doi-tien-so-luong-it-nhat', 'Đổi tiền ít đồng nhất',
        'Tìm số đồng xu ít nhất để tạo đúng một giá trị mục tiêu.',
        'Dòng đầu chứa n và target. Dòng tiếp theo chứa n mệnh giá nguyên dương, có thể dùng không giới hạn.',
        'In số đồng xu ít nhất, hoặc -1 nếu không thể tạo target.', E'3 6\n1 3 4\n', E'2\n', 'ALGORITHMS'),
    (332, 'dem-duong-di-luoi-co-vat-can', 'Đếm đường đi trên lưới',
        'Đếm số đường từ góc trên trái đến góc dưới phải khi chỉ đi sang phải hoặc xuống.',
        'Dòng đầu chứa n và m. Tiếp theo là lưới 0/1; 1 là ô bị chặn, hai đầu mút luôn là 0.',
        'In số đường đi modulo 1000000007.', E'3 3\n0 0 0\n0 1 0\n0 0 0\n', E'2\n', 'ALGORITHMS'),
    (333, 'duong-di-ngan-nhat-do-thi-khong-trong-so', 'Đường đi ngắn nhất không trọng số',
        'Tìm số cạnh ít nhất giữa hai đỉnh bằng BFS.',
        'Dòng đầu chứa n, m, s, t. Tiếp theo là m cạnh vô hướng u v.',
        'In khoảng cách từ s đến t, hoặc -1 nếu không có đường đi.', E'5 5 1 5\n1 2\n2 3\n3 5\n1 4\n4 5\n', E'2\n', 'ALGORITHMS'),
    (334, 'dem-thanh-phan-lien-thong', 'Đếm thành phần liên thông',
        'Đếm số thành phần liên thông của một đồ thị vô hướng.',
        'Dòng đầu chứa n và m. Tiếp theo là m cạnh u v; các đỉnh từ 1 đến n.',
        'In số thành phần liên thông, kể cả đỉnh cô lập.', E'6 3\n1 2\n2 3\n4 5\n', E'3\n', 'ALGORITHMS'),
    (335, 'dijkstra-duong-di-ngan-nhat', 'Dijkstra đường đi ngắn nhất',
        'Tìm khoảng cách ngắn nhất từ một đỉnh nguồn trong đồ thị có trọng số không âm.',
        'Dòng đầu chứa n, m, s. Tiếp theo là m cạnh có hướng u v w với w không âm.',
        'In n khoảng cách từ s theo thứ tự đỉnh 1..n; đỉnh không tới được in -1.', E'4 5 1\n1 2 2\n1 3 5\n2 3 1\n2 4 4\n3 4 1\n', E'0 2 3 4\n', 'ALGORITHMS'),
    (336, 'chon-nhieu-khoang-khong-giao-nhau', 'Chọn nhiều khoảng không giao nhau',
        'Chọn số lượng lớn nhất các khoảng thời gian không chồng lấn.',
        'Dòng đầu chứa n. Mỗi dòng sau chứa start và end với start < end; khoảng sau được bắt đầu tại lúc khoảng trước kết thúc.',
        'In số khoảng tối đa có thể chọn.', E'5\n1 2\n2 3\n3 4\n1 3\n2 5\n', E'3\n', 'ALGORITHMS'),
    (337, 'luy-thua-nhanh-modulo', 'Lũy thừa nhanh modulo',
        'Tính a^b modulo m bằng thuật toán lũy thừa nhị phân.',
        'Ba số nguyên a, b, m với b >= 0 và m > 0.',
        'In a^b mod m.', E'2 10 1000\n', E'24\n', 'ALGORITHMS'),
    (338, 'dem-so-nguyen-to-trong-doan', 'Đếm số nguyên tố trong đoạn',
        'Đếm số nguyên tố trong đoạn đóng bằng sàng.',
        'Hai số nguyên l và r với 1 <= l <= r <= 1000000.',
        'In số lượng số nguyên tố thuộc [l, r].', E'10 30\n', E'6\n', 'ALGORITHMS'),
    (339, 'luong-cao-nhat-moi-phong-ban', 'Lương cao nhất mỗi phòng ban',
        'Tìm mức lương cao nhất của từng phòng ban.',
        'Tạo bảng employees(id, department, salary) và nạp dữ liệu theo mô tả.',
        'Trả department và MAX(salary), sắp xếp department tăng dần.', E'CREATE TABLE employees (id INT, department VARCHAR(20), salary INT);\nINSERT INTO employees VALUES (1, ''IT'', 100), (2, ''HR'', 80), (3, ''IT'', 130), (4, ''HR'', 90);\n', E'HR\t90\nIT\t130\n', 'SQL'),
    (340, 'dem-don-hang-moi-khach-hang', 'Đếm đơn hàng mỗi khách hàng',
        'Đếm số đơn hàng của từng khách hàng, kể cả khách chưa đặt hàng.',
        'Tạo bảng customers(id, name) và orders(id, customer_id) theo dữ liệu mẫu.',
        'Trả name và số đơn hàng; sắp xếp name tăng dần.', E'CREATE TABLE customers (id INT, name VARCHAR(30));\nCREATE TABLE orders (id INT, customer_id INT);\nINSERT INTO customers VALUES (1, ''An''), (2, ''Binh''), (3, ''Chi'');\nINSERT INTO orders VALUES (1, 1), (2, 1), (3, 2);\n', E'An\t2\nBinh\t1\nChi\t0\n', 'SQL'),
    (341, 'san-pham-cao-hon-trung-binh-danh-muc', 'Sản phẩm cao hơn trung bình danh mục',
        'Tìm sản phẩm có giá cao hơn giá trung bình trong chính danh mục.',
        'Tạo bảng products(id, name, category, price) theo dữ liệu mẫu.',
        'Trả name và price của sản phẩm thỏa điều kiện, sắp xếp name tăng dần.', E'CREATE TABLE products (id INT, name VARCHAR(30), category VARCHAR(20), price INT);\nINSERT INTO products VALUES (1, ''A1'', ''A'', 100), (2, ''A2'', ''A'', 200), (3, ''B1'', ''B'', 80), (4, ''B2'', ''B'', 120);\n', E'A2\t200\nB2\t120\n', 'SQL'),
    (342, 'doanh-thu-hang-thang-sql', 'Doanh thu hàng tháng',
        'Tổng hợp doanh thu theo tháng.',
        'Tạo bảng orders(id, order_month, amount) theo dữ liệu mẫu.',
        'Trả order_month và SUM(amount), sắp xếp tháng tăng dần.', E'CREATE TABLE orders (id INT, order_month INT, amount INT);\nINSERT INTO orders VALUES (1, 1, 100), (2, 2, 70), (3, 1, 50), (4, 3, 90);\n', E'1\t150\n2\t70\n3\t90\n', 'SQL'),
    (343, 'email-bi-trung-sql', 'Email bị trùng',
        'Tìm các email xuất hiện nhiều hơn một lần.',
        'Tạo bảng users(id, email) theo dữ liệu mẫu.',
        'Trả email và số lần xuất hiện cho các email có COUNT(*) > 1, sắp xếp email tăng dần.', E'CREATE TABLE users (id INT, email VARCHAR(60));\nINSERT INTO users VALUES (1, ''a@mail.com''), (2, ''b@mail.com''), (3, ''a@mail.com''), (4, ''c@mail.com''), (5, ''b@mail.com'');\n', E'a@mail.com\t2\nb@mail.com\t2\n', 'SQL'),
    (344, 'diem-cao-nhat-moi-lop-sql', 'Điểm cao nhất mỗi lớp',
        'Tìm mọi sinh viên cùng đạt điểm cao nhất trong từng lớp.',
        'Tạo bảng students(id, name, class_name, score) theo dữ liệu mẫu.',
        'Trả class_name, name, score của các sinh viên đứng đầu; sắp xếp class_name rồi name tăng dần.', E'CREATE TABLE students (id INT, name VARCHAR(30), class_name VARCHAR(10), score INT);\nINSERT INTO students VALUES (1, ''An'', ''A'', 9), (2, ''Binh'', ''A'', 8), (3, ''Chi'', ''B'', 10), (4, ''Dung'', ''B'', 10);\n', E'A\tAn\t9\nB\tChi\t10\nB\tDung\t10\n', 'SQL'),
    (345, 'nhan-vien-khong-co-quan-ly', 'Nhân viên không có quản lý',
        'Liệt kê nhân viên cấp cao không có người quản lý trực tiếp.',
        'Tạo bảng employees(id, name, manager_id) theo dữ liệu mẫu.',
        'Trả name của nhân viên có manager_id IS NULL, sắp xếp name tăng dần.', E'CREATE TABLE employees (id INT, name VARCHAR(30), manager_id INT);\nINSERT INTO employees VALUES (1, ''An'', NULL), (2, ''Binh'', 1), (3, ''Chi'', NULL);\n', E'An\nChi\n', 'SQL'),
    (346, 'don-hang-moi-nhat-moi-khach-hang', 'Đơn hàng mới nhất mỗi khách hàng',
        'Tìm ngày đặt hàng mới nhất của từng khách hàng đã có đơn.',
        'Tạo bảng customers(id, name) và orders(id, customer_id, order_date DATE).',
        'Trả name và ngày mới nhất, sắp xếp name tăng dần.', E'CREATE TABLE customers (id INT, name VARCHAR(30));\nCREATE TABLE orders (id INT, customer_id INT, order_date DATE);\nINSERT INTO customers VALUES (1, ''An''), (2, ''Binh'');\nINSERT INTO orders VALUES (1, 1, ''2026-01-01''), (2, 1, ''2026-02-10''), (3, 2, ''2026-03-05'');\n', E'An\t2026-02-10\nBinh\t2026-03-05\n', 'SQL'),
    (347, 'so-du-luy-ke-sql', 'Số dư lũy kế',
        'Tính số dư lũy kế theo thứ tự giao dịch.',
        'Tạo bảng transactions(id, amount) theo dữ liệu mẫu; id xác định thứ tự.',
        'Trả id, amount và tổng amount lũy kế đến id hiện tại, sắp xếp id tăng dần.', E'CREATE TABLE transactions (id INT, amount INT);\nINSERT INTO transactions VALUES (1, 100), (2, -30), (3, 50);\n', E'1\t100\t100\n2\t-30\t70\n3\t50\t120\n', 'SQL'),
    (348, 'luong-cao-thu-hai-phan-biet-sql', 'Lương cao thứ hai phân biệt',
        'Tìm mức lương lớn thứ hai khác với mức lương cao nhất.',
        'Tạo bảng employees(id, salary) và bảo đảm có ít nhất hai mức lương khác nhau.',
        'Trả một giá trị là mức lương cao thứ hai phân biệt.', E'CREATE TABLE employees (id INT, salary INT);\nINSERT INTO employees VALUES (1, 100), (2, 200), (3, 200), (4, 150);\n', E'150\n', 'SQL'),
    (349, 'gia-tri-ton-kho-sql', 'Giá trị tồn kho',
        'Tính tổng giá trị tồn kho của từng sản phẩm.',
        'Tạo bảng inventory(product, quantity, unit_price) theo dữ liệu mẫu.',
        'Trả product và SUM(quantity * unit_price), sắp xếp product tăng dần.', E'CREATE TABLE inventory (product VARCHAR(30), quantity INT, unit_price INT);\nINSERT INTO inventory VALUES (''Keyboard'', 2, 30), (''Mouse'', 5, 10), (''Mouse'', 3, 10);\n', E'Keyboard\t60\nMouse\t80\n', 'SQL'),
    (350, 'danh-muc-chua-co-san-pham', 'Danh mục chưa có sản phẩm',
        'Tìm các danh mục chưa được gắn với sản phẩm nào.',
        'Tạo bảng categories(id, name) và products(id, category_id) theo dữ liệu mẫu.',
        'Trả name của danh mục không có sản phẩm, sắp xếp name tăng dần.', E'CREATE TABLE categories (id INT, name VARCHAR(30));\nCREATE TABLE products (id INT, category_id INT);\nINSERT INTO categories VALUES (1, ''Laptop''), (2, ''Mouse''), (3, ''Phone'');\nINSERT INTO products VALUES (1, 1), (2, 3);\n', E'Mouse\n', 'SQL')
)
INSERT INTO programming_problems (
    id, slug, title, summary, description, input_description, output_description,
    sample_input, sample_output, topic, created_at
)
SELECT ('10000000-0000-0000-0000-' || LPAD(seed_number::text, 12, '0'))::uuid,
       slug, title, goal, goal, input_description, output_description,
       sample_input, sample_output, topic, '2026-09-17T00:00:00Z'
FROM new_problems
ON CONFLICT (id) DO UPDATE SET
    slug = EXCLUDED.slug,
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    description = EXCLUDED.description,
    input_description = EXCLUDED.input_description,
    output_description = EXCLUDED.output_description,
    sample_input = EXCLUDED.sample_input,
    sample_output = EXCLUDED.sample_output,
    topic = EXCLUDED.topic;

-- Curated exercises 351-450. Keep these rows idempotent so both an existing
-- development database and a fresh database receive the same catalog.
WITH new_problems(seed_number, slug, title, goal, input_description, output_description, sample_input, sample_output, topic) AS (VALUES
    (351, 'dem-so-uoc-duong', 'Đếm số ước dương',
        'Đếm số lượng ước nguyên dương của một số nguyên.',
        'Một dòng chứa số nguyên dương n, với n không vượt quá 10^9.',
        'In số lượng ước nguyên dương của n.', E'12\n', E'6\n', 'INTRODUCTION'),
    (352, 'nam-nhuan-theo-lich-gregory', 'Kiểm tra năm nhuận',
        'Xác định một năm dương lịch có phải năm nhuận hay không.',
        'Một dòng chứa số nguyên year trong khoảng từ 1 đến 10^9.',
        'In YES nếu year là năm nhuận, ngược lại in NO.', E'2024\n', E'YES\n', 'INTRODUCTION'),
    (353, 'doi-do-c-sang-do-f', 'Đổi độ C sang độ F',
        'Chuyển nhiệt độ từ thang Celsius sang Fahrenheit.',
        'Một dòng chứa số thực c là nhiệt độ Celsius.',
        'In nhiệt độ Fahrenheit theo công thức f = c * 9 / 5 + 32, làm tròn đến hai chữ số thập phân.', E'25\n', E'77.00\n', 'INTRODUCTION'),
    (354, 'chu-vi-hinh-tron', 'Chu vi hình tròn',
        'Tính chu vi hình tròn từ bán kính cho trước.',
        'Một dòng chứa số thực dương r. Sử dụng pi = 3.141592653589793.',
        'In chu vi 2 * pi * r, làm tròn đến hai chữ số thập phân.', E'5\n', E'31.42\n', 'INTRODUCTION'),
    (355, 'tong-cac-chu-so-chan', 'Tổng các chữ số chẵn',
        'Tính tổng những chữ số chẵn trong biểu diễn thập phân của một số.',
        'Một dòng chứa số nguyên không âm n có không quá 18 chữ số.',
        'In tổng các chữ số chẵn của n.', E'482731\n', E'14\n', 'INTRODUCTION'),
    (356, 'dao-nguoc-chu-so-khong-am', 'Đảo ngược số nguyên',
        'Đảo thứ tự các chữ số của một số nguyên không âm.',
        'Một dòng chứa số nguyên không âm n không vượt quá 10^18.',
        'In số nhận được sau khi đảo chữ số và bỏ các số 0 thừa ở đầu.', E'12040\n', E'4021\n', 'INTRODUCTION'),
    (357, 'dem-chu-cai-in-hoa', 'Đếm chữ cái in hoa',
        'Đếm số chữ cái tiếng Anh viết hoa trong một dòng văn bản.',
        'Một dòng văn bản có độ dài không quá 100000 ký tự.',
        'In số ký tự nằm trong khoảng A đến Z.', E'DevEdu LMS 2026\n', E'5\n', 'INTRODUCTION'),
    (358, 'ky-tu-dau-va-cuoi', 'Ký tự đầu và cuối',
        'Lấy ký tự đầu tiên và ký tự cuối cùng của một chuỗi.',
        'Một dòng chứa chuỗi không rỗng và không có khoảng trắng.',
        'In ký tự đầu và ký tự cuối, cách nhau bởi một dấu cách.', E'DevEdu\n', E'D u\n', 'INTRODUCTION'),
    (359, 'bang-cuu-chuong-cua-n', 'Bảng cửu chương của N',
        'In mười tích đầu tiên của một số nguyên.',
        'Một dòng chứa số nguyên n có trị tuyệt đối không vượt quá 10^6.',
        'In lần lượt n * 1 đến n * 10 trên một dòng, cách nhau bởi một dấu cách.', E'3\n', E'3 6 9 12 15 18 21 24 27 30\n', 'INTRODUCTION'),
    (360, 'tong-uoc-bang-chinh-no', 'Kiểm tra số hoàn hảo',
        'Kiểm tra một số có bằng tổng các ước dương nhỏ hơn chính nó hay không.',
        'Một dòng chứa số nguyên dương n không vượt quá 10^7.',
        'In YES nếu n là số hoàn hảo, ngược lại in NO.', E'28\n', E'YES\n', 'INTRODUCTION'),
    (361, 'phan-loai-chi-so-bmi', 'Phân loại chỉ số BMI',
        'Tính BMI và phân loại theo các ngưỡng cơ bản.',
        'Một dòng chứa cân nặng kg và chiều cao mét, đều là số thực dương.',
        'In UNDERWEIGHT nếu BMI < 18.5, NORMAL nếu BMI < 25, OVERWEIGHT nếu BMI < 30, ngược lại in OBESE.', E'60 1.65\n', E'NORMAL\n', 'INTRODUCTION'),
    (362, 'tinh-tien-dien-bac-thang', 'Tính tiền điện bậc thang',
        'Tính tiền điện theo ba bậc tiêu thụ liên tiếp.',
        'Một dòng chứa số điện kWh nguyên không âm. 50 kWh đầu giá 1000, 50 kWh tiếp giá 1500, phần còn lại giá 2000 đồng.',
        'In tổng số tiền điện phải trả dưới dạng số nguyên.', E'120\n', E'165000\n', 'INTRODUCTION'),
    (363, 'so-ngay-cua-thang', 'Số ngày của tháng',
        'Tìm số ngày của một tháng trong một năm cụ thể.',
        'Một dòng chứa hai số nguyên month và year hợp lệ.',
        'In số ngày của month trong year, có xét năm nhuận.', E'2 2024\n', E'29\n', 'INTRODUCTION'),
    (364, 'giai-phuong-trinh-bac-nhat', 'Giải phương trình bậc nhất',
        'Giải phương trình a * x + b = 0 với hệ số nguyên.',
        'Một dòng chứa hai số nguyên a và b, trong đó a khác 0.',
        'In nghiệm x làm tròn đến hai chữ số thập phân.', E'2 -5\n', E'2.50\n', 'INTRODUCTION'),
    (365, 'doi-giay-thanh-thoi-gian', 'Đổi giây thành thời gian',
        'Chuyển tổng số giây thành giờ, phút và giây.',
        'Một dòng chứa số nguyên không âm s nhỏ hơn 86400.',
        'In ba số h m s lần lượt là giờ, phút và giây còn lại.', E'7384\n', E'2 3 4\n', 'INTRODUCTION'),

    (366, 'duong-cheo-phu-ma-tran-cpp', 'Tổng đường chéo phụ C++',
        'Tính tổng các phần tử trên đường chéo phụ của ma trận vuông.',
        'Dòng đầu chứa n. Tiếp theo là n hàng, mỗi hàng có n số nguyên.',
        'In tổng các phần tử a[i][n - 1 - i].', E'3\n1 2 3\n4 5 6\n7 8 9\n', E'15\n', 'CPP'),
    (367, 'dem-cuc-dai-dia-phuong-cpp', 'Đếm cực đại địa phương C++',
        'Đếm các phần tử lớn hơn hai phần tử đứng liền kề trong dãy.',
        'Dòng đầu chứa n từ 3 đến 100000. Dòng sau chứa n số nguyên.',
        'In số chỉ số i với 0 < i < n - 1 sao cho a[i] lớn hơn cả a[i - 1] và a[i + 1].', E'7\n1 4 2 5 3 6 1\n', E'3\n', 'CPP'),
    (368, 'hop-hai-day-tang-phan-biet-cpp', 'Hợp hai dãy tăng phân biệt',
        'Hợp hai dãy đã tăng dần và loại các giá trị trùng nhau.',
        'Dòng đầu chứa n và m. Hai dòng sau chứa hai dãy số nguyên tăng dần.',
        'In các giá trị thuộc ít nhất một dãy theo thứ tự tăng dần.', E'5 4\n1 2 2 7 9\n2 3 7 8\n', E'1 2 3 7 8 9\n', 'CPP'),
    (369, 'xoay-ma-tran-90-do-cpp', 'Xoay ma trận 90 độ C++',
        'Xoay ma trận vuông 90 độ theo chiều kim đồng hồ.',
        'Dòng đầu chứa n. Tiếp theo là n hàng của ma trận số nguyên.',
        'In ma trận sau khi xoay, mỗi hàng trên một dòng.', E'2\n1 2\n3 4\n', E'3 1\n4 2\n', 'CPP'),
    (370, 'doan-tang-lien-tiep-dai-nhat-cpp', 'Đoạn tăng liên tiếp dài nhất',
        'Tìm độ dài đoạn con liên tiếp tăng chặt dài nhất.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên.',
        'In độ dài lớn nhất của một đoạn liên tiếp có phần tử sau lớn hơn phần tử trước.', E'8\n1 2 5 3 4 6 7 0\n', E'4\n', 'CPP'),
    (371, 'tan-suat-tu-cpp', 'Tần suất từ C++',
        'Đếm số lần xuất hiện của từng từ trong một dòng.',
        'Một dòng gồm các từ chữ thường, phân tách bởi đúng một dấu cách.',
        'In từng từ và tần suất trên một dòng theo thứ tự từ điển.', E'hoc code hoc moi ngay code\n', E'code 2\nhoc 2\nmoi 1\nngay 1\n', 'CPP'),
    (372, 'xoa-trung-giu-thu-tu-cpp', 'Xóa trùng và giữ thứ tự',
        'Loại các phần tử xuất hiện lặp lại nhưng giữ lần xuất hiện đầu tiên.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên.',
        'In dãy sau khi loại trùng theo thứ tự ban đầu.', E'8\n3 1 3 2 1 5 2 4\n', E'3 1 2 5 4\n', 'CPP'),
    (373, 'phan-tich-thua-so-nguyen-to-cpp', 'Phân tích thừa số nguyên tố',
        'Phân tích một số nguyên dương thành tích các thừa số nguyên tố.',
        'Một dòng chứa n trong khoảng từ 2 đến 10^9.',
        'In các thừa số nguyên tố theo thứ tự tăng dần, lặp lại theo số mũ và cách nhau bởi dấu cách.', E'360\n', E'2 2 2 3 3 5\n', 'CPP'),
    (374, 'nhan-so-lon-voi-so-nho-cpp', 'Nhân số lớn với số nhỏ',
        'Nhân một số nguyên rất lớn với một số nguyên nhỏ.',
        'Dòng đầu chứa chuỗi chữ số biểu diễn số không âm có tối đa 10000 chữ số. Dòng sau chứa k từ 0 đến 10^9.',
        'In tích chính xác, không có số 0 thừa ở đầu.', E'99999999999999999999\n9\n', E'899999999999999999991\n', 'CPP'),
    (375, 'do-dai-hop-cac-doan-cpp', 'Độ dài hợp các đoạn',
        'Tính tổng độ dài được phủ bởi các đoạn trên trục số.',
        'Dòng đầu chứa n. Mỗi dòng tiếp theo chứa l và r với l nhỏ hơn hoặc bằng r.',
        'In tổng độ dài hợp của các đoạn; hai đoạn chỉ chạm đầu mút không tạo thêm độ dài.', E'3\n1 5\n3 7\n10 12\n', E'8\n', 'CPP'),
    (376, 'hinh-chu-nhat-lon-nhat-histogram-cpp', 'Hình chữ nhật lớn nhất trong Histogram',
        'Tìm diện tích hình chữ nhật lớn nhất nằm trong biểu đồ cột.',
        'Dòng đầu chứa n. Dòng sau chứa n chiều cao nguyên không âm.',
        'In diện tích lớn nhất có thể tạo bởi các cột liên tiếp.', E'7\n2 1 5 6 2 3 1\n', E'10\n', 'CPP'),
    (377, 'ngoac-dung-dai-nhat-cpp', 'Dãy ngoặc đúng dài nhất C++',
        'Tìm độ dài chuỗi con liên tiếp là dãy ngoặc tròn đúng dài nhất.',
        'Một dòng chứa chuỗi chỉ gồm ký tự mở ngoặc và đóng ngoặc, dài không quá 200000.',
        'In độ dài lớn nhất của chuỗi con ngoặc đúng.', E')()())\n', E'4\n', 'CPP'),
    (378, 'phan-tu-thu-k-trong-day-cpp', 'Phần tử thứ K trong dãy',
        'Tìm phần tử nhỏ thứ k mà không cần giữ nguyên thứ tự dãy.',
        'Dòng đầu chứa n và k với 1 <= k <= n. Dòng sau chứa n số nguyên.',
        'In giá trị đứng ở vị trí k nếu dãy được sắp tăng dần.', E'6 3\n7 2 9 1 5 3\n', E'3\n', 'CPP'),
    (379, 'duyet-ma-tran-xoan-oc-cpp', 'Duyệt ma trận xoắn ốc C++',
        'Duyệt các phần tử ma trận theo vòng xoắn ốc từ góc trên trái.',
        'Dòng đầu chứa n và m. Tiếp theo là n hàng, mỗi hàng có m số nguyên.',
        'In thứ tự duyệt xoắn ốc trên một dòng.', E'3 4\n1 2 3 4\n5 6 7 8\n9 10 11 12\n', E'1 2 3 4 8 12 11 10 9 5 6 7\n', 'CPP'),
    (380, 'dem-chuoi-con-chong-lap-cpp', 'Đếm chuỗi con chồng lấp C++',
        'Đếm số lần một mẫu xuất hiện trong văn bản, cho phép các lần xuất hiện chồng lấp.',
        'Dòng đầu chứa văn bản, dòng sau chứa mẫu; cả hai không có khoảng trắng.',
        'In số vị trí bắt đầu mà mẫu khớp hoàn toàn với văn bản.', E'aaaaa\naa\n', E'4\n', 'CPP'),

    (381, 'dem-tu-camel-case-java', 'Đếm từ Camel Case Java',
        'Đếm số từ được ghép trong một định danh camelCase.',
        'Một dòng chứa định danh bắt đầu bằng chữ thường và chỉ gồm chữ cái tiếng Anh.',
        'In số từ; mỗi chữ in hoa đánh dấu đầu một từ mới.', E'saveStudentProgress\n', E'3\n', 'JAVA'),
    (382, 'sap-xep-ten-tu-dien-java', 'Sắp xếp tên từ điển Java',
        'Sắp xếp danh sách chuỗi theo thứ tự từ điển không phân biệt hoa thường.',
        'Dòng đầu chứa n. Mỗi dòng tiếp theo chứa một tên không có khoảng trắng.',
        'In các tên sau khi sắp xếp, mỗi tên trên một dòng; nếu bằng nhau khi bỏ qua hoa thường thì giữ thứ tự ban đầu.', E'4\nminh\nAn\nbinh\nChi\n', E'An\nbinh\nChi\nminh\n', 'JAVA'),
    (383, 'doi-thap-phan-sang-hex-java', 'Đổi thập phân sang Hex Java',
        'Chuyển một số nguyên không âm sang hệ thập lục phân.',
        'Một dòng chứa n từ 0 đến 2^31 - 1.',
        'In biểu diễn hệ 16 bằng các chữ số 0-9 và A-F, không có số 0 thừa ở đầu.', E'255\n', E'FF\n', 'JAVA'),
    (384, 'giai-nen-rle-java', 'Giải nén RLE Java',
        'Khôi phục chuỗi từ các cặp ký tự và số lần lặp.',
        'Dòng đầu chứa m. M dòng sau chứa một ký tự không phải khoảng trắng và số lần lặp dương.',
        'In chuỗi sau khi nối từng ký tự với đúng số lần tương ứng.', E'3\na 3\nb 1\nc 2\n', E'aaabcc\n', 'JAVA'),
    (385, 'do-sau-ngoac-lon-nhat-java', 'Độ sâu ngoặc lớn nhất Java',
        'Tìm độ sâu lồng nhau lớn nhất của một dãy ngoặc tròn hợp lệ.',
        'Một dòng chứa dãy ngoặc tròn hợp lệ, dài không quá 100000.',
        'In số ngoặc mở lớn nhất đang hoạt động tại cùng thời điểm.', E'(()(()))\n', E'3\n', 'JAVA'),
    (386, 'hau-to-chung-dai-nhat-java', 'Hậu tố chung dài nhất Java',
        'Tìm hậu tố chung dài nhất của nhiều chuỗi.',
        'Dòng đầu chứa n. N dòng sau chứa các chuỗi không có khoảng trắng.',
        'In hậu tố chung dài nhất hoặc EMPTY nếu không tồn tại.', E'3\nlearning\ncoding\ntesting\n', E'ing\n', 'JAVA'),
    (387, 'cong-hai-so-nguyen-lon-java', 'Cộng hai số nguyên lớn Java',
        'Tính tổng hai số nguyên không âm có độ dài lớn.',
        'Hai dòng, mỗi dòng chứa một chuỗi có tối đa 10000 chữ số.',
        'In tổng chính xác của hai số, không có số 0 thừa ở đầu.', E'999999999999999999\n2\n', E'1000000000000000001\n', 'JAVA'),
    (388, 'che-so-dien-thoai-java', 'Che số điện thoại Java',
        'Ẩn các chữ số đầu của số điện thoại và chỉ giữ bốn chữ số cuối.',
        'Một dòng chứa chuỗi gồm từ 4 đến 20 chữ số.',
        'Thay mỗi chữ số trước bốn chữ số cuối bằng dấu sao rồi in kết quả.', E'0987654321\n', E'******4321\n', 'JAVA'),
    (389, 'diem-trung-binh-sinh-vien-java', 'Điểm trung bình sinh viên Java',
        'Tính điểm trung bình và xếp loại một sinh viên.',
        'Một dòng chứa ba điểm thực từ 0 đến 10.',
        'In điểm trung bình với hai chữ số thập phân và loại GOOD nếu từ 8, FAIR nếu từ 6.5, AVERAGE nếu từ 5, ngược lại WEAK.', E'8 7.5 9\n', E'8.17 GOOD\n', 'JAVA'),
    (390, 'hieu-doi-xung-hai-tap-java', 'Hiệu đối xứng hai tập Java',
        'Tìm các giá trị chỉ xuất hiện trong đúng một trong hai tập.',
        'Dòng đầu chứa n và m. Hai dòng sau chứa các phần tử nguyên của hai tập.',
        'In các giá trị thuộc hiệu đối xứng theo thứ tự tăng dần hoặc EMPTY.', E'4 5\n1 2 4 7\n2 3 4 8 9\n', E'1 3 7 8 9\n', 'JAVA'),
    (391, 'xoay-hang-ma-tran-java', 'Xoay từng hàng ma trận Java',
        'Xoay vòng mỗi hàng của ma trận sang phải một vị trí.',
        'Dòng đầu chứa n và m. Tiếp theo là n hàng, mỗi hàng có m số nguyên.',
        'In ma trận sau khi đưa phần tử cuối mỗi hàng lên đầu hàng đó.', E'2 4\n1 2 3 4\n5 6 7 8\n', E'4 1 2 3\n8 5 6 7\n', 'JAVA'),
    (392, 'tong-cot-chan-java', 'Tổng các cột chẵn Java',
        'Tính tổng phần tử ở các cột có chỉ số chẵn của ma trận.',
        'Dòng đầu chứa n và m. Tiếp theo là n hàng, mỗi hàng có m số nguyên; chỉ số cột bắt đầu từ 0.',
        'In tổng của từng cột chẵn theo thứ tự tăng chỉ số.', E'2 5\n1 2 3 4 5\n6 7 8 9 10\n', E'7 11 15\n', 'JAVA'),
    (393, 'tu-ngan-nhat-moi-do-dai-java', 'Từ nhỏ nhất theo độ dài Java',
        'Chọn từ ngắn nhất trong một câu.',
        'Một dòng gồm các từ phân tách bởi một hoặc nhiều khoảng trắng.',
        'In từ có độ dài nhỏ nhất; nếu hòa, in từ nhỏ nhất theo thứ tự từ điển.', E'hoc lap trinh Java moi ngay\n', E'hoc\n', 'JAVA'),
    (394, 'kiem-tra-ma-tran-tam-giac-tren-java', 'Ma trận tam giác trên Java',
        'Kiểm tra mọi phần tử phía dưới đường chéo chính có bằng 0 hay không.',
        'Dòng đầu chứa n. Tiếp theo là n hàng của ma trận vuông.',
        'In YES nếu ma trận là tam giác trên, ngược lại in NO.', E'3\n1 2 3\n0 4 5\n0 0 6\n', E'YES\n', 'JAVA'),
    (395, 'gia-tri-gan-trung-binh-java', 'Giá trị gần trung bình Java',
        'Tìm phần tử có khoảng cách nhỏ nhất tới trung bình cộng của dãy.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên.',
        'In giá trị gần trung bình nhất; nếu hòa, chọn giá trị nhỏ hơn.', E'5\n1 3 8 10 13\n', E'8\n', 'JAVA'),

    (396, 'dem-gia-tri-khac-nhau-python', 'Đếm giá trị khác nhau Python',
        'Đếm số lượng giá trị phân biệt trong một dãy.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên.',
        'In số giá trị khác nhau xuất hiện trong dãy.', E'8\n1 2 2 3 1 4 4 5\n', E'5\n', 'PYTHON'),
    (397, 'nhom-tu-theo-chu-cai-dau-python', 'Nhóm từ theo chữ cái đầu Python',
        'Đếm số từ bắt đầu bằng từng chữ cái.',
        'Một dòng gồm các từ chữ thường phân tách bởi khoảng trắng.',
        'In chữ cái đầu và số từ tương ứng trên từng dòng theo thứ tự chữ cái.', E'code clean build bug test\n', E'b 2\nc 2\nt 1\n', 'PYTHON'),
    (398, 'tong-hai-duong-cheo-python', 'Tổng hai đường chéo Python',
        'Tính tổng các phần tử thuộc ít nhất một trong hai đường chéo của ma trận vuông.',
        'Dòng đầu chứa n. Tiếp theo là n hàng của ma trận số nguyên.',
        'In tổng hai đường chéo và không đếm hai lần phần tử trung tâm.', E'3\n1 2 3\n4 5 6\n7 8 9\n', E'25\n', 'PYTHON'),
    (399, 'loc-tu-theo-do-dai-python', 'Lọc từ theo độ dài Python',
        'Chọn các từ có độ dài ít nhất k.',
        'Dòng đầu chứa k. Dòng sau chứa các từ phân tách bởi khoảng trắng.',
        'In các từ đủ dài theo thứ tự ban đầu, cách nhau bởi dấu cách; nếu không có thì in EMPTY.', E'5\nhoc laptrinh Python moi ngay\n', E'laptrinh Python\n', 'PYTHON'),
    (400, 'cap-gia-tri-co-tong-gan-nhat-python', 'Cặp có tổng gần nhất Python',
        'Tìm cặp phần tử có tổng gần một mục tiêu nhất.',
        'Dòng đầu chứa n và target. Dòng sau chứa n số nguyên, n ít nhất là 2.',
        'In hai giá trị của cặp theo thứ tự tăng; nếu nhiều cặp cùng khoảng cách, chọn cặp từ điển nhỏ hơn.', E'5 10\n1 4 6 8 12\n', E'4 6\n', 'PYTHON'),
    (401, 'chuan-hoa-khoang-trang-python', 'Chuẩn hóa khoảng trắng Python',
        'Loại khoảng trắng thừa trong một dòng văn bản.',
        'Một dòng có thể chứa khoảng trắng ở đầu, cuối và giữa các từ.',
        'In các từ nối với nhau bởi đúng một dấu cách và không có khoảng trắng ở hai đầu.', E'  hoc   Python   moi ngay  \n', E'hoc Python moi ngay\n', 'PYTHON'),
    (402, 'tong-theo-nhom-du-python', 'Tổng theo nhóm dư Python',
        'Tính tổng phần tử theo phần dư khi chia cho k.',
        'Dòng đầu chứa n và k dương. Dòng sau chứa n số nguyên không âm.',
        'In k tổng tương ứng với phần dư từ 0 đến k - 1.', E'6 3\n1 2 3 4 5 6\n', E'9 5 7\n', 'PYTHON'),
    (403, 'day-con-khong-giam-dai-nhat-python', 'Đoạn không giảm dài nhất Python',
        'Tìm độ dài đoạn con liên tiếp không giảm dài nhất.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên.',
        'In độ dài lớn nhất của đoạn liên tiếp thỏa phần tử sau không nhỏ hơn phần tử trước.', E'8\n3 3 4 2 2 5 7 1\n', E'4\n', 'PYTHON'),
    (404, 'ma-hoa-run-length-python', 'Mã hóa Run Length Python',
        'Nén chuỗi bằng số lần lặp liên tiếp của từng ký tự.',
        'Một dòng chứa chuỗi không rỗng và không có khoảng trắng.',
        'In lần lượt ký tự và số lần lặp của mỗi nhóm liên tiếp, cách nhau bởi dấu cách.', E'aaabbccccaa\n', E'a3 b2 c4 a2\n', 'PYTHON'),
    (405, 'giao-nhieu-tap-hop-python', 'Giao nhiều tập hợp Python',
        'Tìm các giá trị có mặt trong tất cả các tập hợp.',
        'Dòng đầu chứa k. Với mỗi tập, một dòng bắt đầu bằng số phần tử rồi đến các số nguyên.',
        'In giao của k tập theo thứ tự tăng dần hoặc EMPTY.', E'3\n4 1 2 3 4\n5 2 3 4 5 6\n3 0 2 4\n', E'2 4\n', 'PYTHON'),
    (406, 'sap-xep-theo-tan-suat-python', 'Sắp xếp theo tần suất Python',
        'Sắp các giá trị theo tần suất giảm dần.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên.',
        'In mỗi giá trị đúng một lần; tần suất lớn hơn đứng trước, nếu hòa thì giá trị nhỏ hơn đứng trước.', E'8\n4 1 2 2 3 1 2 4\n', E'2 1 4 3\n', 'PYTHON'),
    (407, 'kiem-tra-sudoku-hang-python', 'Kiểm tra hàng Sudoku Python',
        'Kiểm tra từng hàng của bảng 9 x 9 có chứa đủ các số từ 1 đến 9 hay không.',
        'Chín dòng, mỗi dòng chứa chín số nguyên từ 1 đến 9.',
        'In YES nếu mọi hàng đều là hoán vị của 1 đến 9, ngược lại in NO.', E'1 2 3 4 5 6 7 8 9\n2 3 4 5 6 7 8 9 1\n3 4 5 6 7 8 9 1 2\n4 5 6 7 8 9 1 2 3\n5 6 7 8 9 1 2 3 4\n6 7 8 9 1 2 3 4 5\n7 8 9 1 2 3 4 5 6\n8 9 1 2 3 4 5 6 7\n9 1 2 3 4 5 6 7 8\n', E'YES\n', 'PYTHON'),
    (408, 'tong-cap-doi-xung-python', 'Tổng cặp đối xứng Python',
        'Tính tổng từng cặp phần tử đối xứng qua giữa dãy.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên.',
        'In các tổng a[i] + a[n - 1 - i] với i từ 0 đến (n - 1) / 2.', E'5\n1 2 3 4 5\n', E'6 6 6\n', 'PYTHON'),
    (409, 'chuoi-con-tang-theo-tu-dien-python', 'Chuỗi con tăng theo từ điển Python',
        'Tìm độ dài đoạn ký tự liên tiếp tăng chặt theo thứ tự từ điển.',
        'Một dòng chứa chuỗi chữ thường không có khoảng trắng.',
        'In độ dài lớn nhất của đoạn mà mỗi ký tự sau lớn hơn ký tự trước.', E'abcfabxyz\n', E'5\n', 'PYTHON'),
    (410, 'ma-tran-ziczac-python', 'Duyệt ma trận ziczac Python',
        'Duyệt các hàng ma trận luân phiên từ trái sang phải và từ phải sang trái.',
        'Dòng đầu chứa n và m. Tiếp theo là n hàng của ma trận.',
        'In thứ tự duyệt ziczac trên một dòng.', E'3 3\n1 2 3\n4 5 6\n7 8 9\n', E'1 2 3 6 5 4 7 8 9\n', 'PYTHON'),

    (411, 'lop-hinh-chu-nhat-oop', 'Hình chữ nhật OOP',
        'Mô hình hóa hình chữ nhật và tính chu vi, diện tích bằng phương thức của lớp.',
        'Một dòng chứa chiều dài và chiều rộng là hai số nguyên dương.',
        'In chu vi và diện tích, cách nhau bởi một dấu cách.', E'6 4\n', E'20 24\n', 'OOP'),
    (412, 'bang-luong-nhan-vien-oop', 'Bảng lương nhân viên OOP',
        'Mô hình hóa nhân viên và tính lương thực nhận sau thưởng.',
        'Dòng đầu chứa lương cơ bản và tỷ lệ thưởng phần trăm. Dòng sau chứa số ngày làm việc trên tổng 22 ngày chuẩn.',
        'In lương thực nhận nguyên theo công thức base * days / 22 rồi cộng thưởng trên phần lương đó.', E'11000000 10\n22\n', E'12100000\n', 'OOP'),
    (413, 'vector-hai-chieu-oop', 'Vector hai chiều OOP',
        'Cài đặt lớp vector hai chiều hỗ trợ phép cộng và tích vô hướng.',
        'Hai dòng, mỗi dòng chứa hai tọa độ nguyên của một vector.',
        'Dòng đầu in vector tổng; dòng sau in tích vô hướng.', E'1 2\n3 4\n', E'4 6\n11\n', 'OOP'),
    (414, 'dong-ho-them-giay-oop', 'Đồng hồ thêm giây OOP',
        'Mô hình hóa thời gian trong ngày và cộng thêm số giây.',
        'Dòng đầu chứa h m s hợp lệ. Dòng sau chứa số giây không âm cần cộng.',
        'In thời gian mới dưới dạng HH:MM:SS và quay vòng sau 24 giờ.', E'23 59 50\n15\n', E'00:00:05\n', 'OOP'),
    (415, 'so-sanh-phan-so-oop', 'So sánh phân số OOP',
        'Cài đặt lớp phân số và so sánh hai phân số mà không dùng số thực.',
        'Hai dòng, mỗi dòng chứa tử số và mẫu số khác 0.',
        'In LESS, EQUAL hoặc GREATER khi phân số thứ nhất nhỏ hơn, bằng hoặc lớn hơn phân số thứ hai.', E'2 3\n4 6\n', E'EQUAL\n', 'OOP'),
    (416, 'gia-sau-chiet-khau-oop', 'Sản phẩm giảm giá OOP',
        'Mô hình hóa sản phẩm và tính giá sau khi áp dụng phần trăm giảm.',
        'Một dòng chứa giá nguyên không âm và tỷ lệ giảm nguyên từ 0 đến 100.',
        'In giá sau giảm bằng phép chia nguyên xuống.', E'250000 15\n', E'212500\n', 'OOP'),
    (417, 'xep-loai-gpa-oop', 'Xếp loại GPA OOP',
        'Mô hình hóa sinh viên và tính GPA có trọng số tín chỉ.',
        'Dòng đầu chứa n môn. N dòng sau chứa số tín chỉ nguyên dương và điểm thực thang 4.',
        'In GPA với hai chữ số thập phân và loại EXCELLENT nếu từ 3.6, GOOD nếu từ 3.2, FAIR nếu từ 2.5, ngược lại AVERAGE.', E'3\n3 4.0\n2 3.0\n1 2.0\n', E'3.33 GOOD\n', 'OOP'),
    (418, 'phi-qua-tram-phuong-tien-oop', 'Phí qua trạm phương tiện OOP',
        'Dùng đa hình để tính phí qua trạm theo loại phương tiện.',
        'Một dòng chứa loại BIKE, CAR hoặc TRUCK và quãng đường nguyên dương. Đơn giá lần lượt là 1, 3 và 5.',
        'In tổng phí bằng quãng đường nhân đơn giá.', E'TRUCK 12\n', E'60\n', 'OOP'),
    (419, 'tiet-kiem-lai-kep-oop', 'Tiết kiệm lãi kép OOP',
        'Mô hình hóa tài khoản tiết kiệm và tính số dư sau nhiều kỳ.',
        'Một dòng chứa số dư ban đầu, lãi suất phần trăm mỗi kỳ và số kỳ nguyên không âm.',
        'In số dư cuối cùng làm tròn đến hai chữ số thập phân.', E'1000 10 2\n', E'1210.00\n', 'OOP'),
    (420, 'nhan-so-phuc-oop', 'Nhân số phức OOP',
        'Cài đặt lớp số phức và phép nhân hai số phức.',
        'Hai dòng, mỗi dòng chứa phần thực và phần ảo nguyên.',
        'In phần thực và phần ảo của tích, cách nhau bởi một dấu cách.', E'1 2\n3 4\n', E'-5 10\n', 'OOP'),

    (421, 'hang-doi-truy-van', 'Hàng đợi truy vấn',
        'Xử lý các thao tác thêm, lấy và xem đầu hàng đợi.',
        'Dòng đầu chứa q. Mỗi dòng sau là PUSH x, POP hoặc FRONT.',
        'Với FRONT, in phần tử đầu hoặc EMPTY. POP trên hàng đợi rỗng không làm gì.', E'7\nPUSH 3\nPUSH 8\nFRONT\nPOP\nFRONT\nPOP\nFRONT\n', E'3\n8\nEMPTY\n', 'DATA_STRUCTURES'),
    (422, 'mo-phong-bo-dem-vong', 'Mô phỏng hàng đợi vòng',
        'Mô phỏng hàng đợi vòng có sức chứa cố định.',
        'Dòng đầu chứa capacity và q. Mỗi dòng sau là PUSH x hoặc POP.',
        'Sau mọi thao tác, in các phần tử từ đầu đến cuối; nếu rỗng in EMPTY. Lệnh PUSH khi đầy bị bỏ qua.', E'3 5\nPUSH 1\nPUSH 2\nPOP\nPUSH 3\nPUSH 4\n', E'2 3 4\n', 'DATA_STRUCTURES'),
    (423, 'kiem-tra-doi-xung-bang-deque', 'Kiểm tra đối xứng bằng Deque',
        'Kiểm tra một dãy số có đối xứng bằng cấu trúc deque hay không.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên.',
        'In YES nếu dãy đọc xuôi và ngược giống nhau, ngược lại in NO.', E'5\n1 2 3 2 1\n', E'YES\n', 'DATA_STRUCTURES'),
    (424, 'ngan-xep-gia-tri-nho-nhat', 'Ngăn xếp giá trị nhỏ nhất',
        'Hỗ trợ truy vấn giá trị nhỏ nhất hiện có trong ngăn xếp.',
        'Dòng đầu chứa q. Các lệnh là PUSH x, POP hoặc MIN.',
        'Với MIN, in giá trị nhỏ nhất hoặc EMPTY nếu ngăn xếp rỗng.', E'7\nPUSH 5\nPUSH 2\nMIN\nPUSH 1\nMIN\nPOP\nMIN\n', E'2\n1\n2\n', 'DATA_STRUCTURES'),
    (425, 'tinh-gia-tri-hau-to', 'Tính giá trị biểu thức hậu tố',
        'Tính biểu thức hậu tố gồm số nguyên và bốn phép toán cơ bản.',
        'Một dòng gồm các token cách nhau bởi dấu cách. Phép chia là chia nguyên về 0 và không chia cho 0.',
        'In giá trị nguyên của biểu thức.', E'5 1 2 + 4 * + 3 -\n', E'14\n', 'DATA_STRUCTURES'),
    (426, 'do-sau-cay-nhi-phan', 'Chiều cao cây nhị phân',
        'Tính chiều cao của cây nhị phân có gốc tại đỉnh 1.',
        'Dòng đầu chứa n. N dòng sau chứa chỉ số con trái và con phải của từng đỉnh; 0 nghĩa là không có con.',
        'In số đỉnh trên đường đi dài nhất từ gốc đến lá; cây rỗng có chiều cao 0.', E'5\n2 3\n4 5\n0 0\n0 0\n0 0\n', E'3\n', 'DATA_STRUCTURES'),
    (427, 'tim-kiem-trong-cay-bst', 'Tìm kiếm trong cây BST',
        'Xây dựng cây tìm kiếm nhị phân và trả lời truy vấn tồn tại.',
        'Dòng đầu chứa n và q. Dòng sau chứa n giá trị chèn lần lượt. Q dòng cuối chứa giá trị cần tìm.',
        'Với mỗi truy vấn, in YES nếu giá trị có trong cây, ngược lại in NO.', E'5 3\n5 2 8 1 3\n3\n7\n5\n', E'YES\nNO\nYES\n', 'DATA_STRUCTURES'),
    (428, 'duong-di-ngan-nhat-tren-luoi', 'Đường đi ngắn nhất trên lưới',
        'Tìm số bước ít nhất từ điểm bắt đầu đến đích trên lưới có vật cản.',
        'Dòng đầu chứa n và m. N dòng sau gồm S, T, dấu chấm và dấu thăng; được đi bốn hướng.',
        'In số bước ít nhất hoặc -1 nếu không thể đến đích.', E'3 4\nS...\n.##.\n...T\n', E'5\n', 'DATA_STRUCTURES'),
    (429, 'k-phan-tu-xuat-hien-nhieu-nhat', 'K phần tử xuất hiện nhiều nhất',
        'Tìm k giá trị có tần suất xuất hiện cao nhất trong dãy.',
        'Dòng đầu chứa n và k. Dòng sau chứa n số nguyên.',
        'In k giá trị theo tần suất giảm dần; nếu hòa, giá trị nhỏ hơn đứng trước.', E'8 2\n1 1 1 2 2 3 3 4\n', E'1 2\n', 'DATA_STRUCTURES'),
    (430, 'ket-noi-tap-hop-dsu', 'Kết nối tập hợp DSU',
        'Xử lý các thao tác hợp nhất và kiểm tra kết nối giữa các đỉnh.',
        'Dòng đầu chứa n và q. Mỗi dòng sau là UNION u v hoặc SAME u v.',
        'Với mỗi SAME, in YES nếu hai đỉnh cùng thành phần liên thông, ngược lại in NO.', E'5 5\nUNION 1 2\nUNION 3 4\nSAME 1 3\nUNION 2 3\nSAME 1 4\n', E'NO\nYES\n', 'DATA_STRUCTURES'),

    (431, 'cap-co-tong-muc-tieu-hai-con-tro', 'Cặp tổng mục tiêu bằng hai con trỏ',
        'Tìm một cặp trong dãy tăng dần có tổng bằng mục tiêu.',
        'Dòng đầu chứa n và target. Dòng sau chứa n số nguyên tăng dần.',
        'In hai chỉ số zero-based của cặp đầu tiên theo thứ tự từ điển hoặc -1 nếu không có.', E'6 11\n1 2 4 7 9 12\n', E'2 3\n', 'ALGORITHMS'),
    (432, 'tong-lon-nhat-cua-so-k', 'Tổng lớn nhất cửa sổ K',
        'Tìm tổng lớn nhất của đúng k phần tử liên tiếp.',
        'Dòng đầu chứa n và k. Dòng sau chứa n số nguyên.',
        'In tổng lớn nhất trong mọi cửa sổ độ dài k.', E'7 3\n2 -1 4 5 -2 3 6\n', E'9\n', 'ALGORITHMS'),
    (433, 'doan-con-tong-lon-nhat-kem-vi-tri', 'Đoạn con tổng lớn nhất kèm vị trí',
        'Tìm đoạn con liên tiếp có tổng lớn nhất và vị trí của nó.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên.',
        'In tổng lớn nhất, chỉ số trái và phải zero-based; nếu hòa, chọn đoạn có cặp chỉ số nhỏ hơn theo thứ tự từ điển.', E'8\n-2 1 -3 4 -1 2 1 -5\n', E'6 3 6\n', 'ALGORITHMS'),
    (434, 'chon-nhieu-cuoc-hop-nhat', 'Chọn nhiều cuộc họp nhất',
        'Chọn số đoạn thời gian không giao nhau nhiều nhất.',
        'Dòng đầu chứa n. N dòng sau chứa thời điểm bắt đầu và kết thúc, với start nhỏ hơn end.',
        'In số cuộc họp tối đa; cuộc họp mới được bắt đầu đúng lúc cuộc trước kết thúc.', E'5\n1 3\n2 5\n4 7\n6 9\n8 10\n', E'3\n', 'ALGORITHMS'),
    (435, 'dem-cach-doi-tien', 'Đếm cách đổi tiền',
        'Đếm số cách tạo tổng target bằng các mệnh giá, mỗi mệnh giá dùng không giới hạn.',
        'Dòng đầu chứa n và target. Dòng sau chứa n mệnh giá nguyên dương phân biệt.',
        'In số cách kết hợp, không phân biệt thứ tự chọn đồng xu.', E'3 5\n1 2 5\n', E'4\n', 'ALGORITHMS'),
    (436, 'balo-gia-tri-lon-nhat', 'Ba lô giá trị lớn nhất',
        'Chọn mỗi vật tối đa một lần để tối đa hóa tổng giá trị trong giới hạn khối lượng.',
        'Dòng đầu chứa n và capacity. N dòng sau chứa weight và value nguyên dương.',
        'In tổng giá trị lớn nhất có thể đạt được.', E'4 7\n1 1\n3 4\n4 5\n5 7\n', E'9\n', 'ALGORITHMS'),
    (437, 'khoang-cach-chinh-sua-hai-chuoi', 'Khoảng cách chỉnh sửa hai chuỗi',
        'Tìm số phép chèn, xóa hoặc thay thế ký tự ít nhất để biến chuỗi thứ nhất thành chuỗi thứ hai.',
        'Hai dòng, mỗi dòng chứa một chuỗi chữ thường có độ dài không quá 1000.',
        'In khoảng cách chỉnh sửa nhỏ nhất.', E'kitten\nsitting\n', E'3\n', 'ALGORITHMS'),
    (438, 'sap-xep-to-po-do-thi', 'Sắp xếp topo đồ thị',
        'Tìm một thứ tự topo của đồ thị có hướng không chu trình.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh có hướng u v.',
        'In thứ tự topo nhỏ nhất theo từ điển; luôn bảo đảm đồ thị là DAG.', E'5 5\n1 3\n2 3\n2 4\n3 5\n4 5\n', E'1 2 3 4 5\n', 'ALGORITHMS'),
    (439, 'dijkstra-tu-mot-dinh', 'Dijkstra từ một đỉnh',
        'Tính khoảng cách ngắn nhất từ một đỉnh nguồn trên đồ thị trọng số không âm.',
        'Dòng đầu chứa n, m và source. M dòng sau chứa cạnh vô hướng u v w.',
        'In n khoảng cách theo thứ tự đỉnh 1 đến n; đỉnh không tới được in -1.', E'4 4 1\n1 2 5\n1 3 2\n3 2 1\n2 4 3\n', E'0 3 2 6\n', 'ALGORITHMS'),
    (440, 'do-dai-day-con-tang-n-log-n', 'Dãy con tăng dài nhất O(n log n)',
        'Tính độ dài dãy con tăng chặt dài nhất với độ phức tạp O(n log n).',
        'Dòng đầu chứa n đến 200000. Dòng sau chứa n số nguyên.',
        'In độ dài của dãy con tăng chặt dài nhất, không yêu cầu liên tiếp.', E'8\n10 9 2 5 3 7 101 18\n', E'4\n', 'ALGORITHMS'),

    (441, 'tong-doanh-thu-theo-ngay-sql', 'Tổng doanh thu theo ngày SQL',
        'Tổng hợp doanh thu bán hàng theo từng ngày.',
        'Bảng sales(id, sold_date, amount) chứa ngày bán và giá trị giao dịch.',
        'Trả sold_date và tổng amount, sắp xếp sold_date tăng dần.', E'CREATE TABLE sales (id INT, sold_date DATE, amount INT);\nINSERT INTO sales VALUES (1, ''2026-01-01'', 100), (2, ''2026-01-01'', 50), (3, ''2026-01-02'', 80);\n', E'2026-01-01\t150\n2026-01-02\t80\n', 'SQL'),
    (442, 'san-pham-ban-chay-nhat-sql', 'Sản phẩm bán chạy nhất SQL',
        'Tìm sản phẩm có tổng số lượng bán cao nhất.',
        'Bảng order_items(product, quantity) chứa các dòng sản phẩm; bảo đảm chỉ có một sản phẩm đứng đầu.',
        'Trả product và tổng quantity của sản phẩm bán nhiều nhất.', E'CREATE TABLE order_items (product VARCHAR(30), quantity INT);\nINSERT INTO order_items VALUES (''A'', 2), (''B'', 5), (''A'', 4), (''C'', 1);\n', E'A\t6\n', 'SQL'),
    (443, 'nhan-vien-khong-co-quan-ly-sql', 'Nhân viên không có quản lý SQL',
        'Liệt kê các nhân viên không được gán quản lý trực tiếp.',
        'Bảng employees(id, name, manager_id) có manager_id có thể NULL.',
        'Trả id và name của nhân viên có manager_id NULL, sắp xếp id tăng dần.', E'CREATE TABLE employees (id INT, name VARCHAR(30), manager_id INT);\nINSERT INTO employees VALUES (1, ''An'', NULL), (2, ''Binh'', 1), (3, ''Chi'', NULL);\n', E'1\tAn\n3\tChi\n', 'SQL'),
    (444, 'lop-co-diem-trung-binh-cao-sql', 'Lớp có điểm trung bình cao SQL',
        'Tìm các lớp có điểm trung bình đạt ngưỡng cho trước.',
        'Bảng scores(class_name, score) chứa điểm nguyên; ngưỡng cố định là 8.',
        'Trả class_name và điểm trung bình làm tròn hai chữ số cho lớp có trung bình từ 8, sắp xếp class_name.', E'CREATE TABLE scores (class_name VARCHAR(20), score INT);\nINSERT INTO scores VALUES (''A'', 8), (''A'', 9), (''B'', 6), (''B'', 8);\n', E'A\t8.50\n', 'SQL'),
    (445, 'don-hang-va-ten-khach-hang-sql', 'Đơn hàng và tên khách hàng SQL',
        'Kết hợp đơn hàng với thông tin khách hàng.',
        'Bảng customers(id, name) và orders(id, customer_id, amount) chứa dữ liệu liên quan.',
        'Trả order id, customer name và amount, sắp xếp order id tăng dần.', E'CREATE TABLE customers (id INT, name VARCHAR(30));\nCREATE TABLE orders (id INT, customer_id INT, amount INT);\nINSERT INTO customers VALUES (1, ''An''), (2, ''Binh'');\nINSERT INTO orders VALUES (10, 2, 50), (11, 1, 70);\n', E'10\tBinh\t50\n11\tAn\t70\n', 'SQL'),
    (446, 'mon-hoc-chua-co-dang-ky-sql', 'Môn học chưa có đăng ký SQL',
        'Tìm các môn học chưa có sinh viên đăng ký.',
        'Bảng courses(id, title) và enrollments(course_id, student_id) chứa dữ liệu lớp học.',
        'Trả id và title của môn học không có enrollment, sắp xếp id tăng dần.', E'CREATE TABLE courses (id INT, title VARCHAR(30));\nCREATE TABLE enrollments (course_id INT, student_id INT);\nINSERT INTO courses VALUES (1, ''Java''), (2, ''Python''), (3, ''SQL'');\nINSERT INTO enrollments VALUES (1, 10), (3, 11);\n', E'2\tPython\n', 'SQL'),
    (447, 'xep-hang-diem-sinh-vien-sql', 'Xếp hạng điểm sinh viên SQL',
        'Xếp hạng sinh viên theo điểm từ cao xuống thấp.',
        'Bảng students(name, score) chứa tên phân biệt và điểm nguyên.',
        'Trả name, score và thứ hạng dùng DENSE_RANK theo score giảm dần; khi cùng điểm, sắp tên tăng dần.', E'CREATE TABLE students (name VARCHAR(30), score INT);\nINSERT INTO students VALUES (''An'', 9), (''Binh'', 8), (''Chi'', 9), (''Dung'', 7);\n', E'An\t9\t1\nChi\t9\t1\nBinh\t8\t2\nDung\t7\t3\n', 'SQL'),
    (448, 'don-hang-lon-nhat-moi-khach-sql', 'Đơn hàng lớn nhất mỗi khách SQL',
        'Tìm giá trị đơn hàng lớn nhất của từng khách hàng.',
        'Bảng orders(customer_id, amount) chứa các đơn hàng và mỗi khách có ít nhất một đơn.',
        'Trả customer_id và MAX(amount), sắp xếp customer_id tăng dần.', E'CREATE TABLE orders (customer_id INT, amount INT);\nINSERT INTO orders VALUES (1, 20), (1, 80), (2, 50), (2, 40);\n', E'1\t80\n2\t50\n', 'SQL'),
    (449, 'ty-le-hoan-thanh-khoa-hoc-sql', 'Tỷ lệ hoàn thành khóa học SQL',
        'Tính tỷ lệ phần trăm bài học đã hoàn thành của từng sinh viên.',
        'Bảng progress(student_id, completed, total) có total dương và mỗi sinh viên một dòng.',
        'Trả student_id và phần trăm completed * 100 / total làm tròn hai chữ số, sắp xếp student_id.', E'CREATE TABLE progress (student_id INT, completed INT, total INT);\nINSERT INTO progress VALUES (1, 3, 4), (2, 1, 5);\n', E'1\t75.00\n2\t20.00\n', 'SQL'),
    (450, 'ba-giao-dich-gan-nhat-sql', 'Ba giao dịch gần nhất SQL',
        'Lấy tối đa ba giao dịch mới nhất của mỗi tài khoản.',
        'Bảng transactions(id, account_id, created_at, amount) chứa thời điểm không trùng trong cùng tài khoản.',
        'Trả account_id, id và amount; mỗi tài khoản giữ ba dòng có created_at mới nhất, sắp account_id tăng rồi created_at giảm.', E'CREATE TABLE transactions (id INT, account_id INT, created_at DATE, amount INT);\nINSERT INTO transactions VALUES (1, 1, ''2026-01-01'', 10), (2, 1, ''2026-01-02'', 20), (3, 1, ''2026-01-03'', 30), (4, 1, ''2026-01-04'', 40), (5, 2, ''2026-02-01'', 50);\n', E'1\t4\t40\n1\t3\t30\n1\t2\t20\n2\t5\t50\n', 'SQL')
)
INSERT INTO programming_problems (
    id, slug, title, summary, description, input_description, output_description,
    sample_input, sample_output, topic, created_at
)
SELECT ('10000000-0000-0000-0000-' || LPAD(seed_number::text, 12, '0'))::uuid,
       slug, title, goal, goal, input_description, output_description,
       sample_input, sample_output, topic, '2026-09-18T00:00:00Z'
FROM new_problems
ON CONFLICT (id) DO UPDATE SET
    slug = EXCLUDED.slug,
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    description = EXCLUDED.description,
    input_description = EXCLUDED.input_description,
    output_description = EXCLUDED.output_description,
    sample_input = EXCLUDED.sample_input,
    sample_output = EXCLUDED.sample_output,
    topic = EXCLUDED.topic;

-- Fifty classic algorithms. These use a separate id range so the catalog can
-- grow without changing or replacing teacher-created exercises.
WITH classic_algorithm_problems(seed_number, slug, title, goal, input_description, output_description, sample_input, sample_output) AS (VALUES
    (451, 'truy-van-tong-doan-prefix', 'Truy vấn tổng đoạn Prefix Sum',
        'Trả lời nhanh tổng các phần tử trên nhiều đoạn liên tiếp bằng mảng cộng dồn.',
        'Dòng đầu chứa n và q. Dòng tiếp theo chứa n số nguyên. Q dòng sau chứa l và r theo chỉ số zero-based, với 0 <= l <= r < n.',
        'Với mỗi truy vấn, in tổng các phần tử từ l đến r trên một dòng.', E'5 3\n2 1 3 4 5\n0 2\n1 4\n3 3\n', E'6\n13\n4\n'),
    (452, 'truy-van-tong-hinh-chu-nhat-2d', 'Truy vấn tổng hình chữ nhật 2D',
        'Dùng prefix sum hai chiều để tính tổng nhanh trên các hình chữ nhật của ma trận.',
        'Dòng đầu chứa n, m, q. Tiếp theo là n hàng ma trận. Mỗi truy vấn chứa r1 c1 r2 c2 theo chỉ số zero-based.',
        'In tổng các ô trong mỗi hình chữ nhật truy vấn.', E'3 3 2\n1 2 3\n4 5 6\n7 8 9\n0 0 1 1\n1 1 2 2\n', E'12\n28\n'),
    (453, 'cap-nhat-doan-mang-hieu', 'Cập nhật đoạn bằng mảng hiệu',
        'Cộng cùng một giá trị lên nhiều đoạn của mảng ban đầu toàn số 0.',
        'Dòng đầu chứa n và q. Mỗi trong q dòng chứa l r x theo chỉ số zero-based, yêu cầu cộng x vào mọi phần tử từ l đến r.',
        'In mảng cuối cùng, các phần tử cách nhau một dấu cách.', E'5 3\n0 2 2\n1 4 1\n3 3 -3\n', E'2 3 3 -2 1\n'),
    (454, 'cay-fenwick-truy-van-tong', 'Cây Fenwick: cập nhật và tổng đoạn',
        'Xử lý cập nhật điểm và truy vấn tổng đoạn bằng Binary Indexed Tree.',
        'Dòng đầu chứa n, q; dòng sau chứa n số. Lệnh là ADD i x hoặc SUM l r, chỉ số tính từ 1.',
        'Với mỗi lệnh SUM, in tổng trên đoạn [l, r].', E'5 4\n1 2 3 4 5\nSUM 1 3\nADD 2 5\nSUM 2 5\nSUM 1 5\n', E'6\n19\n20\n'),
    (455, 'cay-doan-truy-van-min', 'Cây đoạn: giá trị nhỏ nhất',
        'Dùng segment tree để truy vấn giá trị nhỏ nhất và cập nhật điểm.',
        'Dòng đầu chứa n, q; dòng sau chứa n số. Lệnh là MIN l r hoặc SET i x với chỉ số zero-based.',
        'Với mỗi lệnh MIN, in giá trị nhỏ nhất trong đoạn.', E'5 4\n5 2 7 1 3\nMIN 0 4\nSET 3 6\nMIN 1 3\nMIN 2 4\n', E'1\n2\n3\n'),
    (456, 'sparse-table-truy-van-min', 'Sparse Table truy vấn min',
        'Trả lời truy vấn min trên mảng tĩnh bằng Sparse Table.',
        'Dòng đầu chứa n và q. Dòng sau chứa n số nguyên. Mỗi truy vấn chứa l r zero-based.',
        'In giá trị nhỏ nhất của mỗi đoạn [l, r].', E'6 3\n4 6 1 5 7 3\n0 2\n3 5\n1 4\n', E'1\n3\n1\n'),
    (457, 'bai-toan-stock-span', 'Bài toán Stock Span',
        'Tính số ngày liên tiếp trước đó có giá không lớn hơn giá hiện tại bằng stack đơn điệu.',
        'Dòng đầu chứa n. Dòng sau chứa n giá cổ phiếu nguyên dương theo thời gian.',
        'In n span tương ứng, cách nhau một dấu cách.', E'7\n100 80 60 70 60 75 85\n', E'1 1 1 2 1 4 6\n'),
    (458, 'nuoc-mua-giua-cac-cot', 'Nước mưa giữa các cột',
        'Tính lượng nước mưa bị giữ lại giữa các cột có chiều cao khác nhau.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên không âm là chiều cao các cột.',
        'In tổng lượng nước có thể giữ lại.', E'12\n0 1 0 2 1 0 1 3 2 1 2 1\n', E'6\n'),
    (459, 'doan-con-ngan-nhat-tong-toi-thieu', 'Đoạn con ngắn nhất đạt tổng',
        'Dùng hai con trỏ để tìm đoạn liên tiếp ngắn nhất có tổng không nhỏ hơn target.',
        'Dòng đầu chứa n và target. Dòng sau chứa n số nguyên dương.',
        'In độ dài nhỏ nhất, hoặc 0 nếu không có đoạn phù hợp.', E'6 7\n2 3 1 2 4 3\n', E'2\n'),
    (460, 'chuoi-con-khong-lap-dai-nhat', 'Chuỗi con không lặp dài nhất',
        'Tìm độ dài chuỗi con liên tiếp dài nhất không có ký tự lặp.',
        'Một dòng chứa chuỗi chữ thường không có khoảng trắng.',
        'In độ dài lớn nhất.', E'abcabcbb\n', E'3\n'),
    (461, 'cua-so-nho-nhat-chua-mau', 'Cửa sổ nhỏ nhất chứa mẫu',
        'Tìm chuỗi con ngắn nhất của s chứa đủ mọi ký tự của t với đúng số lần xuất hiện.',
        'Một dòng chứa hai chuỗi s và t, cách nhau một dấu cách; mỗi chuỗi không chứa khoảng trắng.',
        'In cửa sổ ngắn nhất; nếu không tồn tại in EMPTY.', E'ADOBECODEBANC ABC\n', E'BANC\n'),
    (462, 'dem-vi-tri-hoan-vi-chuoi', 'Đếm vị trí hoán vị chuỗi',
        'Đếm số chuỗi con của text là một hoán vị của pattern.',
        'Một dòng chứa text và pattern chữ thường, cách nhau một dấu cách.',
        'In số vị trí bắt đầu hợp lệ.', E'cbaebabacd abc\n', E'2\n'),
    (463, 'tim-chuoi-kmp', 'Tìm chuỗi bằng KMP',
        'Áp dụng thuật toán Knuth-Morris-Pratt để tìm lần xuất hiện đầu tiên của pattern.',
        'Một dòng chứa text và pattern, cách nhau một dấu cách.',
        'In chỉ số zero-based đầu tiên của pattern trong text, hoặc -1 nếu không có.', E'ababcabcabababd ababd\n', E'10\n'),
    (464, 'tim-chuoi-z-algorithm', 'Tìm chuỗi bằng Z-algorithm',
        'Dùng mảng Z để liệt kê các vị trí pattern xuất hiện trong text.',
        'Một dòng chứa text và pattern, cách nhau một dấu cách.',
        'In các chỉ số zero-based xuất hiện theo thứ tự tăng, hoặc EMPTY.', E'abacaba aba\n', E'0 4\n'),
    (465, 'tim-chuoi-rabin-karp', 'Tìm chuỗi Rabin-Karp',
        'Dùng rolling hash Rabin-Karp để tìm mọi lần xuất hiện có thể của pattern.',
        'Một dòng chứa text và pattern, cách nhau một dấu cách.',
        'In các chỉ số zero-based xuất hiện theo thứ tự tăng, hoặc EMPTY.', E'aaaaa aa\n', E'0 1 2 3\n'),
    (466, 'manacher-doi-xung-dai-nhat', 'Manacher: đối xứng dài nhất',
        'Tìm độ dài chuỗi con đối xứng dài nhất trong thời gian tuyến tính.',
        'Một dòng chứa chuỗi chữ thường không có khoảng trắng.',
        'In độ dài palindrome liên tiếp dài nhất.', E'babad\n', E'3\n'),
    (467, 'trie-them-va-tim-tu', 'Trie: thêm và tìm từ',
        'Xây dựng cây tiền tố để thêm từ và kiểm tra từ đã tồn tại.',
        'Dòng đầu chứa q. Mỗi dòng sau là ADD word hoặc FIND word, word chỉ gồm chữ cái thường.',
        'Với mỗi FIND, in YES nếu từ đã được thêm, ngược lại in NO.', E'6\nADD apple\nFIND apple\nFIND app\nADD app\nFIND app\nFIND apply\n', E'YES\nNO\nYES\nNO\n'),
    (468, 'duyet-do-thi-dfs', 'Duyệt đồ thị DFS',
        'Duyệt sâu đồ thị vô hướng từ đỉnh 1, luôn thăm đỉnh kề nhỏ hơn trước.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh vô hướng u v; các đỉnh đánh số từ 1.',
        'In thứ tự các đỉnh được thăm, cách nhau một dấu cách.', E'5 5\n1 2\n1 3\n2 4\n3 4\n4 5\n', E'1 2 4 3 5\n'),
    (469, 'phat-hien-chu-trinh-vo-huong', 'Phát hiện chu trình vô hướng',
        'Kiểm tra đồ thị vô hướng có chứa chu trình bằng DFS hoặc DSU.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh vô hướng u v.',
        'In YES nếu có chu trình, ngược lại in NO.', E'4 4\n1 2\n2 3\n3 1\n3 4\n', E'YES\n'),
    (470, 'phat-hien-chu-trinh-co-huong', 'Phát hiện chu trình có hướng',
        'Kiểm tra đồ thị có hướng có chu trình bằng màu DFS hoặc indegree.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh có hướng u v.',
        'In YES nếu có chu trình, ngược lại in NO.', E'4 4\n1 2\n2 3\n3 1\n3 4\n', E'YES\n'),
    (471, 'kiem-tra-do-thi-hai-phia', 'Kiểm tra đồ thị hai phía',
        'Tô hai màu các đỉnh để kiểm tra đồ thị vô hướng có phải bipartite.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh vô hướng u v.',
        'In YES nếu tô được bằng hai màu, ngược lại in NO.', E'4 4\n1 2\n2 3\n3 4\n4 1\n', E'YES\n'),
    (472, 'thanh-phan-lien-thong-manh-kosaraju', 'Kosaraju: thành phần liên thông mạnh',
        'Đếm số thành phần liên thông mạnh của đồ thị có hướng.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh có hướng u v.',
        'In số thành phần liên thông mạnh.', E'5 5\n1 2\n2 1\n2 3\n3 4\n4 3\n', E'3\n'),
    (473, 'canh-cau-tarjan', 'Tarjan: đếm cạnh cầu',
        'Đếm các cạnh cầu của đồ thị vô hướng bằng low-link Tarjan.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh vô hướng u v. Đồ thị không có cạnh lặp.',
        'In số cạnh cầu.', E'5 5\n1 2\n2 3\n3 1\n3 4\n4 5\n', E'2\n'),
    (474, 'dinh-khop-tarjan', 'Tarjan: đếm đỉnh khớp',
        'Đếm các đỉnh khớp của đồ thị vô hướng bằng low-link Tarjan.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh vô hướng u v.',
        'In số đỉnh khớp.', E'5 5\n1 2\n2 3\n3 1\n3 4\n4 5\n', E'2\n'),
    (475, 'cay-khung-nho-nhat-kruskal', 'Cây khung nhỏ nhất Kruskal',
        'Tìm tổng trọng số nhỏ nhất để nối toàn bộ đồ thị bằng Kruskal và DSU.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh vô hướng u v w. Luôn bảo đảm đồ thị liên thông.',
        'In tổng trọng số của cây khung nhỏ nhất.', E'4 5\n1 2 1\n1 3 4\n2 3 2\n2 4 5\n3 4 3\n', E'6\n'),
    (476, 'cay-khung-nho-nhat-prim', 'Cây khung nhỏ nhất Prim',
        'Tìm tổng trọng số nhỏ nhất để nối toàn bộ đồ thị bằng Prim.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh vô hướng u v w. Luôn bảo đảm đồ thị liên thông.',
        'In tổng trọng số của cây khung nhỏ nhất.', E'4 5\n1 2 1\n1 3 4\n2 3 2\n2 4 5\n3 4 3\n', E'6\n'),
    (477, 'bellman-ford-duong-di-ngan-nhat', 'Bellman-Ford đường đi ngắn nhất',
        'Tính khoảng cách ngắn nhất từ source trong đồ thị có cạnh âm nhưng không có chu trình âm đi được từ source.',
        'Dòng đầu chứa n, m, source. M dòng sau chứa cạnh có hướng u v w.',
        'In khoảng cách từ source đến các đỉnh 1..n; đỉnh không tới được in -1.', E'4 5 1\n1 2 4\n1 3 5\n2 3 -2\n3 4 3\n2 4 4\n', E'0 4 2 5\n'),
    (478, 'floyd-warshall-tat-ca-cap', 'Floyd-Warshall mọi cặp đỉnh',
        'Tính đường đi ngắn nhất giữa mọi cặp đỉnh bằng Floyd-Warshall.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh có hướng u v w. Không có chu trình âm.',
        'In ma trận khoảng cách; đỉnh không tới được in -1.', E'3 3\n1 2 4\n2 3 5\n1 3 15\n', E'0 4 9\n-1 0 5\n-1 -1 0\n'),
    (479, 'zero-one-bfs-do-thi', '0-1 BFS trên đồ thị',
        'Tính đường đi ngắn nhất khi mọi cạnh chỉ có trọng số 0 hoặc 1.',
        'Dòng đầu chứa n, m, source. M dòng sau chứa cạnh vô hướng u v w, với w là 0 hoặc 1.',
        'In khoảng cách từ source đến các đỉnh 1..n; đỉnh không tới được in -1.', E'4 5 1\n1 2 0\n1 3 1\n2 3 0\n2 4 1\n3 4 1\n', E'0 0 0 1\n'),
    (480, 'duong-di-ngan-nhat-dag', 'Đường đi ngắn nhất trên DAG',
        'Tính khoảng cách ngắn nhất từ source trong đồ thị có hướng không chu trình.',
        'Dòng đầu chứa n, m, source. M dòng sau chứa cạnh u v w. Luôn bảo đảm đồ thị là DAG.',
        'In khoảng cách từ source đến các đỉnh 1..n; đỉnh không tới được in -1.', E'5 5 1\n1 2 2\n1 3 1\n3 4 1\n2 4 2\n4 5 3\n', E'0 2 1 2 5\n'),
    (481, 'chu-trinh-euler-vo-huong', 'Chu trình Euler vô hướng',
        'Tìm chu trình Euler bắt đầu ở đỉnh 1 trong đồ thị vô hướng có chu trình Euler.',
        'Dòng đầu chứa n và m. M dòng sau chứa cạnh vô hướng u v. Luôn bảo đảm tồn tại chu trình Euler qua đỉnh 1.',
        'In một chu trình Euler bắt đầu từ 1; khi có lựa chọn, luôn chọn đỉnh kề nhỏ nhất.', E'4 4\n1 2\n2 3\n3 4\n4 1\n', E'1 2 3 4 1\n'),
    (482, 'to-tien-chung-thap-nhat-lca', 'Tổ tiên chung thấp nhất LCA',
        'Trả lời truy vấn tổ tiên chung thấp nhất của các cặp đỉnh trên cây gốc 1.',
        'Dòng đầu chứa n và q. N-1 dòng sau là cạnh cây. Q dòng cuối chứa hai đỉnh u v.',
        'In LCA của mỗi cặp trên một dòng.', E'7 3\n1 2\n1 3\n2 4\n2 5\n3 6\n3 7\n4 5\n4 6\n6 7\n', E'2\n1\n3\n'),
    (483, 'duong-kinh-cay', 'Đường kính của cây',
        'Tìm số cạnh trên đường đi dài nhất giữa hai đỉnh của cây.',
        'Dòng đầu chứa n. N-1 dòng sau chứa cạnh vô hướng u v của cây.',
        'In độ dài đường kính theo số cạnh.', E'6\n1 2\n2 3\n3 4\n2 5\n5 6\n', E'4\n'),
    (484, 'tong-gia-tri-cay-con', 'Tổng giá trị cây con',
        'Tính tổng giá trị trong cây con của các đỉnh trên cây gốc 1.',
        'Dòng đầu chứa n, q. Dòng sau chứa n giá trị đỉnh. N-1 dòng sau là cạnh, Q dòng cuối chứa đỉnh cần hỏi.',
        'In tổng giá trị của mỗi cây con.', E'5 3\n1 2 3 4 5\n1 2\n1 3\n3 4\n3 5\n1\n3\n4\n', E'15\n12\n4\n'),
    (485, 'balo-vo-han', 'Ba lô vô hạn',
        'Tối đa hóa giá trị ba lô khi mỗi loại vật có thể chọn nhiều lần.',
        'Dòng đầu chứa n và capacity. Dòng sau chứa n khối lượng, dòng tiếp theo chứa n giá trị tương ứng.',
        'In tổng giá trị lớn nhất không vượt quá capacity.', E'3 10\n2 3 4\n4 5 7\n', E'20\n'),
    (486, 'chia-tap-co-tong-bang-nhau', 'Chia tập có tổng bằng nhau',
        'Kiểm tra có thể chia mảng thành hai tập con có tổng bằng nhau hay không.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên dương.',
        'In YES nếu chia được, ngược lại in NO.', E'4\n1 5 11 5\n', E'YES\n'),
    (487, 'cat-thanh-doan', 'Cắt thanh tối đa lợi nhuận',
        'Tính doanh thu lớn nhất khi cắt thanh có độ dài n với giá bán từng độ dài.',
        'Dòng đầu chứa n. Dòng sau chứa n giá, phần tử thứ i là giá đoạn dài i.',
        'In doanh thu lớn nhất có thể nhận được.', E'8\n1 5 8 9 10 17 17 20\n', E'22\n'),
    (488, 'nhan-chuoi-ma-tran', 'Nhân chuỗi ma trận',
        'Tìm số phép nhân vô hướng ít nhất để nhân một chuỗi ma trận.',
        'Dòng đầu chứa n là số ma trận. Dòng sau chứa n+1 kích thước, ma trận i có kích thước d[i-1] x d[i].',
        'In số phép nhân vô hướng ít nhất.', E'3\n10 30 5 60\n', E'4500\n'),
    (489, 'tha-trung-va-trung', 'Thả trứng và tầng',
        'Tìm số lần thử ít nhất trong trường hợp xấu nhất để xác định tầng giới hạn.',
        'Một dòng chứa e và f, lần lượt là số trứng và số tầng.',
        'In số lần thử ít nhất trong trường hợp xấu nhất.', E'2 10\n', E'4\n'),
    (490, 'day-con-doi-xung-dai-nhat', 'Dãy con đối xứng dài nhất',
        'Tìm độ dài dãy con đối xứng dài nhất bằng quy hoạch động.',
        'Một dòng chứa chuỗi chữ thường không có khoảng trắng.',
        'In độ dài dãy con đối xứng dài nhất.', E'bbbab\n', E'4\n'),
    (491, 'day-con-tang-tong-lon-nhat', 'Dãy con tăng có tổng lớn nhất',
        'Tìm tổng lớn nhất của một dãy con tăng chặt không cần liên tiếp.',
        'Dòng đầu chứa n. Dòng sau chứa n số nguyên dương.',
        'In tổng lớn nhất có thể.', E'7\n1 101 2 3 100 4 5\n', E'106\n'),
    (492, 'phan-tu-nho-thu-k-quickselect', 'Phần tử nhỏ thứ K Quickselect',
        'Tìm phần tử nhỏ thứ k trung bình O(n) bằng Quickselect.',
        'Dòng đầu chứa n và k, với k tính từ 1. Dòng sau chứa n số nguyên phân biệt.',
        'In phần tử nhỏ thứ k.', E'6 3\n3 2 1 5 6 4\n', E'3\n'),
    (493, 'sinh-hoan-vi-theo-thu-tu', 'Sinh hoán vị theo thứ tự',
        'Sinh toàn bộ hoán vị của các số từ 1 đến n bằng quay lui.',
        'Một dòng chứa n, với 1 <= n <= 8.',
        'In các hoán vị theo thứ tự từ điển, mỗi hoán vị một dòng.', E'3\n', E'1 2 3\n1 3 2\n2 1 3\n2 3 1\n3 1 2\n3 2 1\n'),
    (494, 'dat-hau-n-queens', 'Đặt hậu N-Queens',
        'Đếm số cách đặt n quân hậu trên bàn cờ n x n sao cho không quân nào ăn nhau.',
        'Một dòng chứa n, với 1 <= n <= 12.',
        'In số cách đặt hợp lệ.', E'4\n', E'2\n'),
    (495, 'sinh-ngoac-hop-le', 'Sinh ngoặc hợp lệ',
        'Sinh mọi chuỗi ngoặc tròn hợp lệ gồm n cặp.',
        'Một dòng chứa n, với 1 <= n <= 10.',
        'In các chuỗi theo thứ tự từ điển, mỗi chuỗi một dòng.', E'2\n', E'(())\n()()\n'),
    (496, 'tim-tu-trong-luoi', 'Tìm từ trong lưới',
        'Kiểm tra một từ có thể được tạo từ các ô kề cạnh trong lưới chữ cái.',
        'Dòng đầu chứa n và m. N dòng sau là lưới chữ in hoa. Dòng cuối là word; mỗi ô chỉ dùng một lần trong một đường đi.',
        'In YES nếu tìm được word, ngược lại in NO.', E'3 4\nABCE\nSFCS\nADEE\nABCCED\n', E'YES\n'),
    (497, 'to-mau-vung-luoi', 'Tô màu vùng lưới',
        'Tô lại toàn bộ vùng 4 hướng cùng màu với ô bắt đầu bằng flood fill.',
        'Dòng đầu chứa n, m, r, c, newColor; r và c tính từ 1. N dòng sau là lưới số nguyên.',
        'In lưới sau khi tô, mỗi hàng một dòng.', E'3 3 1 1 2\n1 1 1\n1 1 0\n1 0 1\n', E'2 2 2\n2 2 0\n2 0 1\n'),
    (498, 'dem-duong-di-me-cung', 'Đếm đường đi trong mê cung',
        'Đếm số đường từ góc trên trái đến góc dưới phải khi chỉ đi xuống hoặc sang phải qua ô mở.',
        'Dòng đầu chứa n và m. N dòng sau chứa 0 hoặc 1, trong đó 1 là ô đi được.',
        'In số đường đi hợp lệ.', E'3 3\n1 1 0\n1 1 1\n0 1 1\n', E'2\n'),
    (499, 'chia-mang-gia-tri-lon-nhat-nho-nhat', 'Chia mảng tối ưu',
        'Chia mảng thành đúng k đoạn liên tiếp để giá trị lớn nhất của tổng các đoạn là nhỏ nhất.',
        'Dòng đầu chứa n và k. Dòng sau chứa n số nguyên dương.',
        'In giá trị lớn nhất nhỏ nhất có thể.', E'5 2\n7 2 5 10 8\n', E'18\n'),
    (500, 'nguoi-ban-hang-tsp', 'Người bán hàng TSP',
        'Tìm chi phí chu trình ngắn nhất đi từ thành phố 1, qua mọi thành phố đúng một lần rồi quay lại 1.',
        'Dòng đầu chứa n, với 2 <= n <= 15. N dòng sau là ma trận chi phí, phần tử đường chéo bằng 0.',
        'In chi phí nhỏ nhất của chu trình Hamilton.', E'4\n0 10 15 20\n10 0 35 25\n15 35 0 30\n20 25 30 0\n', E'80\n')
)
INSERT INTO programming_problems (
    id, slug, title, summary, description, input_description, output_description,
    sample_input, sample_output, topic, allowed_languages, starter_codes, difficulty, created_at
)
SELECT ('10000000-0000-0000-0000-' || LPAD(seed_number::text, 12, '0'))::uuid,
       slug, title, goal, goal, input_description, output_description,
       sample_input, sample_output, 'ALGORITHMS', 'CPP,JAVA,PYTHON', '{}', 'MEDIUM', '2026-09-19T00:00:00Z'
FROM classic_algorithm_problems
ON CONFLICT (id) DO UPDATE SET
    slug = EXCLUDED.slug,
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    description = EXCLUDED.description,
    input_description = EXCLUDED.input_description,
    output_description = EXCLUDED.output_description,
    sample_input = EXCLUDED.sample_input,
    sample_output = EXCLUDED.sample_output,
    topic = EXCLUDED.topic,
    allowed_languages = EXCLUDED.allowed_languages;

-- Keep the problem statement, input/output requirements and samples separate.
-- Older bundled rows did not have dedicated requirement columns, so provide a
-- useful baseline for every existing problem without overwriting teacher edits.
UPDATE programming_problems
SET input_description = CASE topic
        WHEN 'SQL' THEN 'Tạo dữ liệu mẫu theo phần mô tả và đọc các cột cần thiết từ bảng được cung cấp.'
        WHEN 'HTML' THEN 'Tạo cấu trúc HTML theo yêu cầu, sử dụng đúng nội dung và thuộc tính được mô tả.'
        ELSE 'Đọc dữ liệu từ stdin theo đúng định dạng được mô tả trong đề bài.'
    END
WHERE BTRIM(input_description) = '';

UPDATE programming_problems
SET output_description = CASE topic
        WHEN 'SQL' THEN 'Trả về đúng các cột, thứ tự và cách sắp xếp được yêu cầu; không thêm cột ngoài đề bài.'
        WHEN 'HTML' THEN 'Hiển thị đúng cấu trúc và nội dung được yêu cầu trong trang HTML.'
        ELSE 'In kết quả ra stdout theo đúng định dạng yêu cầu, không thêm nội dung thừa.'
    END
WHERE BTRIM(output_description) = '';

-- Rewrite the bundled statements that historically mixed the task with its
-- input/output format. The task text stays focused on the goal while the two
-- dedicated requirement fields carry the complete format and constraints.
UPDATE programming_problems
SET description = CASE slug
        WHEN 'xin-chao-devedu' THEN 'Tạo lời chào từ tên người dùng.'
        WHEN 'tong-hai-so' THEN 'Tính tổng của hai số nguyên.'
        WHEN 'phan-tu-lon-nhat' THEN 'Tìm phần tử lớn nhất trong một mảng số nguyên.'
        WHEN 'dem-so-tu' THEN 'Đếm số từ trong một dòng văn bản.'
        WHEN 'tai-khoan-ngan-hang' THEN 'Mô phỏng các giao dịch trên tài khoản ngân hàng và duy trì số dư hợp lệ.'
        WHEN 'mo-phong-ngan-xep' THEN 'Mô phỏng các thao tác push, pop và top trên ngăn xếp số nguyên.'
        WHEN 'tim-kiem-nhi-phan' THEN 'Tìm vị trí một giá trị trong mảng đã sắp xếp bằng tìm kiếm nhị phân.'
        WHEN 'sinh-vien-diem-cao' THEN 'Lọc và sắp xếp danh sách sinh viên theo điểm.'
        WHEN 'kiem-tra-chan-le' THEN 'Xác định tính chẵn lẻ của một số nguyên.'
        WHEN 'dien-tich-hinh-chu-nhat' THEN 'Tính diện tích hình chữ nhật.'
        WHEN 'dao-nguoc-mang' THEN 'Đảo ngược thứ tự các phần tử trong mảng.'
        WHEN 'dem-so-duong' THEN 'Đếm các phần tử dương trong mảng.'
        WHEN 'tong-phan-tu-mang' THEN 'Tính tổng các phần tử trong mảng.'
        WHEN 'chuoi-dai-nhat-java' THEN 'Tìm chuỗi dài nhất xuất hiện đầu tiên trong danh sách.'
        WHEN 'da-hinh-dien-tich' THEN 'Tính diện tích của hình được chọn bằng đa hình.'
        WHEN 'dem-boi-so-cua-ba' THEN 'Đếm các phần tử chia hết cho ba trong mảng.'
        WHEN 'dem-sinh-vien' THEN 'Đếm số sinh viên trong bảng dữ liệu.'
        WHEN 'dem-so-nguyen-to' THEN 'Đếm số nguyên tố trong mảng.'
        WHEN 'diem-cao-nhat' THEN 'Tìm sinh viên có điểm cao nhất.'
        WHEN 'diem-trung-binh-sinh-vien' THEN 'Tính điểm trung bình của một sinh viên bằng lớp Student.'
        WHEN 'gop-chuoi-java' THEN 'Ghép các từ trong danh sách thành một chuỗi duy nhất.'
        WHEN 'kiem-tra-day-doi-xung-python' THEN 'Kiểm tra một dãy có đối xứng hay không.'
        WHEN 'lop-hinh-chu-nhat' THEN 'Mô hình hóa hình chữ nhật bằng lớp Rectangle và tính diện tích.'
        WHEN 'luong-nhan-vien' THEN 'Tính tổng lương của nhân viên bằng lớp Employee.'
        WHEN 'nhan-vien-da-hinh' THEN 'Tính tổng lương của các nhân viên bằng đa hình.'
        WHEN 'nhan-vien-thuong' THEN 'Tính tổng thu nhập của nhân viên sau khi cộng tiền thưởng.'
        WHEN 'so-lon-hon' THEN 'So sánh hai số nguyên.'
        WHEN 'sap-xep-tang-dan' THEN 'Sắp xếp các phần tử trong mảng theo thứ tự tăng dần.'
        WHEN 'phan-tu-phan-biet' THEN 'Đếm số giá trị khác nhau trong mảng.'
        ELSE description
    END,
    input_description = CASE slug
        WHEN 'xin-chao-devedu' THEN 'Một chuỗi name không chứa khoảng trắng.'
        WHEN 'tong-hai-so' THEN 'Hai số nguyên a và b trên cùng một dòng; trị tuyệt đối mỗi số không vượt quá 10^9.'
        WHEN 'phan-tu-lon-nhat' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên; 1 <= n <= 100000.'
        WHEN 'dem-so-tu' THEN 'Một dòng văn bản không rỗng; các từ được phân tách bởi một hoặc nhiều khoảng trắng.'
        WHEN 'tai-khoan-ngan-hang' THEN 'Dòng đầu chứa số dư ban đầu. Dòng tiếp theo chứa q thao tác deposit x hoặc withdraw x.'
        WHEN 'mo-phong-ngan-xep' THEN 'Dòng đầu chứa q. Mỗi dòng sau là một lệnh push x, pop hoặc top.'
        WHEN 'tim-kiem-nhi-phan' THEN 'Mảng tăng dần gồm n số nguyên và giá trị x.'
        WHEN 'sinh-vien-diem-cao' THEN 'Bảng students gồm các cột id, name và score.'
        WHEN 'kiem-tra-chan-le' THEN 'Một số nguyên n.'
        WHEN 'dien-tich-hinh-chu-nhat' THEN 'Hai số nguyên dương w và h lần lượt là chiều rộng và chiều cao.'
        WHEN 'dao-nguoc-mang' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'dem-so-duong' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'tong-phan-tu-mang' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'chuoi-dai-nhat-java' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n chuỗi không có khoảng trắng.'
        WHEN 'da-hinh-dien-tich' THEN 'Một dòng gồm loại RECTANGLE hoặc SQUARE và các kích thước nguyên tương ứng.'
        WHEN 'dem-boi-so-cua-ba' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'dem-sinh-vien' THEN 'Bảng students gồm các cột id và name.'
        WHEN 'dem-so-nguyen-to' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên dương.'
        WHEN 'diem-cao-nhat' THEN 'Bảng students gồm các cột id, name và score.'
        WHEN 'diem-trung-binh-sinh-vien' THEN 'Ba số nguyên là điểm của sinh viên.'
        WHEN 'gop-chuoi-java' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n từ không có khoảng trắng.'
        WHEN 'kiem-tra-day-doi-xung-python' THEN 'Một số nguyên n và dãy gồm n số nguyên.'
        WHEN 'lop-hinh-chu-nhat' THEN 'Hai số nguyên width và height.'
        WHEN 'luong-nhan-vien' THEN 'Hai số nguyên baseSalary và bonus.'
        WHEN 'nhan-vien-da-hinh' THEN 'Dòng đầu chứa n. Mỗi dòng sau mô tả nhân viên FULL salary hoặc PART hours rate.'
        WHEN 'nhan-vien-thuong' THEN 'Lương cơ bản và phần trăm thưởng của nhân viên.'
        WHEN 'so-lon-hon' THEN 'Hai số nguyên a và b.'
        WHEN 'sap-xep-tang-dan' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'phan-tu-phan-biet' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        ELSE input_description
    END,
    output_description = CASE slug
        WHEN 'xin-chao-devedu' THEN 'In Hello, name! trên một dòng.'
        WHEN 'tong-hai-so' THEN 'In giá trị a + b trên một dòng.'
        WHEN 'phan-tu-lon-nhat' THEN 'In phần tử lớn nhất trong mảng.'
        WHEN 'dem-so-tu' THEN 'In số từ trong văn bản.'
        WHEN 'tai-khoan-ngan-hang' THEN 'In số dư cuối cùng sau khi thực hiện các giao dịch.'
        WHEN 'mo-phong-ngan-xep' THEN 'Với top, in phần tử trên cùng hoặc EMPTY nếu ngăn xếp rỗng.'
        WHEN 'tim-kiem-nhi-phan' THEN 'In chỉ số đầu tiên của x theo zero-based hoặc -1 nếu không tìm thấy; độ phức tạp O(log n).'
        WHEN 'sinh-vien-diem-cao' THEN 'In name và score với score giảm dần, sau đó name tăng dần.'
        WHEN 'kiem-tra-chan-le' THEN 'In EVEN nếu n chẵn, ngược lại in ODD.'
        WHEN 'dien-tich-hinh-chu-nhat' THEN 'In diện tích w * h.'
        WHEN 'dao-nguoc-mang' THEN 'In các phần tử theo thứ tự đảo ngược, cách nhau bởi một khoảng trắng.'
        WHEN 'dem-so-duong' THEN 'In số lượng phần tử dương.'
        WHEN 'tong-phan-tu-mang' THEN 'In tổng tất cả phần tử.'
        WHEN 'chuoi-dai-nhat-java' THEN 'In chuỗi dài nhất đầu tiên.'
        WHEN 'da-hinh-dien-tich' THEN 'In diện tích của object hình phù hợp.'
        WHEN 'dem-boi-so-cua-ba' THEN 'In số phần tử chia hết cho 3.'
        WHEN 'dem-sinh-vien' THEN 'In số lượng sinh viên trong bảng.'
        WHEN 'dem-so-nguyen-to' THEN 'In số lượng số nguyên tố trong dãy.'
        WHEN 'diem-cao-nhat' THEN 'In name và score của sinh viên có điểm cao nhất; nếu bằng điểm, ưu tiên name tăng dần.'
        WHEN 'diem-trung-binh-sinh-vien' THEN 'In phần nguyên của điểm trung bình.'
        WHEN 'gop-chuoi-java' THEN 'In một chuỗi gồm các từ nối liền nhau.'
        WHEN 'kiem-tra-day-doi-xung-python' THEN 'In YES nếu dãy đối xứng, ngược lại in NO.'
        WHEN 'lop-hinh-chu-nhat' THEN 'In diện tích được tính qua phương thức của object.'
        WHEN 'luong-nhan-vien' THEN 'In tổng lương qua phương thức của object.'
        WHEN 'nhan-vien-da-hinh' THEN 'In tổng lương của tất cả nhân viên.'
        WHEN 'nhan-vien-thuong' THEN 'In tổng thu nhập, làm tròn xuống đến số nguyên gần nhất.'
        WHEN 'so-lon-hon' THEN 'In số lớn hơn trong hai số.'
        WHEN 'sap-xep-tang-dan' THEN 'In dãy đã sắp xếp tăng dần, cách nhau bởi một khoảng trắng.'
        WHEN 'phan-tu-phan-biet' THEN 'In số lượng giá trị phân biệt.'
        ELSE output_description
    END
WHERE slug IN (
      'xin-chao-devedu', 'tong-hai-so', 'phan-tu-lon-nhat', 'dem-so-tu',
      'tai-khoan-ngan-hang', 'mo-phong-ngan-xep', 'tim-kiem-nhi-phan',
      'sinh-vien-diem-cao', 'kiem-tra-chan-le', 'dien-tich-hinh-chu-nhat',
      'dao-nguoc-mang', 'dem-so-duong', 'tong-phan-tu-mang', 'chuoi-dai-nhat-java',
      'da-hinh-dien-tich', 'dem-boi-so-cua-ba', 'dem-sinh-vien', 'dem-so-nguyen-to',
      'diem-cao-nhat', 'diem-trung-binh-sinh-vien', 'gop-chuoi-java',
      'kiem-tra-day-doi-xung-python', 'lop-hinh-chu-nhat', 'luong-nhan-vien',
      'nhan-vien-da-hinh', 'nhan-vien-thuong', 'so-lon-hon', 'sap-xep-tang-dan',
      'phan-tu-phan-biet'
  );

-- Keep each new bundled task statement focused on its goal. Input and output
-- formats are stored separately from the sample data shown to learners.
UPDATE programming_problems
SET description = CASE slug
        WHEN 'dem-so-chia-het-cho-5' THEN 'Đếm các phần tử trong dãy chia hết cho 5.'
        WHEN 'chuyen-doi-giay' THEN 'Đổi một khoảng thời gian từ giây sang giờ, phút và giây.'
        WHEN 'dem-ky-tu-in-hoa' THEN 'Đếm số chữ cái in hoa trong một dòng văn bản.'
        WHEN 'khoang-cach-hai-diem' THEN 'Tính khoảng cách Euclid giữa hai điểm trên mặt phẳng.'
        WHEN 'giao-hai-tap-hop-cpp' THEN 'Tìm các giá trị xuất hiện trong cả hai dãy số.'
        WHEN 'tong-duong-cheo-phu-ma-tran-cpp' THEN 'Tính tổng đường chéo phụ của một ma trận vuông.'
        WHEN 'kiem-tra-so-chinh-phuong-cpp' THEN 'Xác định một số nguyên có phải số chính phương hay không.'
        WHEN 'ma-hoa-dich-vong-cpp' THEN 'Mã hóa chuỗi chữ thường bằng phép dịch Caesar.'
        WHEN 'dem-ky-tu-java' THEN 'Đếm số lần một ký tự xuất hiện trong chuỗi.'
        WHEN 'sap-xep-tu-java' THEN 'Sắp xếp danh sách từ theo thứ tự từ điển tăng dần.'
        WHEN 'chuyen-vi-ma-tran-java' THEN 'Tạo ma trận chuyển vị từ ma trận đầu vào.'
        WHEN 'dem-nguyen-am-java' THEN 'Đếm số nguyên âm trong một dòng văn bản.'
        WHEN 'rut-gon-khoang-trang-python' THEN 'Chuẩn hóa khoảng trắng giữa các từ trong một dòng.'
        WHEN 'dem-tu-khong-trung-python' THEN 'Đếm số từ khác nhau trong một dòng.'
        WHEN 'xoay-mang-phai-python' THEN 'Xoay vòng các phần tử của mảng sang phải.'
        WHEN 'phan-tu-xuat-hien-nhieu-nhat-python' THEN 'Tìm giá trị có tần suất xuất hiện cao nhất trong mảng.'
        WHEN 'lop-hinh-vuong-oop' THEN 'Mô hình hóa hình vuông bằng lớp Square có phương thức tính chu vi.'
        WHEN 'lop-hinh-tron-oop' THEN 'Mô hình hóa hình tròn bằng lớp Circle có phương thức tính diện tích.'
        WHEN 'mo-phong-tai-khoan-ngan-hang-oop' THEN 'Mô phỏng số dư tài khoản ngân hàng qua lớp BankAccount.'
        WHEN 'lop-sinh-vien-diem-trung-binh-oop' THEN 'Tính điểm trung bình của sinh viên thông qua lớp Student.'
        WHEN 'mo-phong-hang-doi-them' THEN 'Thực hiện các thao tác cơ bản và truy vấn phần tử đầu của hàng đợi.'
        WHEN 'ngan-xep-min-stack' THEN 'Hỗ trợ truy vấn phần tử nhỏ nhất hiện tại trong ngăn xếp.'
        WHEN 'phan-tu-lon-hon-ben-phai' THEN 'Tìm phần tử lớn hơn gần nhất bên phải cho từng vị trí trong mảng.'
        WHEN 'loai-bo-phan-tu-trung-giu-thu-tu' THEN 'Loại bỏ các giá trị lặp lại nhưng giữ thứ tự xuất hiện đầu tiên.'
        WHEN 'tong-doan-con-lien-tiep-lon-nhat-2' THEN 'Tìm tổng lớn nhất của một đoạn con liên tiếp không rỗng.'
        WHEN 'hai-so-co-tong-bang-muc-tieu-2' THEN 'Tìm hai phần tử có tổng bằng giá trị mục tiêu.'
        WHEN 'tron-cac-doan-giao-nhau' THEN 'Gộp các đoạn số giao nhau hoặc tiếp giáp.'
        WHEN 'dem-nhan-vien-theo-phong' THEN 'Thống kê số nhân viên trong từng phòng ban bằng SQL.'
        WHEN 'khach-hang-chua-dat-hang' THEN 'Tìm những khách hàng chưa từng có đơn đặt hàng.'
        WHEN 'doanh-thu-theo-san-pham' THEN 'Tính doanh thu từ các dòng chi tiết đơn hàng của từng sản phẩm.'
        ELSE description
    END,
    input_description = CASE slug
        WHEN 'dem-so-chia-het-cho-5' THEN 'Dòng đầu chứa n (1 <= n <= 100000). Dòng tiếp theo chứa n số nguyên.'
        WHEN 'chuyen-doi-giay' THEN 'Một số nguyên không âm s là tổng số giây.'
        WHEN 'dem-ky-tu-in-hoa' THEN 'Một dòng văn bản có độ dài không quá 100000 ký tự.'
        WHEN 'khoang-cach-hai-diem' THEN 'Bốn số thực x1, y1, x2, y2 trên một dòng.'
        WHEN 'giao-hai-tap-hop-cpp' THEN 'Dòng đầu chứa n và m. Hai dòng tiếp theo lần lượt chứa n và m số nguyên.'
        WHEN 'tong-duong-cheo-phu-ma-tran-cpp' THEN 'Dòng đầu chứa n (1 <= n <= 500). Tiếp theo là n dòng, mỗi dòng có n số nguyên.'
        WHEN 'kiem-tra-so-chinh-phuong-cpp' THEN 'Một số nguyên không âm n không vượt quá 10^18.'
        WHEN 'ma-hoa-dich-vong-cpp' THEN 'Một chuỗi chữ thường tiếng Anh và số nguyên k (0 <= k <= 25) trên cùng một dòng.'
        WHEN 'dem-ky-tu-java' THEN 'Dòng đầu là chuỗi s. Dòng thứ hai chứa một ký tự c.'
        WHEN 'sap-xep-tu-java' THEN 'Dòng đầu chứa n (1 <= n <= 10000). Dòng tiếp theo chứa n từ chỉ gồm chữ cái tiếng Anh.'
        WHEN 'chuyen-vi-ma-tran-java' THEN 'Dòng đầu chứa n và m. Tiếp theo là n dòng, mỗi dòng có m số nguyên.'
        WHEN 'dem-nguyen-am-java' THEN 'Một dòng văn bản tiếng Anh có độ dài không quá 100000 ký tự.'
        WHEN 'rut-gon-khoang-trang-python' THEN 'Một dòng chứa các từ chỉ gồm chữ cái tiếng Anh, được phân tách bởi dấu cách.'
        WHEN 'dem-tu-khong-trung-python' THEN 'Một dòng gồm các từ không rỗng, phân tách bởi một dấu cách.'
        WHEN 'xoay-mang-phai-python' THEN 'Dòng đầu chứa n và k. Dòng thứ hai chứa n số nguyên.'
        WHEN 'phan-tu-xuat-hien-nhieu-nhat-python' THEN 'Dòng đầu chứa n (1 <= n <= 100000). Dòng tiếp theo chứa n số nguyên.'
        WHEN 'lop-hinh-vuong-oop' THEN 'Một số nguyên dương a là độ dài cạnh hình vuông.'
        WHEN 'lop-hinh-tron-oop' THEN 'Một số thực dương r là bán kính hình tròn.'
        WHEN 'mo-phong-tai-khoan-ngan-hang-oop' THEN 'Dòng đầu chứa số dư ban đầu và q. Mỗi dòng sau là DEPOSIT x hoặc WITHDRAW x.'
        WHEN 'lop-sinh-vien-diem-trung-binh-oop' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n điểm nguyên.'
        WHEN 'mo-phong-hang-doi-them' THEN 'Dòng đầu chứa q. Mỗi dòng tiếp theo là PUSH x, POP hoặc FRONT.'
        WHEN 'ngan-xep-min-stack' THEN 'Dòng đầu chứa q. Mỗi dòng tiếp theo là PUSH x, POP hoặc MIN.'
        WHEN 'phan-tu-lon-hon-ben-phai' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'loai-bo-phan-tu-trung-giu-thu-tu' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'tong-doan-con-lien-tiep-lon-nhat-2' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'hai-so-co-tong-bang-muc-tieu-2' THEN 'Dòng đầu chứa n và target. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'tron-cac-doan-giao-nhau' THEN 'Dòng đầu chứa n. Mỗi dòng tiếp theo chứa hai số nguyên left và right của một đoạn.'
        WHEN 'dem-nhan-vien-theo-phong' THEN 'Tạo bảng employees(id, department) và nạp dữ liệu theo phần mô tả.'
        WHEN 'khach-hang-chua-dat-hang' THEN 'Tạo hai bảng customers và orders rồi nạp dữ liệu theo phần mô tả.'
        WHEN 'doanh-thu-theo-san-pham' THEN 'Tạo hai bảng products và order_items rồi nạp dữ liệu theo phần mô tả.'
        ELSE input_description
    END,
    output_description = CASE slug
        WHEN 'dem-so-chia-het-cho-5' THEN 'In số lượng phần tử chia hết cho 5.'
        WHEN 'chuyen-doi-giay' THEN 'In thời gian theo HH:MM:SS; mỗi thành phần có ít nhất hai chữ số.'
        WHEN 'dem-ky-tu-in-hoa' THEN 'In số lượng chữ cái viết hoa từ A đến Z.'
        WHEN 'khoang-cach-hai-diem' THEN 'In khoảng cách với đúng hai chữ số sau dấu thập phân.'
        WHEN 'giao-hai-tap-hop-cpp' THEN 'In các phần tử phân biệt của giao theo thứ tự tăng dần, hoặc EMPTY.'
        WHEN 'tong-duong-cheo-phu-ma-tran-cpp' THEN 'In tổng các phần tử trên đường chéo phụ.'
        WHEN 'kiem-tra-so-chinh-phuong-cpp' THEN 'In YES nếu n là số chính phương, ngược lại in NO.'
        WHEN 'ma-hoa-dich-vong-cpp' THEN 'In chuỗi sau khi dịch từng chữ cái k vị trí theo bảng chữ cái vòng.'
        WHEN 'dem-ky-tu-java' THEN 'In số lần ký tự c xuất hiện trong chuỗi s.'
        WHEN 'sap-xep-tu-java' THEN 'In các từ theo thứ tự từ điển tăng dần trên một dòng.'
        WHEN 'chuyen-vi-ma-tran-java' THEN 'In ma trận chuyển vị, mỗi hàng trên một dòng và các phần tử cách nhau bởi dấu cách.'
        WHEN 'dem-nguyen-am-java' THEN 'In số ký tự nguyên âm a, e, i, o, u trong dòng, không phân biệt hoa thường.'
        WHEN 'rut-gon-khoang-trang-python' THEN 'In dòng đã bỏ khoảng trắng đầu/cuối và chỉ có một dấu cách giữa hai từ.'
        WHEN 'dem-tu-khong-trung-python' THEN 'In số lượng từ khác nhau, có phân biệt chữ hoa và chữ thường.'
        WHEN 'xoay-mang-phai-python' THEN 'In mảng sau khi xoay phải k vị trí trên một dòng.'
        WHEN 'phan-tu-xuat-hien-nhieu-nhat-python' THEN 'In phần tử có tần suất cao nhất; nếu hòa, in giá trị nhỏ nhất.'
        WHEN 'lop-hinh-vuong-oop' THEN 'In chu vi hình vuông được tính qua phương thức của lớp Square.'
        WHEN 'lop-hinh-tron-oop' THEN 'In diện tích hình tròn với đúng hai chữ số sau dấu thập phân.'
        WHEN 'mo-phong-tai-khoan-ngan-hang-oop' THEN 'In số dư cuối cùng sau khi thực hiện các giao dịch.'
        WHEN 'lop-sinh-vien-diem-trung-binh-oop' THEN 'In điểm trung bình với đúng hai chữ số sau dấu thập phân.'
        WHEN 'mo-phong-hang-doi-them' THEN 'Với mỗi FRONT, in phần tử đầu hàng đợi hoặc EMPTY.'
        WHEN 'ngan-xep-min-stack' THEN 'Với mỗi MIN, in phần tử nhỏ nhất hiện tại hoặc EMPTY.'
        WHEN 'phan-tu-lon-hon-ben-phai' THEN 'In phần tử lớn hơn gần nhất bên phải cho từng vị trí, hoặc -1.'
        WHEN 'loai-bo-phan-tu-trung-giu-thu-tu' THEN 'In các giá trị duy nhất theo thứ tự xuất hiện đầu tiên.'
        WHEN 'tong-doan-con-lien-tiep-lon-nhat-2' THEN 'In tổng lớn nhất của một đoạn con liên tiếp không rỗng.'
        WHEN 'hai-so-co-tong-bang-muc-tieu-2' THEN 'In cặp chỉ số zero-based đầu tiên hoặc -1 -1 nếu không tồn tại.'
        WHEN 'tron-cac-doan-giao-nhau' THEN 'In mỗi đoạn sau khi gộp trên một dòng, theo thứ tự tăng dần.'
        WHEN 'dem-nhan-vien-theo-phong' THEN 'Trả mỗi department cùng số nhân viên, sắp xếp department tăng dần.'
        WHEN 'khach-hang-chua-dat-hang' THEN 'Trả tên khách hàng không có đơn hàng, sắp xếp tên tăng dần.'
        WHEN 'doanh-thu-theo-san-pham' THEN 'Trả tên sản phẩm và doanh thu, sắp xếp doanh thu giảm dần rồi tên tăng dần.'
        ELSE output_description
    END
WHERE slug IN (
    'dem-so-chia-het-cho-5', 'chuyen-doi-giay', 'dem-ky-tu-in-hoa', 'khoang-cach-hai-diem',
    'giao-hai-tap-hop-cpp', 'tong-duong-cheo-phu-ma-tran-cpp', 'kiem-tra-so-chinh-phuong-cpp', 'ma-hoa-dich-vong-cpp',
    'dem-ky-tu-java', 'sap-xep-tu-java', 'chuyen-vi-ma-tran-java', 'dem-nguyen-am-java',
    'rut-gon-khoang-trang-python', 'dem-tu-khong-trung-python', 'xoay-mang-phai-python', 'phan-tu-xuat-hien-nhieu-nhat-python',
    'lop-hinh-vuong-oop', 'lop-hinh-tron-oop', 'mo-phong-tai-khoan-ngan-hang-oop', 'lop-sinh-vien-diem-trung-binh-oop',
    'mo-phong-hang-doi-them', 'ngan-xep-min-stack', 'phan-tu-lon-hon-ben-phai', 'loai-bo-phan-tu-trung-giu-thu-tu',
    'tong-doan-con-lien-tiep-lon-nhat-2', 'hai-so-co-tong-bang-muc-tieu-2', 'tron-cac-doan-giao-nhau',
    'dem-nhan-vien-theo-phong', 'khach-hang-chua-dat-hang', 'doanh-thu-theo-san-pham'
);

-- Normalize the bundled catalog: the statement only explains the goal, while
-- the two requirement fields own every detail about data and output format.
UPDATE programming_problems
SET input_description = BTRIM(SPLIT_PART(description, '. In ', 1)) || '.',
    output_description = 'In ' || BTRIM(SPLIT_PART(description, '. In ', 2))
WHERE id::text LIKE '10000000-0000-0000-0000-%'
  AND input_description = 'Đọc dữ liệu từ stdin theo đúng định dạng được mô tả trong đề bài.'
  AND POSITION('. In ' IN description) > 0;

UPDATE programming_problems
SET input_description = CASE slug
        WHEN 'dem-nguyen-am' THEN 'Một dòng chỉ gồm chữ cái Latin, có độ dài không quá 100000 ký tự.'
        WHEN 'mo-phong-hang-doi' THEN 'Dòng đầu chứa q. Mỗi dòng tiếp theo là push x, pop hoặc front.'
        WHEN 'tieu-de-devedu' THEN 'Không có dữ liệu stdin. Viết mã HTML tạo một thẻ h1 có nội dung DevEdu.'
        WHEN 'tong-doan' THEN 'Dòng đầu chứa n và q. Dòng tiếp theo chứa n số nguyên. Mỗi trong q dòng sau chứa l và r theo chỉ số 1-based.'
        WHEN 'mo-phong-hang-doi-hai-dau' THEN 'Dòng đầu chứa q. Mỗi dòng sau là push_front x, push_back x, pop_front, pop_back hoặc front.'
        WHEN 'phan-tu-lon-hon-ke-tiep' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'sap-xep-chon' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'tron-hai-day' THEN 'Dòng đầu chứa n và m. Hai dòng tiếp theo lần lượt chứa n và m số nguyên đã sắp xếp tăng dần.'
        WHEN 'xoay-mang-python' THEN 'Dòng đầu chứa n và d. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'danh-sach-lien-ket' THEN 'Dòng đầu chứa q. Mỗi dòng sau là append x, remove x hoặc print.'
        WHEN 'ma-hoa-dich-chuyen' THEN 'Một chuỗi chữ thường không có khoảng trắng và số nguyên k trên cùng một dòng.'
        WHEN 'truy-van-hang-doi' THEN 'Dòng đầu chứa q. Mỗi dòng sau là enqueue x, dequeue hoặc size.'
        WHEN 'tron-sort' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'sap-xep-chen' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'ngan-xep-min' THEN 'Dòng đầu chứa q. Mỗi dòng sau là push x, pop hoặc min.'
        WHEN 'duyet-cay-nhi-phan' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n giá trị nguyên phân biệt theo thứ tự chèn vào cây tìm kiếm nhị phân.'
        WHEN 'sap-xep-nhanh' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'sap-xep-noi-bot-cpp' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'tinh-trung-vi-python' THEN 'Dòng đầu chứa số lẻ n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'dien-tich-hinh-tron-oop' THEN 'Một số thực dương r là bán kính hình tròn.'
        WHEN 'hang-doi-vong' THEN 'Dòng đầu chứa q. Mỗi dòng sau là push x, pop, front hoặc size.'
        WHEN 'dem-nguyen-am-cpp' THEN 'Một dòng văn bản có độ dài không quá 100000 ký tự.'
        WHEN 'dien-tich-tam-giac-oop' THEN 'Hai số thực dương b và h là đáy và chiều cao của tam giác.'
        WHEN 'sap-xep-chen-java' THEN 'Dòng đầu chứa n. Dòng tiếp theo chứa n số nguyên.'
        WHEN 'tinh-chu-vi-hinh-tron-oop' THEN 'Một số thực dương r là bán kính hình tròn.'
        ELSE input_description
    END,
    output_description = CASE slug
        WHEN 'dem-nguyen-am' THEN 'In số ký tự a, e, i, o, u; không phân biệt chữ hoa và chữ thường.'
        WHEN 'mo-phong-hang-doi' THEN 'Với mỗi lệnh front, in phần tử đầu hàng đợi hoặc EMPTY nếu hàng đợi rỗng.'
        WHEN 'tieu-de-devedu' THEN 'Kết quả HTML phải chính xác là <h1>DevEdu</h1>.'
        WHEN 'tong-doan' THEN 'Với mỗi truy vấn, in tổng các phần tử từ vị trí l đến r trên một dòng.'
        WHEN 'mo-phong-hang-doi-hai-dau' THEN 'Với mỗi lệnh front, in phần tử đầu deque hoặc EMPTY nếu deque rỗng.'
        WHEN 'phan-tu-lon-hon-ke-tiep' THEN 'In n giá trị; mỗi giá trị là phần tử lớn hơn đầu tiên bên phải, hoặc -1 nếu không tồn tại.'
        WHEN 'sap-xep-chon' THEN 'In dãy theo thứ tự tăng dần, các phần tử cách nhau bởi một dấu cách.'
        WHEN 'tron-hai-day' THEN 'In dãy đã gộp theo thứ tự tăng dần, giữ nguyên phần tử trùng.'
        WHEN 'xoay-mang-python' THEN 'In dãy sau khi xoay vòng sang phải d modulo n vị trí.'
        WHEN 'danh-sach-lien-ket' THEN 'Với mỗi lệnh print, in các phần tử theo thứ tự hiện tại hoặc EMPTY.'
        WHEN 'ma-hoa-dich-chuyen' THEN 'In chuỗi sau khi dịch mỗi chữ cái sang phải k vị trí và quay vòng từ z về a.'
        WHEN 'truy-van-hang-doi' THEN 'Với mỗi lệnh size, in số phần tử hiện có trong hàng đợi.'
        WHEN 'tron-sort' THEN 'In dãy tăng dần, các phần tử cách nhau bởi một dấu cách.'
        WHEN 'sap-xep-chen' THEN 'In dãy tăng dần, các phần tử cách nhau bởi một dấu cách.'
        WHEN 'ngan-xep-min' THEN 'Với mỗi lệnh min, in giá trị nhỏ nhất hiện tại hoặc EMPTY nếu ngăn xếp rỗng.'
        WHEN 'duyet-cay-nhi-phan' THEN 'In các giá trị theo thứ tự inorder, cách nhau bởi một dấu cách.'
        WHEN 'sap-xep-nhanh' THEN 'In dãy tăng dần, các phần tử cách nhau bởi một dấu cách.'
        WHEN 'sap-xep-noi-bot-cpp' THEN 'In dãy tăng dần, các phần tử cách nhau bởi một dấu cách.'
        WHEN 'tinh-trung-vi-python' THEN 'Sau khi sắp xếp, in phần tử ở vị trí chính giữa.'
        WHEN 'dien-tich-hinh-tron-oop' THEN 'Tạo object hình tròn và in diện tích với đúng hai chữ số sau dấu thập phân.'
        WHEN 'hang-doi-vong' THEN 'Với front, in phần tử đầu hoặc EMPTY; với size, in kích thước hiện tại.'
        WHEN 'dem-nguyen-am-cpp' THEN 'In số nguyên âm a, e, i, o, u; không phân biệt chữ hoa và chữ thường.'
        WHEN 'dien-tich-tam-giac-oop' THEN 'Tạo object tam giác và in b * h / 2 với đúng hai chữ số sau dấu thập phân.'
        WHEN 'sap-xep-chen-java' THEN 'In dãy tăng dần, các phần tử cách nhau bởi một dấu cách.'
        WHEN 'tinh-chu-vi-hinh-tron-oop' THEN 'Tạo object hình tròn và in 2 * pi * r với đúng hai chữ số sau dấu thập phân.'
        ELSE output_description
    END
WHERE id::text LIKE '10000000-0000-0000-0000-%'
  AND slug IN (
      'dem-nguyen-am', 'mo-phong-hang-doi', 'tieu-de-devedu', 'tong-doan',
      'mo-phong-hang-doi-hai-dau', 'phan-tu-lon-hon-ke-tiep', 'sap-xep-chon',
      'tron-hai-day', 'xoay-mang-python', 'danh-sach-lien-ket', 'ma-hoa-dich-chuyen',
      'truy-van-hang-doi', 'tron-sort', 'sap-xep-chen', 'ngan-xep-min',
      'duyet-cay-nhi-phan', 'sap-xep-nhanh', 'sap-xep-noi-bot-cpp',
      'tinh-trung-vi-python', 'dien-tich-hinh-tron-oop', 'hang-doi-vong',
      'dem-nguyen-am-cpp', 'dien-tich-tam-giac-oop', 'sap-xep-chen-java',
      'tinh-chu-vi-hinh-tron-oop'
  );

UPDATE programming_problems
SET input_description = BTRIM(SPLIT_PART(description, '. ', 1))
        || '. Thực thi các lệnh CREATE TABLE và INSERT được cung cấp trước khi chạy truy vấn.',
    output_description = BTRIM(SUBSTRING(description FROM POSITION('. ' IN description) + 2))
WHERE id::text LIKE '10000000-0000-0000-0000-%'
  AND topic = 'SQL'
  AND input_description = 'Tạo dữ liệu mẫu theo phần mô tả và đọc các cột cần thiết từ bảng được cung cấp.';

UPDATE programming_problems
SET description = summary
WHERE id::text LIKE '10000000-0000-0000-0000-%';

-- Replace requirement placeholders left by problems created before Input and
-- Output became mandatory fields. Keep the statement focused on the goal.
UPDATE programming_problems
SET summary = CASE slug
        WHEN 'tong' THEN 'Tính tổng của hai số nguyên.'
        ELSE summary
    END,
    description = CASE slug
        WHEN 'binh-phuong' THEN 'Tính bình phương của một số nguyên.'
        WHEN 'dong-ho-12-gio' THEN 'Chuyển thời gian từ định dạng 24 giờ sang định dạng 12 giờ.'
        WHEN 'hieu-hai-so' THEN 'Tính hiệu của hai số nguyên.'
        WHEN 'tich-hai-so' THEN 'Tính tích của hai số nguyên.'
        WHEN 'tong' THEN 'Tính tổng của hai số nguyên.'
        WHEN 'tong-ba-so' THEN 'Tính tổng của ba số nguyên.'
        ELSE description
    END,
    input_description = CASE slug
        WHEN 'binh-phuong' THEN 'Một dòng chứa số nguyên n.'
        WHEN 'dong-ho-12-gio' THEN 'Một dòng chứa hai số nguyên h và m, lần lượt là giờ trong khoảng 0 đến 23 và phút trong khoảng 0 đến 59.'
        WHEN 'hieu-hai-so' THEN 'Một dòng chứa hai số nguyên a và b.'
        WHEN 'tich-hai-so' THEN 'Một dòng chứa hai số nguyên a và b.'
        WHEN 'tong' THEN 'Một dòng chứa hai số nguyên a và b.'
        WHEN 'tong-ba-so' THEN 'Một dòng chứa ba số nguyên a, b và c.'
        ELSE input_description
    END,
    output_description = CASE slug
        WHEN 'binh-phuong' THEN 'In ra giá trị n bình phương trên một dòng.'
        WHEN 'dong-ho-12-gio' THEN 'In thời gian ở định dạng hh:mm AM hoặc hh:mm PM; giờ và phút luôn gồm hai chữ số.'
        WHEN 'hieu-hai-so' THEN 'In ra hiệu a - b trên một dòng.'
        WHEN 'tich-hai-so' THEN 'In ra tích a * b trên một dòng.'
        WHEN 'tong' THEN 'In ra tổng a + b trên một dòng.'
        WHEN 'tong-ba-so' THEN 'In ra tổng a + b + c trên một dòng.'
        ELSE output_description
    END
WHERE input_description = 'Đọc dữ liệu từ stdin theo đúng định dạng được mô tả trong đề bài.'
  AND slug IN ('binh-phuong', 'dong-ho-12-gio', 'hieu-hai-so', 'tich-hai-so', 'tong', 'tong-ba-so');

-- Older startup scripts applied seed defaults to every row, including problems
-- created by teachers. Restore those rows from their saved starter-code keys
-- before applying defaults only to the bundled seed data below.
UPDATE programming_problems AS problem
SET allowed_languages = restored.allowed_languages
FROM (
    SELECT id,
           string_agg(language, ',' ORDER BY CASE language
               WHEN 'CPP' THEN 1
               WHEN 'JAVA' THEN 2
               WHEN 'PYTHON' THEN 3
               WHEN 'HTML' THEN 4
               WHEN 'MYSQL' THEN 5
               ELSE 6
           END) AS allowed_languages
    FROM programming_problems
    CROSS JOIN LATERAL jsonb_object_keys(starter_codes::jsonb) AS language
    WHERE id::text NOT LIKE '10000000-0000-0000-0000-%'
      AND jsonb_typeof(starter_codes::jsonb) = 'object'
    GROUP BY id
) AS restored
WHERE problem.id = restored.id
  AND restored.allowed_languages <> '';

UPDATE programming_problems
SET allowed_languages = CASE topic
    WHEN 'CPP' THEN 'CPP'
    WHEN 'JAVA' THEN 'JAVA'
    WHEN 'PYTHON' THEN 'PYTHON'
    WHEN 'SQL' THEN 'MYSQL'
    ELSE 'CPP,JAVA,PYTHON'
END
WHERE id::text LIKE '10000000-0000-0000-0000-%';
UPDATE programming_problems
SET allowed_languages = 'HTML'
WHERE id::text LIKE '10000000-0000-0000-0000-%'
  AND slug = 'tieu-de-devedu';

UPDATE programming_problems
SET starter_codes = (
    CASE WHEN POSITION('CPP' IN allowed_languages) > 0 THEN jsonb_build_object(
        'CPP', E'#include <iostream>\nusing namespace std;\n\nint main() {\n    // TODO: ' || title || E'\n    return 0;\n}\n'
    ) ELSE '{}'::jsonb END
    || CASE WHEN POSITION('JAVA' IN allowed_languages) > 0 THEN jsonb_build_object(
        'JAVA', E'import java.io.*;\nimport java.util.*;\n\npublic class Main {\n    public static void main(String[] args) throws Exception {\n        Scanner scanner = new Scanner(System.in);\n\n        // TODO: ' || title || E'\n    }\n}\n'
    ) ELSE '{}'::jsonb END
    || CASE WHEN POSITION('PYTHON' IN allowed_languages) > 0 THEN jsonb_build_object(
        'PYTHON', E'import sys\n\n# TODO: ' || title || E'\ndef solve():\n    pass\n\nif __name__ == "__main__":\n    solve()\n'
    ) ELSE '{}'::jsonb END
    || CASE WHEN POSITION('HTML' IN allowed_languages) > 0 THEN jsonb_build_object(
        'HTML', E'<!doctype html>\n<html lang="vi">\n<head>\n  <meta charset="UTF-8">\n  <title>' || title || E'</title>\n</head>\n<body>\n  <!-- TODO: Hoàn thành ' || title || E' -->\n</body>\n</html>\n'
    ) ELSE '{}'::jsonb END
    || CASE WHEN POSITION('MYSQL' IN allowed_languages) > 0 THEN jsonb_build_object(
        'MYSQL', E'-- ' || title || E'\n-- Viết truy vấn của bạn bên dưới\nSELECT *\nFROM your_table;\n'
    ) ELSE '{}'::jsonb END
)::text
WHERE starter_codes = '{}'
  AND id::text LIKE '10000000-0000-0000-0000-%';

-- Keep the bundled C++ templates lightweight. Loading the all-in-one GCC header
-- adds noticeable compile latency for simple exercises on Docker Desktop.
UPDATE programming_problems
SET starter_codes = jsonb_set(
        starter_codes::jsonb,
        '{CPP}',
        to_jsonb(replace(
                starter_codes::jsonb ->> 'CPP',
                '#include <bits/stdc++.h>',
                '#include <iostream>'
        ))
    )::text
WHERE id::text LIKE '10000000-0000-0000-0000-%'
  AND starter_codes::jsonb ? 'CPP'
  AND starter_codes::jsonb ->> 'CPP' LIKE '#include <bits/stdc++.h>%';

UPDATE programming_problems
SET starter_codes = jsonb_set(
        starter_codes::jsonb,
        '{CPP}',
        to_jsonb(
            replace(
                replace(starter_codes::jsonb ->> 'CPP', E'    ios::sync_with_stdio(false);\n', ''),
                E'    cin.tie(nullptr);\n', ''
            )
        )
    )::text
WHERE starter_codes::jsonb ? 'CPP'
  AND (
      starter_codes::jsonb ->> 'CPP' LIKE '%ios::sync_with_stdio(false);%'
      OR starter_codes::jsonb ->> 'CPP' LIKE '%cin.tie(nullptr);%'
  );

UPDATE problem_drafts
SET source_code = replace(
        replace(source_code, E'    ios::sync_with_stdio(false);\n', ''),
        E'    cin.tie(nullptr);\n', ''
    )
WHERE source_code LIKE '%ios::sync_with_stdio(false);%'
   OR source_code LIKE '%cin.tie(nullptr);%';

UPDATE programming_problems
SET difficulty = 'EASY'
WHERE id::text LIKE '10000000-0000-0000-0000-%';
UPDATE programming_problems SET difficulty = 'MEDIUM' WHERE slug IN (
    'so-fibonacci', 'tai-khoan-ngan-hang', 'mo-phong-ngan-xep', 'tim-kiem-nhi-phan', 'diem-cao-nhat',
    'tong-tu-mot-den-n', 'phan-tu-phan-biet', 'tong-doan', 'tan-suat-phan-tu', 'hai-chuoi-hoan-vi',
    'so-lon-thu-hai', 'luong-nhan-vien', 'mo-phong-hang-doi-hai-dau',
    'hai-so-co-tong-bang-muc-tieu', 'diem-trung-binh-theo-lop',
    'ucln-cua-hai-so', 'so-doi-xung', 'sap-xep-chon', 'doan-con-lien-tiep-lon-nhat',
    'tron-hai-day', 'sap-xep-tan-suat-java', 'loai-bo-ky-tu-trung-java',
    'chuyen-doi-nhiet-do-java', 'xoay-mang-python', 'kiem-tra-ngoac-python',
    'mo-phong-tai-khoan', 'hang-doi-uu-tien', 'danh-sach-lien-ket',
    'tim-kiem-nhi-phan-dau-tien', 'giao-nhau-doan', 'so-cach-leo-cau-thang',
    'tong-luong-phong-ban', 'san-pham-chua-ban', 'boi-chung-nho-nhat',
    'phan-tich-thua-so', 'day-con-dai-nhat-khong-giam', 'ma-hoa-dich-chuyen',
    'kiem-tra-so-armstrong', 'ma-tran-chuyen-vi-java', 'day-con-tong-muc-tieu',
    'dem-doan-lien-tiep', 'tach-so-am', 'nhan-vien-thuong', 'truy-van-hang-doi',
    'quy-hoach-duong-di', 'doanh-thu-theo-thang', 'dem-uoc-so', 'doi-co-so-nhi-phan',
    'day-con-tang-dai-nhat-cpp', 'tim-phan-tu-xuat-hien-nhieu-nhat', 'chuyen-co-so-16',
    'loc-so-nguyen-to', 'dem-cap-ky-tu', 'ngay-thu-trong-nam', 'doan-tang-lien-tiep-dai-nhat',
    'kiem-tra-so-hoan-hao', 'so-nghiem-phuong-trinh-bac-hai', 'the-tich-hinh-tru',
    'nhan-vien-da-hinh', 'ngan-xep-min', 'sap-xep-nhanh', 'luong-trung-binh-phong-ban',
    'dem-so-nguyen-to', 'sap-xep-noi-bot-cpp', 'tong-duong-cheo-phu-cpp', 'tong-lon-nhat-doan-cpp',
    'kiem-tra-so-nguyen-to-java', 'tinh-trung-vi-python', 'hang-doi-vong', 'chieu-cao-cay-nhi-phan',
    'duyet-rong-do-thi', 'luy-thua-nhanh', 'sap-xep-top-k', 'tong-doanh-thu-theo-danh-muc',
    'khach-hang-chua-co-don', 'nhi-phan-sang-thap-phan', 'dem-so-nguyen-to', 'sap-xep-giam-dan-cpp',
    'tong-hang-lon-nhat-cpp', 'dem-cap-bang-nhau-cpp', 'liet-ke-uoc-java', 'sap-xep-tu-theo-do-dai-python',
    'so-lon-thu-ba-python', 'hoa-hong-nhan-vien-oop', 'kiem-tra-ngoac-stack', 'dao-nguoc-danh-sach-lien-ket',
    'tim-so-bi-thieu', 'so-don-theo-khach-hang', 'nhan-vien-luong-cao-hon-trung-binh',
    'so-gan-0-nhat', 'dem-so-am-cpp', 'tim-phan-tu-nho-thu-hai-cpp', 'hieu-hai-ma-tran-cpp',
    'dem-doan-tang-cpp', 'hex-sang-thap-phan-cpp', 'ma-tran-doi-xung-java', 'sap-xep-chen-java',
    'sap-xep-tu-theo-do-dai-python', 'chuoi-xoay-python', 'gop-hai-day-python', 'phuong-tien-da-hinh-oop',
    'don-hang-co-thue-oop', 'dem-tan-suat-bang-map', 'doan-con-tong-k', 'sap-xep-dem',
    'san-pham-gia-trung-binh', 'don-hang-theo-thang'
    ,'khoang-cach-hai-diem', 'giao-hai-tap-hop-cpp', 'ma-hoa-dich-vong-cpp',
    'sap-xep-tu-java', 'chuyen-vi-ma-tran-java', 'xoay-mang-phai-python',
    'phan-tu-xuat-hien-nhieu-nhat-python', 'mo-phong-tai-khoan-ngan-hang-oop',
    'ngan-xep-min-stack', 'phan-tu-lon-hon-ben-phai', 'tong-doan-con-lien-tiep-lon-nhat-2',
    'hai-so-co-tong-bang-muc-tieu-2', 'tron-cac-doan-giao-nhau',
    'dem-nhan-vien-theo-phong', 'khach-hang-chua-dat-hang', 'doanh-thu-theo-san-pham'
);
UPDATE programming_problems SET difficulty = 'HARD' WHERE slug IN (
    'dau-ngoac-hop-le', 'da-hinh-dien-tich', 'phan-tu-lon-hon-ke-tiep',
    'duong-di-ngan-nhat-theo-chieu-rong', 'khach-hang-va-don-hang',
    'ma-tran-xoan-oc', 'day-con-tang-dai-nhat-python', 'dem-cap-nghich-the',
    'vung-lien-thong', 'doi-tien-it-xu-nhat', 'kiem-tra-ma-tran-doi-xung',
    'ma-tran-tich-java', 'duyet-cay-nhi-phan', 'bieu-thuc-hau-to', 'day-con-chung-dai-nhat',
    'day-con-tang-dai-nhat-java', 'duyet-cay-preorder', 'duyet-cay-postorder', 'duyet-do-thi-theo-chieu-sau'
);

UPDATE programming_problems
SET difficulty = CASE
    WHEN slug IN (
        'kiem-tra-ngoac-ba-loai', 'tinh-bieu-thuc-hau-to', 'cuc-dai-cua-so-truot',
        'hop-nhat-tap-hop-dsu', 'dem-cap-nghich-the-nang-cao',
        'do-dai-day-con-tang-dai-nhat', 'do-dai-day-con-chung-dai-nhat',
        'doi-tien-so-luong-it-nhat', 'dem-duong-di-luoi-co-vat-can',
        'dijkstra-duong-di-ngan-nhat', 'dem-so-nguyen-to-trong-doan',
        'san-pham-cao-hon-trung-binh-danh-muc', 'diem-cao-nhat-moi-lop-sql',
        'so-du-luy-ke-sql',
        'hinh-chu-nhat-lon-nhat-histogram-cpp', 'ngoac-dung-dai-nhat-cpp',
        'khoang-cach-chinh-sua-hai-chuoi', 'sap-xep-to-po-do-thi',
        'dijkstra-tu-mot-dinh', 'do-dai-day-con-tang-n-log-n',
        'ba-giao-dich-gan-nhat-sql'
    ) THEN 'HARD'
    WHEN topic IN ('DATA_STRUCTURES', 'ALGORITHMS', 'SQL') OR slug IN (
        'dem-so-nho-hon-trung-binh', 'kiem-tra-tam-giac-can', 'tong-boi-so-trong-doan',
        'so-ngay-trong-thang', 'tinh-giai-thua-kep', 'gop-hai-day-da-sap-xep-cpp',
        'hang-co-tong-lon-nhat-cpp', 'cot-co-tong-nho-nhat-cpp',
        'kiem-tra-ma-tran-don-vi-cpp', 'chen-phan-tu-vao-day-tang-cpp',
        'ucln-cua-day-cpp', 'dao-nguoc-tung-tu-cpp', 'trung-vi-day-le-cpp',
        'ky-tu-khong-lap-dau-tien-java', 'tong-bien-ma-tran-java',
        'chuan-hoa-ho-ten-java', 'tien-to-chung-dai-nhat-java',
        'so-lon-thu-hai-phan-biet-java', 'xoay-mang-trai-java',
        'kiem-tra-anagram-java', 'giao-hai-mang-java',
        'tan-suat-ky-tu-python-nang-cao', 'so-chan-truoc-so-le-python',
        'doi-xung-bo-khoang-trang-python', 'dem-cap-gia-tri-bang-nhau-python',
        'xoay-trai-day-python', 'giao-day-sap-xep-python',
        'sap-xep-theo-tri-tuyet-do-python', 'nen-chuoi-rle-python',
        'the-tich-hinh-tru-oop', 'luong-lam-them-oop', 'phan-so-toi-gian-oop',
        'cong-so-phuc-oop', 'tiet-kiem-lai-don-oop', 'xep-loai-sinh-vien-oop'
    ) THEN 'MEDIUM'
    ELSE 'EASY'
END
WHERE id::text BETWEEN '10000000-0000-0000-0000-000000000251'
                   AND '10000000-0000-0000-0000-000000000500';

UPDATE programming_problems
SET difficulty = 'HARD'
WHERE slug IN (
    'cay-doan-truy-van-min', 'sparse-table-truy-van-min',
    'cua-so-nho-nhat-chua-mau', 'tim-chuoi-kmp', 'tim-chuoi-z-algorithm',
    'tim-chuoi-rabin-karp', 'manacher-doi-xung-dai-nhat',
    'thanh-phan-lien-thong-manh-kosaraju', 'canh-cau-tarjan',
    'dinh-khop-tarjan', 'bellman-ford-duong-di-ngan-nhat',
    'floyd-warshall-tat-ca-cap', 'chu-trinh-euler-vo-huong',
    'to-tien-chung-thap-nhat-lca', 'nhan-chuoi-ma-tran',
    'tha-trung-va-trung', 'dat-hau-n-queens', 'nguoi-ban-hang-tsp'
);

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
    ('50000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000008', E'CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100), score INT);\nINSERT INTO students VALUES (1, ''An'', 9), (2, ''Binh'', 8), (3, ''Chi'', 7);\n', E'An\t9\nBinh\t8\n', 3000, 1),
    ('50000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000009', E'7\n', E'ODD\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000010', E'4 6\n', E'24\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000011', E'5\n1 2 3 4 5\n', E'5 4 3 2 1\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000015', '10000000-0000-0000-0000-000000000012', E'6\n-2 0 3 5 -1 4\n', E'3\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-000000000013', E'5\n2 -1 4 3 2\n', E'10\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000017', '10000000-0000-0000-0000-000000000014', E'level\n', E'YES\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000018', '10000000-0000-0000-0000-000000000015', E'DevEdu\n', E'3\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000019', '10000000-0000-0000-0000-000000000016', E'10\n', E'55\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000020', '10000000-0000-0000-0000-000000000017', E'8 9 7\n', E'8\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000018', E'4 5\n', E'20\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000022', '10000000-0000-0000-0000-000000000019', E'7\npush 4\npush 8\nfront\npop\nfront\npop\nfront\n', E'4\n8\nEMPTY\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000023', '10000000-0000-0000-0000-000000000020', E'([]{})\n', E'YES\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000024', '10000000-0000-0000-0000-000000000021', E'48 18\n', E'6\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000025', '10000000-0000-0000-0000-000000000022', E'5\n5 1 4 2 3\n', E'1 2 3 4 5\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000026', '10000000-0000-0000-0000-000000000023', E'CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100));\nINSERT INTO students VALUES (1, ''An''), (2, ''Binh''), (3, ''Chi'');\n', E'3\n', 3000, 1),
    ('50000000-0000-0000-0000-000000000027', '10000000-0000-0000-0000-000000000024', E'CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100), score INT);\nINSERT INTO students VALUES (1, ''An'', 9), (2, ''Binh'', 8), (3, ''Chi'', 9);\n', E'An\t9\n', 3000, 1),
    ('50000000-0000-0000-0000-000000000028', '10000000-0000-0000-0000-000000000025', '', E'<h1>DevEdu</h1>\n', 1000, 1),
    ('50000000-0000-0000-0000-000000000029', '10000000-0000-0000-0000-000000000026', E'10\n', E'55\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000030', '10000000-0000-0000-0000-000000000027', E'6\n1 2 2 3 1 4\n', E'4\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000031', '10000000-0000-0000-0000-000000000028', E'5 2\n1 2 3 4 5\n1 3\n2 5\n', E'6\n14\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000032', '10000000-0000-0000-0000-000000000029', E'7\n1 2 3 2 4 2 5\n2\n', E'3\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000033', '10000000-0000-0000-0000-000000000030', E'3\n1 2 3\n4 5 6\n7 8 9\n', E'15\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000034', '10000000-0000-0000-0000-000000000031', E'listen\nsilent\n', E'YES\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000035', '10000000-0000-0000-0000-000000000032', E'6\n4 9 2 9 7 4\n', E'7\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000036', '10000000-0000-0000-0000-000000000033', E'1000 250\n', E'1250\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000037', '10000000-0000-0000-0000-000000000034', E'RECTANGLE 4 5\n', E'20\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000038', '10000000-0000-0000-0000-000000000035', E'6\npush_back 2\npush_front 1\nfront\npop_front\nfront\npop_back\n', E'1\n2\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000039', '10000000-0000-0000-0000-000000000036', E'5\n2 1 5 3 4\n', E'5 5 -1 4 -1\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000040', '10000000-0000-0000-0000-000000000037', E'4\n2 7 11 15\n9\n', E'0 1\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000041', '10000000-0000-0000-0000-000000000038', E'5 5 1 5\n1 2\n2 5\n1 3\n3 4\n4 5\n', E'2\n', 2000, 1),
    ('50000000-0000-0000-0000-000000000042', '10000000-0000-0000-0000-000000000039', E'CREATE TABLE students (id INT, class_name VARCHAR(20), score INT);\nINSERT INTO students VALUES (1, ''A'', 8), (2, ''A'', 10), (3, ''B'', 7);\n', E'A\t9.0000\nB\t7.0000\n', 3000, 1),
    ('50000000-0000-0000-0000-000000000043', '10000000-0000-0000-0000-000000000040', E'CREATE TABLE customers (id INT, name VARCHAR(50));\nCREATE TABLE orders (id INT, customer_id INT, amount INT);\nINSERT INTO customers VALUES (1, ''An''), (2, ''Binh'');\nINSERT INTO orders VALUES (1, 1, 60), (2, 1, 50), (3, 2, 40);\n', E'An\t110\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000044', '10000000-0000-0000-0000-000000000041', E'17\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000045', '10000000-0000-0000-0000-000000000042', E'7\n1 3 5 6 8 9 10\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000046', '10000000-0000-0000-0000-000000000043', E'84 30\n', E'6\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000047', '10000000-0000-0000-0000-000000000044', E'12040\n', E'4021\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000048', '10000000-0000-0000-0000-000000000045', E'2026\n', E'10\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000049', '10000000-0000-0000-0000-000000000046', E'1221\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000050', '10000000-0000-0000-0000-000000000047', E'6\n5 2 8 1 4 3\n', E'1 2 3 4 5 8\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000051', '10000000-0000-0000-0000-000000000048', E'8\n-2 3 -1 5 -6 4 2 -1\n', E'7\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000052', '10000000-0000-0000-0000-000000000049', E'3 3\n1 2 3\n4 5 6\n7 8 9\n', E'1 2 3 6 9 8 7 4 5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000053', '10000000-0000-0000-0000-000000000050', E'4 3\n1 4 7 10\n2 3 9\n', E'1 2 3 4 7 9 10\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000054', '10000000-0000-0000-0000-000000000051', E'4\ncat\nprogramming\njava\ncode\n', E'programming\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000055', '10000000-0000-0000-0000-000000000052', E'7\n4 5 6 5 4 4 6\n', E'4 4 4 5 5 6 6\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000056', '10000000-0000-0000-0000-000000000053', E'programming\n', E'progamin\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000057', '10000000-0000-0000-0000-000000000054', E'25\n', E'77.00\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000058', '10000000-0000-0000-0000-000000000055', E'  hoc   lap trinh Java  \n', E'4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000059', '10000000-0000-0000-0000-000000000056', E'banana\n', E'a\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000060', '10000000-0000-0000-0000-000000000057', E' hoc lap trinh python \n', E'python\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000061', '10000000-0000-0000-0000-000000000058', E'5 2\n1 2 3 4 5\n', E'4 5 1 2 3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000062', '10000000-0000-0000-0000-000000000059', E'{[()]}\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000063', '10000000-0000-0000-0000-000000000060', E'6\n10 9 2 5 3 7\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000064', '10000000-0000-0000-0000-000000000061', E'100\n5\ndeposit 50\nwithdraw 30\nwithdraw 150\ndeposit 20\nwithdraw 10\n', E'130\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000065', '10000000-0000-0000-0000-000000000062', E'2\n', E'12.57\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000066', '10000000-0000-0000-0000-000000000063', E'4\n2 40\n5 10\n5 8\n1 99\n', E'8 10 40 99\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000067', '10000000-0000-0000-0000-000000000064', E'6\nappend 3\nappend 5\nremove 3\nappend 7\nprint\nremove 9\n', E'5 7\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000068', '10000000-0000-0000-0000-000000000065', E'7\n1 2 2 2 5 8 9\n2\n', E'1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000069', '10000000-0000-0000-0000-000000000066', E'3\n1 10\n3 8\n5 12\n', E'5 8\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000070', '10000000-0000-0000-0000-000000000067', E'5\n', E'8\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000071', '10000000-0000-0000-0000-000000000068', E'CREATE TABLE students (id INT, class_name VARCHAR(20));\nINSERT INTO students VALUES (1, ''A''), (2, ''B''), (3, ''A''), (4, ''C''), (5, ''B'');\n', E'A\t2\nB\t2\nC\t1\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000072', '10000000-0000-0000-0000-000000000069', E'CREATE TABLE employees (id INT, department VARCHAR(20), salary INT);\nINSERT INTO employees VALUES (1, ''IT'', 100), (2, ''HR'', 80), (3, ''IT'', 120);\n', E'IT\t220\nHR\t80\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000073', '10000000-0000-0000-0000-000000000070', E'CREATE TABLE products (id INT, name VARCHAR(50));\nCREATE TABLE order_items (product_id INT);\nINSERT INTO products VALUES (1, ''Keyboard''), (2, ''Mouse''), (3, ''Monitor'');\nINSERT INTO order_items VALUES (1), (3);\n', E'Mouse\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000074', '10000000-0000-0000-0000-000000000071', E'49\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000075', '10000000-0000-0000-0000-000000000072', E'12345\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000076', '10000000-0000-0000-0000-000000000073', E'12 18\n', E'36\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000077', '10000000-0000-0000-0000-000000000074', E'60\n', E'2 2 3 5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000078', '10000000-0000-0000-0000-000000000075', E'3 4 5\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000079', '10000000-0000-0000-0000-000000000076', E'2 2\n1 2\n3 4\n5 6\n7 8\n', E'6 8\n10 12\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000080', '10000000-0000-0000-0000-000000000077', E'8\n1 3 2 2 4 3 5 6\n', E'6\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000081', '10000000-0000-0000-0000-000000000078', E'5\n2 4 1 3 5\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000082', '10000000-0000-0000-0000-000000000079', E'DevEdu 2026\n', E'6\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000083', '10000000-0000-0000-0000-000000000080', E'abcxyz 2\n', E'cdezab\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000084', '10000000-0000-0000-0000-000000000081', E'6\n2 5 8 7 4 1\n', E'13\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000085', '10000000-0000-0000-0000-000000000082', E'153\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000086', '10000000-0000-0000-0000-000000000083', E'4\nDev\nEdu\nCode\n2026\n', E'DevEduCode2026\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000087', '10000000-0000-0000-0000-000000000084', E'2 3\n1 2 3\n4 5 6\n', E'1 4\n2 5\n3 6\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000088', '10000000-0000-0000-0000-000000000085', E'2024 2\n', E'29\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000089', '10000000-0000-0000-0000-000000000086', E'5 6\n2 3 1 2 4\n', E'2 4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000090', '10000000-0000-0000-0000-000000000087', E'6\n1 2 -1 3 4 5\n', E'9\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000091', '10000000-0000-0000-0000-000000000088', E'code learn python java\n', E'code java learn python\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000092', '10000000-0000-0000-0000-000000000089', E'6\n-2 5 -7 0 -1 4\n', E'3 -10\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000093', '10000000-0000-0000-0000-000000000090', E'2 2\n1 2\n3 4\n', E'1 3\n2 4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000094', '10000000-0000-0000-0000-000000000091', E'4 6\n', E'24 20\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000095', '10000000-0000-0000-0000-000000000092', E'1000 10\n', E'1100\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000096', '10000000-0000-0000-0000-000000000093', E'abccba\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000097', '10000000-0000-0000-0000-000000000094', E'5 3\n1 2\n2 3\n4 5\n', E'2\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000098', '10000000-0000-0000-0000-000000000095', E'5\nenqueue 4\nenqueue 7\nsize\ndequeue\nsize\n', E'2\n1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000099', '10000000-0000-0000-0000-000000000096', E'7\n9 1 5 3 8 2 4\n', E'1 2 3 4 5 8 9\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000100', '10000000-0000-0000-0000-000000000097', E'3 4\n', E'10\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000101', '10000000-0000-0000-0000-000000000098', E'3 11\n1 2 5\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000102', '10000000-0000-0000-0000-000000000099', E'CREATE TABLE students (id INT, name VARCHAR(50), score INT);\nINSERT INTO students VALUES (1, ''An'', 9), (2, ''Binh'', 7), (3, ''Chi'', 8);\n', E'An\nChi\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000103', '10000000-0000-0000-0000-000000000100', E'CREATE TABLE orders (id INT, order_month INT, amount INT);\nINSERT INTO orders VALUES (1, 1, 100), (2, 2, 80), (3, 1, 50);\n', E'1\t150\n2\t80\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000104', '10000000-0000-0000-0000-000000000101', E'123\n', E'6\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000105', '10000000-0000-0000-0000-000000000102', E'8 3 10\n', E'10\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000106', '10000000-0000-0000-0000-000000000103', E'12\n', E'6\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000107', '10000000-0000-0000-0000-000000000104', E'13\n', E'1101\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000108', '10000000-0000-0000-0000-000000000105', E'5\n', E'120\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000109', '10000000-0000-0000-0000-000000000106', E'7\n1 2 2 3 1 4 5\n', E'5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000110', '10000000-0000-0000-0000-000000000107', E'3\n1 2 3\n2 4 5\n3 5 6\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000111', '10000000-0000-0000-0000-000000000108', E'4\n5 2 4 1\n', E'1 2 4 5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000112', '10000000-0000-0000-0000-000000000109', E'8\n3 1 3 2 3 2 1 3\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000113', '10000000-0000-0000-0000-000000000110', E'255\n', E'FF\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000114', '10000000-0000-0000-0000-000000000111', E'6\n', E'720\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000115', '10000000-0000-0000-0000-000000000112', E'7\n2 3 4 5 6 7 8\n', E'2 3 5 7\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000116', '10000000-0000-0000-0000-000000000113', E'abracadabra\n', E'a 5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000117', '10000000-0000-0000-0000-000000000114', E'2 2 2\n1 2\n3 4\n5 6\n7 8\n', E'19 22\n43 50\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000118', '10000000-0000-0000-0000-000000000115', E'2024 3 1\n', E'61\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000119', '10000000-0000-0000-0000-000000000116', E'5\n1 2 3 4 5\n', E'1 3 6 10 15\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000120', '10000000-0000-0000-0000-000000000117', E'7\n1 2 3 2 4 5 6\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000121', '10000000-0000-0000-0000-000000000118', E'7\n1 2 2 3 2 4 5\n2\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000122', '10000000-0000-0000-0000-000000000119', E'28\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000123', '10000000-0000-0000-0000-000000000120', E'1 -3 2\n', E'2\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000124', '10000000-0000-0000-0000-000000000121', E'100 3 10\n', E'270\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000125', '10000000-0000-0000-0000-000000000122', E'2 3\n', E'37.70\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000126', '10000000-0000-0000-0000-000000000123', E'2\nFULL 1000\nPART 8 100\n', E'1800\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000127', '10000000-0000-0000-0000-000000000124', E'5\npush 5\npush 2\nmin\npop\nmin\n', E'2\n5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000128', '10000000-0000-0000-0000-000000000125', E'3\n2 1 3\n', E'1 2 3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000129', '10000000-0000-0000-0000-000000000126', E'5 1 2 + 4 * + 3 -\n', E'14\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000130', '10000000-0000-0000-0000-000000000127', E'6\n10 7 8 9 1 5\n', E'1 5 7 8 9 10\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000131', '10000000-0000-0000-0000-000000000128', E'abcde\nace\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000132', '10000000-0000-0000-0000-000000000129', E'CREATE TABLE employees (id INT, department VARCHAR(20), salary INT);\nINSERT INTO employees VALUES (1, ''IT'', 100), (2, ''HR'', 80), (3, ''IT'', 120);\n', E'HR\t80.0000\nIT\t110.0000\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000133', '10000000-0000-0000-0000-000000000130', E'CREATE TABLE orders (id INT, amount INT);\nINSERT INTO orders VALUES (1, 100), (2, 250), (3, 180);\n', E'2\t250\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000134', '10000000-0000-0000-0000-000000000131', E'2024\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000135', '10000000-0000-0000-0000-000000000132', E'123456\n', E'12\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000136', '10000000-0000-0000-0000-000000000133', E'49\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000137', '10000000-0000-0000-0000-000000000134', E'8\n2 4 5 9 11 12 13 15\n', E'4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000138', '10000000-0000-0000-0000-000000000135', E'1 2 5\n', E'25\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000139', '10000000-0000-0000-0000-000000000136', E'5\n5 1 4 2 8\n', E'1 2 4 5 8\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000140', '10000000-0000-0000-0000-000000000137', E'3\n1 2 3\n4 5 6\n7 8 9\n', E'15\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000141', '10000000-0000-0000-0000-000000000138', E'7\n-2 3 -1 5 -6 4 2\n', E'7\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000142', '10000000-0000-0000-0000-000000000139', E'5\n1 2 3 4 5\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000143', '10000000-0000-0000-0000-000000000140', E'DevEdu2026\n', E'2\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000144', '10000000-0000-0000-0000-000000000141', E'6\n4 9 2 7 9 1\n7\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000145', '10000000-0000-0000-0000-000000000142', E'hello\n', E'olleh\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000146', '10000000-0000-0000-0000-000000000143', E'6\n1 2 3 4 5 6\n', E'9\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000147', '10000000-0000-0000-0000-000000000144', E'29\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000148', '10000000-0000-0000-0000-000000000145', E'hoc lap trinh java\n', E'4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000149', '10000000-0000-0000-0000-000000000146', E'banana\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000150', '10000000-0000-0000-0000-000000000147', E'8\n0 1 0 2 3 0 4 0\n', E'4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000151', '10000000-0000-0000-0000-000000000148', E'5\n1 2 3 2 1\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000152', '10000000-0000-0000-0000-000000000149', E'5\n7 1 9 3 5\n', E'5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000153', '10000000-0000-0000-0000-000000000150', E'2 3\n1 2 3\n4 5 6\n', E'6\n15\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000154', '10000000-0000-0000-0000-000000000151', E'2\n', E'12.57\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000155', '10000000-0000-0000-0000-000000000152', E'1000 5\n', E'1050\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000156', '10000000-0000-0000-0000-000000000153', E'1000 5 50\n', E'1250\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000157', '10000000-0000-0000-0000-000000000154', E'5\npush 1\npush 2\npop\nfront\nsize\n', E'2\n1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000158', '10000000-0000-0000-0000-000000000155', E'7\n4 2 6 1 3 5 7\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000159', '10000000-0000-0000-0000-000000000156', E'5 4 1\n1 2\n1 3\n2 4\n3 5\n', E'1 2 3 4 5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000160', '10000000-0000-0000-0000-000000000157', E'2 10\n', E'1024\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000161', '10000000-0000-0000-0000-000000000158', E'6 3\n4 9 1 7 3 8\n', E'9 8 7\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000162', '10000000-0000-0000-0000-000000000159', E'CREATE TABLE sales (id INT, category VARCHAR(20), amount INT);\nINSERT INTO sales VALUES (1, ''A'', 100), (2, ''B'', 80), (3, ''A'', 50);\n', E'A\t150\nB\t80\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000163', '10000000-0000-0000-0000-000000000160', E'CREATE TABLE customers (id INT, name VARCHAR(50));\nCREATE TABLE orders (id INT, customer_id INT);\nINSERT INTO customers VALUES (1, ''An''), (2, ''Binh''), (3, ''Chi'');\nINSERT INTO orders VALUES (1, 1), (2, 3);\n', E'Binh\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000164', '10000000-0000-0000-0000-000000000161', E'6\n5 7 10 12 15 20\n', E'4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000165', '10000000-0000-0000-0000-000000000162', E'8 3 10\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000166', '10000000-0000-0000-0000-000000000163', E'10101\n', E'21\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000167', '10000000-0000-0000-0000-000000000164', E'100200\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000168', '10000000-0000-0000-0000-000000000165', E'8 12\n', E'2/3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000169', '10000000-0000-0000-0000-000000000166', E'6\n-2 5 -7 0 -1 4\n', E'-10\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000170', '10000000-0000-0000-0000-000000000167', E'5\n3 9 1 7 5\n', E'9 7 5 3 1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000171', '10000000-0000-0000-0000-000000000168', E'2 3\n1 2 3\n4 5 6\n', E'15\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000172', '10000000-0000-0000-0000-000000000169', E'6\n1 2 1 2 1 3\n', E'4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000173', '10000000-0000-0000-0000-000000000170', E'DevEdu\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000174', '10000000-0000-0000-0000-000000000171', E'5\n1 2 3 4 5\n', E'55\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000175', '10000000-0000-0000-0000-000000000172', E'7\n1 2 1 3 2 3 1\n', E'1 2 3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000176', '10000000-0000-0000-0000-000000000173', E'2 3\n1 2 3\n4 5 6\n', E'5 7 9\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000177', '10000000-0000-0000-0000-000000000174', E'DevEdu2026\n', E'4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000178', '10000000-0000-0000-0000-000000000175', E'24\n', E'1 2 3 4 6 8 12 24\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000179', '10000000-0000-0000-0000-000000000176', E'4\n1 2 3 4\n', E'4 3 2 1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000180', '10000000-0000-0000-0000-000000000177', E'6\n-2 5 -7 0 -1 4\n', E'9\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000181', '10000000-0000-0000-0000-000000000178', E'4\ncode learn ai dev\n', E'ai dev code learn\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000182', '10000000-0000-0000-0000-000000000179', E'education\n', E'5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000183', '10000000-0000-0000-0000-000000000180', E'7\n4 9 2 9 7 4 8\n', E'7\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000184', '10000000-0000-0000-0000-000000000181', E'3 4\n', E'6.00\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000185', '10000000-0000-0000-0000-000000000182', E'2000 15\n', E'2300\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000186', '10000000-0000-0000-0000-000000000183', E'3\n100 200 50\n', E'350\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000187', '10000000-0000-0000-0000-000000000184', E'([{}])\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000188', '10000000-0000-0000-0000-000000000185', E'5\n1 2 3 4 5\n', E'5 4 3 2 1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000189', '10000000-0000-0000-0000-000000000186', E'5\n4 2 1 3 5\n', E'4 2 1 3 5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000190', '10000000-0000-0000-0000-000000000187', E'6\n10 9 2 5 3 7\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000191', '10000000-0000-0000-0000-000000000188', E'5\n0 1 3 4 5\n', E'2\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000192', '10000000-0000-0000-0000-000000000189', E'CREATE TABLE customers (id INT, name VARCHAR(50));\nCREATE TABLE orders (id INT, customer_id INT);\nINSERT INTO customers VALUES (1, ''An''), (2, ''Binh''), (3, ''Chi'');\nINSERT INTO orders VALUES (1, 1), (2, 1), (3, 3);\n', E'An\t2\nBinh\t0\nChi\t1\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000193', '10000000-0000-0000-0000-000000000190', E'CREATE TABLE employees (id INT, name VARCHAR(50), salary INT);\nINSERT INTO employees VALUES (1, ''An'', 100), (2, ''Binh'', 80), (3, ''Chi'', 120);\n', E'Chi\t120\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000194', '10000000-0000-0000-0000-000000000191', E'5\n1 2 3 4 5\n', E'9\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000195', '10000000-0000-0000-0000-000000000192', E'123456\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000196', '10000000-0000-0000-0000-000000000193', E'5\n-7 -2 3 9 -1\n', E'-1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000197', '10000000-0000-0000-0000-000000000194', E'135\n', E'2 15\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000198', '10000000-0000-0000-0000-000000000195', E'2 3 4\n', E'14\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000199', '10000000-0000-0000-0000-000000000196', E'6\n-2 5 -7 0 -1 4\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000200', '10000000-0000-0000-0000-000000000197', E'6\n4 1 9 1 7 4\n', E'4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000201', '10000000-0000-0000-0000-000000000198', E'2 2\n5 6\n7 8\n1 2\n3 4\n', E'4 4\n4 4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000202', '10000000-0000-0000-0000-000000000199', E'5\n1 2 3 2 4\n', E'2\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000203', '10000000-0000-0000-0000-000000000200', E'1A\n', E'26\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000204', '10000000-0000-0000-0000-000000000201', E'6\n1 2 3 4 5 6\n', E'12\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000205', '10000000-0000-0000-0000-000000000202', E'5\n4 9 2 7 1\n', E'1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000206', '10000000-0000-0000-0000-000000000203', E'devedu\n', E'DEVEDU\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000207', '10000000-0000-0000-0000-000000000204', E'3\n1 2 3\n2 4 5\n3 5 6\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000208', '10000000-0000-0000-0000-000000000205', E'5\n5 2 4 1 3\n', E'1 2 3 4 5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000209', '10000000-0000-0000-0000-000000000206', E'3\n-2 3 4\n', E'29\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000210', '10000000-0000-0000-0000-000000000207', E'5\n-3 7 2 9 1\n', E'9\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000211', '10000000-0000-0000-0000-000000000208', E'6\n-2 5 -7 0 -1 4\n', E'-2 -7 -1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000212', '10000000-0000-0000-0000-000000000209', E'abc\nbca\n', E'YES\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000213', '10000000-0000-0000-0000-000000000210', E'3 2\n1 4 7\n2 5\n', E'1 2 4 5 7\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000214', '10000000-0000-0000-0000-000000000211', E'2\n', E'12.57\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000215', '10000000-0000-0000-0000-000000000212', E'CAR 120 2\n', E'240\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000216', '10000000-0000-0000-0000-000000000213', E'100 2 10\n', E'220\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000217', '10000000-0000-0000-0000-000000000214', E'7\n3 1 3 2 3 2 1\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000218', '10000000-0000-0000-0000-000000000215', E'5\n4 2 1 3 5\n', E'1 3 2 5 4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000219', '10000000-0000-0000-0000-000000000216', E'5 4 1\n1 2\n1 3\n2 4\n3 5\n', E'1 2 4 3 5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000220', '10000000-0000-0000-0000-000000000217', E'5 9\n2 3 4 1 5\n', E'1 3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000221', '10000000-0000-0000-0000-000000000218', E'7\n3 1 4 1 5 9 2\n', E'1 1 2 3 4 5 9\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000222', '10000000-0000-0000-0000-000000000219', E'CREATE TABLE products (id INT, category VARCHAR(20), price INT);\nINSERT INTO products VALUES (1, ''A'', 100), (2, ''B'', 80), (3, ''A'', 200);\n', E'A\t150.0000\nB\t80.0000\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000223', '10000000-0000-0000-0000-000000000220', E'CREATE TABLE orders (id INT, order_month INT);\nINSERT INTO orders VALUES (1, 1), (2, 2), (3, 1), (4, 3);\n', E'1\t2\n2\t1\n3\t1\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000224', '10000000-0000-0000-0000-000000000221', E'5\n7 10 12 -5 0\n', E'3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000225', '10000000-0000-0000-0000-000000000222', E'59\n', E'00:00:59\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000226', '10000000-0000-0000-0000-000000000223', E'HELLO world\n', E'5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000227', '10000000-0000-0000-0000-000000000224', E'1 1 4 5\n', E'5.00\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000228', '10000000-0000-0000-0000-000000000225', E'3 3\n-1 0 1\n1 2 -1\n', E'-1 1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000229', '10000000-0000-0000-0000-000000000226', E'2\n1 2\n3 4\n', E'5\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000230', '10000000-0000-0000-0000-000000000227', E'50\n', E'NO\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000231', '10000000-0000-0000-0000-000000000228', E'xyz 3\n', E'abc\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000232', '10000000-0000-0000-0000-000000000229', E'Hello Java\nl\n', E'2\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000233', '10000000-0000-0000-0000-000000000230', E'4\npear apple banana kiwi\n', E'apple banana kiwi pear\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000234', '10000000-0000-0000-0000-000000000231', E'2 2\n7 8\n9 10\n', E'7 9\n8 10\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000235', '10000000-0000-0000-0000-000000000232', E'JAVA\n', E'2\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000236', '10000000-0000-0000-0000-000000000233', E'hoc   python   vui\n', E'hoc python vui\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000237', '10000000-0000-0000-0000-000000000234', E'A a A\n', E'2\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000238', '10000000-0000-0000-0000-000000000235', E'4 0\n1 2 3 4\n', E'1 2 3 4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000239', '10000000-0000-0000-0000-000000000236', E'4\n4 5 5 4\n', E'4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000240', '10000000-0000-0000-0000-000000000237', E'7\n', E'28\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000241', '10000000-0000-0000-0000-000000000238', E'1\n', E'3.14\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000242', '10000000-0000-0000-0000-000000000239', E'50 2\nWITHDRAW 10\nDEPOSIT 5\n', E'45\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000243', '10000000-0000-0000-0000-000000000240', E'4\n60 70 80 90\n', E'75.00\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000244', '10000000-0000-0000-0000-000000000241', E'5\nPUSH 9\nFRONT\nPOP\nFRONT\nPOP\n', E'9\nEMPTY\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000245', '10000000-0000-0000-0000-000000000242', E'5\nPUSH 8\nPUSH 3\nMIN\nPOP\nMIN\n', E'3\n8\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000246', '10000000-0000-0000-0000-000000000243', E'5\n2 1 5 3 4\n', E'5 5 -1 4 -1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000247', '10000000-0000-0000-0000-000000000244', E'5\n1 1 2 1 3\n', E'1 2 3\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000248', '10000000-0000-0000-0000-000000000245', E'5\n-8 -3 -6 -2 -5\n', E'-2\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000249', '10000000-0000-0000-0000-000000000246', E'3 10\n1 2 3\n', E'-1 -1\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000250', '10000000-0000-0000-0000-000000000247', E'2\n1 2\n2 4\n', E'1 4\n', 2000, 1)
    ,('50000000-0000-0000-0000-000000000251', '10000000-0000-0000-0000-000000000248', E'CREATE TABLE employees (id INT, department VARCHAR(20));\nINSERT INTO employees VALUES (1, ''IT''), (2, ''HR''), (3, ''IT'');\n', E'HR\t1\nIT\t2\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000252', '10000000-0000-0000-0000-000000000249', E'CREATE TABLE customers (id INT, name VARCHAR(30));\nCREATE TABLE orders (id INT, customer_id INT);\nINSERT INTO customers VALUES (1, ''Nam''), (2, ''An''), (3, ''Mai'');\nINSERT INTO orders VALUES (1, 1);\n', E'An\nMai\n', 3000, 1)
    ,('50000000-0000-0000-0000-000000000253', '10000000-0000-0000-0000-000000000250', E'CREATE TABLE products (id INT, name VARCHAR(30));\nCREATE TABLE order_items (product_id INT, quantity INT, unit_price INT);\nINSERT INTO products VALUES (1, ''Mouse''), (2, ''Keyboard'');\nINSERT INTO order_items VALUES (1, 2, 10), (2, 3, 15), (1, 2, 10);\n', E'Keyboard\t45\nMouse\t40\n', 3000, 1)
ON CONFLICT DO NOTHING;

WITH new_cases(problem_number, input, expected_output, time_limit_ms) AS (VALUES
    (251, E'90817\n', E'8\n', 2000),
    (252, E'4\n1 1 9 9\n', E'2\n', 2000),
    (253, E'59\n', E'0 59\n', 2000),
    (254, E'2 3 4\n', E'NO\n', 2000),
    (255, E'5\n', E'55\n', 2000),
    (256, E'86420\n', E'0\n', 2000),
    (257, E'1000\n', E'1\n', 2000),
    (258, E'-5 5 2\n', E'0\n', 2000),
    (259, E'59\n', E'F\n', 2000),
    (260, E'2 2100\n', E'28\n', 2000),
    (261, E'4\n5 5 5 5\n', E'3\n', 2000),
    (262, E'8\n', E'384\n', 2000),
    (263, E'1 2 4\n', E'NO\n', 2000),
    (264, E'2 3\n1 4\n1 2 4\n', E'1 1 2 4 4\n', 2000),
    (265, E'one two\n', E'2\n', 2000),
    (266, E'2 2\n1 2\n3 0\n', E'0\n', 2000),
    (267, E'3 2\n1 9\n2 0\n3 -1\n', E'0\n', 2000),
    (268, E'2\n1 0\n1 1\n', E'NO\n', 2000),
    (269, E'3\n9 8 7\n0\n', E'8 7\n', 2000),
    (270, E'3\n1 2 2\n2\n', E'1 2 2 2\n', 2000),
    (271, E'a bb cccc ddd\n', E'4\n', 2000),
    (272, E'a1b20\n', E'3\n', 2000),
    (273, E'3\n14 21 35\n', E'7\n', 2000),
    (274, E'abc de\n', E'cba ed\n', 2000),
    (275, E'3\n1 2 2\n', E'NO\n', 2000),
    (276, E'3\n10 -1 4\n', E'4\n', 2000),
    (277, E'999\n', E'27\n', 2000),
    (278, E'aabb\n', E'NONE\n', 2000),
    (279, E'2 3\n1 2 3\n4 5 6\n', E'21\n', 2000),
    (280, E'le THI mai\n', E'Le Thi Mai\n', 2000),
    (281, E'3\ndog dove door\n', E'do\n', 2000),
    (282, E'4\n7 7 5 1\n', E'5\n', 2000),
    (283, E'0\n', E'0\n', 2000),
    (284, E'0\n', E'0\n', 2000),
    (285, E'4\n2 4 6 8\n', E'0\n', 2000),
    (286, E'4 5\n1 2 3 4\n', E'2 3 4 1\n', 2000),
    (287, E'hello\nolelh\n', E'YES\n', 2000),
    (288, E'orange banana Egg ice\n', E'3\n', 2000),
    (289, E'2 2\n1 3\n2 4\n', E'EMPTY\n', 2000),
    (290, E'3\n1 2 3\n', E'0\n', 2000),
    (291, E'a bb ccc dd\n', E'ccc\n', 2000),
    (292, E'abca\n', E'a 2\nb 1\nc 1\n', 2000),
    (293, E'3 2\n1 2\n3 4\n5 6\n', E'3 7 11\n', 2000),
    (294, E'1 3\n7 8 9\n', E'7 8 9\n', 2000),
    (295, E'4\n-2 -1 0 3\n', E'-2 0 -1 3\n', 2000),
    (296, E'Hello olleh\n', E'YES\n', 2000),
    (297, E'4\n5 5 5 5\n', E'6\n', 2000),
    (298, E'5\n1 1 1 1 1\n', E'0\n', 2000),
    (299, E'4 1\n1 2 3 4\n', E'2 3 4 1\n', 2000),
    (300, E'2 3\n1 2\n3 4 5\n', E'EMPTY\n', 2000),
    (301, E'5\n-3 1 -1 3 0\n', E'0 -1 1 -3 3\n', 2000),
    (302, E'zzzz\n', E'z4\n', 2000),
    (303, E'1 1\n', E'4\n', 2000),
    (304, E'1 10\n', E'31.40\n', 2000),
    (305, E'99.99 0\n', E'99.99\n', 2000),
    (306, E'40 10\n', E'400.00\n', 2000),
    (307, E'0\n', E'32.00\n', 2000),
    (308, E'-6 8\n', E'-3/4\n', 2000),
    (309, E'-1 5 3 -2\n', E'2 3\n', 2000),
    (310, E'-2 4 5 1\n', E'-6\n', 2000),
    (311, E'0\n', E'0\n', 2000),
    (312, E'1\n', E'10000\n', 2000),
    (313, E'500 10 3\n', E'650.00\n', 2000),
    (314, E'2\n4 5\n', E'4.50 FAIL\n', 2000),
    (315, E'([)]\n', E'NO\n', 2000),
    (316, E'10 2 / 3 +\n', E'8\n', 2000),
    (317, E'3\n-1 0 5\n', E'5 0 -1\n', 2000),
    (318, E'5\nTYPE x\nPRINT\nUNDO\nPRINT\nUNDO\n', E'x\nEMPTY\n', 2000),
    (319, E'4\n-1 -1 0 2\n', E'-1 2\n0 1\n2 1\n', 2000),
    (320, E'aabb\n', E'NONE\n', 2000),
    (321, E'5 2\n4 2 12 3 8\n', E'4 12 12 8\n', 2000),
    (322, E'3\n5 1 2\n', E'5 3\n', 2000),
    (323, E'2\nTAIL 5\nHEAD 4\n', E'4 5\n', 2000),
    (324, E'5 2\n3 9 1 9 2\n', E'9\n', 2000),
    (325, E'abca\n', E'NO\n', 2000),
    (326, E'4 3\nSAME 1 2\nUNION 1 2\nSAME 1 2\n', E'NO\nYES\n', 2000),
    (327, E'3 8\n1 3 7\n', E'3\n', 2000),
    (328, E'5\n1 2 3 4 5\n', E'0\n', 3000),
    (329, E'5\n5 4 3 2 1\n', E'1\n', 3000),
    (330, E'abc\ndef\n', E'0\n', 3000),
    (331, E'2 7\n2 4\n', E'-1\n', 3000),
    (332, E'2 2\n0 0\n0 0\n', E'2\n', 3000),
    (333, E'3 1 1 3\n1 2\n', E'-1\n', 3000),
    (334, E'4 0\n', E'4\n', 3000),
    (335, E'3 1 2\n2 3 7\n', E'-1 0 7\n', 3000),
    (336, E'3\n1 4\n2 3\n3 5\n', E'2\n', 3000),
    (337, E'5 0 7\n', E'1\n', 3000),
    (338, E'1 10\n', E'4\n', 3000),
    (339, E'CREATE TABLE employees (id INT, department VARCHAR(20), salary INT);\nINSERT INTO employees VALUES (1, ''Sales'', 10), (2, ''Sales'', 20), (3, ''IT'', 5);\n', E'IT\t5\nSales\t20\n', 3000),
    (340, E'CREATE TABLE customers (id INT, name VARCHAR(30));\nCREATE TABLE orders (id INT, customer_id INT);\nINSERT INTO customers VALUES (1, ''Zoe''), (2, ''An'');\nINSERT INTO orders VALUES (1, 1);\n', E'An\t0\nZoe\t1\n', 3000),
    (341, E'CREATE TABLE products (id INT, name VARCHAR(30), category VARCHAR(20), price INT);\nINSERT INTO products VALUES (1, ''X'', ''C'', 10), (2, ''Y'', ''C'', 30), (3, ''Z'', ''D'', 20), (4, ''W'', ''D'', 20);\n', E'Y\t30\n', 3000),
    (342, E'CREATE TABLE orders (id INT, order_month INT, amount INT);\nINSERT INTO orders VALUES (1, 12, 5), (2, 1, 8), (3, 12, 15);\n', E'1\t8\n12\t20\n', 3000),
    (343, E'CREATE TABLE users (id INT, email VARCHAR(60));\nINSERT INTO users VALUES (1, ''x@mail.com''), (2, ''x@mail.com''), (3, ''y@mail.com''), (4, ''x@mail.com'');\n', E'x@mail.com\t3\n', 3000),
    (344, E'CREATE TABLE students (id INT, name VARCHAR(30), class_name VARCHAR(10), score INT);\nINSERT INTO students VALUES (1, ''A'', ''C'', 8), (2, ''B'', ''C'', 9);\n', E'C\tB\t9\n', 3000),
    (345, E'CREATE TABLE employees (id INT, name VARCHAR(30), manager_id INT);\nINSERT INTO employees VALUES (1, ''Root'', NULL), (2, ''Staff'', 1);\n', E'Root\n', 3000),
    (346, E'CREATE TABLE customers (id INT, name VARCHAR(30));\nCREATE TABLE orders (id INT, customer_id INT, order_date DATE);\nINSERT INTO customers VALUES (1, ''Mai'');\nINSERT INTO orders VALUES (1, 1, ''2025-01-01''), (2, 1, ''2025-12-31'');\n', E'Mai\t2025-12-31\n', 3000),
    (347, E'CREATE TABLE transactions (id INT, amount INT);\nINSERT INTO transactions VALUES (1, -5), (2, 10);\n', E'1\t-5\t-5\n2\t10\t5\n', 3000),
    (348, E'CREATE TABLE employees (id INT, salary INT);\nINSERT INTO employees VALUES (1, 5), (2, 1);\n', E'1\n', 3000),
    (349, E'CREATE TABLE inventory (product VARCHAR(30), quantity INT, unit_price INT);\nINSERT INTO inventory VALUES (''A'', 1, 7), (''A'', 2, 7);\n', E'A\t21\n', 3000),
    (350, E'CREATE TABLE categories (id INT, name VARCHAR(30));\nCREATE TABLE products (id INT, category_id INT);\nINSERT INTO categories VALUES (1, ''A''), (2, ''B'');\nINSERT INTO products VALUES (1, 2);\n', E'A\n', 3000)
)
INSERT INTO problem_test_cases (id, problem_id, input, expected_output, time_limit_ms, position)
SELECT ('50000000-0000-0000-0000-' || LPAD((problem_number + 3)::text, 12, '0'))::uuid,
       ('10000000-0000-0000-0000-' || LPAD(problem_number::text, 12, '0'))::uuid,
       input, expected_output, time_limit_ms, 1
FROM new_cases
ON CONFLICT (id) DO UPDATE SET
    input = EXCLUDED.input,
    expected_output = EXCLUDED.expected_output,
    time_limit_ms = EXCLUDED.time_limit_ms,
    position = EXCLUDED.position;

-- Seed the public sample as the first verified case for every classic
-- algorithm. The common case completion block below then creates positions
-- two and three so each new exercise has the same minimum judge coverage.
INSERT INTO problem_test_cases (id, problem_id, input, expected_output, time_limit_ms, position)
SELECT (
           SUBSTRING(case_hash, 1, 8) || '-' || SUBSTRING(case_hash, 9, 4) || '-' ||
           SUBSTRING(case_hash, 13, 4) || '-' || SUBSTRING(case_hash, 17, 4) || '-' ||
           SUBSTRING(case_hash, 21, 12)
       )::uuid,
       problem.id,
       problem.sample_input,
       problem.sample_output,
       3000,
       1
FROM (
    SELECT id,
           sample_input,
           sample_output,
           md5('devedu-classic-algorithm-case:' || id::text || ':1') AS case_hash
    FROM programming_problems
    WHERE id::text BETWEEN '10000000-0000-0000-0000-000000000451'
                       AND '10000000-0000-0000-0000-000000000500'
) AS problem
ON CONFLICT (problem_id, position) DO UPDATE SET
    input = EXCLUDED.input,
    expected_output = EXCLUDED.expected_output,
    time_limit_ms = EXCLUDED.time_limit_ms;

-- Every existing exercise exposes at least three judge cases. The first missing
-- case uses the documented sample; any remaining slot reuses the verified
-- baseline judge case so the expected output always matches the input.
WITH problem_case_state AS (
    SELECT problem.id,
           problem.sample_input,
           problem.sample_output,
           COUNT(test_case.id)::integer AS test_count
    FROM programming_problems AS problem
    LEFT JOIN problem_test_cases AS test_case ON test_case.problem_id = problem.id
    GROUP BY problem.id, problem.sample_input, problem.sample_output
    HAVING COUNT(test_case.id) < 3
), missing_cases AS (
    SELECT state.id AS problem_id,
           state.test_count,
           generated.position,
           state.sample_input,
           state.sample_output,
           baseline.input AS baseline_input,
           baseline.expected_output AS baseline_output,
           baseline.time_limit_ms
    FROM problem_case_state AS state
    LEFT JOIN LATERAL (
        SELECT input, expected_output, time_limit_ms
        FROM problem_test_cases
        WHERE problem_id = state.id
        ORDER BY position
        LIMIT 1
    ) AS baseline ON TRUE
    CROSS JOIN LATERAL generate_series(state.test_count + 1, 3) AS generated(position)
), prepared_cases AS (
    SELECT problem_id,
           position,
           CASE WHEN position = test_count + 1 THEN sample_input ELSE COALESCE(baseline_input, sample_input) END AS input,
           CASE WHEN position = test_count + 1 THEN sample_output ELSE COALESCE(baseline_output, sample_output) END AS expected_output,
           COALESCE(time_limit_ms, 1000) AS time_limit_ms,
           md5('devedu-problem-case:' || problem_id::text || ':' || position::text) AS id_hash
    FROM missing_cases
)
INSERT INTO problem_test_cases (id, problem_id, input, expected_output, time_limit_ms, position)
SELECT (SUBSTRING(id_hash, 1, 8) || '-' || SUBSTRING(id_hash, 9, 4) || '-' ||
        SUBSTRING(id_hash, 13, 4) || '-' || SUBSTRING(id_hash, 17, 4) || '-' ||
        SUBSTRING(id_hash, 21, 12))::uuid,
       problem_id, input, expected_output, time_limit_ms, position
FROM prepared_cases
ON CONFLICT (problem_id, position) DO NOTHING;

-- SQL exercises do not read stdin. Move their public schema/data setup into
-- the editable SQL starter while keeping per-test setup in the judge only.
UPDATE problem_test_cases AS test_case
SET input = problem.sample_input,
    expected_output = problem.sample_output
FROM programming_problems AS problem
WHERE test_case.problem_id = problem.id
  AND test_case.position = 1
  AND problem.topic = 'SQL'
  AND problem.id::text LIKE '10000000-0000-0000-0000-%'
  AND BTRIM(problem.sample_input) <> '';

UPDATE programming_problems
SET starter_codes = jsonb_set(
        starter_codes::jsonb,
        '{MYSQL}',
        to_jsonb(
            E'-- DEVEDU_SAMPLE_DATA_BEGIN\n'
            || RTRIM(sample_input)
            || E'\n-- DEVEDU_SAMPLE_DATA_END\n\n'
            || (starter_codes::jsonb ->> 'MYSQL')
        )
    )::text
WHERE topic = 'SQL'
  AND id::text LIKE '10000000-0000-0000-0000-%'
  AND BTRIM(sample_input) <> ''
  AND starter_codes::jsonb ? 'MYSQL'
  AND starter_codes::jsonb ->> 'MYSQL' NOT LIKE '%-- DEVEDU_SAMPLE_DATA_BEGIN%';

UPDATE programming_problems
SET input_description = 'Không có dữ liệu đầu vào từ stdin. Các câu lệnh tạo bảng và dữ liệu mẫu đã được đặt sẵn trong code.',
    sample_input = ''
WHERE topic = 'SQL'
  AND id::text LIKE '10000000-0000-0000-0000-%';
