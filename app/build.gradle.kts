// app/build.gradle.kts

import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.oqba26.monthlypaymentapp"
    compileSdk = 34

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    fun getProp(name: String): String? {
        return System.getenv(name) ?: project.findProperty(name) as? String ?: (keystoreProperties[name] as? String)
    }

    defaultConfig {
        applicationId = "com.oqba26.monthlypaymentapp"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val storeFileProp = getProp("RELEASE_STORE_FILE")
            if (storeFileProp != null) {
                storeFile = file(storeFileProp)
                storePassword = getProp("RELEASE_STORE_PASSWORD")
                keyAlias = getProp("RELEASE_KEY_ALIAS")
                keyPassword = getProp("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "SUPABASE_URL", "\"https://ftufsygeartwukonclkz.supabase.co\"")
            buildConfigField("String", "SUPABASE_KEY", "\"sb_publishable_H4mAU1Ds-vZ22HwMSfCaBQ_QjBfmRd5\"")
        }
        release {
            val storeFileProp = getProp("RELEASE_STORE_FILE")
            if (storeFileProp != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "SUPABASE_URL", "\"https://ftufsygeartwukonclkz.supabase.co\"")
            buildConfigField("String", "SUPABASE_KEY", "\"sb_publishable_H4mAU1Ds-vZ22HwMSfCaBQ_QjBfmRd5\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "21" }
    kotlin { jvmToolchain(21) }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    // اسکیمای تولیدشده در app/schemas/ ذخیره و کامیت می‌شود.
    // MigrationTest برای بازسازی دیتابیس نسخه‌ی قبل به همین فایل‌ها نیاز دارد.
    sourceSets.getByName("androidTest") {
        assets.srcDirs("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended-android:1.6.7")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Core & Coroutines
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlin.serialization.json)
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Database (Room)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Utility
    implementation(project(":PersianDate"))
    implementation(libs.google.material)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Desugaring
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)

    // Testing
    testImplementation(libs.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidTest.junit.ext)
    androidTestImplementation(libs.androidTest.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidTest.compose.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
}