// SPDX-License-Identifier: Apache-2.0

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

repositories {
    google()
    mavenCentral()
}

android {
    compileSdk = 34
    namespace = "com.hedera.android_example"

    defaultConfig {
        applicationId = "com.hedera.android_example"
        minSdk = 26
        targetSdk =  34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles.add(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
}

dependencies {
    // --- Remove to use a published SDK version ---
    implementation(platform("org.hiero:hiero-dependency-versions"))
    // ---------------------------------------------

    implementation("com.hedera.hashgraph:sdk:2.58.0")

    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.appcompat:appcompat")
    implementation("androidx.constraintlayout:constraintlayout")
    implementation("androidx.coordinatorlayout:coordinatorlayout")
    implementation("androidx.fragment:fragment")
    implementation("androidx.lifecycle:lifecycle-common")
    implementation("androidx.lifecycle:lifecycle-viewmodel")
    implementation("androidx.recyclerview:recyclerview")
    implementation("androidx.viewpager2:viewpager2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    runtimeOnly("io.grpc:grpc-okhttp")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
}
