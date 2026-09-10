# Semen Analysis Assist

تطبيق أندرويد (Kotlin + Jetpack Compose + CameraX) لتحليل مبدئي للتركيز والحركة في عينة سائل منوي عبر تصوير فيديو من خلال عدسة ميكروسكوب.

## ⚠️ تنبيه طبي

هذه **أداة بحثية مساعدة وليست جهازًا طبيًا معتمدًا**. النتائج تحتاج مراجعة فنية بشرية دائمًا.

## بناء الـ APK عبر GitHub Actions

1. ادخل تبويب **Actions**
2. اختر workflow **Build Debug APK**
3. بعد النجاح، حمّل الـ Artifact باسم `semen-analysis-assist-debug-apk`

يمكنك أيضًا تشغيل البناء يدويًا من Actions → Run workflow.

## السيرفر (Backend)

يحتاج التطبيق إلى سيرفر FastAPI. عدّل `BACKEND_BASE_URL` في `app/build.gradle.kts` أو مرّره كـ `-PbackendUrl=...` وقت البناء.
