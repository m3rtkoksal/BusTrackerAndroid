plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun readMapsApiKey(): String {
    localProperties.getProperty("MAPS_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val googleServices = rootProject.file("app/google-services.json")
    if (!googleServices.exists()) return ""
    val match = Regex(""""current_key"\s*:\s*"([^"]+)"""").find(googleServices.readText())
    return match?.groupValues?.get(1).orEmpty()
}

val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.mikatechnology.BusTracker"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mikatechnology.BusTracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val mapsApiKey = readMapsApiKey()
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                val storeFilePath = keystoreProperties.getProperty("storeFile")
                    ?: error("keystore.properties: storeFile eksik")
                storeFile = rootProject.file(storeFilePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                    ?: keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias") ?: "key0"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystoreProperties.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

if (keystoreProperties.isEmpty()) {
    tasks.matching { it.name == "bundleRelease" }.configureEach {
        doFirst {
            throw GradleException(
                """
                Release AAB imzalanamaz: keystore.properties eksik.
                1) cp keystore.properties.example keystore.properties
                2) storePassword ve keyPassword doldurun (Untitled.jks / alias bustracker)
                3) ./gradlew bundleRelease
                Alternatif: Android Studio → Build → Generate Signed App Bundle
                """.trimIndent()
            )
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}