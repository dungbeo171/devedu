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
    sample_input TEXT NOT NULL DEFAULT '',
    sample_output TEXT NOT NULL DEFAULT '',
    topic VARCHAR(30) NOT NULL CHECK (
        topic IN ('INTRODUCTION', 'CPP', 'JAVA', 'PYTHON', 'OOP', 'DATA_STRUCTURES', 'ALGORITHMS', 'SQL')
    ),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS sample_input TEXT NOT NULL DEFAULT '';
ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS sample_output TEXT NOT NULL DEFAULT '';
ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS difficulty VARCHAR(20) NOT NULL DEFAULT 'EASY';
ALTER TABLE programming_problems ADD COLUMN IF NOT EXISTS allowed_languages VARCHAR(100) NOT NULL DEFAULT 'CPP,JAVA,PYTHON';
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
    )
ON CONFLICT (id) DO UPDATE SET
    slug = EXCLUDED.slug,
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    description = EXCLUDED.description,
    sample_input = EXCLUDED.sample_input,
    sample_output = EXCLUDED.sample_output,
    topic = EXCLUDED.topic;

UPDATE programming_problems
SET allowed_languages = CASE topic
    WHEN 'CPP' THEN 'CPP'
    WHEN 'JAVA' THEN 'JAVA'
    WHEN 'PYTHON' THEN 'PYTHON'
    WHEN 'SQL' THEN 'MYSQL'
    ELSE 'CPP,JAVA,PYTHON'
END;
UPDATE programming_problems SET allowed_languages = 'HTML' WHERE slug = 'tieu-de-devedu';

UPDATE programming_problems SET difficulty = 'EASY';
UPDATE programming_problems SET difficulty = 'MEDIUM' WHERE slug IN (
    'so-fibonacci', 'tai-khoan-ngan-hang', 'mo-phong-ngan-xep', 'tim-kiem-nhi-phan', 'diem-cao-nhat',
    'tong-tu-mot-den-n', 'phan-tu-phan-biet', 'tong-doan', 'tan-suat-phan-tu', 'hai-chuoi-hoan-vi',
    'so-lon-thu-hai', 'luong-nhan-vien', 'mo-phong-hang-doi-hai-dau',
    'hai-so-co-tong-bang-muc-tieu', 'diem-trung-binh-theo-lop'
);
UPDATE programming_problems SET difficulty = 'HARD' WHERE slug IN (
    'dau-ngoac-hop-le', 'da-hinh-dien-tich', 'phan-tu-lon-hon-ke-tiep',
    'duong-di-ngan-nhat-theo-chieu-rong', 'khach-hang-va-don-hang'
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
