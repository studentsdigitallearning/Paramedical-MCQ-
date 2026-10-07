import os
import urllib.request
import json
import sys
from datetime import datetime, timezone, timedelta

PROJECT_REF = os.environ.get("SUPABASE_PROJECT_REF", sys.argv[1] if len(sys.argv) > 1 else "")
ACCESS_TOKEN = os.environ.get("SUPABASE_ACCESS_TOKEN", sys.argv[2] if len(sys.argv) > 2 else "")

if not PROJECT_REF or not ACCESS_TOKEN:
    print("Usage: python3 supabase_seed_data.py <SUPABASE_PROJECT_REF> <SUPABASE_ACCESS_TOKEN>")
    print("Or set SUPABASE_PROJECT_REF and SUPABASE_ACCESS_TOKEN environment variables.")
    print("Alternatively, you can copy supabase/migrations/20261004_seed_data.sql into Supabase SQL Editor.")
    sys.exit(1)

API_URL = f"https://api.supabase.com/v1/projects/{PROJECT_REF}/database/query"


def execute_sql(sql):
    req = urllib.request.Request(
        API_URL,
        data=json.dumps({"query": sql}).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {ACCESS_TOKEN}",
            "Content-Type": "application/json"
        },
        method="POST"
    )
    try:
        with urllib.request.urlopen(req) as resp:
            data = resp.read().decode("utf-8")
            return json.loads(data)
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8")
        print(f"HTTP Error {e.code}: {err}", file=sys.stderr)
        raise

ist_now = datetime.now(timezone.utc) + timedelta(hours=5, minutes=30)
today_key = ist_now.strftime("%Y-%m-%d")

print(f"Seeding Supabase data for today {today_key}...")

# 1. Classes
classes_sql = """
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
"""
execute_sql(classes_sql)
print("Classes seeded.")

# 2. Categories
categories_sql = """
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
"""
execute_sql(categories_sql)
print("Categories seeded.")

# 3. Subjects for Class 10
subjects_sql = """
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
class_id = EXCLUDED.class_id,
name = EXCLUDED.name,
icon_key = EXCLUDED.icon_key,
chapter_count = EXCLUDED.chapter_count,
progress_percent = EXCLUDED.progress_percent,
is_active = EXCLUDED.is_active;
"""
execute_sql(subjects_sql)
print("Subjects seeded.")

# 4. Chapters for Class 10
chapters_sql = """
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
('ch_math_7', 'sub_10_math', 7, 'Coordinate Geometry', 2, 70, true),

('ch_soc_1', 'sub_10_soc', 1, 'The Rise of Nationalism in Europe', 2, 50, true),
('ch_soc_2', 'sub_10_soc', 2, 'Nationalism in India', 2, 60, true),
('ch_eng_1', 'sub_10_eng', 1, 'A Letter to God', 2, 85, true),
('ch_eng_2', 'sub_10_eng', 2, 'Nelson Mandela: Long Walk to Freedom', 2, 40, true)
ON CONFLICT (id) DO UPDATE SET
subject_id = EXCLUDED.subject_id,
chapter_number = EXCLUDED.chapter_number,
name = EXCLUDED.name,
test_count = EXCLUDED.test_count,
progress_percent = EXCLUDED.progress_percent,
is_active = EXCLUDED.is_active;
"""
execute_sql(chapters_sql)
print("Chapters seeded.")

# 5. Tests
tests_sql = """
INSERT INTO public.tests (
    id, class_id, category_id, subject_id, subject_name, chapter_id, chapter_name,
    title, subtitle, question_count, duration_minutes, total_marks, negative_marking,
    difficulty, status, is_daily, expiry_note, is_published, is_active
) VALUES
-- 1. Daily Test (ONLY in Daily Test section!)
('test_daily_10', 'class_10', 'cat_daily', NULL, 'Daily MCQ', NULL, NULL,
 'Daily MCQ Challenge', 'Today''s All-India Challenge Test', 10, 15, 50.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', true, 'Expires at 12:00 AM midnight', true, true),

-- 2. Science Chapter 1 Test
('test_sci_ch1_1', 'class_10', 'cat_subject', 'sub_10_sci', 'Science', 'ch_sci_1', 'Chemical Reactions and Equations',
 'Chemical Reactions & Equations - Set 1', 'Types of Reactions & Oxidation', 10, 15, 20.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', false, NULL, true, true),

-- 3. Science Chapter 2 Test
('test_sci_ch2_1', 'class_10', 'cat_subject', 'sub_10_sci', 'Science', 'ch_sci_2', 'Acids, Bases and Salts',
 'Acids, Bases & Salts - Set 1', 'Indicators, pH Scale & Important Salts', 10, 15, 20.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', false, NULL, true, true),

-- 4. Science Chapter 4 Test
('test_sci_ch4_1', 'class_10', 'cat_subject', 'sub_10_sci', 'Science', 'ch_sci_4', 'Life Processes',
 'Life Processes - Set 1', 'Nutrition, Respiration, Transport & Excretion', 10, 15, 20.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', false, NULL, true, true),

-- 5. Math Chapter 1 Test
('test_math_ch1_1', 'class_10', 'cat_subject', 'sub_10_math', 'Mathematics', 'ch_math_1', 'Real Numbers',
 'Real Numbers - Set 1', 'Fundamental Theorem of Arithmetic & HCF/LCM', 10, 15, 20.0, 0.25,
 'EASY', 'NOT_ATTEMPTED', false, NULL, true, true),

-- 6. Math Chapter 2 Test
('test_math_ch2_1', 'class_10', 'cat_subject', 'sub_10_math', 'Mathematics', 'ch_math_2', 'Polynomials',
 'Polynomials - Set 1', 'Zeroes & Coefficients of Quadratic Polynomials', 10, 15, 20.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', false, NULL, true, true),

-- 7. Mock Tests
('test_mock_1', 'class_10', 'cat_mock', NULL, 'All Subjects', NULL, NULL,
 'Class 10 All-India Mock Test 1', 'Full Syllabus • Time-bound Exam', 10, 30, 50.0, 0.25,
 'HARD', 'NOT_ATTEMPTED', false, NULL, true, true),

('test_mock_2', 'class_10', 'cat_mock', NULL, 'All Subjects', NULL, NULL,
 'Class 10 All-India Mock Test 2', 'Full Syllabus Assessment', 10, 30, 50.0, 0.25,
 'MEDIUM', 'NOT_ATTEMPTED', false, NULL, true, true),

-- 8. Previous Year Papers
('test_prev_2024', 'class_10', 'cat_previous', NULL, 'Science & Math', NULL, NULL,
 'Board Examination 2024 (Official)', 'Official Past Year Paper', 10, 30, 50.0, 0.25,
 'HARD', 'NOT_ATTEMPTED', false, NULL, true, true)
ON CONFLICT (id) DO UPDATE SET
class_id = EXCLUDED.class_id,
category_id = EXCLUDED.category_id,
subject_id = EXCLUDED.subject_id,
subject_name = EXCLUDED.subject_name,
chapter_id = EXCLUDED.chapter_id,
chapter_name = EXCLUDED.chapter_name,
title = EXCLUDED.title,
subtitle = EXCLUDED.subtitle,
question_count = EXCLUDED.question_count,
duration_minutes = EXCLUDED.duration_minutes,
total_marks = EXCLUDED.total_marks,
negative_marking = EXCLUDED.negative_marking,
difficulty = EXCLUDED.difficulty,
status = EXCLUDED.status,
is_daily = EXCLUDED.is_daily,
expiry_note = EXCLUDED.expiry_note,
is_published = EXCLUDED.is_published,
is_active = EXCLUDED.is_active;
"""
execute_sql(tests_sql)
print("Tests seeded.")

# 6. Questions
# CRITICAL: Daily Test has its OWN questions (only in test_daily_10).
# Each subject/chapter test has its OWN separate questions!

def escape_sql(s):
    if s is None:
        return "NULL"
    return "'" + s.replace("'", "''") + "'"

def json_sql(val):
    if val is None:
        return "NULL"
    s = json.dumps(val, ensure_ascii=False)
    return "'" + s.replace("'", "''") + "'::jsonb"

questions = []

# --- 1. DAILY TEST QUESTIONS (ONLY for test_daily_10) ---
daily_qs = [
    {
        "id": "q_daily_1",
        "test_id": "test_daily_10",
        "question_index": 0,
        "text": "What type of chemical reaction occurs when quicklime (calcium oxide) reacts vigorously with water?",
        "options": ["Combination reaction and exothermic", "Decomposition reaction and endothermic", "Displacement reaction", "Double displacement reaction"],
        "correct_index": 0,
        "explanation": "CaO + H2O -> Ca(OH)2 + Heat. Two substances combine to form a single product with release of heat, so it is an exothermic combination reaction.",
        "chapter_name": "Daily Challenge",
        "marks": 5.0,
        "text_hindi": "जब बिना बुझा चूना (कैल्शियम ऑक्साइड) जल के साथ तीव्रता से अभिक्रिया करता है, तो यह किस प्रकार की अभिक्रिया है?",
        "options_hindi": ["संयोजन अभिक्रिया तथा ऊष्माक्षेपी", "वियोजन अभिक्रिया तथा ऊष्माशोषी", "विस्थापन अभिक्रिया", "द्विविस्थापन अभिक्रिया"],
        "explanation_hindi": "CaO + H2O -> Ca(OH)2 + ऊष्मा। यह एकल उत्पाद बनाने वाली ऊष्माक्षेपी संयोजन अभिक्रिया है।"
    },
    {
        "id": "q_daily_2",
        "test_id": "test_daily_10",
        "question_index": 1,
        "text": "The xylem in plants are responsible for:",
        "options": ["Transport of water", "Transport of food", "Transport of amino acids", "Transport of oxygen"],
        "correct_index": 0,
        "explanation": "Xylem tissue transports water and dissolved minerals from roots to all parts of the plant.",
        "chapter_name": "Daily Challenge",
        "marks": 5.0,
        "text_hindi": "पादप में जाइलम किसके लिए उत्तरदायी है?",
        "options_hindi": ["जल का वहन", "भोजन का वहन", "अमीनो अम्ल का वहन", "ऑक्सीजन का वहन"],
        "explanation_hindi": "जाइलम ऊतक जड़ों से पौधों के विभिन्न भागों तक जल और खनिजों का परिवहन करता है।"
    },
    {
        "id": "q_daily_3",
        "test_id": "test_daily_10",
        "question_index": 2,
        "text": "If HCF(306, 657) = 9, what is LCM(306, 657)?",
        "options": ["22,338", "21,438", "23,338", "22,438"],
        "correct_index": 0,
        "explanation": "LCM = (Product of two numbers) / HCF = (306 * 657) / 9 = 34 * 657 = 22,338.",
        "chapter_name": "Daily Challenge",
        "marks": 5.0,
        "text_hindi": "यदि HCF(306, 657) = 9 है, तो LCM(306, 657) का मान क्या होगा?",
        "options_hindi": ["22,338", "21,438", "23,338", "22,438"],
        "explanation_hindi": "LCM = (a * b) / HCF = (306 * 657) / 9 = 22,338."
    },
    {
        "id": "q_daily_4",
        "test_id": "test_daily_10",
        "question_index": 3,
        "text": "An aqueous solution turns red litmus paper blue. Excess addition of which solution would reverse this change?",
        "options": ["Baking powder", "Lime", "Ammonium hydroxide solution", "Hydrochloric acid"],
        "correct_index": 3,
        "explanation": "The solution is basic because it turns red litmus blue. Adding excess acid (HCl) makes it acidic and turns blue litmus red.",
        "chapter_name": "Daily Challenge",
        "marks": 5.0,
        "text_hindi": "एक विलयन लाल लिटमस को नीला कर देता है। इसमें किस पदार्थ को आधिक्य में मिलाने पर यह परिवर्तन उलट जाएगा?",
        "options_hindi": ["बेकिंग पाउडर", "चूना", "अमोनियम हाइड्रॉक्साइड विलयन", "हाइड्रोक्लोरिक अम्ल"],
        "explanation_hindi": "विलयन क्षारीय है। अतिरिक्त हाइड्रोक्लोरिक अम्ल मिलाने पर यह अम्लीय हो जाएगा।"
    },
    {
        "id": "q_daily_5",
        "test_id": "test_daily_10",
        "question_index": 4,
        "text": "The breakdown of pyruvate to give carbon dioxide, water and energy takes place in:",
        "options": ["Cytoplasm", "Mitochondria", "Chloroplast", "Nucleus"],
        "correct_index": 1,
        "explanation": "Aerobic respiration breaks down pyruvate in the mitochondria of the cell to release CO2, H2O, and 36-38 ATP.",
        "chapter_name": "Daily Challenge",
        "marks": 5.0,
        "text_hindi": "पाइरुवेट के विखंडन से कार्बन डाइऑक्साइड, जल तथा ऊर्जा प्राप्त होने की क्रिया कहाँ होती है?",
        "options_hindi": ["कोशिकाद्रव्य", "माइटोकॉन्ड्रिया", "क्लोरोप्लास्ट", "केन्द्रक"],
        "explanation_hindi": "वायवीय श्वसन माइटोकॉन्ड्रिया में संपन्न होता है।"
    },
    {
        "id": "q_daily_6",
        "test_id": "test_daily_10",
        "question_index": 5,
        "text": "The zeroes of the quadratic polynomial x² - 2x - 8 are:",
        "options": ["4, -2", "-4, 2", "4, 2", "-4, -2"],
        "correct_index": 0,
        "explanation": "x² - 2x - 8 = (x - 4)(x + 2) = 0 => x = 4 or x = -2.",
        "chapter_name": "Daily Challenge",
        "marks": 5.0,
        "text_hindi": "द्विघात बहुपद x² - 2x - 8 के शून्यक हैं:",
        "options_hindi": ["4, -2", "-4, 2", "4, 2", "-4, -2"],
        "explanation_hindi": "x² - 4x + 2x - 8 = (x - 4)(x + 2) = 0 => x = 4, -2."
    },
    {
        "id": "q_daily_7",
        "test_id": "test_daily_10",
        "question_index": 6,
        "text": "Which of the following is a plant hormone that inhibits growth and causes wilting of leaves?",
        "options": ["Auxin", "Gibberellin", "Cytokinin", "Abscisic acid"],
        "correct_index": 3,
        "explanation": "Abscisic acid (ABA) is a plant stress hormone that inhibits growth and triggers leaf senescence and wilting.",
        "chapter_name": "Daily Challenge",
        "marks": 5.0,
        "text_hindi": "निम्नलिखित में से कौन-सा पादप हार्मोन वृद्धि को रोकता है तथा पत्तियों के मुरझाने के लिए उत्तरदायी है?",
        "options_hindi": ["ऑक्सिन", "जिबरेलिन", "साइटोकाइनिन", "एब्सिसिक अम्ल"],
        "explanation_hindi": "एब्सिसिक अम्ल (ABA) वृद्धि रोधक हार्मोन है जो पत्तियों को मुरझाने के लिए प्रेरित करता है।"
    },
    {
        "id": "q_daily_8",
        "test_id": "test_daily_10",
        "question_index": 7,
        "text": "A device that reverses the direction of flow of current through a circuit is called:",
        "options": ["Galvanometer", "Commutator (Split ring)", "Voltmeter", "Ammeter"],
        "correct_index": 1,
        "explanation": "A commutator or split ring reverses the current direction in an electric motor every half rotation.",
        "chapter_name": "Daily Challenge",
        "marks": 5.0,
        "text_hindi": "परिपथ में विद्युत धारा की दिशा को उत्क्रमित (उल्टा) करने वाली युक्ति कहलाती है:",
        "options_hindi": ["गैल्वेनोमीटर", "दिक्परिवर्तक (विभक्त वलय)", "वोल्टमीटर", "ऐमीटर"],
        "explanation_hindi": "विभक्त वलय या दिक्परिवर्तक धारा की दिशा को प्रत्येक आधे घूर्णन के बाद पलट देता है।"
    },
    {
        "id": "q_daily_9",
        "test_id": "test_daily_10",
        "question_index": 8,
        "text": "If discriminant D = b² - 4ac > 0 and is not a perfect square, the roots of ax² + bx + c = 0 are:",
        "options": ["Real, unequal and irrational", "Real, unequal and rational", "Real and equal", "Imaginary"],
        "correct_index": 0,
        "explanation": "When D > 0 and not a square, the square root of D remains a surd, yielding two distinct irrational roots.",
        "chapter_name": "Daily Challenge",
        "marks": 5.0,
        "text_hindi": "यदि विविक्तकर D = b² - 4ac > 0 और पूर्ण वर्ग नहीं है, तो समीकरण ax² + bx + c = 0 के मूल होंगे:",
        "options_hindi": ["वास्तविक, भिन्न तथा अपरिमेय", "वास्तविक, भिन्न तथा परिमेय", "वास्तविक तथा बराबर", "काल्पनिक"],
        "explanation_hindi": "जब D > 0 और पूर्ण वर्ग न हो तो मूल वास्तविक, असमान और अपरिमेय होते हैं।"
    },
    {
        "id": "q_daily_10",
        "test_id": "test_daily_10",
        "question_index": 9,
        "text": "Which gas is flushed into bags of potato chips to prevent rancidity due to oxidation?",
        "options": ["Oxygen", "Nitrogen", "Hydrogen", "Carbon dioxide"],
        "correct_index": 1,
        "explanation": "Nitrogen is an inert gas that displaces oxygen and prevents fatty oils in chips from becoming rancid.",
        "chapter_name": "Daily Challenge",
        "marks": 5.0,
        "text_hindi": "चिप्स की थैली में से ऑक्सीजन हटाकर कौन-सी गैस भरी जाती है ताकि विकृतगंधिता न हो?",
        "options_hindi": ["ऑक्सीजन", "नाइट्रोजन", "हाइड्रोजन", "कार्बन डाइऑक्साइड"],
        "explanation_hindi": "नाइट्रोजन एक अक्रिय गैस है जो तेल और वसा के उपचयन (ऑक्सीकरण) को रोकती है।"
    }
]
questions.extend(daily_qs)

# --- 2. SCIENCE CH 1 QUESTIONS (Chemical Reactions) ---
sci_ch1_qs = [
    {
        "id": "q_sci_ch1_1",
        "test_id": "test_sci_ch1_1",
        "question_index": 0,
        "text": "Which of the following is a displacement reaction?",
        "options": ["MgCO3 -> MgO + CO2", "2Na + 2H2O -> 2NaOH + H2", "H2 + Cl2 -> 2HCl", "2KClO3 -> 2KCl + 3O2"],
        "correct_index": 1,
        "explanation": "Sodium displaces hydrogen from water to form sodium hydroxide and hydrogen gas, so it is a single displacement reaction.",
        "chapter_name": "Chemical Reactions and Equations",
        "marks": 2.0,
        "text_hindi": "निम्नलिखित में से कौन-सी विस्थापन अभिक्रिया है?",
        "options_hindi": ["MgCO3 -> MgO + CO2", "2Na + 2H2O -> 2NaOH + H2", "H2 + Cl2 -> 2HCl", "2KClO3 -> 2KCl + 3O2"],
        "explanation_hindi": "सोडियम जल में से हाइड्रोजन को विस्थापित करता है, अत: यह विस्थापन अभिक्रिया है।"
    },
    {
        "id": "q_sci_ch1_2",
        "test_id": "test_sci_ch1_1",
        "question_index": 1,
        "text": "When lead nitrate powder is heated in a boiling tube, brown fumes of which gas are evolved?",
        "options": ["Nitrogen monoxide", "Nitrogen dioxide", "Dinitrogen oxide", "Nitrogen pentoxide"],
        "correct_index": 1,
        "explanation": "2Pb(NO3)2 -> 2PbO + 4NO2 + O2. NO2 (nitrogen dioxide) produces characteristic brown fumes.",
        "chapter_name": "Chemical Reactions and Equations",
        "marks": 2.0,
        "text_hindi": "लेड नाइट्रेट चूर्ण को गर्म करने पर भूरे रंग का धुआँ निकलता है, यह धुआँ किस गैस का है?",
        "options_hindi": ["नाइट्रोजन मोनोऑक्साइड", "नाइट्रोजन डाइऑक्साइड", "डाइनाइट्रोजन ऑक्साइड", "नाइट्रोजन पेंटाऑक्साइड"],
        "explanation_hindi": "लेड नाइट्रेट के तापीय अपघटन पर नाइट्रोजन डाइऑक्साइड (NO2) के भूरे धुएँ निकलते हैं।"
    },
    {
        "id": "q_sci_ch1_3",
        "test_id": "test_sci_ch1_1",
        "question_index": 2,
        "text": "In the reaction: CuO + H2 -> Cu + H2O, which substance is being oxidized?",
        "options": ["CuO", "H2", "Cu", "H2O"],
        "correct_index": 1,
        "explanation": "Hydrogen gains oxygen to become H2O, so H2 is oxidized. CuO loses oxygen and is reduced to Cu.",
        "chapter_name": "Chemical Reactions and Equations",
        "marks": 2.0,
        "text_hindi": "अभिक्रिया CuO + H2 -> Cu + H2O में किस पदार्थ का उपचयन (ऑक्सीकरण) हो रहा है?",
        "options_hindi": ["CuO", "H2", "Cu", "H2O"],
        "explanation_hindi": "हाइड्रोजन ऑक्सीजन ग्रहण करके H2O बनाता है, इसलिए H2 का उपचयन हो रहा है।"
    },
    {
        "id": "q_sci_ch1_4",
        "test_id": "test_sci_ch1_1",
        "question_index": 3,
        "text": "The chemical formula of marble (chalk / limestone) is:",
        "options": ["Ca(OH)2", "CaO", "CaCO3", "CaSO4"],
        "correct_index": 2,
        "explanation": "Marble, limestone, and eggshells are all chemically composed of Calcium Carbonate (CaCO3).",
        "chapter_name": "Chemical Reactions and Equations",
        "marks": 2.0,
        "text_hindi": "संगमरमर (चूना पत्थर) का रासायनिक सूत्र क्या है?",
        "options_hindi": ["Ca(OH)2", "CaO", "CaCO3", "CaSO4"],
        "explanation_hindi": "संगमरमर और चूना पत्थर का रासायनिक सूत्र CaCO3 (कैल्शियम कार्बोनेट) है।"
    },
    {
        "id": "q_sci_ch1_5",
        "test_id": "test_sci_ch1_1",
        "question_index": 4,
        "text": "What happens when dilute hydrochloric acid is added to iron filings?",
        "options": ["Hydrogen gas and iron(II) chloride are produced", "Chlorine gas and iron hydroxide are produced", "No reaction takes place", "Iron salt and water are produced"],
        "correct_index": 0,
        "explanation": "Fe + 2HCl -> FeCl2 + H2. Iron displaces hydrogen, releasing H2 gas and forming iron(II) chloride.",
        "chapter_name": "Chemical Reactions and Equations",
        "marks": 2.0,
        "text_hindi": "लौह चूर्ण पर तनु हाइड्रोक्लोरिक अम्ल डालने से क्या होता है?",
        "options_hindi": ["हाइड्रोजन गैस एवं आयरन क्लोराइड बनता है", "क्लोरीन गैस एवं आयरन हाइड्रॉक्साइड बनता है", "कोई अभिक्रिया नहीं होती", "आयरन लवण एवं जल बनता है"],
        "explanation_hindi": "Fe + 2HCl -> FeCl2 + H2 (हाइड्रोजन गैस और आयरन क्लोराइड)।"
    },
    {
        "id": "q_sci_ch1_6",
        "test_id": "test_sci_ch1_1",
        "question_index": 5,
        "text": "Respiration is considered which type of reaction?",
        "options": ["Endothermic reaction", "Exothermic reaction", "Decomposition reaction", "Reduction reaction"],
        "correct_index": 1,
        "explanation": "Glucose reacts with oxygen to release heat and ATP energy (C6H12O6 + 6O2 -> 6CO2 + 6H2O + Energy), making it exothermic.",
        "chapter_name": "Chemical Reactions and Equations",
        "marks": 2.0,
        "text_hindi": "श्वसन किस प्रकार की रासायनिक अभिक्रिया है?",
        "options_hindi": ["ऊष्माशोषी अभिक्रिया", "ऊष्माक्षेपी अभिक्रिया", "वियोजन अभिक्रिया", "अपचयन अभिक्रिया"],
        "explanation_hindi": "ग्लूकोज के ऑक्सीकरण से ऊर्जा मुक्त होती है, इसलिए श्वसन एक ऊष्माक्षेपी अभिक्रिया है।"
    },
    {
        "id": "q_sci_ch1_7",
        "test_id": "test_sci_ch1_1",
        "question_index": 6,
        "text": "When barium chloride solution is added to sodium sulphate solution, a white precipitate of which substance is formed?",
        "options": ["Sodium chloride", "Barium sulphate", "Barium sulphite", "Sodium sulphide"],
        "correct_index": 1,
        "explanation": "BaCl2 + Na2SO4 -> BaSO4 (white ppt) + 2NaCl. This is a double displacement precipitation reaction.",
        "chapter_name": "Chemical Reactions and Equations",
        "marks": 2.0,
        "text_hindi": "बेरियम क्लोराइड के जलीय विलयन में सोडियम सल्फेट मिलाने पर किस श्वेत अवक्षेप का निर्माण होता है?",
        "options_hindi": ["सोडियम क्लोराइड", "बेरियम सल्फेट", "बेरियम सल्फाइट", "सोडियम सल्फाइड"],
        "explanation_hindi": "BaSO4 (बेरियम सल्फेट) का सफेद अवक्षेप बनता है।"
    },
    {
        "id": "q_sci_ch1_8",
        "test_id": "test_sci_ch1_1",
        "question_index": 7,
        "text": "Corrosion of copper in moist air produces a protective green coating of:",
        "options": ["CuSO4", "Basic copper carbonate [CuCO3.Cu(OH)2]", "CuO", "CuCl2"],
        "correct_index": 1,
        "explanation": "Copper reacts with moist CO2 in air to form green basic copper carbonate.",
        "chapter_name": "Chemical Reactions and Equations",
        "marks": 2.0,
        "text_hindi": "तांबे के बर्तन पर नम वायु में किस पदार्थ की हरी परत चढ़ जाती है?",
        "options_hindi": ["कॉपर सल्फेट", "क्षारीय कॉपर कार्बोनेट", "कॉपर ऑक्साइड", "कॉपर क्लोराइड"],
        "explanation_hindi": "नम वायु की CO2 से अभिक्रिया करके क्षारीय कॉपर कार्बोनेट [CuCO3.Cu(OH)2] की हरी परत बनती है।"
    },
    {
        "id": "q_sci_ch1_9",
        "test_id": "test_sci_ch1_1",
        "question_index": 8,
        "text": "The law of conservation of mass states that in a chemical reaction:",
        "options": ["Mass can be created", "Mass can be destroyed", "Total mass of reactants equals total mass of products", "Mass fluctuates randomly"],
        "correct_index": 2,
        "explanation": "Mass is neither created nor destroyed in a chemical reaction; hence equations must be balanced.",
        "chapter_name": "Chemical Reactions and Equations",
        "marks": 2.0,
        "text_hindi": "द्रव्यमान संरक्षण का नियम कहता है कि किसी रासायनिक अभिक्रिया में:",
        "options_hindi": ["द्रव्यमान का सृजन हो सकता है", "द्रव्यमान का विनाश हो सकता है", "अभिकारकों का कुल द्रव्यमान उत्पादों के कुल द्रव्यमान के बराबर होता है", "द्रव्यमान घटता-बढ़ता रहता है"],
        "explanation_hindi": "अभिक्रिया में द्रव्यमान का न तो निर्माण होता है न विनाश; कुल द्रव्यमान संरक्षित रहता है।"
    },
    {
        "id": "q_sci_ch1_10",
        "test_id": "test_sci_ch1_1",
        "question_index": 9,
        "text": "Which metal coating is applied to iron to prevent rusting in the galvanization process?",
        "options": ["Tin", "Zinc", "Chromium", "Nickel"],
        "correct_index": 1,
        "explanation": "Galvanization is the application of a thin layer of metallic zinc over steel or iron to prevent rusting.",
        "chapter_name": "Chemical Reactions and Equations",
        "marks": 2.0,
        "text_hindi": "लोहे को जंग से बचाने के लिए यशदलेपन (गैल्वनीकरण) में किस धातु की परत चढ़ाई जाती है?",
        "options_hindi": ["टिन", "जिंक (जस्ता)", "क्रोमियम", "निकेल"],
        "explanation_hindi": "गैल्वनीकरण में लोहे पर जिंक (जस्ता) की पतली परत चढ़ाई जाती है।"
    }
]
questions.extend(sci_ch1_qs)

# --- 3. SCIENCE CH 2 QUESTIONS (Acids, Bases and Salts) ---
sci_ch2_qs = [
    {
        "id": "q_sci_ch2_1",
        "test_id": "test_sci_ch2_1",
        "question_index": 0,
        "text": "What is the chemical formula of bleaching powder?",
        "options": ["CaO", "Ca(OH)2", "CaOCl2", "CaCl2"],
        "correct_index": 2,
        "explanation": "Bleaching powder is Calcium oxychloride, with chemical formula CaOCl2.",
        "chapter_name": "Acids, Bases and Salts",
        "marks": 2.0,
        "text_hindi": "विरंजक चूर्ण (ब्लीचिंग पाउडर) का रासायनिक सूत्र क्या है?",
        "options_hindi": ["CaO", "Ca(OH)2", "CaOCl2", "CaCl2"],
        "explanation_hindi": "विरंजक चूर्ण का रासायनिक सूत्र CaOCl2 (कैल्शियम ऑक्सीक्लोराइड) है।"
    },
    {
        "id": "q_sci_ch2_2",
        "test_id": "test_sci_ch2_1",
        "question_index": 1,
        "text": "Tooth enamel contains calcium hydroxyapatite. Tooth decay starts when the pH of mouth is below:",
        "options": ["7.0", "6.5", "5.5", "4.0"],
        "correct_index": 2,
        "explanation": "Bacteria produce acid from leftover food particles, dropping mouth pH below 5.5 and corroding enamel.",
        "chapter_name": "Acids, Bases and Salts",
        "marks": 2.0,
        "text_hindi": "मुँह का pH मान कितने से कम होने पर दाँतों का क्षय (सड़ना) प्रारंभ हो जाता है?",
        "options_hindi": ["7.0", "6.5", "5.5", "4.0"],
        "explanation_hindi": "मुँह का pH मान 5.5 से कम होने पर दाँतों का इनैमल संक्षारित होने लगता है।"
    },
    {
        "id": "q_sci_ch2_3",
        "test_id": "test_sci_ch2_1",
        "question_index": 2,
        "text": "What is the chemical formula of Plaster of Paris?",
        "options": ["CaSO4.2H2O", "CaSO4.½H2O", "CaSO4.H2O", "2CaSO4.H2O"],
        "correct_index": 1,
        "explanation": "Plaster of Paris is calcium sulphate hemihydrate: CaSO4.½H2O.",
        "chapter_name": "Acids, Bases and Salts",
        "marks": 2.0,
        "text_hindi": "प्लास्टर ऑफ पेरिस का रासायनिक सूत्र क्या है?",
        "options_hindi": ["CaSO4.2H2O", "CaSO4.½H2O", "CaSO4.H2O", "2CaSO4.H2O"],
        "explanation_hindi": "प्लास्टर ऑफ पेरिस कैल्शियम सल्फेट हेमीहाइड्रेट (CaSO4.½H2O) होता है।"
    },
    {
        "id": "q_sci_ch2_4",
        "test_id": "test_sci_ch2_1",
        "question_index": 3,
        "text": "Which acid is present in ant sting that causes burning pain?",
        "options": ["Acetic acid", "Methanoic (Formic) acid", "Citric acid", "Oxalic acid"],
        "correct_index": 1,
        "explanation": "Ant stings contain methanoic acid (HCOOH). Applying mild baking soda relieves the sting.",
        "chapter_name": "Acids, Bases and Salts",
        "marks": 2.0,
        "text_hindi": "चींटी के डंक में कौन-सा अम्ल पाया जाता है जो जलन पैदा करता है?",
        "options_hindi": ["एसिटिक अम्ल", "मेथेनॉइक (फॉर्मिक) अम्ल", "साइट्रिक अम्ल", "ऑक्सैलिक अम्ल"],
        "explanation_hindi": "चींटी के डंक में मेथेनॉइक अम्ल (HCOOH) होता है।"
    },
    {
        "id": "q_sci_ch2_5",
        "test_id": "test_sci_ch2_1",
        "question_index": 4,
        "text": "The pH of a neutral solution at 25°C is:",
        "options": ["0", "7", "14", "1"],
        "correct_index": 1,
        "explanation": "Pure water and neutral solutions have pH = 7 at room temperature.",
        "chapter_name": "Acids, Bases and Salts",
        "marks": 2.0,
        "text_hindi": "उदासीन विलयन का pH मान कितना होता है?",
        "options_hindi": ["0", "7", "14", "1"],
        "explanation_hindi": "शुद्ध जल और उदासीन विलयन का pH मान 7 होता है।"
    },
    {
        "id": "q_sci_ch2_6",
        "test_id": "test_sci_ch2_1",
        "question_index": 5,
        "text": "What is the chemical name and formula of Baking Soda?",
        "options": ["Sodium carbonate (Na2CO3)", "Sodium hydrogen carbonate (NaHCO3)", "Sodium hydroxide (NaOH)", "Calcium carbonate (CaCO3)"],
        "correct_index": 1,
        "explanation": "Baking soda is sodium hydrogen carbonate (sodium bicarbonate), NaHCO3.",
        "chapter_name": "Acids, Bases and Salts",
        "marks": 2.0,
        "text_hindi": "बेकिंग सोडा (खाने का सोडा) का रासायनिक नाम और सूत्र क्या है?",
        "options_hindi": ["सोडियम कार्बोनेट (Na2CO3)", "सोडियम हाइड्रोजन कार्बोनेट (NaHCO3)", "सोडियम हाइड्रॉक्साइड (NaOH)", "कैल्शियम कार्बोनेट (CaCO3)"],
        "explanation_hindi": "बेकिंग सोडा सोडियम हाइड्रोजन कार्बोनेट (NaHCO3) होता है।"
    },
    {
        "id": "q_sci_ch2_7",
        "test_id": "test_sci_ch2_1",
        "question_index": 6,
        "text": "Which gas is evolved when an acid reacts with a metal carbonate?",
        "options": ["Hydrogen gas", "Oxygen gas", "Carbon dioxide gas", "Nitrogen gas"],
        "correct_index": 2,
        "explanation": "Metal carbonate + Acid -> Salt + Water + Carbon dioxide (CO2).",
        "chapter_name": "Acids, Bases and Salts",
        "marks": 2.0,
        "text_hindi": "जब अम्ल धातु कार्बोनेट के साथ अभिक्रिया करता है, तो कौन-सी गैस उत्सर्जित होती है?",
        "options_hindi": ["हाइड्रोजन गैस", "ऑक्सीजन गैस", "कार्बन डाइऑक्साइड गैस", "नाइट्रोजन गैस"],
        "explanation_hindi": "धातु कार्बोनेट + अम्ल -> लवण + जल + CO2।"
    },
    {
        "id": "q_sci_ch2_8",
        "test_id": "test_sci_ch2_1",
        "question_index": 7,
        "text": "What is the number of water molecules of crystallization in washing soda (Na2CO3.10H2O)?",
        "options": ["2", "5", "7", "10"],
        "correct_index": 3,
        "explanation": "Washing soda is sodium carbonate decahydrate, containing 10 water molecules of crystallization.",
        "chapter_name": "Acids, Bases and Salts",
        "marks": 2.0,
        "text_hindi": "धावन सोडा (Na2CO3.10H2O) में क्रिस्टलन के जल के कितने अणु होते हैं?",
        "options_hindi": ["2", "5", "7", "10"],
        "explanation_hindi": "धावन सोडा में क्रिस्टलन के जल के 10 अणु होते हैं।"
    },
    {
        "id": "q_sci_ch2_9",
        "test_id": "test_sci_ch2_1",
        "question_index": 8,
        "text": "A milkman adds a very small amount of baking soda to fresh milk to:",
        "options": ["Make it taste sweet", "Prevent it from turning sour by making it slightly alkaline", "Increase the cream content", "Decrease the boiling point"],
        "correct_index": 1,
        "explanation": "Baking soda shifts the milk pH slightly above 7, preventing lactic acid formation from quickly souring the milk.",
        "chapter_name": "Acids, Bases and Salts",
        "marks": 2.0,
        "text_hindi": "ताजे दूध में थोड़ा बेकिंग सोडा क्यों मिलाया जाता है?",
        "options_hindi": ["स्वाद मीठा करने के लिए", "दूध को थोड़ा क्षारीय बनाकर जल्दी खट्टा होने से रोकने के लिए", "मलाई बढ़ाने के लिए", "क्वथनांक कम करने के लिए"],
        "explanation_hindi": "बेकिंग सोडा दूध को थोड़ा क्षारीय बना देता है जिससे लैक्टिक अम्ल बनने में अधिक समय लगता है।"
    },
    {
        "id": "q_sci_ch2_10",
        "test_id": "test_sci_ch2_1",
        "question_index": 9,
        "text": "Phenolphthalein indicator turns which color in an alkaline solution?",
        "options": ["Colorless", "Pink", "Yellow", "Blue"],
        "correct_index": 1,
        "explanation": "Phenolphthalein is colorless in acidic solutions and turns bright pink in basic/alkaline solutions.",
        "chapter_name": "Acids, Bases and Salts",
        "marks": 2.0,
        "text_hindi": "फिनॉल्फथलीन सूचक क्षारीय विलयन में कैसा रंग देता है?",
        "options_hindi": ["रंगहीन", "गुलाबी", "पीला", "नीला"],
        "explanation_hindi": "फिनॉल्फथलीन अम्ल में रंगहीन और क्षार में गुलाबी रंग देता है।"
    }
]
questions.extend(sci_ch2_qs)

# --- 4. MATH CH 1 QUESTIONS (Real Numbers) ---
math_ch1_qs = [
    {
        "id": "q_math_ch1_1",
        "test_id": "test_math_ch1_1",
        "question_index": 0,
        "text": "According to the Fundamental Theorem of Arithmetic, every composite number can be expressed uniquely as a product of:",
        "options": ["Odd numbers", "Even numbers", "Prime numbers", "Co-prime numbers"],
        "correct_index": 2,
        "explanation": "Every composite number can be factored uniquely as a product of primes, apart from the order of factors.",
        "chapter_name": "Real Numbers",
        "marks": 2.0,
        "text_hindi": "अंकगणित की आधारभूत प्रमेय के अनुसार प्रत्येक भाज्य संख्या को किसके गुणनफल के रूप में व्यक्त किया जा सकता है?",
        "options_hindi": ["विषम संख्याओं", "सम संख्याओं", "अभाज्य संख्याओं", "सह-अभाज्य संख्याओं"],
        "explanation_hindi": "प्रत्येक भाज्य संख्या को अभाज्य संख्याओं के गुणनफल के रूप में अद्वितीय रूप से लिखा जा सकता है।"
    },
    {
        "id": "q_math_ch1_2",
        "test_id": "test_math_ch1_1",
        "question_index": 1,
        "text": "The decimal expansion of 23 / (2³ × 5²) will terminate after how many decimal places?",
        "options": ["1", "2", "3", "4"],
        "correct_index": 2,
        "explanation": "The maximum power of 2 or 5 in the denominator is max(3, 2) = 3. Therefore it terminates after 3 places.",
        "chapter_name": "Real Numbers",
        "marks": 2.0,
        "text_hindi": "परिमेय संख्या 23 / (2³ × 5²) का दशमलव प्रसार कितने स्थानों के बाद शांत होगा?",
        "options_hindi": ["1", "2", "3", "4"],
        "explanation_hindi": "हर में 2 की अधिकतम घात 3 है, इसलिए दशमलव प्रसार 3 स्थानों के बाद समाप्त होगा।"
    },
    {
        "id": "q_math_ch1_3",
        "test_id": "test_math_ch1_1",
        "question_index": 2,
        "text": "Which of the following is an irrational number?",
        "options": ["√4", "√9", "√5", "0.375"],
        "correct_index": 2,
        "explanation": "√5 cannot be expressed as p/q where p, q are integers and q != 0; its decimal is non-terminating and non-repeating.",
        "chapter_name": "Real Numbers",
        "marks": 2.0,
        "text_hindi": "निम्नलिखित में से कौन-सी अपरिमेय संख्या है?",
        "options_hindi": ["√4", "√9", "√5", "0.375"],
        "explanation_hindi": "√5 एक अपरिमेय संख्या है क्योंकि इसे p/q के रूप में व्यक्त नहीं किया जा सकता।"
    },
    {
        "id": "q_math_ch1_4",
        "test_id": "test_math_ch1_1",
        "question_index": 3,
        "text": "If two positive numbers a and b have HCF(a, b) = 12 and a × b = 1800, what is LCM(a, b)?",
        "options": ["150", "120", "180", "160"],
        "correct_index": 0,
        "explanation": "LCM(a, b) = (a × b) / HCF(a, b) = 1800 / 12 = 150.",
        "chapter_name": "Real Numbers",
        "marks": 2.0,
        "text_hindi": "यदि दो संख्याओं a और b का HCF 12 है और a × b = 1800 है, तो उनका LCM क्या होगा?",
        "options_hindi": ["150", "120", "180", "160"],
        "explanation_hindi": "LCM = (a × b) / HCF = 1800 / 12 = 150."
    },
    {
        "id": "q_math_ch1_5",
        "test_id": "test_math_ch1_1",
        "question_index": 4,
        "text": "The HCF of two consecutive even integers is always:",
        "options": ["1", "2", "4", "0"],
        "correct_index": 1,
        "explanation": "Two consecutive even integers can be written as 2n and 2n+2. HCF(2n, 2n+2) = HCF(2n, 2) = 2.",
        "chapter_name": "Real Numbers",
        "marks": 2.0,
        "text_hindi": "दो क्रमागत सम संख्याओं का महत्तम समापवर्तक (HCF) सदैव होता है:",
        "options_hindi": ["1", "2", "4", "0"],
        "explanation_hindi": "दो क्रमागत सम संख्याएँ 2n और 2n+2 होती हैं, जिनका HCF हमेशा 2 होता है।"
    },
    {
        "id": "q_math_ch1_6",
        "test_id": "test_math_ch1_1",
        "question_index": 5,
        "text": "What is the HCF of the smallest prime number and smallest composite number?",
        "options": ["1", "2", "4", "6"],
        "correct_index": 1,
        "explanation": "Smallest prime is 2; smallest composite is 4. HCF(2, 4) = 2.",
        "chapter_name": "Real Numbers",
        "marks": 2.0,
        "text_hindi": "सबसे छोटी अभाज्य संख्या और सबसे छोटी भाज्य संख्या का HCF कितना है?",
        "options_hindi": ["1", "2", "4", "6"],
        "explanation_hindi": "सबसे छोटी अभाज्य संख्या 2 है और सबसे छोटी भाज्य संख्या 4 है। HCF(2, 4) = 2।"
    },
    {
        "id": "q_math_ch1_7",
        "test_id": "test_math_ch1_1",
        "question_index": 6,
        "text": "If p is a prime number, then √p is always:",
        "options": ["Rational", "Irrational", "An integer", "Natural number"],
        "correct_index": 1,
        "explanation": "The square root of any prime number is always irrational.",
        "chapter_name": "Real Numbers",
        "marks": 2.0,
        "text_hindi": "यदि p एक अभाज्य संख्या है, तो √p सदैव क्या होगी?",
        "options_hindi": ["परिमेय", "अपरिमेय", "पूर्णांक", "प्राकृत संख्या"],
        "explanation_hindi": "किसी भी अभाज्य संख्या का वर्गमूल सदैव एक अपरिमेय संख्या होता है।"
    },
    {
        "id": "q_math_ch1_8",
        "test_id": "test_math_ch1_1",
        "question_index": 7,
        "text": "The sum or difference of a rational number and an irrational number is always:",
        "options": ["Rational", "Irrational", "Zero", "Integer"],
        "correct_index": 1,
        "explanation": "A rational number plus or minus an irrational number always results in an irrational number.",
        "chapter_name": "Real Numbers",
        "marks": 2.0,
        "text_hindi": "एक परिमेय संख्या और एक अपरिमेय संख्या का जोड़ या घटाव सदैव क्या होता है?",
        "options_hindi": ["परिमेय", "अपरिमेय", "शून्य", "पूर्णांक"],
        "explanation_hindi": "परिमेय और अपरिमेय संख्या का योग या अंतर सदैव अपरिमेय होता है।"
    },
    {
        "id": "q_math_ch1_9",
        "test_id": "test_math_ch1_1",
        "question_index": 8,
        "text": "Express 140 as a product of its prime factors:",
        "options": ["2² × 5 × 7", "2 × 5² × 7", "2³ × 5 × 7", "2² × 3 × 5"],
        "correct_index": 0,
        "explanation": "140 = 2 × 70 = 2 × 2 × 35 = 2² × 5 × 7.",
        "chapter_name": "Real Numbers",
        "marks": 2.0,
        "text_hindi": "140 को अभाज्य गुणनखंडों के गुणनफल के रूप में व्यक्त कीजिए:",
        "options_hindi": ["2² × 5 × 7", "2 × 5² × 7", "2³ × 5 × 7", "2² × 3 × 5"],
        "explanation_hindi": "140 = 2 × 2 × 5 × 7 = 2² × 5 × 7।"
    },
    {
        "id": "q_math_ch1_10",
        "test_id": "test_math_ch1_1",
        "question_index": 9,
        "text": "For any positive integer n, 6^n can never end with the digit:",
        "options": ["6", "0", "Both 0 and 5", "5"],
        "correct_index": 1,
        "explanation": "For a number to end with 0, its prime factorization must contain both 2 and 5. 6^n = (2 × 3)^n contains no factor 5, so it can never end with 0.",
        "chapter_name": "Real Numbers",
        "marks": 2.0,
        "text_hindi": "किसी प्राकृत संख्या n के लिए, 6^n अंक किस पर कभी समाप्त नहीं हो सकता?",
        "options_hindi": ["6", "0", "0 और 5 दोनों", "5"],
        "explanation_hindi": "6^n = (2 × 3)^n में अभाज्य गुणनखंड 5 नहीं है, अत: यह कभी भी अंक 0 पर समाप्त नहीं हो सकता।"
    }
]
questions.extend(math_ch1_qs)

# --- 5. MOCK TEST 1 QUESTIONS ---
mock_qs = [
    {
        "id": "q_mock_1",
        "test_id": "test_mock_1",
        "question_index": 0,
        "text": "The focal length of a concave mirror having radius of curvature 30 cm is:",
        "options": ["-15 cm", "+15 cm", "-30 cm", "+60 cm"],
        "correct_index": 0,
        "explanation": "f = R / 2. By Cartesian sign convention, concave mirror has negative focal length: f = -30/2 = -15 cm.",
        "chapter_name": "Light - Reflection and Refraction",
        "marks": 5.0,
        "text_hindi": "30 सेमी वक्रता त्रिज्या वाले अवतल दर्पण की फोकस दूरी होगी:",
        "options_hindi": ["-15 सेमी", "+15 सेमी", "-30 सेमी", "+60 सेमी"],
        "explanation_hindi": "अवतल दर्पण की फोकस दूरी f = R/2 = -30/2 = -15 सेमी होती है।"
    },
    {
        "id": "q_mock_2",
        "test_id": "test_mock_1",
        "question_index": 1,
        "text": "Which of the following equations represents a pair of parallel lines?",
        "options": ["2x + 3y = 7 and 4x + 6y = 15", "x + y = 2 and 2x + 2y = 4", "x - y = 0 and x + y = 0", "3x + 2y = 5 and 2x + 3y = 5"],
        "correct_index": 0,
        "explanation": "Parallel lines satisfy a1/a2 = b1/b2 != c1/c2. Here 2/4 = 3/6 = 1/2 != 7/15.",
        "chapter_name": "Pair of Linear Equations",
        "marks": 5.0,
        "text_hindi": "निम्नलिखित में से कौन-सा समीकरण युग्म समांतर रेखाओं को निरूपित करता है?",
        "options_hindi": ["2x + 3y = 7 और 4x + 6y = 15", "x + y = 2 और 2x + 2y = 4", "x - y = 0 और x + y = 0", "3x + 2y = 5 और 2x + 3y = 5"],
        "explanation_hindi": "समांतर रेखाओं के लिए a1/a2 = b1/b2 != c1/c2 होता है। 2/4 = 3/6 != 7/15।"
    },
    {
        "id": "q_mock_3",
        "test_id": "test_mock_1",
        "question_index": 2,
        "text": "In a pea plant, a pure tall plant (TT) is crossed with a short plant (tt). The ratio of pure tall to hybrid tall plants in the F2 generation is:",
        "options": ["1 : 2", "3 : 1", "1 : 1", "2 : 1"],
        "correct_index": 0,
        "explanation": "In F2: TT : Tt : tt = 1 : 2 : 1. The ratio of pure tall (TT = 1) to hybrid tall (Tt = 2) is 1 : 2.",
        "chapter_name": "Heredity",
        "marks": 5.0,
        "text_hindi": "शुद्ध लंबे (TT) और बौने (tt) मटर के पौधे के संकरण में F2 पीढ़ी में शुद्ध लंबे और संकर लंबे पौधों का अनुपात क्या होगा?",
        "options_hindi": ["1 : 2", "3 : 1", "1 : 1", "2 : 1"],
        "explanation_hindi": "F2 पीढ़ी का जीनोटाइप TT : Tt : tt = 1 : 2 : 1 होता है, अत: शुद्ध लंबे : संकर लंबे = 1 : 2।"
    },
    {
        "id": "q_mock_4",
        "test_id": "test_mock_1",
        "question_index": 3,
        "text": "What is the 10th term of the Arithmetic Progression 2, 7, 12, ...?",
        "options": ["47", "42", "52", "37"],
        "correct_index": 0,
        "explanation": "a = 2, d = 5. a_10 = a + 9d = 2 + 9(5) = 2 + 45 = 47.",
        "chapter_name": "Arithmetic Progressions",
        "marks": 5.0,
        "text_hindi": "समांतर श्रेढ़ी 2, 7, 12, ... का 10वाँ पद क्या है?",
        "options_hindi": ["47", "42", "52", "37"],
        "explanation_hindi": "a_10 = a + (10 - 1)d = 2 + 9 × 5 = 47।"
    },
    {
        "id": "q_mock_5",
        "test_id": "test_mock_1",
        "question_index": 4,
        "text": "The SI unit of electric resistivity is:",
        "options": ["Ohm (Ω)", "Ohm-meter (Ω·m)", "Volt", "Ampere"],
        "correct_index": 1,
        "explanation": "Resistivity ρ = R × (A / L) = Ω × (m² / m) = Ω·m (Ohm-meter).",
        "chapter_name": "Electricity",
        "marks": 5.0,
        "text_hindi": "विद्युत प्रतिरोधकता का SI मात्रक क्या है?",
        "options_hindi": ["ओम (Ω)", "ओम-मीटर (Ω·m)", "वोल्ट", "ऐम्पियर"],
        "explanation_hindi": "प्रतिरोधकता का SI मात्रक ओम-मीटर (Ω·m) होता है।"
    }
]
questions.extend(mock_qs)

# --- 6. PREVIOUS YEAR 2024 PAPER QUESTIONS ---
prev_qs = [
    {
        "id": "q_prev24_1",
        "test_id": "test_prev_2024",
        "question_index": 0,
        "text": "Which of the following phenomena of light is responsible for the twinkling of stars?",
        "options": ["Atmospheric reflection", "Atmospheric refraction", "Internal reflection", "Dispersion"],
        "correct_index": 1,
        "explanation": "Light from stars passes through atmospheric layers of varying optical densities, causing continuous refraction and twinkling.",
        "chapter_name": "Human Eye and the Colourful World",
        "marks": 5.0,
        "text_hindi": "तारों का टिमटिमाना प्रकाश की किस परिघटना का परिणाम है?",
        "options_hindi": ["वायुमंडलीय परावर्तन", "वायुमंडलीय अपवर्तन", "आंतरिक परावर्तन", "वर्ण विक्षेपण"],
        "explanation_hindi": "वायुमंडल की बदलती घनत्व वाली परतों के कारण तारों का प्रकाश अपवर्तित होता है, जिससे तारे टिमटिमाते हैं।"
    },
    {
        "id": "q_prev24_2",
        "test_id": "test_prev_2024",
        "question_index": 1,
        "text": "If sin θ + cos θ = √2 cos θ, then the value of tan θ is:",
        "options": ["√2 - 1", "√2 + 1", "1 / √2", "√3"],
        "correct_index": 0,
        "explanation": "sin θ = √2 cos θ - cos θ = (√2 - 1) cos θ => sin θ / cos θ = tan θ = √2 - 1.",
        "chapter_name": "Introduction to Trigonometry",
        "marks": 5.0,
        "text_hindi": "यदि sin θ + cos θ = √2 cos θ है, तो tan θ का मान क्या होगा?",
        "options_hindi": ["√2 - 1", "√2 + 1", "1 / √2", "√3"],
        "explanation_hindi": "sin θ = (√2 - 1) cos θ => tan θ = √2 - 1."
    },
    {
        "id": "q_prev24_3",
        "test_id": "test_prev_2024",
        "question_index": 2,
        "text": "Which part of the human brain controls involuntary actions like blood pressure, salivation and vomiting?",
        "options": ["Cerebrum", "Cerebellum", "Medulla oblongata", "Pons"],
        "correct_index": 2,
        "explanation": "The medulla in the hindbrain controls involuntary reflexes including blood pressure, salivation, and vomiting.",
        "chapter_name": "Control and Coordination",
        "marks": 5.0,
        "text_hindi": "मानव मस्तिष्क का कौन-सा भाग रक्तचाप, लार आना तथा वमन (उल्टी) जैसी अनैच्छिक क्रियाओं को नियंत्रित करता है?",
        "options_hindi": ["प्रमस्तिष्क", "अनुमस्तिष्क", "मेडुला ऑबलोंगाटा", "पॉन्स"],
        "explanation_hindi": "पश्च मस्तिष्क में स्थित मेडुला अनैच्छिक क्रियाओं का केंद्र होता है।"
    },
    {
        "id": "q_prev24_4",
        "test_id": "test_prev_2024",
        "question_index": 3,
        "text": "The distance of point P(-6, 8) from the origin is:",
        "options": ["8", "2√7", "10", "6"],
        "correct_index": 2,
        "explanation": "Distance = √((-6)² + 8²) = √(36 + 64) = √100 = 10 units.",
        "chapter_name": "Coordinate Geometry",
        "marks": 5.0,
        "text_hindi": "बिंदु P(-6, 8) की मूल बिंदु से दूरी है:",
        "options_hindi": ["8", "2√7", "10", "6"],
        "explanation_hindi": "मूल बिंदु से दूरी = √((-6)² + 8²) = √(36 + 64) = 10।"
    },
    {
        "id": "q_prev24_5",
        "test_id": "test_prev_2024",
        "question_index": 4,
        "text": "Which functional group is present in butanone (C4H8O)?",
        "options": ["Alcohol", "Aldehyde", "Ketone", "Carboxylic acid"],
        "correct_index": 2,
        "explanation": "Butanone contains the carbonyl group (>C=O) bonded to two carbon atoms, characteristic of ketones.",
        "chapter_name": "Carbon and its Compounds",
        "marks": 5.0,
        "text_hindi": "ब्यूटेनोन (C4H8O) में कौन-सा प्रकार्यात्मक समूह उपस्थित होता है?",
        "options_hindi": ["ऐल्कोहॉल", "ऐल्डिहाइड", "कीटोन", "कार्बोक्सिलिक अम्ल"],
        "explanation_hindi": "ब्यूटेनोन में कीटोन (>C=O) समूह उपस्थित होता है।"
    }
]
questions.extend(prev_qs)

# Insert questions in batches
print(f"Inserting {len(questions)} unique questions...")
for q in questions:
    q_sql = f"""
    INSERT INTO public.questions (
        id, test_id, question_index, text, options, correct_index, explanation,
        chapter_name, marks, text_hindi, options_hindi, explanation_hindi
    ) VALUES (
        {escape_sql(q['id'])},
        {escape_sql(q['test_id'])},
        {q['question_index']},
        {escape_sql(q['text'])},
        {json_sql(q['options'])},
        {q['correct_index']},
        {escape_sql(q['explanation'])},
        {escape_sql(q['chapter_name'])},
        {q['marks']},
        {escape_sql(q.get('text_hindi'))},
        {json_sql(q.get('options_hindi'))},
        {escape_sql(q.get('explanation_hindi'))}
    ) ON CONFLICT (id) DO UPDATE SET
        test_id = EXCLUDED.test_id,
        question_index = EXCLUDED.question_index,
        text = EXCLUDED.text,
        options = EXCLUDED.options,
        correct_index = EXCLUDED.correct_index,
        explanation = EXCLUDED.explanation,
        chapter_name = EXCLUDED.chapter_name,
        marks = EXCLUDED.marks,
        text_hindi = EXCLUDED.text_hindi,
        options_hindi = EXCLUDED.options_hindi,
        explanation_hindi = EXCLUDED.explanation_hindi;
    """
    execute_sql(q_sql)

print(f"Successfully inserted all {len(questions)} questions.")

# 7. Seed Leaderboard with active live entries for today
leaderboard_peers = [
    ("std_1", "Aarav Sharma", "Class 10", 49.0, 50.0, 480, 1),
    ("std_2", "Diya Patel", "Class 10", 48.0, 50.0, 495, 1),
    ("std_3", "Priya Verma", "Class 10", 47.5, 50.0, 502, 1),
    ("std_4", "Kavya Singh", "Class 10", 46.5, 50.0, 510, 1),
    ("std_5", "Ananya Gupta", "Class 10", 46.0, 50.0, 520, 1),
    ("std_6", "Vikram Malhotra", "Class 10", 45.5, 50.0, 535, 1),
    ("std_7", "Rahul Mehra", "Class 10", 45.0, 50.0, 540, 1),
    ("std_8", "Neha Reddy", "Class 10", 44.5, 50.0, 550, 1),
    ("std_9", "Aditya Joshi", "Class 10", 44.0, 50.0, 560, 1),
    ("std_10", "Sneha Nair", "Class 10", 43.5, 50.0, 570, 1),
    ("std_11", "Ayush Tiwari", "Class 10", 43.0, 50.0, 580, 1),
    ("std_12", "Tanvi Sengupta", "Class 10", 42.5, 50.0, 590, 1),
    ("std_13", "Manish Kumar", "Class 10", 42.0, 50.0, 600, 1),
    ("std_14", "Deepak Yadav", "Class 10", 41.5, 50.0, 615, 1),
    ("std_15", "Ishita Bose", "Class 10", 41.0, 50.0, 620, 1),
    ("std_16", "Siddharth Jain", "Class 10", 40.5, 50.0, 630, 1),
    ("std_17", "Meera Pillai", "Class 10", 40.0, 50.0, 640, 1),
    ("std_18", "Rajesh Pandey", "Class 10", 39.5, 50.0, 650, 1),
    ("std_19", "Divya Rathore", "Class 10", 39.0, 50.0, 660, 1),
    ("std_20", "Gaurav Bhatt", "Class 10", 38.5, 50.0, 670, 1),
]

print("Seeding leaderboard...")
for peer in leaderboard_peers:
    lb_sql = f"""
    INSERT INTO public.leaderboard (
        student_id, student_name, class_name, date_key, score, total_marks,
        completion_time_seconds, attempts_count, submitted_at
    ) VALUES (
        {escape_sql(peer[0])},
        {escape_sql(peer[1])},
        {escape_sql(peer[2])},
        {escape_sql(today_key)},
        {peer[3]},
        {peer[4]},
        {peer[5]},
        {peer[6]},
        {int(datetime.now().timestamp() * 1000)}
    );
    """
    execute_sql(lb_sql)

execute_sql("NOTIFY pgrst, 'reload schema';")
print("Database seeding completed successfully!")
