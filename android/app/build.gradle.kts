// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import groovy.json.JsonOutput
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Sync
import java.io.ByteArrayOutputStream
import java.io.File

fun resolvePomPath(gradleCacheRoot: File, group: String, name: String, version: String): String? {
    val versionDir = File(
        gradleCacheRoot,
        listOf(group, name, version).joinToString(File.separator),
    )
    if (!versionDir.exists()) {
        return null
    }

    return versionDir
        .walkTopDown()
        .maxDepth(2)
        .firstOrNull { candidate ->
            candidate.isFile && candidate.extension.equals("pom", ignoreCase = true)
        }
        ?.absolutePath
}

fun Project.readWindowsSmartAppControlState(): Int? {
    if (!System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        return null
    }

    val stdout = ByteArrayOutputStream()
    val result = exec {
        commandLine(
            "cmd",
            "/c",
            "reg",
            "query",
            "HKLM\\SYSTEM\\CurrentControlSet\\Control\\CI\\Policy",
            "/v",
            "VerifiedAndReputablePolicyState",
        )
        standardOutput = stdout
        errorOutput = stdout
        isIgnoreExitValue = true
    }

    if (result.exitValue != 0) {
        return null
    }

    val text = stdout.toString(Charsets.UTF_8.name())
    val raw = Regex("""VerifiedAndReputablePolicyState\s+REG_DWORD\s+([^\s]+)""")
        .find(text)
        ?.groupValues
        ?.getOrNull(1)
        ?: return null

    return raw.removePrefix("0x").toIntOrNull(16) ?: raw.toIntOrNull()
}

fun Project.runWindowsSmartAppControlProbe(repoRootDir: File): Pair<Int, String> {
    val stdout = ByteArrayOutputStream()
    val result = exec {
        workingDir = repoRootDir
        commandLine(
            "python",
            repoRootDir.resolve("backend/tools/rustc_host_wrapper.py").absolutePath,
            "--probe-smart-app-control",
        )
        standardOutput = stdout
        errorOutput = stdout
        isIgnoreExitValue = true
    }

    return result.exitValue to stdout.toString(Charsets.UTF_8.name()).trim()
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.mozilla.rust-android-gradle.rust-android")
}

val repoRootDir = rootProject.projectDir.parentFile
val gradleCacheRoot = File(gradle.gradleUserHomeDir, "caches/modules-2/files-2.1")
val androidRuntimeReportFile = layout.buildDirectory.file("generated/openSource/android-runtime-deps.json")
val openSourceAssetsDir = layout.buildDirectory.dir("generated/openSourceAssets")
val androidCargoTargetDir = File("D:/android-build/baidupcs-cargo-target")

android {
    namespace = "com.baidupcs.android"
    compileSdk = 34
    ndkVersion = "26.3.11579264"

    defaultConfig {
        applicationId = "com.baidupcs.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            pickFirsts += setOf(
                "/META-INF/AL2.0",
                "/META-INF/LGPL2.1",
            )
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(openSourceAssetsDir)
            jniLibs.srcDir(layout.buildDirectory.dir("generated/rustJniLibs"))
        }
    }
}

val exportReleaseRuntimeClasspathReport by tasks.registering {
    outputs.file(androidRuntimeReportFile)

    doLast {
        val artifacts = configurations
            .getByName("releaseRuntimeClasspath")
            .resolvedConfiguration
            .resolvedArtifacts
            .sortedWith(
                compareBy(
                    { it.moduleVersion.id.group },
                    { it.moduleVersion.id.name },
                    { it.moduleVersion.id.version },
                ),
            )

        val report = artifacts.mapNotNull { artifact ->
            val moduleId = artifact.moduleVersion.id
            if (moduleId.group.isBlank() || moduleId.name.isBlank() || moduleId.version.isBlank()) {
                return@mapNotNull null
            }

            mapOf(
                "group" to moduleId.group,
                "name" to moduleId.name,
                "version" to moduleId.version,
                "artifactPath" to artifact.file.absolutePath,
                "pomPath" to resolvePomPath(
                    gradleCacheRoot = gradleCacheRoot,
                    group = moduleId.group,
                    name = moduleId.name,
                    version = moduleId.version,
                ),
            )
        }

        val outputFile = androidRuntimeReportFile.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            JsonOutput.prettyPrint(JsonOutput.toJson(report)),
            Charsets.UTF_8,
        )
    }
}

val generateOpenSourceAssets by tasks.registering {
    dependsOn(exportReleaseRuntimeClasspathReport)

    inputs.file(repoRootDir.resolve("LICENSE"))
    inputs.file(repoRootDir.resolve("NOTICE.txt"))
    inputs.file(repoRootDir.resolve("backend/Cargo.toml"))
    inputs.file(repoRootDir.resolve("backend/Cargo.lock"))
    outputs.dir(openSourceAssetsDir)

    doLast {
        val outputDir = openSourceAssetsDir.get().asFile
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        exec {
            workingDir = repoRootDir
            commandLine(
                "python",
                repoRootDir.resolve("scripts/generate_open_source_assets.py").absolutePath,
                "--repo-root",
                repoRootDir.absolutePath,
                "--android-report",
                androidRuntimeReportFile.get().asFile.absolutePath,
                "--out-dir",
                outputDir.absolutePath,
            )
        }
    }
}

val syncRustJniLibs by tasks.registering(Sync::class) {
    dependsOn("cargoBuild")

    from(androidCargoTargetDir.resolve("aarch64-linux-android/release/libbaidu_netdisk_rust.so")) {
        into("arm64-v8a")
    }
    from(androidCargoTargetDir.resolve("x86_64-linux-android/release/libbaidu_netdisk_rust.so")) {
        into("x86_64")
    }

    into(layout.buildDirectory.dir("generated/rustJniLibs"))
}

val cleanPluginRustJniLibs by tasks.registering(Delete::class) {
    dependsOn(syncRustJniLibs)
    delete(layout.buildDirectory.dir("rustJniLibs"))
}

val patchLinkerWrapper by tasks.registering {
    dependsOn(":generateLinkerWrapper")

    doLast {
        val wrapper = rootProject.layout.buildDirectory.file("linker-wrapper/linker-wrapper.py").get().asFile
        if (!wrapper.exists()) {
            return@doLast
        }

        val original = wrapper.readText()
        val patched = original
            .replace("import pipes", "import shlex")
            .replace("pipes.quote", "shlex.quote")

        if (patched != original) {
            wrapper.writeText(patched)
        }
    }
}

val verifyWindowsRustBuildPolicy by tasks.registering {
    group = "verification"
    description = "Fails early only when Smart App Control is confirmed to block Cargo build scripts."

    doLast {
        val state = project.readWindowsSmartAppControlState() ?: return@doLast
        if (state == 1) {
            val (probeExit, probeOutput) = project.runWindowsSmartAppControlProbe(repoRootDir)
            if (probeExit == 2) {
                throw GradleException(
                    """
                    Windows Smart App Control is currently enforcing Verified and Reputable signing,
                    and the Cargo build-script probe was blocked on this machine.

                    Effective ways to unblock:
                    1. Turn off Smart App Control in Windows Security and reboot.
                       Note: Windows may require a reinstall/reset to turn it back on later.
                    2. Build on another machine / VM / WSL environment where Smart App Control does not block
                       local Cargo build scripts.

                    Probe output:
                    ${probeOutput.ifBlank { "No probe details were captured." }}
                    """.trimIndent(),
                )
            }

            logger.warn(
                "Smart App Control is ON, but the Cargo build-script probe did not reproduce a block. " +
                    "Continuing with the Android Rust build.\n{}",
                probeOutput.ifBlank { "No probe details were captured." },
            )
        }
    }
}

patchLinkerWrapper.configure {
    mustRunAfter(verifyWindowsRustBuildPolicy)
}

gradle.allprojects {
    tasks.matching { it.name == "generateLinkerWrapper" }.configureEach {
        mustRunAfter(verifyWindowsRustBuildPolicy)
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateOpenSourceAssets)
    dependsOn(syncRustJniLibs)
}

tasks.configureEach {
    if (name.startsWith("cargoBuild")) {
        dependsOn(verifyWindowsRustBuildPolicy)
        dependsOn(patchLinkerWrapper)
    }
}

val rustJniMergeTasks = setOf(
    "mergeDebugJniLibFolders",
    "mergeReleaseJniLibFolders",
)

tasks.configureEach {
    if (name in rustJniMergeTasks) {
        dependsOn(cleanPluginRustJniLibs)
    }
}

cargo {
    module = "../../backend"
    libname = "baidu_netdisk_rust"
    targets = listOf("arm64", "x86_64")
    targetDirectory = androidCargoTargetDir.absolutePath
    targetIncludes = arrayOf("libbaidu_netdisk_rust.so")
    extraCargoBuildArguments = listOf("--lib")
    profile = "release"
    apiLevel = 26
    verbose = true
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.google.android.material:material:1.12.0")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
