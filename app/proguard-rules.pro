# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve stack trace line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- Hilt ----------
# Keep Hilt-generated component classes and injection entry points
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <init>(...);
}

# ---------- Room ----------
# Keep Room entities (annotated data classes mapped to DB tables)
-keep @androidx.room.Entity class * { *; }
# Keep DAO interfaces and their implementations
-keep @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
# Keep Room's generated _Impl classes
-keep class **_Impl { *; }
-keep class **_Impl$* { *; }

# ---------- DataStore ----------
-keep class androidx.datastore.** { *; }
-keep class androidx.datastore.preferences.** { *; }

# ---------- Firebase / Gemini ----------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**

# ---------- Kotlin ----------
# Keep Kotlin metadata (required for reflection and coroutines)
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlin.**

# ---------- Coroutines ----------
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---------- Coil ----------
-keep class coil.** { *; }
-dontwarn coil.**

# ---------- Timber ----------
-dontwarn org.slf4j.**
