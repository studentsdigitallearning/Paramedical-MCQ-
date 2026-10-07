-- ==============================================================================
-- Supabase Data Seed: Academic Content, Tests, Questions, and Leaderboard
-- Version: 20261004_seed_data.sql
-- ==============================================================================

-- 1. Classes & Courses
INSERT INTO public.classes (id, name, subject_count, display_order, is_active) VALUES
('class_8', 'Class 8', 5, 1, true),
('class_9', 'Class 9', 5, 2, true),
('class_10', 'Class 10', 5, 3, true),
('class_11', 'Class 11', 6, 4, true),
('class_12', 'Class 12', 6, 5, true),
('class_dmlt', 'DMLT', 4, 6, true),
('class_bmlt', 'BMLT', 4, 7, true),
('class_ot', 'OT Technician', 4, 8, true)
ON CONFLICT (id) DO UPDATE SET
name = EXCLUDED.name,
subject_count = EXCLUDED.subject_count,
display_order = EXCLUDED.display_order,
is_active = EXCLUDED.is_active;

-- 2. Categories
INSERT INTO public.categories (id, name, type, icon_key, subtitle, test_count, is_active) VALUES
('cat_subject', 'Subject Wise', 'SUBJECT', 'subject', 'Chapter-wise tests', 24, true),
('cat_mock', 'Mock Tests', 'MOCK', 'mock', 'Full syllabus', 8, true),
('cat_previous', 'Previous Tests', 'PREVIOUS', 'previous', 'Past board papers', 5, true),
('cat_daily', 'Daily Test', 'DAILY', 'daily', 'Today''s challenge', 1, true)
ON CONFLICT (id) DO UPDATE SET
name = EXCLUDED.name,
type = EXCLUDED.type,
icon_key = EXCLUDED.icon_key,
subtitle = EXCLUDED.subtitle,
test_count = EXCLUDED.test_count,
is_active = EXCLUDED.is_active;

-- 3. Subjects
INSERT INTO public.subjects (id, class_id, name, icon_key, chapter_count, progress_percent, is_active) VALUES
('sub_10_sci', 'class_10', 'Science', 'science', 6, 45, true),
('sub_10_math', 'class_10', 'Mathematics', 'math', 7, 60, true),
('sub_10_soc', 'class_10', 'Social Science', 'social', 5, 30, true),
('sub_10_eng', 'class_10', 'English', 'english', 4, 50, true),
('sub_10_hin', 'class_10', 'Hindi', 'hindi', 4, 40, true),
('sub_12_phy', 'class_12', 'Physics', 'science', 6, 20, true),
('sub_12_chem', 'class_12', 'Chemistry', 'science', 6, 25, true),
('sub_12_bio', 'class_12', 'Biology', 'science', 6, 30, true)
ON CONFLICT (id) DO UPDATE SET
name = EXCLUDED.name,
icon_key = EXCLUDED.icon_key,
chapter_count = EXCLUDED.chapter_count,
progress_percent = EXCLUDED.progress_percent,
is_active = EXCLUDED.is_active;

-- 4. Chapters for Class 10
INSERT INTO public.chapters (id, subject_id, chapter_number, name, test_count, progress_percent, is_active) VALUES
('ch_sci_1', 'sub_10_sci', 1, 'Chemical Reactions and Equations', 3, 75, true),
('ch_sci_2', 'sub_10_sci', 2, 'Acids, Bases and Salts', 3, 50, true),
('ch_sci_3', 'sub_10_sci', 3, 'Metals and Non-metals', 2, 40, true),
('ch_sci_4', 'sub_10_sci', 4, 'Life Processes', 3, 65, true),
('ch_sci_5', 'sub_10_sci', 5, 'Control and Coordination', 2, 20, true),
('ch_sci_6', 'sub_10_sci', 6, 'Electricity', 2, 30, true),
('ch_math_1', 'sub_10_math', 1, 'Real Numbers', 3, 80, true),
('ch_math_2', 'sub_10_math', 2, 'Polynomials', 3, 60, true),
('ch_math_3', 'sub_10_math', 3, 'Pair of Linear Equations in Two Variables', 2, 45, true),
('ch_math_4', 'sub_10_math', 4, 'Quadratic Equations', 2, 50, true),
('ch_math_5', 'sub_10_math', 5, 'Arithmetic Progressions', 2, 40, true),
('ch_math_6', 'sub_10_math', 6, 'Triangles', 2, 35, true),
('ch_math_7', 'sub_10_math', 7, 'Coordinate Geometry', 2, 70, true)
ON CONFLICT (id) DO UPDATE SET
name = EXCLUDED.name,
test_count = EXCLUDED.test_count,
progress_percent = EXCLUDED.progress_percent,
is_active = EXCLUDED.is_active;

-- 5. Tests
INSERT INTO public.tests (
    id, class_id, category_id, subject_id, subject_name, chapter_id, chapter_name,
    title, subtitle, question_count, duration_minutes, total_marks, negative_marking,
    difficulty, status, is_daily, expiry_note, is_published, is_active
) VALUES
('test_daily_10', 'class_10', 'cat_daily', 'sub_10_sci', 'Science & General', NULL, NULL,
 'Daily MCQ Challenge', 'Today''s All-India Challenge Test', 10, 15, 50.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', true, 'Expires at 12:00 AM midnight', true, true),
('test_sci_ch1_1', 'class_10', 'cat_subject', 'sub_10_sci', 'Science', 'ch_sci_1', 'Chemical Reactions and Equations',
 'Chemical Reactions & Equations - Set 1', 'Types of Reactions & Oxidation', 10, 15, 20.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', false, NULL, true, true),
('test_sci_ch2_1', 'class_10', 'cat_subject', 'sub_10_sci', 'Science', 'ch_sci_2', 'Acids, Bases and Salts',
 'Acids, Bases & Salts - Set 1', 'Indicators, pH Scale & Important Salts', 10, 15, 20.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', false, NULL, true, true),
('test_sci_ch4_1', 'class_10', 'cat_subject', 'sub_10_sci', 'Science', 'ch_sci_4', 'Life Processes',
 'Life Processes - Set 1', 'Nutrition, Respiration, Transport & Excretion', 10, 15, 20.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', false, NULL, true, true),
('test_math_ch1_1', 'class_10', 'cat_subject', 'sub_10_math', 'Mathematics', 'ch_math_1', 'Real Numbers',
 'Real Numbers - Set 1', 'Fundamental Theorem of Arithmetic & HCF/LCM', 10, 15, 20.0, 0.25,
 'EASY', 'NOT_ATTEMPTED', false, NULL, true, true),
('test_math_ch2_1', 'class_10', 'cat_subject', 'sub_10_math', 'Mathematics', 'ch_math_2', 'Polynomials',
 'Polynomials - Set 1', 'Zeroes & Coefficients of Quadratic Polynomials', 10, 15, 20.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', false, NULL, true, true),
('test_mock_1', 'class_10', 'cat_mock', NULL, 'All Subjects', NULL, NULL,
 'Class 10 All-India Mock Test 1', 'Full Syllabus • Time-bound Exam', 10, 30, 50.0, 0.25,
 'HARD', 'NOT_ATTEMPTED', false, NULL, true, true),
('test_prev_2024', 'class_10', 'cat_previous', NULL, 'Science & Math', NULL, NULL,
 'Board Examination 2024 (Official)', 'Official Past Year Paper', 10, 30, 50.0, 0.25,
 'HARD', 'NOT_ATTEMPTED', false, NULL, true, true)
ON CONFLICT (id) DO UPDATE SET
title = EXCLUDED.title,
subtitle = EXCLUDED.subtitle,
question_count = EXCLUDED.question_count,
duration_minutes = EXCLUDED.duration_minutes,
total_marks = EXCLUDED.total_marks,
is_published = EXCLUDED.is_published,
is_active = EXCLUDED.is_active;

NOTIFY pgrst, 'reload schema';
