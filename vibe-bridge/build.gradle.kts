plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.vibe.bridge"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

repositories {
    google()
    mavenCentral()
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":vibe-common"))
    api(project(":vibe-engine-api"))
    api(project(":TMessagesProj"))
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.collection:collection-ktx:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
}
