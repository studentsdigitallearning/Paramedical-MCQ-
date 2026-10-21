# JAC MCQ Master - Production R8 / ProGuard Configuration
# Preservation rules for data models, Supabase SDK, OkHttp, and Coroutines

# 1. Keep all application data models and serializable classes
-keep class com.jacboard.mcqmaster.model.** { *; }
-keepclassmembers class com.jacboard.mcqmaster.model.** { *; }

# 2. Keep Supabase client and config
-keep class com.jacboard.mcqmaster.data.supabase.** { *; }
-keepclassmembers class com.jacboard.mcqmaster.data.supabase.** { *; }
-keep class io.github.jan.supabase.** { *; }
-keepclassmembers class io.github.jan.supabase.** { *; }

# 3. Moshi & Kotlin Serialization rules
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# 4. OkHttp & Ktor rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# 5. Coroutines rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# 6. Keep utility & ZXing QR generator classes
-keep class com.jacboard.mcqmaster.util.** { *; }
-dontwarn com.google.zxing.**

# 7. Preserve Line Numbers for Crash Reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
