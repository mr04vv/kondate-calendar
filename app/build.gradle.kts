plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.github.mr04vv.kondatecalendar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.mr04vv.kondatecalendar"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("kondate.versionCode").get().toInt()
        versionName = providers.gradleProperty("kondate.versionName").get()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.activity.compose)
}
