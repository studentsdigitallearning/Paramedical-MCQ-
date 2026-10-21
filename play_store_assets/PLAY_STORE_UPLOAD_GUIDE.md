# Google Play Store Production Upload Guide — JAC MCQ Master

This folder and workspace contain everything required to publish **JAC MCQ Master** (`com.jacboard.mcqmaster`) to the **Google Play Console**.

---

## 1. Signed Production Build Files (What to Upload)

| File Path | Format | Purpose |
| :--- | :--- | :--- |
| **`jac_mcq_master_release.aab`** *(or `app/build/outputs/bundle/release/app-release.aab`)* | **Android App Bundle (`.aab`)** | **UPLOAD THIS TO GOOGLE PLAY STORE** under **Production → Create new release → App bundles**. Google Play requires `.aab` for all new apps. |
| **`jac_mcq_master_release.apk`** *(or `app/build/outputs/apk/release/app-release.apk`)* | **Signed Release APK (`.apk`)** | Use for direct phone installation, WhatsApp/Telegram sharing, or offline testing. |
| **`jac-mcq-master-release.jks`** | **Java Keystore (`.jks`)** | Your production signing key. Keep a safe backup of this file for all future app updates. |

### Production Signature Credentials
* **Keystore File:** `jac-mcq-master-release.jks`
* **Key Alias:** `jacmcqmaster`
* **Certificate Subject:** `CN=JAC MCQ Master, OU=Education, O=JAC Board Prep, L=Ranchi, ST=Jharkhand, C=IN`
* **Key Algorithm:** RSA 2048-bit (`SHA384withRSA`), Validity: 10,000 days

---

## 2. Store Listing Materials (Copy-Paste into Play Console)

### A. App Details (`Grow → Store presence → Main store listing`)
* **App Name (Max 30 characters):**
  ```text
  JAC MCQ Master
  ```
* **Short Description (Max 80 characters):**
  ```text
  JAC Board Class 8 to 12 MCQ Tests, Daily Live Quiz, PYQs & Bilingual Practice.
  ```
* **Full Description (Max 4000 characters):**
  ```text
  Prepare smarter and score higher in your Jharkhand Academic Council (JAC Board) examinations with JAC MCQ Master — the complete bilingual (Hindi & English) objective exam preparation app for Class 8, Class 9, Class 10, Class 11, and Class 12 students.

  KEY FEATURES:

  • Daily Live Challenge & Real-Time Top 100 Leaderboard
  Compete daily with students across Jharkhand in timed MCQ challenges. Track your live rank, accuracy, and completion speed on the real-time leaderboard that resets fresh every midnight (12:00 AM IST).

  • Chapter-Wise & Subject-Wise MCQ Practice
  Master every chapter of Mathematics, Science, Physics, Chemistry, Biology, Social Science, History, Geography, Economics, Hindi, English, and Computer Science with syllabus-aligned objective questions.

  • Full-Length Mock Tests & Previous Year Questions (PYQs)
  Simulate real board exam conditions with countdown timers, question palette navigation, mark-for-review, and instant OMR-style result analysis.

  • Exam Mode & Practice Mode
  Choose Practice Mode for instant answer verification and detailed explanations after every question, or Exam Mode for full timed test simulation.

  • Bilingual Support (English & हिन्दी) + Natural Voice Reader (TTS)
  Switch seamlessly between English and Hindi at any time during a test, or listen to questions and options read aloud with built-in Indian English and Hindi Text-to-Speech.

  • Instant Performance Analytics & Digital Certificate
  Review your score breakdown, accuracy percentage, time spent per question, and weak chapters. Download or share your personalized achievement certificate after completing tests.

  • Fast & Offline-Friendly Smart Caching
  Enjoy fast loading with automatic local caching for classes, subjects, chapters, and tests while keeping your daily leaderboard rankings 100% live.
  ```

---

## 3. Graphic Assets to Upload (`Main store listing → Graphics`)

1. **App Icon (512 × 512 px, 32-bit PNG):**
   * Upload `play_store_assets/app_icon_512x512.png`
2. **Feature Graphic (1024 × 500 px, PNG):**
   * Upload `play_store_assets/feature_graphic_1024x500.png`
3. **Phone Screenshots (2 to 8 screenshots, 9:16 or 16:9):**
   * Capture 4–6 screenshots from the running app:
     1. **Home Screen** (Dynamic Hero Banners, Daily Live Challenge card & Explore Tests grid)
     2. **Daily Live Leaderboard Screen** (Top 3 Podium + Top 100 Live Rankings)
     3. **Tests Hub / Chapter Selection Screen** (Subject & Chapter list)
     4. **Live MCQ Test Screen** (Bilingual EN/HI toggle, Voice TTS control & options)
     5. **Result & Analytics Screen** (Score breakdown, solutions & Certificate preview)

---

## 4. App Content & Policy Declarations (`Policy → App content`)

1. **Privacy Policy URL:**
   * Host `privacy_policy.html` (included in project root) on your Admin website, GitHub Pages, Vercel, or Netlify, and paste its public HTTPS link in Play Console → **Privacy policy**.
   * Also paste the same URL in your Admin Panel (`app_config` table → `privacy_policy_url` and `terms_conditions_url`).
2. **App Access:**
   * Select **"All functionality in my app is available without any access restrictions"** (Students can sign up with their Name & Class or browse directly without a paid login wall).
3. **Ads Declaration:**
   * Select **"No, my app does not contain ads"**.
4. **Content Rating (IARC Questionnaire):**
   * Category: **Reference, News, or Educational**
   * Violence / Sexuality / Gambling / User-to-User Chat: **No** (Produces **Rated for 3+ / Everyone**).
5. **Target Audience & Content:**
   * Target Age Group: **13–15, 16–17, 18 and over** (Class 8 to Class 12 students). Do NOT select "Under 13" unless you want strict Families Policy review.
6. **Data Safety Form:**
   * **Does your app collect or share any of the required user data types?** → **Yes**
   * **Data types collected:**
     * *Personal Info → Name* (Collected, Optional/Required for student profile & daily leaderboard display, Not shared with third parties)
     * *App activity / App info and performance* (Test scores and leaderboard attempts stored over HTTPS in Supabase)
   * **Is all of the user data collected by your app encrypted in transit?** → **Yes** (HTTPS/TLS via Supabase).
   * **Do you provide a way for users to request that their data is deleted?** → **Yes** (Users can reset/delete their local data in Profile/Settings or request deletion via the support email listed in `privacy_policy.html`, and Daily Leaderboard entries are automatically purged every midnight at 12:00 AM IST).
