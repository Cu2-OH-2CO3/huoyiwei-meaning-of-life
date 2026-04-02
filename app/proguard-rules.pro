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

# 保留泛型签名，这对于Gson的TypeToken解析至关重要
-keepattributes Signature

# 保留Gson的TypeToken类及其内部处理逻辑
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# 如果你有自定义的、需要被Gson解析的Bean类，也要保留它们的完整构造（可选，视情况而定）
# -keep class com.memoria.meaningoflife.model.** { *; }

# 移除日志（正式版不输出日志）
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
# 保持彩蛋游戏相关类不被混淆
-keep class com.memoria.meaningoflife.ui.settings.DinoGameActivity { *; }
-keep class com.memoria.meaningoflife.ui.settings.DinoGameActivity$* { *; }

# 保持内部类
-keepattributes InnerClasses
-keepclassmembers class com.memoria.meaningoflife.ui.settings.DinoGameActivity$* {
    *;
}

# 保持所有 View 子类
-keep public class * extends android.view.View

# 保持 BitmapFactory 相关
-keepclassmembers class android.graphics.BitmapFactory {
    public static android.graphics.Bitmap decodeResource(android.content.res.Resources, int);
}

# 保持随机数相关
-keep class kotlin.random.Random { *; }
-keep class kotlin.random.Random$* { *; }

# 保持协程相关
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# 保持所有 Paint 相关
-keep class android.graphics.Paint { *; }
-keep class android.graphics.Rect { *; }
-keep class android.graphics.RectF { *; }
-keep class android.graphics.Canvas { *; }

# 保持动画相关
-keep class android.animation.ValueAnimator { *; }
-keep class android.animation.Animator$AnimatorListener { *; }