pluginManagement {
    repositories {
        // رسمی‌ها (بهتره اول باشند برای اطمینان و امنیت)
        gradlePluginPortal()
        google()
        mavenCentral()
        // آینهٔ مِیون‌سنترال (گوگل)
        maven("https://maven-central.storage-download.googleapis.com/maven2/")

        // آینه‌های Aliyun (برای مواقع تحریم/اختلال)
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // رسمی‌ها
        google()
        mavenCentral()
        // آینهٔ مِیون‌سنترال (گوگل)
        maven("https://maven-central.storage-download.googleapis.com/maven2/")

        // JitPack برای PersianDate
        maven("https://jitpack.io")

        // آینه‌های Aliyun
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
    }
}

rootProject.name = "MonthlyPaymentApp"
include(":app")