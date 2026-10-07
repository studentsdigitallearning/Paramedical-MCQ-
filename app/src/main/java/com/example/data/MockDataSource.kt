package com.example.data

import com.example.model.BadgeItem
import com.example.model.CategoryType
import com.example.model.Chapter
import com.example.model.ClassData
import com.example.model.Difficulty
import com.example.model.FAQItem
import com.example.model.LeaderboardEntry
import com.example.model.NotificationItem
import com.example.model.Question
import com.example.model.Student
import com.example.model.Subject
import com.example.model.Test
import com.example.model.TestCategory
import com.example.model.TestStatus

object MockDataSource {

    val defaultStudent = Student(
        id = "",
        name = "Student",
        email = "",
        selectedClassId = "class_10",
        xp = 0,
        level = 1,
        streakDays = 0,
        testsAttempted = 0,
        averageScorePercent = 0,
        questionsSolved = 0,
        currentRank = 0,
        weeklyGoal = 5,
        weeklyCompleted = 0
    )

    val classes = listOf(
        ClassData("class_8", "Class 8", 5, 1),
        ClassData("class_9", "Class 9", 5, 2),
        ClassData("class_10", "Class 10", 5, 3),
        ClassData("class_11", "Class 11", 5, 4),
        ClassData("class_12", "Class 12", 5, 5)
    )

    val subjectsByClass = mapOf(
        "class_10" to listOf(
            Subject("sub_math_10", "class_10", "Mathematics", "math", 7, 60),
            Subject("sub_science_10", "class_10", "Science", "science", 8, 40),
            Subject("sub_sst_10", "class_10", "Social Science", "social", 6, 20),
            Subject("sub_eng_10", "class_10", "English", "english", 5, 80),
            Subject("sub_hindi_10", "class_10", "Hindi", "hindi", 5, 50)
        ),
        "class_9" to listOf(
            Subject("sub_math_9", "class_9", "Mathematics", "math", 6, 45),
            Subject("sub_science_9", "class_9", "Science", "science", 7, 30),
            Subject("sub_sst_9", "class_9", "Social Science", "social", 5, 15),
            Subject("sub_eng_9", "class_9", "English", "english", 5, 70),
            Subject("sub_hindi_9", "class_9", "Hindi", "hindi", 5, 40)
        ),
        "class_8" to listOf(
            Subject("sub_math_8", "class_8", "Mathematics", "math", 5, 50),
            Subject("sub_science_8", "class_8", "Science", "science", 6, 35),
            Subject("sub_sst_8", "class_8", "Social Science", "social", 4, 25),
            Subject("sub_eng_8", "class_8", "English", "english", 4, 60),
            Subject("sub_hindi_8", "class_8", "Hindi", "hindi", 4, 45)
        ),
        "class_11" to listOf(
            Subject("sub_physics_11", "class_11", "Physics", "science", 8, 30),
            Subject("sub_chem_11", "class_11", "Chemistry", "science", 8, 25),
            Subject("sub_math_11", "class_11", "Mathematics", "math", 9, 35),
            Subject("sub_bio_11", "class_11", "Biology", "science", 7, 20),
            Subject("sub_eng_11", "class_11", "English", "english", 6, 75)
        ),
        "class_12" to listOf(
            Subject("sub_physics_12", "class_12", "Physics", "science", 9, 40),
            Subject("sub_chem_12", "class_12", "Chemistry", "science", 9, 35),
            Subject("sub_math_12", "class_12", "Mathematics", "math", 10, 50),
            Subject("sub_bio_12", "class_12", "Biology", "science", 8, 30),
            Subject("sub_eng_12", "class_12", "English", "english", 6, 85)
        )
    )

    val chaptersBySubject = mapOf(
        "sub_math_10" to listOf(
            Chapter("ch_1", "sub_math_10", 1, "Real Numbers", 3, 67),
            Chapter("ch_2", "sub_math_10", 2, "Polynomials", 3, 40),
            Chapter("ch_3", "sub_math_10", 3, "Pair of Linear Equations", 3, 20),
            Chapter("ch_4", "sub_math_10", 4, "Quadratic Equations", 5, 0),
            Chapter("ch_5", "sub_math_10", 5, "Arithmetic Progressions", 3, 80),
            Chapter("ch_6", "sub_math_10", 6, "Triangles", 3, 60),
            Chapter("ch_7", "sub_math_10", 7, "Coordinate Geometry", 3, 30)
        ),
        "sub_science_10" to listOf(
            Chapter("ch_sci_1", "sub_science_10", 1, "Chemical Reactions and Equations", 4, 50),
            Chapter("ch_sci_2", "sub_science_10", 2, "Acids, Bases and Salts", 3, 30),
            Chapter("ch_sci_3", "sub_science_10", 3, "Metals and Non-metals", 3, 20),
            Chapter("ch_sci_4", "sub_science_10", 4, "Life Processes", 4, 60),
            Chapter("ch_sci_5", "sub_science_10", 5, "Control and Coordination", 3, 10),
            Chapter("ch_sci_6", "sub_science_10", 6, "Light: Reflection and Refraction", 4, 40)
        )
    )

    val categories = listOf(
        TestCategory("cat_subject", "Subject Wise", CategoryType.SUBJECT, "subject", "Explore chapter-wise tests", 35),
        TestCategory("cat_mock", "Mock Tests", CategoryType.MOCK, "mock", "Timed full syllabus simulation", 12),
        TestCategory("cat_previous", "Previous Tests", CategoryType.PREVIOUS, "previous", "Past board question papers", 15),
        TestCategory("cat_daily", "Daily Test", CategoryType.DAILY, "daily", "Today's daily challenge test", 10),
        TestCategory("cat_recommended", "Recommended for you", CategoryType.RECOMMENDED, "star", "Tailored to your weak areas", 3)
    )

    val defaultFallbackTest: Test
        get() = testsList.first()

    val testsList = listOf(
        Test(
            id = "test_daily_10",
            classId = "class_10",
            categoryId = "cat_daily",
            subjectId = null,
            subjectName = "Daily MCQ",
            title = "Today's MCQ Challenge",
            subtitle = "Daily Challenge • All India",
            questionCount = 10,
            durationMinutes = 15,
            totalMarks = 50f,
            negativeMarking = 0.25f,
            difficulty = Difficulty.MEDIUM,
            status = TestStatus.NOT_ATTEMPTED,
            isDaily = true
        ),
        Test(
            id = "test_mock_1",
            classId = "class_10",
            categoryId = "cat_mock",
            subjectId = "sub_math_10",
            subjectName = "Mathematics",
            title = "Full Syllabus Mock Test 1",
            subtitle = "Comprehensive Class 10 Maths",
            questionCount = 20,
            durationMinutes = 30,
            totalMarks = 20f,
            negativeMarking = 0.25f,
            difficulty = Difficulty.MEDIUM,
            status = TestStatus.NOT_ATTEMPTED
        ),
        Test(
            id = "test_mock_2",
            classId = "class_10",
            categoryId = "cat_mock",
            subjectId = "sub_science_10",
            subjectName = "Science",
            title = "Full Syllabus Mock Test 2",
            subtitle = "Class 10 Science & Lab Concepts",
            questionCount = 25,
            durationMinutes = 45,
            totalMarks = 25f,
            negativeMarking = 0.25f,
            difficulty = Difficulty.HARD,
            status = TestStatus.NOT_ATTEMPTED
        ),
        Test(
            id = "test_mock_3",
            classId = "class_10",
            categoryId = "cat_mock",
            subjectId = "sub_math_10",
            subjectName = "Mathematics",
            chapterId = "ch_1",
            chapterName = "Real Numbers",
            title = "Chapter Wise Mock Test",
            subtitle = "Real Numbers & Polynomials Mastery",
            questionCount = 20,
            durationMinutes = 30,
            totalMarks = 20f,
            negativeMarking = 0.25f,
            difficulty = Difficulty.EASY,
            status = TestStatus.COMPLETED,
            scoreObtained = 18f,
            scorePercent = 90
        ),
        Test(
            id = "test_mock_4",
            classId = "class_10",
            categoryId = "cat_mock",
            subjectId = "sub_sst_10",
            subjectName = "Social Science",
            title = "Previous Year Paper 2024",
            subtitle = "Official CBSE Board Paper 2024",
            questionCount = 50,
            durationMinutes = 90,
            totalMarks = 50f,
            negativeMarking = 0.25f,
            difficulty = Difficulty.MEDIUM,
            status = TestStatus.LOCKED
        ),
        Test(
            id = "test_mock_5",
            classId = "class_10",
            categoryId = "cat_mock",
            subjectId = "sub_math_10",
            subjectName = "Mathematics",
            title = "Previous Year Paper 2023",
            subtitle = "Official CBSE Board Paper 2023",
            questionCount = 50,
            durationMinutes = 90,
            totalMarks = 50f,
            negativeMarking = 0.25f,
            difficulty = Difficulty.EASY,
            status = TestStatus.EXPIRED,
            expiryNote = "Expired on Sep 30"
        ),
        // Chapter tests for Real Numbers
        Test(
            id = "test_ch1_1",
            classId = "class_10",
            categoryId = "cat_subject",
            subjectId = "sub_math_10",
            subjectName = "Mathematics",
            chapterId = "ch_1",
            chapterName = "Real Numbers",
            title = "Real Numbers - Level 1",
            subtitle = "Euclid's Division & Fundamental Theorem",
            questionCount = 15,
            durationMinutes = 20,
            totalMarks = 15f,
            negativeMarking = 0.25f,
            difficulty = Difficulty.EASY,
            status = TestStatus.COMPLETED,
            scoreObtained = 14f,
            scorePercent = 93
        ),
        Test(
            id = "test_ch1_2",
            classId = "class_10",
            categoryId = "cat_subject",
            subjectId = "sub_math_10",
            subjectName = "Mathematics",
            chapterId = "ch_1",
            chapterName = "Real Numbers",
            title = "Real Numbers - Level 2",
            subtitle = "Irrationality Proofs & Decimal Expansions",
            questionCount = 15,
            durationMinutes = 20,
            totalMarks = 15f,
            negativeMarking = 0.25f,
            difficulty = Difficulty.MEDIUM,
            status = TestStatus.NOT_ATTEMPTED
        )
    )

    // Dedicated questions for the Daily Test ONLY (Requirement: Daily Test questions should be in daily test section only)
    val questionsDaily = listOf(
        Question(
            id = "q_daily_1",
            testId = "test_daily_10",
            questionIndex = 0,
            text = "What type of chemical reaction occurs when quicklime (calcium oxide) reacts vigorously with water?",
            options = listOf("Combination reaction and exothermic", "Decomposition reaction and endothermic", "Displacement reaction", "Double displacement reaction"),
            correctIndex = 0,
            explanation = "CaO + H2O -> Ca(OH)2 + Heat. Two substances combine to form a single product with release of heat, so it is an exothermic combination reaction.",
            chapterName = "Daily Challenge",
            marks = 5.0f,
            textHindi = "जब बिना बुझा चूना (कैल्शियम ऑक्साइड) जल के साथ तीव्रता से अभिक्रिया करता है, तो यह किस प्रकार की अभिक्रिया है?",
            optionsHindi = listOf("संयोजन अभिक्रिया तथा ऊष्माक्षेपी", "वियोजन अभिक्रिया तथा ऊष्माशोषी", "विस्थापन अभिक्रिया", "द्विविस्थापन अभिक्रिया"),
            explanationHindi = "CaO + H2O -> Ca(OH)2 + ऊष्मा। यह एकल उत्पाद बनाने वाली ऊष्माक्षेपी संयोजन अभिक्रिया है।"
        ),
        Question(
            id = "q_daily_2",
            testId = "test_daily_10",
            questionIndex = 1,
            text = "The xylem in plants are responsible for:",
            options = listOf("Transport of water", "Transport of food", "Transport of amino acids", "Transport of oxygen"),
            correctIndex = 0,
            explanation = "Xylem tissue transports water and dissolved minerals from roots to all parts of the plant.",
            chapterName = "Daily Challenge",
            marks = 5.0f,
            textHindi = "पादप में जाइलम किसके लिए उत्तरदायी है?",
            optionsHindi = listOf("जल का वहन", "भोजन का वहन", "अमीनो अम्ल का वहन", "ऑक्सीजन का वहन"),
            explanationHindi = "जाइलम ऊतक जड़ों से पौधों के विभिन्न भागों तक जल और खनिजों का परिवहन करता है।"
        ),
        Question(
            id = "q_daily_3",
            testId = "test_daily_10",
            questionIndex = 2,
            text = "If HCF(306, 657) = 9, what is LCM(306, 657)?",
            options = listOf("22,338", "21,438", "23,338", "22,438"),
            correctIndex = 0,
            explanation = "LCM = (Product of two numbers) / HCF = (306 * 657) / 9 = 34 * 657 = 22,338.",
            chapterName = "Daily Challenge",
            marks = 5.0f,
            textHindi = "यदि HCF(306, 657) = 9 है, तो LCM(306, 657) का मान क्या होगा?",
            optionsHindi = listOf("22,338", "21,438", "23,338", "22,438"),
            explanationHindi = "LCM = (a * b) / HCF = (306 * 657) / 9 = 22,338."
        ),
        Question(
            id = "q_daily_4",
            testId = "test_daily_10",
            questionIndex = 3,
            text = "An aqueous solution turns red litmus paper blue. Excess addition of which solution would reverse this change?",
            options = listOf("Baking powder", "Lime", "Ammonium hydroxide solution", "Hydrochloric acid"),
            correctIndex = 3,
            explanation = "The solution is basic because it turns red litmus blue. Adding excess acid (HCl) makes it acidic and turns blue litmus red.",
            chapterName = "Daily Challenge",
            marks = 5.0f,
            textHindi = "एक विलयन लाल लिटमस को नीला कर देता है। इसमें किस पदार्थ को आधिक्य में मिलाने पर यह परिवर्तन उलट जाएगा?",
            optionsHindi = listOf("बेकिंग पाउडर", "चूना", "अमोनियम हाइड्रॉक्साइड विलयन", "हाइड्रोक्लोरिक अम्ल"),
            explanationHindi = "विलयन क्षारीय है। अतिरिक्त हाइड्रोक्लोरिक अम्ल मिलाने पर यह अम्लीय हो जाएगा।"
        ),
        Question(
            id = "q_daily_5",
            testId = "test_daily_10",
            questionIndex = 4,
            text = "The breakdown of pyruvate to give carbon dioxide, water and energy takes place in:",
            options = listOf("Cytoplasm", "Mitochondria", "Chloroplast", "Nucleus"),
            correctIndex = 1,
            explanation = "Aerobic respiration breaks down pyruvate in the mitochondria of the cell to release CO2, H2O, and 36-38 ATP.",
            chapterName = "Daily Challenge",
            marks = 5.0f,
            textHindi = "पाइरुवेट के विखंडन से कार्बन डाइऑक्साइड, जल तथा ऊर्जा प्राप्त होने की क्रिया कहाँ होती है?",
            optionsHindi = listOf("कोशिकाद्रव्य", "माइटोकॉन्ड्रिया", "क्लोरोप्लास्ट", "केन्द्रक"),
            explanationHindi = "वायवीय श्वसन माइटोकॉन्ड्रिया में संपन्न होता है।"
        ),
        Question(
            id = "q_daily_6",
            testId = "test_daily_10",
            questionIndex = 5,
            text = "The zeroes of the quadratic polynomial x² - 2x - 8 are:",
            options = listOf("4, -2", "-4, 2", "4, 2", "-4, -2"),
            correctIndex = 0,
            explanation = "x² - 2x - 8 = (x - 4)(x + 2) = 0 => x = 4 or x = -2.",
            chapterName = "Daily Challenge",
            marks = 5.0f,
            textHindi = "द्विघात बहुपद x² - 2x - 8 के शून्यक हैं:",
            optionsHindi = listOf("4, -2", "-4, 2", "4, 2", "-4, -2"),
            explanationHindi = "x² - 4x + 2x - 8 = (x - 4)(x + 2) = 0 => x = 4, -2."
        ),
        Question(
            id = "q_daily_7",
            testId = "test_daily_10",
            questionIndex = 6,
            text = "Which of the following is a plant hormone that inhibits growth and causes wilting of leaves?",
            options = listOf("Auxin", "Gibberellin", "Cytokinin", "Abscisic acid"),
            correctIndex = 3,
            explanation = "Abscisic acid (ABA) is a plant stress hormone that inhibits growth and triggers leaf senescence and wilting.",
            chapterName = "Daily Challenge",
            marks = 5.0f,
            textHindi = "निम्नलिखित में से कौन-सा पादप हार्मोन वृद्धि को रोकता है तथा पत्तियों के मुरझाने के लिए उत्तरदायी है?",
            optionsHindi = listOf("ऑक्सिन", "जिबरेलिन", "साइटोकाइनिन", "एब्सिसिक अम्ल"),
            explanationHindi = "एब्सिसिक अम्ल (ABA) वृद्धि रोधक हार्मोन है जो पत्तियों को मुरझाने के लिए प्रेरित करता है।"
        ),
        Question(
            id = "q_daily_8",
            testId = "test_daily_10",
            questionIndex = 7,
            text = "A device that reverses the direction of flow of current through a circuit is called:",
            options = listOf("Galvanometer", "Commutator (Split ring)", "Voltmeter", "Ammeter"),
            correctIndex = 1,
            explanation = "A commutator or split ring reverses the current direction in an electric motor every half rotation.",
            chapterName = "Daily Challenge",
            marks = 5.0f,
            textHindi = "परिपथ में विद्युत धारा की दिशा को उत्क्रमित (उल्टा) करने वाली युक्ति कहलाती है:",
            optionsHindi = listOf("गैल्वेनोमीटर", "दिक्परिवर्तक (विभक्त वलय)", "वोल्टमीटर", "ऐमीटर"),
            explanationHindi = "विभक्त वलय या दिक्परिवर्तक धारा की दिशा को प्रत्येक आधे घूर्णन के बाद पलट देता है।"
        ),
        Question(
            id = "q_daily_9",
            testId = "test_daily_10",
            questionIndex = 8,
            text = "If discriminant D = b² - 4ac > 0 and is not a perfect square, the roots of ax² + bx + c = 0 are:",
            options = listOf("Real, unequal and irrational", "Real, unequal and rational", "Real and equal", "Imaginary"),
            correctIndex = 0,
            explanation = "When D > 0 and not a square, the square root of D remains a surd, yielding two distinct irrational roots.",
            chapterName = "Daily Challenge",
            marks = 5.0f,
            textHindi = "यदि विविक्तकर D = b² - 4ac > 0 और पूर्ण वर्ग नहीं है, तो समीकरण ax² + bx + c = 0 के मूल होंगे:",
            optionsHindi = listOf("वास्तविक, भिन्न तथा अपरिमेय", "वास्तविक, भिन्न तथा परिमेय", "वास्तविक तथा बराबर", "काल्पनिक"),
            explanationHindi = "जब D > 0 और पूर्ण वर्ग न हो तो मूल वास्तविक, असमान और अपरिमेय होते हैं।"
        ),
        Question(
            id = "q_daily_10",
            testId = "test_daily_10",
            questionIndex = 9,
            text = "Which gas is flushed into bags of potato chips to prevent rancidity due to oxidation?",
            options = listOf("Oxygen", "Nitrogen", "Hydrogen", "Carbon dioxide"),
            correctIndex = 1,
            explanation = "Nitrogen is an inert gas that displaces oxygen and prevents fatty oils in chips from becoming rancid.",
            chapterName = "Daily Challenge",
            marks = 5.0f,
            textHindi = "चिप्स की थैली में से ऑक्सीजन हटाकर कौन-सी गैस भरी जाती है ताकि विकृतगंधिता न हो?",
            optionsHindi = listOf("ऑक्सीजन", "नाइट्रोजन", "हाइड्रोजन", "कार्बन डाइऑक्साइड"),
            explanationHindi = "नाइट्रोजन एक अक्रिय गैस है जो तेल और वसा के उपचयन (ऑक्सीकरण) को रोकती है।"
        )
    )

    // Dedicated questions for Science Chapter 1 (Chemical Reactions & Equations)
    val questionsScienceCh1 = listOf(
        Question(
            id = "q_sci_ch1_1",
            testId = "test_sci_ch1_1",
            questionIndex = 0,
            text = "Which of the following is a displacement reaction?",
            options = listOf("MgCO3 -> MgO + CO2", "2Na + 2H2O -> 2NaOH + H2", "H2 + Cl2 -> 2HCl", "2KClO3 -> 2KCl + 3O2"),
            correctIndex = 1,
            explanation = "Sodium displaces hydrogen from water to form sodium hydroxide and hydrogen gas, so it is a single displacement reaction.",
            chapterName = "Chemical Reactions and Equations",
            marks = 2.0f,
            textHindi = "निम्नलिखित में से कौन-सी विस्थापन अभिक्रिया है?",
            optionsHindi = listOf("MgCO3 -> MgO + CO2", "2Na + 2H2O -> 2NaOH + H2", "H2 + Cl2 -> 2HCl", "2KClO3 -> 2KCl + 3O2"),
            explanationHindi = "सोडियम जल में से हाइड्रोजन को विस्थापित करता है, अत: यह विस्थापन अभिक्रिया है।"
        ),
        Question(
            id = "q_sci_ch1_2",
            testId = "test_sci_ch1_1",
            questionIndex = 1,
            text = "When lead nitrate powder is heated in a boiling tube, brown fumes of which gas are evolved?",
            options = listOf("Nitrogen monoxide", "Nitrogen dioxide", "Dinitrogen oxide", "Nitrogen pentoxide"),
            correctIndex = 1,
            explanation = "2Pb(NO3)2 -> 2PbO + 4NO2 + O2. NO2 (nitrogen dioxide) produces characteristic brown fumes.",
            chapterName = "Chemical Reactions and Equations",
            marks = 2.0f,
            textHindi = "लेड नाइट्रेट चूर्ण को गर्म करने पर भूरे रंग का धुआँ निकलता है, यह धुआँ किस गैस का है?",
            optionsHindi = listOf("नाइट्रोजन मोनोऑक्साइड", "नाइट्रोजन डाइऑक्साइड", "डाइनाइट्रोजन ऑक्साइड", "नाइट्रोजन पेंटाऑक्साइड"),
            explanationHindi = "लेड नाइट्रेट के तापीय अपघटन पर नाइट्रोजन डाइऑक्साइड (NO2) के भूरे धुएँ निकलते हैं।"
        ),
        Question(
            id = "q_sci_ch1_3",
            testId = "test_sci_ch1_1",
            questionIndex = 2,
            text = "In the reaction: CuO + H2 -> Cu + H2O, which substance is being oxidized?",
            options = listOf("CuO", "H2", "Cu", "H2O"),
            correctIndex = 1,
            explanation = "Hydrogen gains oxygen to become H2O, so H2 is oxidized. CuO loses oxygen and is reduced to Cu.",
            chapterName = "Chemical Reactions and Equations",
            marks = 2.0f,
            textHindi = "अभिक्रिया CuO + H2 -> Cu + H2O में किस पदार्थ का उपचयन (ऑक्सीकरण) हो रहा है?",
            optionsHindi = listOf("CuO", "H2", "Cu", "H2O"),
            explanationHindi = "हाइड्रोजन ऑक्सीजन ग्रहण करके H2O बनाता है, इसलिए H2 का उपचयन हो रहा है।"
        ),
        Question(
            id = "q_sci_ch1_4",
            testId = "test_sci_ch1_1",
            questionIndex = 3,
            text = "The chemical formula of marble (chalk / limestone) is:",
            options = listOf("Ca(OH)2", "CaO", "CaCO3", "CaSO4"),
            correctIndex = 2,
            explanation = "Marble, limestone, and eggshells are all chemically composed of Calcium Carbonate (CaCO3).",
            chapterName = "Chemical Reactions and Equations",
            marks = 2.0f,
            textHindi = "संगमरमर (चूना पत्थर) का रासायनिक सूत्र क्या है?",
            optionsHindi = listOf("Ca(OH)2", "CaO", "CaCO3", "CaSO4"),
            explanationHindi = "संगमरमर और चूना पत्थर का रासायनिक सूत्र CaCO3 (कैल्शियम कार्बोनेट) है।"
        ),
        Question(
            id = "q_sci_ch1_5",
            testId = "test_sci_ch1_1",
            questionIndex = 4,
            text = "What happens when dilute hydrochloric acid is added to iron filings?",
            options = listOf("Hydrogen gas and iron(II) chloride are produced", "Chlorine gas and iron hydroxide are produced", "No reaction takes place", "Iron salt and water are produced"),
            correctIndex = 0,
            explanation = "Fe + 2HCl -> FeCl2 + H2. Iron displaces hydrogen, releasing H2 gas and forming iron(II) chloride.",
            chapterName = "Chemical Reactions and Equations",
            marks = 2.0f,
            textHindi = "लौह चूर्ण पर तनु हाइड्रोक्लोरिक अम्ल डालने से क्या होता है?",
            optionsHindi = listOf("हाइड्रोजन गैस एवं आयरन क्लोराइड बनता है", "क्लोरीन गैस एवं आयरन हाइड्रॉक्साइड बनता है", "कोई अभिक्रिया नहीं होती", "आयरन लवण एवं जल बनता है"),
            explanationHindi = "Fe + 2HCl -> FeCl2 + H2 (हाइड्रोजन गैस और आयरन क्लोराइड)।"
        )
    )

    // Dedicated questions for Science Chapter 2 (Acids, Bases & Salts)
    val questionsScienceCh2 = listOf(
        Question(
            id = "q_sci_ch2_1",
            testId = "test_sci_ch2_1",
            questionIndex = 0,
            text = "What is the chemical formula of bleaching powder?",
            options = listOf("CaO", "Ca(OH)2", "CaOCl2", "CaCl2"),
            correctIndex = 2,
            explanation = "Bleaching powder is Calcium oxychloride, with chemical formula CaOCl2.",
            chapterName = "Acids, Bases and Salts",
            marks = 2.0f,
            textHindi = "विरंजक चूर्ण (ब्लीचिंग पाउडर) का रासायनिक सूत्र क्या है?",
            optionsHindi = listOf("CaO", "Ca(OH)2", "CaOCl2", "CaCl2"),
            explanationHindi = "विरंजक चूर्ण का रासायनिक सूत्र CaOCl2 (कैल्शियम ऑक्सीक्लोराइड) है।"
        ),
        Question(
            id = "q_sci_ch2_2",
            testId = "test_sci_ch2_1",
            questionIndex = 1,
            text = "Tooth enamel contains calcium hydroxyapatite. Tooth decay starts when the pH of mouth is below:",
            options = listOf("7.0", "6.5", "5.5", "4.0"),
            correctIndex = 2,
            explanation = "Bacteria produce acid from leftover food particles, dropping mouth pH below 5.5 and corroding enamel.",
            chapterName = "Acids, Bases and Salts",
            marks = 2.0f,
            textHindi = "मुँह का pH मान कितने से कम होने पर दाँतों का क्षय (सड़ना) प्रारंभ हो जाता है?",
            optionsHindi = listOf("7.0", "6.5", "5.5", "4.0"),
            explanationHindi = "मुँह का pH मान 5.5 से कम होने पर दाँतों का इनैमल संक्षारित होने लगता है।"
        ),
        Question(
            id = "q_sci_ch2_3",
            testId = "test_sci_ch2_1",
            questionIndex = 2,
            text = "What is the chemical formula of Plaster of Paris?",
            options = listOf("CaSO4.2H2O", "CaSO4.½H2O", "CaSO4.H2O", "2CaSO4.H2O"),
            correctIndex = 1,
            explanation = "Plaster of Paris is calcium sulphate hemihydrate: CaSO4.½H2O.",
            chapterName = "Acids, Bases and Salts",
            marks = 2.0f,
            textHindi = "प्लास्टर ऑफ पेरिस का रासायनिक सूत्र क्या है?",
            optionsHindi = listOf("CaSO4.2H2O", "CaSO4.½H2O", "CaSO4.H2O", "2CaSO4.H2O"),
            explanationHindi = "प्लास्टर ऑफ पेरिस कैल्शियम सल्फेट हेमीहाइड्रेट (CaSO4.½H2O) होता है।"
        )
    )

    val questionsForMockTest1 = listOf(
        Question(
            id = "q_1",
            testId = "test_mock_1",
            questionIndex = 0,
            text = "If two positive integers a and b are written as a = x³y² and b = xy³, where x, y are prime numbers, then HCF(a, b) is:",
            options = listOf("xy", "xy²", "x³y³", "x²y²"),
            correctIndex = 1,
            explanation = "HCF is the product of the smallest power of each common prime factor involved in the numbers: min power of x is 1, min power of y is 2. Therefore, HCF(a, b) = xy².",
            chapterName = "Real Numbers",
            textHindi = "यदि दो धनात्मक पूर्णांक a और b को a = x³y² और b = xy³ के रूप में लिखा जाता है, जहाँ x, y अभाज्य संख्याएँ हैं, तो HCF(a, b) है:",
            optionsHindi = listOf("xy", "xy²", "x³y³", "x²y²"),
            explanationHindi = "HCF प्रत्येक उभयनिष्ठ अभाज्य गुणनखंड की न्यूनतम घात का गुणनफल होता है। अत: HCF(a, b) = xy²."
        ),
        Question(
            id = "q_2",
            testId = "test_mock_1",
            questionIndex = 1,
            text = "The decimal expansion of the rational number 14587 / 1250 will terminate after how many decimal places?",
            options = listOf("One", "Two", "Three", "Four"),
            correctIndex = 3,
            explanation = "1250 = 2 × 5⁴. The highest power of 2 or 5 in the denominator is 4, so it terminates after 4 decimal places.",
            chapterName = "Real Numbers",
            textHindi = "परिमेय संख्या 14587 / 1250 का दशमलव प्रसार कितने दशमलव स्थानों के बाद समाप्त होगा?",
            optionsHindi = listOf("एक", "दो", "तीन", "चार"),
            explanationHindi = "1250 = 2 × 5⁴। हर में 5 की अधिकतम घात 4 है, इसलिए दशमलव प्रसार 4 स्थानों के बाद समाप्त होगा।"
        ),
        Question(
            id = "q_3",
            testId = "test_mock_1",
            questionIndex = 2,
            text = "The value of √64 is:",
            options = listOf("6", "8", "10", "12"),
            correctIndex = 1,
            explanation = "The principal square root of 64 is 8, since 8 × 8 = 64.",
            chapterName = "Real Numbers",
            textHindi = "√64 का मान है:",
            optionsHindi = listOf("6", "8", "10", "12"),
            explanationHindi = "64 का मुख्य वर्गमूल 8 है, क्योंकि 8 × 8 = 64 होता है।"
        ),
        Question(
            id = "q_4",
            testId = "test_mock_1",
            questionIndex = 3,
            text = "If one zero of the quadratic polynomial x² + 3x + k is 2, then the value of k is:",
            options = listOf("10", "-10", "-7", "-2"),
            correctIndex = 1,
            explanation = "Since 2 is a zero of p(x), p(2) = 0. (2)² + 3(2) + k = 0 => 4 + 6 + k = 0 => k = -10.",
            chapterName = "Polynomials",
            textHindi = "यदि द्विघात बहुपद x² + 3x + k का एक शून्यक 2 है, तो k का मान है:",
            optionsHindi = listOf("10", "-10", "-7", "-2"),
            explanationHindi = "चूँकि 2 p(x) का एक शून्यक है, p(2) = 0 => 4 + 6 + k = 0 => k = -10."
        ),
        Question(
            id = "q_5",
            testId = "test_mock_1",
            questionIndex = 4,
            text = "The discriminant of the quadratic equation 2x² - 4x + 3 = 0 is:",
            options = listOf("-8", "10", "-16", "8"),
            correctIndex = 0,
            explanation = "D = b² - 4ac = (-4)² - 4(2)(3) = 16 - 24 = -8. Since D < 0, the equation has no real roots.",
            chapterName = "Quadratic Equations",
            textHindi = "द्विघात समीकरण 2x² - 4x + 3 = 0 का विविक्तकर (Discriminant) है:",
            optionsHindi = listOf("-8", "10", "-16", "8"),
            explanationHindi = "D = b² - 4ac = 16 - 24 = -8. चूँकि D < 0 है, समीकरण के कोई वास्तविक मूल नहीं हैं।"
        ),
        Question(
            id = "q_6",
            testId = "test_mock_1",
            questionIndex = 5,
            text = "The 10th term of the AP: 2, 7, 12, ... is:",
            options = listOf("47", "52", "45", "50"),
            correctIndex = 0,
            explanation = "Here a = 2 and common difference d = 7 - 2 = 5. a₁₀ = a + (10 - 1)d = 2 + 9(5) = 2 + 45 = 47.",
            chapterName = "Arithmetic Progressions"
        ),
        Question(
            id = "q_7",
            testId = "test_mock_1",
            questionIndex = 6,
            text = "The distance of the point P(-6, 8) from the origin is:",
            options = listOf("8", "2√7", "10", "6"),
            correctIndex = 2,
            explanation = "Distance from origin = √(x² + y²) = √((-6)² + 8²) = √(36 + 64) = √100 = 10 units.",
            chapterName = "Coordinate Geometry"
        ),
        Question(
            id = "q_8",
            testId = "test_mock_1",
            questionIndex = 7,
            text = "If tan θ = 4/3, then the value of (sin θ + cos θ) / (sin θ - cos θ) is:",
            options = listOf("7", "1", "-7", "1/7"),
            correctIndex = 0,
            explanation = "Dividing numerator and denominator by cos θ gives (tan θ + 1) / (tan θ - 1) = (4/3 + 1) / (4/3 - 1) = (7/3) / (1/3) = 7.",
            chapterName = "Trigonometry"
        ),
        Question(
            id = "q_9",
            testId = "test_mock_1",
            questionIndex = 8,
            text = "In a circle of radius 21 cm, an arc subtends an angle of 60° at the centre. The length of the arc is:",
            options = listOf("22 cm", "11 cm", "44 cm", "33 cm"),
            correctIndex = 0,
            explanation = "Length of arc = (θ/360) × 2πr = (60/360) × 2 × (22/7) × 21 = (1/6) × 132 = 22 cm.",
            chapterName = "Areas Related to Circles"
        ),
        Question(
            id = "q_10",
            testId = "test_mock_1",
            questionIndex = 9,
            text = "If the perimeter and the area of a circle are numerically equal, then the radius of the circle is:",
            options = listOf("2 units", "π units", "4 units", "7 units"),
            correctIndex = 0,
            explanation = "2πr = πr² => r² - 2r = 0 => r(r - 2) = 0. Since r > 0, r = 2 units.",
            chapterName = "Areas Related to Circles"
        ),
        Question(
            id = "q_11",
            testId = "test_mock_1",
            questionIndex = 10,
            text = "A card is drawn from a well-shuffled deck of 52 playing cards. The probability of getting a king of black suit is:",
            options = listOf("1/26", "1/13", "1/52", "2/13"),
            correctIndex = 0,
            explanation = "There are 2 black kings (Spades and Clubs). P = 2 / 52 = 1 / 26.",
            chapterName = "Probability"
        ),
        Question(
            id = "q_12",
            testId = "test_mock_1",
            questionIndex = 11,
            text = "Which of the following cannot be the probability of an event?",
            options = listOf("2/3", "-1.5", "15%", "0.7"),
            correctIndex = 1,
            explanation = "Probability of any event satisfies 0 ≤ P(E) ≤ 1. It cannot be negative.",
            chapterName = "Probability"
        ),
        Question(
            id = "q_13",
            testId = "test_mock_1",
            questionIndex = 12,
            text = "For an event E, P(E) + P(not E) is equal to:",
            options = listOf("0", "1", "-1", "2"),
            correctIndex = 1,
            explanation = "The sum of probabilities of an event and its complementary event is always equal to 1.",
            chapterName = "Probability"
        ),
        Question(
            id = "q_14",
            testId = "test_mock_1",
            questionIndex = 13,
            text = "The mode and mean of a dataset are 12 and 15 respectively. The median of the data is:",
            options = listOf("13", "14", "15", "16"),
            correctIndex = 1,
            explanation = "Using the empirical formula: Mode = 3 Median - 2 Mean => 12 = 3 Median - 30 => 3 Median = 42 => Median = 14.",
            chapterName = "Statistics"
        ),
        Question(
            id = "q_15",
            testId = "test_mock_1",
            questionIndex = 14,
            text = "If sin A = 1/2, then the value of cot A is:",
            options = listOf("√3", "1/√3", "1", "√3/2"),
            correctIndex = 0,
            explanation = "sin A = 1/2 implies A = 30°. cot 30° = √3.",
            chapterName = "Trigonometry"
        ),
        Question(
            id = "q_16",
            testId = "test_mock_1",
            questionIndex = 15,
            text = "The pair of linear equations 9x + 3y + 12 = 0 and 18x + 6y + 24 = 0 represent lines which are:",
            options = listOf("Intersecting", "Parallel", "Coincident", "Perpendicular"),
            correctIndex = 2,
            explanation = "a₁/a₂ = 9/18 = 1/2; b₁/b₂ = 3/6 = 1/2; c₁/c₂ = 12/24 = 1/2. Since a₁/a₂ = b₁/b₂ = c₁/c₂, the lines are coincident.",
            chapterName = "Linear Equations"
        ),
        Question(
            id = "q_17",
            testId = "test_mock_1",
            questionIndex = 16,
            text = "The zeroes of the quadratic polynomial x² + 7x + 10 are:",
            options = listOf("2 and 5", "-2 and -5", "-2 and 5", "2 and -5"),
            correctIndex = 1,
            explanation = "x² + 7x + 10 = (x + 2)(x + 5) = 0 => x = -2 or x = -5.",
            chapterName = "Polynomials"
        ),
        Question(
            id = "q_18",
            testId = "test_mock_1",
            questionIndex = 17,
            text = "If the points (0, 0), (1, 2) and (x, y) are collinear, then:",
            options = listOf("x = y", "2x = y", "x = 2y", "2x = -y"),
            correctIndex = 1,
            explanation = "Area of triangle formed = 0 => 0(2 - y) + 1(y - 0) + x(0 - 2) = 0 => y - 2x = 0 => 2x = y.",
            chapterName = "Coordinate Geometry"
        ),
        Question(
            id = "q_19",
            testId = "test_mock_1",
            questionIndex = 18,
            text = "The total number of factors of any prime number is:",
            options = listOf("1", "2", "3", "Infinite"),
            correctIndex = 1,
            explanation = "A prime number is divisible by exactly two positive divisors: 1 and itself.",
            chapterName = "Real Numbers"
        ),
        Question(
            id = "q_20",
            testId = "test_mock_1",
            questionIndex = 19,
            text = "If △ABC is right-angled at C, then the value of cos(A + B) is:",
            options = listOf("0", "1", "1/2", "√3/2"),
            correctIndex = 0,
            explanation = "In △ABC, A + B + C = 180°. Since C = 90°, A + B = 90°. cos 90° = 0.",
            chapterName = "Trigonometry",
            marks = 2.0f
        ),
        Question(
            id = "q_21",
            testId = "test_mock_1",
            questionIndex = 20,
            text = "The distance of point P(3, 4) from the origin is:",
            options = listOf("3 units", "4 units", "5 units", "7 units"),
            correctIndex = 2,
            explanation = "Distance from origin = √(x² + y²) = √(3² + 4²) = √(9 + 16) = √25 = 5 units.",
            chapterName = "Coordinate Geometry",
            marks = 2.0f
        ),
        Question(
            id = "q_22",
            testId = "test_mock_1",
            questionIndex = 21,
            text = "If sin θ + cos θ = √2 cos θ, then the value of (cos θ - sin θ) is:",
            options = listOf("√2 sin θ", "2 sin θ", "-√2 sin θ", "sin θ"),
            correctIndex = 0,
            explanation = "Squaring both sides and simplifying yields (cos θ - sin θ)² = 2 sin² θ, so cos θ - sin θ = √2 sin θ.",
            chapterName = "Trigonometry",
            marks = 2.0f
        ),
        Question(
            id = "q_23",
            testId = "test_mock_1",
            questionIndex = 22,
            text = "If the common difference of an AP is 5, then what is a₁₈ - a₁₃?",
            options = listOf("5", "20", "25", "30"),
            correctIndex = 2,
            explanation = "a₁₈ - a₁₃ = (a + 17d) - (a + 12d) = 5d = 5 × 5 = 25.",
            chapterName = "Arithmetic Progressions",
            marks = 2.0f
        ),
        Question(
            id = "q_24",
            testId = "test_mock_1",
            questionIndex = 23,
            text = "Which of the following is not a quadratic equation?",
            options = listOf("(x + 2)² = 2(x + 3)", "x² + 3x = (-1)(1 - 3x)²", "(x + 2)(x - 1) = x² - 2x - 3", "x³ - x² = (x - 1)³"),
            correctIndex = 2,
            explanation = "(x + 2)(x - 1) = x² + x - 2. On comparing with x² - 2x - 3, the x² terms cancel out leaving a linear equation: 3x + 1 = 0.",
            chapterName = "Quadratic Equations",
            marks = 2.0f
        ),
        Question(
            id = "q_25",
            testId = "test_mock_1",
            questionIndex = 24,
            text = "The probability of getting a bad egg in a lot of 400 eggs is 0.035. The number of bad eggs in the lot is:",
            options = listOf("7", "14", "21", "28"),
            correctIndex = 1,
            explanation = "Number of bad eggs = Probability × Total eggs = 0.035 × 400 = 14.",
            chapterName = "Probability",
            marks = 2.0f
        )
    )

    val questionsMathCh1 = listOf(
        Question(
            id = "q_math_ch1_1",
            testId = "test_math_ch1_1",
            questionIndex = 0,
            text = "According to the Fundamental Theorem of Arithmetic, every composite number can be expressed uniquely as a product of:",
            options = listOf("Primes", "Odd numbers", "Even numbers", "Composite numbers"),
            correctIndex = 0,
            explanation = "Every composite number can be written as a product of prime powers uniquely, apart from the order of factors.",
            chapterName = "Real Numbers",
            marks = 2.0f,
            textHindi = "अंकगणित की आधारभूत प्रमेय के अनुसार प्रत्येक भाज्य संख्या को किसके गुणनफल के रूप में व्यक्त किया जा सकता है?",
            optionsHindi = listOf("अभाज्य संख्याएं", "विषम संख्याएं", "सम संख्याएं", "भाज्य संख्याएं"),
            explanationHindi = "प्रत्येक भाज्य संख्या को अभाज्य संख्याओं के गुणनफल के रूप में लिखा जा सकता है।"
        ),
        Question(
            id = "q_math_ch1_2",
            testId = "test_math_ch1_1",
            questionIndex = 1,
            text = "The decimal expansion of 23 / (2³ × 5²) will terminate after how many decimal places?",
            options = listOf("1", "2", "3", "4"),
            correctIndex = 2,
            explanation = "Max exponent of 2 and 5 in the denominator is 3, so it terminates after 3 decimal places: 23 / 200 = 0.115.",
            chapterName = "Real Numbers",
            marks = 2.0f,
            textHindi = "23 / (2³ × 5²) का दशमलव प्रसार कितने दशमलव स्थानों के बाद शांत होगा?",
            optionsHindi = listOf("1", "2", "3", "4"),
            explanationHindi = "हर में 2 और 5 की अधिकतम घात 3 है, अत: 3 स्थानों के बाद शांत होगा।"
        ),
        Question(
            id = "q_math_ch1_3",
            testId = "test_math_ch1_1",
            questionIndex = 2,
            text = "Which of the following is an irrational number?",
            options = listOf("√4", "√9", "√7", "0.25"),
            correctIndex = 2,
            explanation = "7 is not a perfect square, so √7 is an irrational number.",
            chapterName = "Real Numbers",
            marks = 2.0f,
            textHindi = "निम्नलिखित में से कौन सी एक अपरिमेय संख्या है?",
            optionsHindi = listOf("√4", "√9", "√7", "0.25"),
            explanationHindi = "7 पूर्ण वर्ग नहीं है, अत: √7 अपरिमेय है।"
        ),
        Question(
            id = "q_math_ch1_4",
            testId = "test_math_ch1_1",
            questionIndex = 3,
            text = "If two positive numbers a and b have HCF(a, b) = 12 and a × b = 1800, what is LCM(a, b)?",
            options = listOf("120", "150", "180", "200"),
            correctIndex = 1,
            explanation = "LCM = (a × b) / HCF = 1800 / 12 = 150.",
            chapterName = "Real Numbers",
            marks = 2.0f,
            textHindi = "यदि दो संख्याओं a और b का HCF 12 और a × b = 1800 है, तो उनका LCM क्या होगा?",
            optionsHindi = listOf("120", "150", "180", "200"),
            explanationHindi = "LCM = 1800 / 12 = 150."
        ),
        Question(
            id = "q_math_ch1_5",
            testId = "test_math_ch1_1",
            questionIndex = 4,
            text = "The HCF of two consecutive even integers is always:",
            options = listOf("1", "2", "4", "0"),
            correctIndex = 1,
            explanation = "Two consecutive even integers 2k and 2k + 2 share 2 as their greatest common divisor.",
            chapterName = "Real Numbers",
            marks = 2.0f,
            textHindi = "दो क्रमागत सम पूर्णांकों का HCF सदैव क्या होता है?",
            optionsHindi = listOf("1", "2", "4", "0"),
            explanationHindi = "दो क्रमागत सम संख्याओं का सार्व गुणनखंड 2 होता है।"
        )
    )

    fun getQuestionsForTest(testId: String): List<Question> {
        return when (testId) {
            "test_daily_10" -> questionsDaily
            "test_sci_ch1_1" -> questionsScienceCh1
            "test_sci_ch2_1" -> questionsScienceCh2
            "test_math_ch1_1" -> questionsMathCh1
            "test_mock_1" -> questionsForMockTest1
            else -> {
                // If it is explicitly a daily test, give daily questions
                if (testId.contains("daily", ignoreCase = true)) {
                    questionsDaily
                } else if (testId.contains("math", ignoreCase = true)) {
                    questionsMathCh1
                } else if (testId.contains("sci", ignoreCase = true)) {
                    questionsScienceCh1
                } else {
                    questionsForMockTest1
                }
            }
        }
    }

    fun generateDefaultDailyPeers(): List<com.example.model.DailyLeaderboardEntry> {
        return emptyList()
    }

    val leaderboardDaily: List<LeaderboardEntry> = emptyList()

    val leaderboardClass: List<LeaderboardEntry> = emptyList()

    val notifications = listOf(
        NotificationItem("n_1", "Welcome to MCQ Practice!", "Start your learning journey with daily tests and chapter quizzes.", "Just now", "ANNOUNCEMENT", false),
        NotificationItem("n_2", "Daily MCQ Challenge", "Attempt today's challenge to earn XP and rank on the leaderboard.", "Today", "DAILY", false)
    )

    val badges = listOf(
        BadgeItem("b_1", "Fast Learner", "Complete your first test with >80% accuracy", "flash_on", false, null),
        BadgeItem("b_2", "Streak Champion", "Maintain a 7-day continuous practice streak", "local_fire_department", false, null),
        BadgeItem("b_3", "Centurion", "Solve 100+ questions successfully", "workspace_premium", false, null),
        BadgeItem("b_4", "Board Ready", "Score 90%+ in 3 full syllabus mock tests", "emoji_events", false, null),
        BadgeItem("b_5", "Speed Demon", "Finish a 30-min test in under 15 minutes", "timer", false, null)
    )

    val faqs = listOf(
        FAQItem("How is negative marking calculated?", "In tests with negative marking, 0.25 marks are deducted for every incorrect option. Unanswered questions do not incur any deduction."),
        FAQItem("Can I resume an in-progress test?", "Yes! Your answers and remaining time are continuously saved. You can leave the test and resume anytime before the timer expires."),
        FAQItem("When does the Daily MCQ Challenge reset?", "The Daily Challenge resets every midnight at 00:00:00 IST (Indian Standard Time)."),
        FAQItem("How do I switch my class?", "Tap the class chip at the top of the Home screen or go to Profile -> Switch Class. Your content and subjects will update instantly.")
    )

    val promotionalBanners = listOf(
        com.example.model.PromotionalBanner(
            id = "banner_daily_mcq",
            isDailyChallenge = true,
            badgeText = "TODAY'S CHALLENGE",
            metaText = "20 Questions • 30 Minutes",
            title = "Today's MCQ Challenge",
            description = "Test your knowledge with today's challenge.",
            buttonText = "Start Test",
            actionType = com.example.model.BannerActionType.OPEN_DAILY_TEST
        ),
        com.example.model.PromotionalBanner(
            id = "banner_scholarship",
            isDailyChallenge = false,
            badgeText = "ALL-INDIA EXAM",
            metaText = "NCERT 2026 Batch",
            title = "Board Exam Prep Series",
            description = "Complete Chapter-wise Practice with Instant Solutions & Detailed Analysis.",
            buttonText = "Explore Tests",
            actionType = com.example.model.BannerActionType.OPEN_INTERNAL_PAGE,
            targetId = "categories"
        ),
        com.example.model.PromotionalBanner(
            id = "banner_youtube_lectures",
            isDailyChallenge = false,
            badgeText = "FREE VIDEO LECTURES",
            metaText = "YouTube Channel",
            title = "Daily Chapter Video Solutions",
            description = "Watch expert mentors explain tricky concepts and elimination tricks.",
            buttonText = "Watch Videos",
            actionType = com.example.model.BannerActionType.OPEN_VIDEO,
            url = com.example.util.DailyTestTimeUtils.YOUTUBE_URL
        ),
        com.example.model.PromotionalBanner(
            id = "banner_telegram_community",
            isDailyChallenge = false,
            badgeText = "STUDY COMMUNITY",
            metaText = "15,000+ Students",
            title = "Daily Doubt Solving & Notes",
            description = "Join our Telegram community for free formula cheat sheets and mock updates.",
            buttonText = "Join Telegram",
            actionType = com.example.model.BannerActionType.OPEN_WEBSITE,
            url = com.example.util.DailyTestTimeUtils.TELEGRAM_URL
        )
    )
}

