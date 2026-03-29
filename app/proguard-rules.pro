# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# 保留自定义View
-keep public class * extends android.view.View

# 保留 Room 相关类
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# 保留 Gson 相关
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.gson.**

# 保留 Glide 相关
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# 保留 MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# 保留 MaterialCalendarView
-keep class com.prolificinteractive.materialcalendarview.** { *; }
-dontwarn com.prolificinteractive.materialcalendarview.**

# 保留实体类（Room 需要）
-keep class com.memoria.meaningoflife.data.database.** { *; }

# 保留 ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# 保留 DataBinding 相关
-keep class com.memoria.meaningoflife.databinding.** { *; }

# 移除日志（正式版不输出日志）
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}