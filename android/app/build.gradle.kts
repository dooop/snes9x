plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

base { archivesName.set("snes9x") }

ktlint {
    android.set(true)
    outputToConsole.set(true)
}

val releaseAar =
    providers.gradleProperty("snes9x.releaseAar").orElse(
        layout.projectDirectory
            .file("libs/snes9x-release.aar")
            .asFile.absolutePath,
    )

val mavenVersion = providers.gradleProperty("snes9x.mavenVersion").getOrElse("0.0.0")

val configuredAbis =
    providers
        .gradleProperty("snes9x.abis")
        .getOrElse("arm64-v8a,x86_64")
        .split(",")
        .map(String::trim)
        .filter(String::isNotEmpty)

android {
    namespace = "com.snes9x.app"
    compileSdk { version = release(37) }
    enableKotlin = true

    defaultConfig {
        applicationId = "com.snes9x"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters += configuredAbis }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    flavorDimensions += "engineSource"
    productFlavors {
        create("local") {
            dimension = "engineSource"
            buildConfigField("String", "ENGINE_SOURCE", "\"local\"")
        }
        create("maven") {
            dimension = "engineSource"
            versionNameSuffix = "-maven"
            buildConfigField("String", "ENGINE_SOURCE", "\"maven:io.github.dooop:snes9x:$mavenVersion\"")
        }
    }
    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = true
        // x86_64 is enabled by default; lint cannot resolve the configurable ABI provider.
        disable += "ChromeOsAbiSupport"
    }
}

dependencies {
    //noinspection UseTomlInstead -- the version is intentionally supplied as a release property.
    "mavenImplementation"("io.github.dooop:snes9x:$mavenVersion")
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}

afterEvaluate {
    dependencies {
        "localDebugImplementation"(project(":snes9x"))
        "localReleaseImplementation"(files(releaseAar))
    }
}

val verifyReleaseAar =
    tasks.register("verifyReleaseAar") {
        group = "verification"
        description = "Checks that the prebuilt snes9x AAR exists."
        doLast {
            val aar = file(releaseAar.get())
            require(aar.isFile) {
                "The local release build requires a prebuilt snes9x AAR at ${aar.path}. " +
                    "Pass -Psnes9x.releaseAar=/absolute/path/to/snes9x-release.aar."
            }
        }
    }

tasks.configureEach {
    if (name == "preLocalReleaseBuild") dependsOn(verifyReleaseAar)
}
