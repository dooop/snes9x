plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    `maven-publish`
}

group = "io.github.dooop"
version = providers.gradleProperty("snes.version").getOrElse("0.0.0-SNAPSHOT")

ktlint {
    android.set(true)
    outputToConsole.set(true)
}

val configuredAbis =
    providers
        .gradleProperty("snes.abis")
        .getOrElse("arm64-v8a,x86_64")
        .split(",")
        .map(String::trim)
        .filter(String::isNotEmpty)

android {
    namespace = "snes9x"
    compileSdk { version = release(37) }
    enableKotlin = true
    ndkVersion = providers.gradleProperty("snes.ndkVersion").get()

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
        ndk { abiFilters += configuredAbis }
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-fexceptions", "-frtti")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = true
        // x86_64 is enabled by default; lint cannot resolve the configurable ABI provider.
        disable += "ChromeOsAbiSupport"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "snes"
                pom {
                    name.set("snes9x")
                    description.set("SwiftUI and Android Compose wrappers around Snes9x")
                    url.set("https://github.com/dooop/snes")
                    licenses {
                        license {
                            name.set("Snes9x License (non-commercial)")
                            url.set("https://github.com/snes9xgit/snes9x/blob/master/LICENSE")
                            distribution.set("repo")
                        }
                    }
                    scm {
                        url.set("https://github.com/dooop/snes")
                        connection.set("scm:git:https://github.com/dooop/snes.git")
                        developerConnection.set("scm:git:ssh://git@github.com/dooop/snes.git")
                    }
                }
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url =
                    uri(
                        "https://maven.pkg.github.com/${providers.gradleProperty(
                            "snes.githubRepository",
                        ).getOrElse("dooop/snes")}",
                    )
                credentials {
                    username =
                        providers
                            .gradleProperty("gpr.user")
                            .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                            .orNull
                    password =
                        providers
                            .gradleProperty("gpr.key")
                            .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                            .orNull
                }
            }
        }
    }
}
