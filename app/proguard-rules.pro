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


# 保持 Retrofit 相关类（防止 Retrofit 运行时找不到接口）
-keep class retrofit2.** { *; }

# 保持 OkHttp 相关类（防止 OkHttp 网络请求失败）
-keep class okhttp3.** { *; }

# 保持 Retrofit 请求方法的参数（防止 API 方法参数丢失）
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# 保留 Gson 反序列化的类
-keep class com.xxh.ringbones.data.** { *; }
-keep class com.xxh.ringbones.media3.** { *; }
-keepattributes *Annotation*

# 保留 Gson 相关类（防止 Gson 反射失败）
-keep class com.google.gson.** { *; }
-keep class common.reflect.TypeToken.**{ *; }

# 保留 Kotlin data class
-keep class com.xxh.ringbones.data.** { public <fields>; }


## 保留 Kotlin 类
#-keep class kotlin.Metadata { *; }
#
## Keep assets folder and its contents
#-keep class **.raw.** { *; }



