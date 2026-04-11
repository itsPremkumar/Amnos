# Keep local script assets accessible after shrinking.
-keepclassmembers class **.R$raw { *; }

# ============================================================================
# AMNOS SECURITY - ProGuard Rules for Code Protection
# ============================================================================

# Keep application class
-keep class com.privacy.browser.** { *; }

# Obfuscation settings
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin specific
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Compose specific
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# OkHttp specific
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# WebView specific
-keep class android.webkit.** { *; }
-keep class androidx.webkit.** { *; }

# Keep BuildConfig
-keep class com.privacy.browser.BuildConfig { *; }

# Anti-tampering: Keep security-critical classes from being renamed
-keep class com.privacy.browser.core.security.** { *; }
-keep class com.privacy.browser.core.fingerprint.** { *; }
-keep class com.privacy.browser.core.network.NetworkSecurityManager { *; }

# Remove debug information
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Optimize and obfuscate
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
