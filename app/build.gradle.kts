// app/build.gradle.kts  — FULL REPLACEMENT FILE
// The key fix: localProperties uses Kotlin DSL syntax, not Groovy.
// Also added: buildFeatures { buildConfig = true } to enable BuildConfig fields.

import java.util.Properties
import java.io.FileInputStream

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.error404.communityvolunteerplatform"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.error404.communityvolunteerplatform"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Cloudinary credentials injected from local.properties
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME",
            "\"${localProperties["CLOUDINARY_CLOUD_NAME"]}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY",
            "\"${localProperties["CLOUDINARY_API_KEY"]}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET",
            "\"${localProperties["CLOUDINARY_API_SECRET"]}\"")

        // Pass these to AndroidManifest.xml as well
        manifestPlaceholders["CLOUDINARY_CLOUD_NAME"] = localProperties["CLOUDINARY_CLOUD_NAME"] ?: ""
        manifestPlaceholders["CLOUDINARY_API_KEY"] = localProperties["CLOUDINARY_API_KEY"] ?: ""
        manifestPlaceholders["CLOUDINARY_API_SECRET"] = localProperties["CLOUDINARY_API_SECRET"] ?: ""
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true   // ← REQUIRED for BuildConfig.CLOUDINARY_* to work
    }

    packaging {
        resources {
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("com.cloudinary:cloudinary-android:2.3.1")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Glide — needed by PassportActivity for profile photo loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Play Services Location for Edit Profile
    implementation("com.google.android.gms:play-services-location:21.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}