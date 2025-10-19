// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    // ⭐️ پلاگین Serialization فقط یک بار با alias تعریف می‌شود (رفع خطای تکراری)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.oqba26.monthlypaymentapp"
    compileSdk = 34

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
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    buildTypes {
        debug {
            // ⭐️ استفاده از آدرس نهایی سرور برای Consistency
            buildConfigField("String", "API_BASE_URL", "\"http://167.235.136.65:8080/\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // ⭐️ استفاده از آدرس نهایی سرور برای Consistency
            buildConfigField("String", "API_BASE_URL", "\"http://167.235.136.65:8080/\"")
        }
    }

    compileOptions {
        // هم‌تراز با JDK 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    // اطمینان از JDK 17
    kotlin { jvmToolchain(17) }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
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
    implementation("androidx.compose.material:material-icons-extended-android:1.6.7")

    // Core & Coroutines
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Networking (Retrofit, OkHttp)
    implementation(libs.retrofit.core)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // ⭐️ Kotlinx Serialization - حذف وابستگی‌های Gson و تکراری
    implementation(libs.kotlin.serialization.json) // kotlinx.serialization.json
    // ⭐️ مبدل Retrofit برای Kotlinx Serialization (فقط این یکی لازم است)
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Database (Room)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Utility
    implementation(libs.persiandate)
    implementation(libs.google.material)

    // Desugaring
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)

    // Testing
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.androidTest.junit.ext)
    androidTestImplementation(libs.androidTest.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidTest.compose.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
}