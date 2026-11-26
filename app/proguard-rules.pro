# 保持 JS 注入接口
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ---------------------------------------------------------------------------
# 内核反射入口
# AppGraph 用 Class.forName("...ChromiumEngine") + create(Context) 反射选内核，
# R8 看不到引用，必须显式保留，否则内核版启动直接 ClassNotFoundException。
# ---------------------------------------------------------------------------
-keep class com.anxin.browser.core.engine.ChromiumEngine {
    public static *** create(...);
}
-keep class com.anxin.browser.core.engine.ChromiumEngineHooks { *; }

# 内核 Java 层与 JNI 生成类：native 绑定按类名/方法名解析，一律不参与混淆
-keep class dev.anxin.engine.aw.** { *; }
-keep class org.chromium.** { *; }
-keep class org.jni_zero.** { *; }
-keep class J.N { *; }

# Chromium 依赖的可选类在裁剪后可能缺失，只降噪不阻断
-dontwarn org.chromium.**
-dontwarn dev.anxin.engine.**
-dontwarn org.jni_zero.**

# Room 生成的实现类由注解处理器产出，保留构造函数
-keep class * extends androidx.room.RoomDatabase { <init>(); }
