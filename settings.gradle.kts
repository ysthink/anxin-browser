// CI（GitHub Actions）在海外，直连官方源；本地走阿里云镜像加速。
// GitHub Actions 自动注入 CI=true。注意 pluginManagement 是独立编译块，
// 不能引用外部变量，判断需写在块内。
pluginManagement {
    val inCi = System.getenv("CI") == "true"
    repositories {
        if (inCi) {
            google()
            mavenCentral()
            gradlePluginPortal()
        } else {
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    val inCi = System.getenv("CI") == "true"
    repositories {
        if (inCi) {
            google()
            mavenCentral()
        } else {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
            google()
            mavenCentral()
        }
    }
}

rootProject.name = "anxin-browser"

include(":app")
include(":core:design")
include(":core:browser")
