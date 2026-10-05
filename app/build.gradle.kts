plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.mergedgames"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.example.mergedgames"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            // Phones sold in the last several years are arm64. Add "armeabi-v7a" / "x86_64" if needed.
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_shared")
                cppFlags += listOf("-std=c++17")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets["main"].java.srcDir("../third_party/SDL2/android-project/app/src/main/java")
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/zeldaAssets"))

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

// Copies the OpenZelda content package into the APK's assets under "openzelda/".
val syncZeldaContent by tasks.registering(Copy::class) {
    from("../third_party/openzelda-content")
    into(layout.buildDirectory.dir("generated/zeldaAssets/openzelda"))
}
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(syncZeldaContent)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
