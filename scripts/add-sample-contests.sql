-- Opt-in local data only. Never run from application startup/schema.sql.
-- Uses existing Problems and an existing ADMIN as creator. No fake participants,
-- submissions, results or ratings. A repeat run keeps existing schedules intact.
BEGIN;
SELECT pg_advisory_xact_lock(684390124);

DO $$
DECLARE
    creator_id UUID;
    sample RECORD;
    missing_count INTEGER;
BEGIN
    SELECT id INTO creator_id FROM users WHERE role = 'ADMIN' ORDER BY public_id LIMIT 1;
    IF creator_id IS NULL THEN
        RAISE EXCEPTION 'An existing ADMIN account is required to add sample contests';
    END IF;

    FOR sample IN
        SELECT * FROM (VALUES
            ('deed0001-2026-4000-8000-000000000001'::uuid,
             'DevEdu Warm-up #1', 'WEEKLY', INTERVAL '1 day', 120, 'BEGINNER',
             ARRAY['tong-hai-so', 'phan-tu-lon-nhat', 'dem-so-tu']),
            ('deed0001-2026-4000-8000-000000000002'::uuid,
             'DevEdu Algorithm Sprint #1', 'PRACTICE', INTERVAL '-15 minutes', 180, 'INTERMEDIATE',
             ARRAY['tim-kiem-nhi-phan', 'mo-phong-ngan-xep', 'balo-gia-tri-lon-nhat', 'zero-one-bfs-do-thi']),
            ('deed0001-2026-4000-8000-000000000003'::uuid,
             'DevEdu Practice Archive #1', 'PRACTICE', INTERVAL '-2 days', 120, 'BEGINNER',
             ARRAY['tong-hai-so', 'dem-so-tu', 'phan-tu-lon-nhat', 'tim-kiem-nhi-phan'])
        ) AS samples(id, name, type, start_offset, duration_minutes, difficulty, slugs)
    LOOP
        IF EXISTS (SELECT 1 FROM contests WHERE id = sample.id) THEN
            CONTINUE;
        END IF;
        SELECT COUNT(*) INTO missing_count
        FROM unnest(sample.slugs) AS requested(slug)
        LEFT JOIN programming_problems p ON p.slug = requested.slug AND NOT p.deleted
        WHERE p.id IS NULL;
        IF missing_count > 0 THEN
            RAISE EXCEPTION 'Missing active problems for %; no sample contests were added', sample.name;
        END IF;

        INSERT INTO contests (id, name, type, starts_at, ends_at, duration_minutes, created_by, rules, rated, difficulty)
        VALUES (sample.id, sample.name, sample.type,
                CURRENT_TIMESTAMP + sample.start_offset,
                CURRENT_TIMESTAMP + sample.start_offset + make_interval(mins => sample.duration_minutes),
                sample.duration_minutes, creator_id,
                'Contest mẫu để trải nghiệm DevEdu. Không tính rating. Mỗi bài được cộng điểm một lần khi Accepted; có thể nộp lại trong thời gian thi. Lượt sai không bị phạt. Xếp hạng theo điểm, sau đó thời gian giải bài cuối cùng và ID công khai. Các bài tham chiếu thư viện hiện có.',
                FALSE, sample.difficulty);

        INSERT INTO contest_problems (contest_id, problem_id, position, points)
        SELECT sample.id, p.id, requested.ordinality - 1, requested.ordinality * 100
        FROM unnest(sample.slugs) WITH ORDINALITY AS requested(slug, ordinality)
        JOIN programming_problems p ON p.slug = requested.slug AND NOT p.deleted;
    END LOOP;
END $$;
COMMIT;

SELECT name, starts_at, ends_at,
       CASE WHEN CURRENT_TIMESTAMP < starts_at THEN 'UPCOMING'
            WHEN CURRENT_TIMESTAMP < ends_at THEN 'ONGOING' ELSE 'FINISHED' END AS status
FROM contests
WHERE id IN ('deed0001-2026-4000-8000-000000000001',
             'deed0001-2026-4000-8000-000000000002',
             'deed0001-2026-4000-8000-000000000003')
ORDER BY starts_at DESC;
