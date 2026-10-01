# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep WebView and JavaScript interfaces (CRITICAL for Crisp Chat in Release builds)
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**

# Keep SupportWebActivity and SupportChatManager
-keep class com.example.ui.activities.SupportWebActivity { *; }
-keepclassmembers class com.example.ui.activities.SupportWebActivity { *; }
-keep class com.example.util.SupportChatManager { *; }
-keepclassmembers class com.example.util.SupportChatManager { *; }

# Keep Data Models and JSON reflection
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.repository.** { *; }
-keepclassmembers class com.example.data.repository.** { *; }
-keep class org.json.** { *; }

# Firebase Realtime Database & Common
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.firebase.database.** { *; }
-keepclassmembers class com.google.firebase.database.** { *; }

# Kotlin Coroutines and Flow
-keep class kotlinx.coroutines.** { *; }
