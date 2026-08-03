plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.vibe.common"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.all {
            (it as? org.gradle.process.JavaForkOptions)?.jvmArgs(
                "--add-opens", "org.bouncycastle.pqc.crypto.mlkem/org.bouncycastle.pqc.crypto.mlkem=ALL-UNNAMED"
            )
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    
    // Post-quantum cryptography (ML-KEM-768 + ML-DSA-65)
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.79")
    
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
