plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val useChromiumKernel = providers.gradleProperty("useChromiumKernel")
    .map { it.equals("true", ignoreCase = true) }
    .getOrElse(false)

android {
    namespace = "com.anxin.browser"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anxin.browser"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("boolean", "USE_CHROMIUM_KERNEL", useChromiumKernel.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        getByName("main") {
            if (useChromiumKernel) {
                jniLibs.srcDirs("src/chromium/jniLibs")
                assets.srcDirs("src/chromium/assets")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    androidResources {
        // Chromium 通过 AAssetManager_openFileDescriptor 直接 mmap 资源
        // （base::android::OpenApkAsset → assets/icudtl.dat 等），该 API 只能
        // 读取「未压缩」条目。默认 AAPT2 会压缩 .dat/.pak，导致
        // icu_util.cc CHECK 失败、进程启动即崩溃，故显式声明不压缩。
        noCompress += listOf("dat", "pak", "bin")
    }
}

// ---------------------------------------------------------------------------
// Chromium 引擎 AAR 的 R 类补齐
//
// GN 的 dist_aar 只带 R.txt（不带 res/，也不带 R 类），Gradle 也不会替它生成
// org.chromium.*.R；而 Chromium 的 Java 层是直接引用这些 R 常量的。缺失时
// 启动即 NoClassDefFoundError: org/chromium/ui/R$integer。
//
// 做法：在 AGP 完成资源合并（processDebugResources）之后，读最终的 R.txt，
// 按包名把常量 R 生成为 Java 源码参与编译。int 常量会被 javac 内联成字面量，
// 运行时不产生任何查找开销。
// ---------------------------------------------------------------------------
val chromiumRPackages: Map<String, List<String>> = mapOf(
    "dev.anxin.engine.aw" to listOf("id", "layout"),
    "org.chromium.android_webview" to listOf("color", "dimen", "drawable", "id", "layout", "string"),
    "org.chromium.android_webview.devui" to
        listOf("color", "drawable", "id", "layout", "menu", "string", "style"),
    "org.chromium.components.autofill" to listOf("color", "dimen", "id", "layout", "string"),
    "org.chromium.components.embedder_support.delegate" to
        listOf("drawable", "id", "layout", "string"),
    "org.chromium.components.embedder_support.selection" to listOf("dimen"),
    "org.chromium.content" to
        listOf("array", "dimen", "drawable", "id", "layout", "string", "style"),
    "org.chromium.ui" to listOf(
        "attr", "color", "dimen", "drawable", "id", "integer",
        "layout", "string", "style", "styleable",
    ),
)

val chromiumRSrcDir = layout.buildDirectory.dir("generated/chromiumR")

val generateChromiumRClasses = tasks.register("generateChromiumRClasses") {
    val rTxt = layout.buildDirectory.file(
        "intermediates/runtime_symbol_list/debug/processDebugResources/R.txt",
    )
    // R.txt 由 AGP 的资源合并产出，必须先跑完才能读到最终资源 ID。
    dependsOn("processDebugResources")
    inputs.file(rTxt)
    outputs.dir(chromiumRSrcDir)

    doLast {
        val source = rTxt.get().asFile
        if (!source.isFile) {
            logger.lifecycle("generateChromiumRClasses: R.txt 未生成，跳过")
            return@doLast
        }

        // type -> [(name, id)]；styleable 单独存 {name -> "0x.., 0x.."}
        val flat = linkedMapOf<String, MutableList<Pair<String, String>>>()
        val styleableValues = linkedMapOf<String, String>()
        source.forEachLine { raw ->
            val line = raw.trim()
            if (line.startsWith("int[] ")) {
                val parts = line.removePrefix("int[] ").split(" ", limit = 3)
                if (parts.size == 3) {
                    styleableValues[parts[1]] =
                        parts[2].trim().removePrefix("{").removeSuffix("}").trim()
                }
            } else if (line.startsWith("int ")) {
                val parts = line.split(" ", limit = 4)
                if (parts.size == 4) {
                    flat.getOrPut(parts[1]) { mutableListOf() }
                        .add(parts[2] to parts[3])
                }
            }
        }
        // attr id -> 名称，用于给 styleable 生成 <Styleable>_<attr> 下标常量
        val attrNameById = flat["attr"]?.associate { (name, id) -> id to name } ?: emptyMap()

        val rootDir = chromiumRSrcDir.get().asFile
        rootDir.deleteRecursively()
        rootDir.mkdirs()

        var written = 0
        chromiumRPackages.forEach { (pkg, types) ->
            val sb = StringBuilder()
            sb.append("package ").append(pkg).append(";\n\n")
            sb.append("/** 由 generateChromiumRClasses 依最终资源表生成，勿手改。 */\n")
            sb.append("public final class R {\n")
            sb.append("    private R() {}\n")
            types.forEach { type ->
                if (type == "styleable") {
                    if (styleableValues.isEmpty()) return@forEach
                    sb.append("    public static final class styleable {\n")
                    styleableValues.forEach { (name, values) ->
                        sb.append("        public static final int[] ").append(name)
                        sb.append(" = { ").append(values).append(" };\n")
                        values.split(",").map { it.trim() }.forEachIndexed { idx, id ->
                            val attrName = attrNameById[id] ?: return@forEachIndexed
                            sb.append("        public static final int ").append(name)
                            sb.append('_').append(attrName).append(" = ").append(idx).append(";\n")
                        }
                    }
                    sb.append("    }\n")
                    return@forEach
                }
                val entries = flat[type] ?: return@forEach
                if (entries.isEmpty()) return@forEach
                sb.append("    public static final class ").append(type).append(" {\n")
                entries.forEach { (name, id) ->
                    sb.append("        public static final int ").append(name)
                    sb.append(" = ").append(id).append(";\n")
                }
                sb.append("    }\n")
            }
            sb.append("}\n")

            val pkgDir = File(rootDir, pkg.replace('.', '/'))
            pkgDir.mkdirs()
            File(pkgDir, "R.java").writeText(sb.toString())
            written++
        }
        logger.lifecycle("generateChromiumRClasses: 生成 $written 个包的 R.java")
    }
}

android.sourceSets.getByName("main").java.srcDir(chromiumRSrcDir)
// 生成的 R.java 位于 main 源集里，所有读取 java 源的任务都要排在生成之后
// （KSP 会扫描源集，不显式声明会触发 Gradle 的隐式依赖校验失败）。
tasks.matching {
    it.name == "kspDebugKotlin" ||
        it.name == "compileDebugKotlin" ||
        it.name == "compileDebugJavaWithJavac"
}.configureEach {
    dependsOn(generateChromiumRClasses)
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:browser"))

    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.documentfile:documentfile:1.0.1")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
