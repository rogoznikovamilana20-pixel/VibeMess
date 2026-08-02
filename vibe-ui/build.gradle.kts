plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.vibe.ui"
    compileSdk = 35

    val aiApiKey = rootProject.file("local.properties").takeIf { it.exists() }?.let { f ->
        f.readLines().firstOrNull { it.startsWith("AI_API_KEY=") }?.substringAfter("=")?.trim()?.trim('"')
    } ?: ""

    val supabaseUrl = rootProject.file("local.properties").takeIf { it.exists() }?.let { f ->
        f.readLines().firstOrNull { it.startsWith("SUPABASE_URL=") }?.substringAfter("=")?.trim()?.trim('"')
    } ?: ""
    val supabaseAnonKey = rootProject.file("local.properties").takeIf { it.exists() }?.let { f ->
        f.readLines().firstOrNull { it.startsWith("SUPABASE_ANON_KEY=") }?.substringAfter("=")?.trim()?.trim('"')
    } ?: ""

    val rustServerUrl = rootProject.file("local.properties").takeIf { it.exists() }?.let { f ->
        f.readLines().firstOrNull { it.startsWith("RUST_SERVER_URL=") }?.substringAfter("=")?.trim()?.trim('"')
    } ?: ""

    val rustWsUrl = rootProject.file("local.properties").takeIf { it.exists() }?.let { f ->
        f.readLines().firstOrNull { it.startsWith("RUST_WS_URL=") }?.substringAfter("=")?.trim()?.trim('"')
    } ?: ""

    defaultConfig {
        minSdk = 21
        buildConfigField("String", "AI_API_KEY", "\"$aiApiKey\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "RUST_SERVER_URL", "\"$rustServerUrl\"")
        buildConfigField("String", "RUST_WS_URL", "\"$rustWsUrl\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        baseline = file("lint-baseline.xml")
        disable += "UseTomlInstead"
    }

    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")
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
    implementation(project(":vibe-bridge"))
    
    // AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.animation:animation")
    
    // Accompanist
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
    
    // Material Design Components (for FloatingActionButton etc.)
    implementation("com.google.android.material:material:1.11.0")
    
    // Lottie animations (Vaybik)
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    
    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // Paging 3
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")

    // MQTT signaling for WebRTC
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // Supabase Realtime (WebSocket)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Coil image loading for avatars
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Firebase Crashlytics
    implementation("com.google.firebase:firebase-crashlytics:18.6.4")
    implementation("com.google.firebase:firebase-analytics:21.5.1")
    
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.json:json:20231013")
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
}
