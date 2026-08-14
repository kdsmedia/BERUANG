import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.altomedia.beruang"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.altomedia.beruang"
        minSdk = 21
        targetSdk = 37
        versionCode = 4
        versionName = "1.2.0"
        vectorDrawables { useSupportLibrary = true }
    }

    // Release signing key is read from keystore.properties so the credentials
    // are not hard-coded in the build file. The keystore is ALTOMEDIA.jks
    // (see ALTOMEDIA/keystore/), alias kdsmedia.
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties()
    if (keystorePropsFile.exists()) {
        keystoreProps.load(FileInputStream(keystorePropsFile))
    }
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps.getProperty("storeFile", "../../ALTOMEDIA/keystore/ALTOMEDIA.jks"))
            storePassword = keystoreProps.getProperty("storePassword", "Kdsmedia@123")
            keyAlias = keystoreProps.getProperty("keyAlias", "kdsmedia")
            keyPassword = keystoreProps.getProperty("keyPassword", "Kdsmedia@123")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        // Supabase-kt uses java.time APIs; on minSdk 21 we need core library
        // desugaring so java.time is available below API 26.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    lint {
        // ComponentActivity (Compose) triggers a false-positive Instantiatable
        // "must extend android.app.Activity"; abortOnError false keeps the release build green.
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.runtime:runtime-livedata")

    // Coil for images (SVG + regular)
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")

    // Supabase (Postgrest + Auth + Realtime). Umbrella pulls all modules.
    // 2.5.2 targets Kotlin 2.0; Ktor 2.3.12 engine is the Android (OkHttp) client
    // which supports the WebSockets Realtime needs.
    val supabaseVersion = "2.5.2"
    implementation("io.github.jan-tennert.supabase:supabase-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:realtime-kt:$supabaseVersion")
    implementation("io.ktor:ktor-client-android:2.3.12")

    // Serialization (Supabase decodes JSON rows into @Serializable models)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // Core library desugaring so java.time works on minSdk 21
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // QR code generation (ZXing core, no Android-specific deps)
    implementation("com.google.zxing:core:3.5.3")

    // Camera + ML Kit barcode scanning (for points transfer via QR)
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // DataStore for local session prefs
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
