plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// 可选内核：-PuseChromiumKernel=true 时编译 ChromiumEngine 并引入 GN 产出的 AAR。
val useChromiumKernel = providers.gradleProperty("useChromiumKernel")
    .map { it.equals("true", ignoreCase = true) }
    .getOrElse(false)

android {
    namespace = "com.anxin.browser.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    sourceSets {
        getByName("main") {
            if (useChromiumKernel) java.srcDir("src/chromium/java")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    if (useChromiumKernel) {
        implementation(files(rootProject.file("libs/anxin_engine_aw.aar")))
    }
}
