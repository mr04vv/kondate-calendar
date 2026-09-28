import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// The issue token stays out of git: put `github.issueToken=...` in local.properties.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

// Build-time constants for common code, which has no BuildConfig.
val generateBuildInfo by tasks.registering {
    val values = mapOf(
        "GITHUB_REPO" to "mr04vv/kondate-calendar",
        "GITHUB_ISSUE_TOKEN" to localProperties.getProperty("github.issueToken", ""),
        "VERSION_NAME" to providers.gradleProperty("kondate.versionName").get(),
        "VERSION_CODE" to providers.gradleProperty("kondate.versionCode").get(),
    )
    val outputDir = layout.buildDirectory.dir("generated/buildInfo")
    inputs.properties(values)
    outputs.dir(outputDir)
    doLast {
        val constants = values.entries.joinToString("\n") { (name, value) -> "    const val $name = \"$value\"" }
        outputDir.get().file("BuildInfo.kt").asFile.writeText(
            "package com.github.mr04vv.kondatecalendar\n\ninternal object BuildInfo {\n$constants\n}\n",
        )
    }
}

kotlin {
    compilerOptions {
        // Room's generated database constructor is an expect/actual object.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.github.mr04vv.kondatecalendar.shared"
        compileSdk = 36
        minSdk = 26
        androidResources.enable = true
        withHostTest {}
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildInfo)
            dependencies {
                api(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons)
                implementation(libs.compose.resources)
                api(libs.lifecycle.viewmodel.compose)
                implementation(libs.lifecycle.runtime.compose)
                api(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(libs.serialization.json)
                implementation(libs.datetime)
                implementation(libs.ktor.client.core)
            }
        }
        androidMain {
            // KSP does not register its Android output with the KMP Android library plugin on AGP 8.x.
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/android/androidMain/kotlin"))
        }
        androidMain.dependencies {
            implementation(libs.activity.compose)
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
        }
    }
}

compose.resources {
    packageOfResClass = "com.github.mr04vv.kondatecalendar.resources"
}

tasks.matching { it.name == "compileAndroidMain" }.configureEach { dependsOn("kspAndroidMain") }

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { add(it, libs.room.compiler) }
}
