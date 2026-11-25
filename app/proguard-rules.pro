# 保持 JS 注入接口
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
