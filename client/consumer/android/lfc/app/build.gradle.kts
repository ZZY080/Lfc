import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun loadLocalProperty(key: String): String? {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }
    return localProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
}

/** Retrofit requires a trailing slash on baseUrl. */
fun normalizeApiBaseUrl(url: String): String =
    if (url.endsWith("/")) url else "$url/"

fun resolveDeviceApiBaseUrl(): String {
    val fromLocal = loadLocalProperty("API_BASE_URL")
    val fromGradle = (project.findProperty("DEV_API_BASE_URL") as String?)?.trim()
    return normalizeApiBaseUrl(fromLocal ?: fromGradle ?: "https://www.dev.lfc.neptia.cn/api/")
}

android {
    namespace = "com.lfc.consumer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lfc.consumer"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val amapApiKey = (project.findProperty("AMAP_API_KEY") as String?)?.trim().orEmpty()
        buildConfigField("String", "AMAP_API_KEY", "\"$amapApiKey\"")
        manifestPlaceholders["AMAP_API_KEY"] = amapApiKey
    }

    flavorDimensions += "target"
    productFlavors {
        create("emulator") {
            dimension = "target"
            buildConfigField("String", "API_BASE_URL", "\"${resolveDeviceApiBaseUrl()}\"")
        }
        create("device") {
            dimension = "target"
            buildConfigField("String", "API_BASE_URL", "\"${resolveDeviceApiBaseUrl()}\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

/** 模拟器可选：adb reverse 让 127.0.0.1:8000 也能连宿主机（App 已优先用 10.0.2.2） */
tasks.register<Exec>("adbReverseDevServer") {
    group = "android"
    description = "adb reverse tcp:8000 tcp:8000 for emulator localhost fallback"
    commandLine("adb", "reverse", "tcp:8000", "tcp:8000")
    isIgnoreExitValue = true
}

tasks.matching { it.name.startsWith("install") && it.name.contains("Emulator", ignoreCase = true) }
    .configureEach { dependsOn("adbReverseDevServer") }

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Image & Coroutines & QR
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Alipay App Pay
    implementation("com.alipay.sdk:alipaysdk-android:15.8.17")

    // Amap location
    implementation("com.amap.api:location:6.4.9")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
