import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// Signing release — keystore/password dibaca dari local.properties (tidak pernah di-commit).
// Properti yang diharapkan:
//   RELEASE_STORE_FILE      (relatif ke root project, cth: lapakhunian.keystore)
//   RELEASE_STORE_PASSWORD
//   RELEASE_KEY_ALIAS
//   RELEASE_KEY_PASSWORD
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("local.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.lapakhunian.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lapakhunian.app"
        minSdk = 24
        targetSdk = 35
        // CI release: -PappVersionName=<dari tag> -PappVersionCode=<jumlah tag v*>
        // tanpa properti → nilai default (build lokal).
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "1.0.0-beta.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // API base URL (prod) — dev override 10.0.2.2:3000 di buildType debug bila perlu
        buildConfigField("String", "API_BASE_URL", "\"https://lapakhunian.com\"")

        // Google Sign-In — Web OAuth Client ID (serverClientId untuk Credential Manager).
        // Diisi dari gradle.properties → GOOGLE_WEB_CLIENT_ID (bukan secret, tapi jangan hardcode).
        val googleWebClientId = (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?) ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    signingConfigs {
        create("release") {
            val storeFileProp = keystoreProperties.getProperty("RELEASE_STORE_FILE")
            val storePass = keystoreProperties.getProperty("RELEASE_STORE_PASSWORD")
            if (storeFileProp != null && storePass != null && rootProject.file(storeFileProp).exists()) {
                storeFile = rootProject.file(storeFileProp)
                storePassword = storePass
                keyAlias = keystoreProperties.getProperty("RELEASE_KEY_ALIAS") ?: "lapakhunian"
                keyPassword = keystoreProperties.getProperty("RELEASE_KEY_PASSWORD") ?: storePass
            }
        }
    }

    buildTypes {
        debug {
            // Default debug = produksi (Coolify: lapakhunian.com) — server lokal sering
            // tidak jalan sehingga semua request gagal. Untuk dev lokal override:
            //   ./gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:3000
            val debugApiUrl = (project.findProperty("apiBaseUrl") as String?) ?: "https://lapakhunian.com"
            buildConfigField("String", "API_BASE_URL", "\"$debugApiUrl\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signed hanya bila keystore tersedia di local.properties; tanpa itu → unsigned
            val hasSigning = keystoreProperties.getProperty("RELEASE_STORE_FILE")?.let { rootProject.file(it).exists() } == true
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    // Nama file APK tetap di semua rilis — release → lapakhunian-beta.apk
    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "lapakhunian-beta.apk"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time (OffsetDateTime dsb) aman untuk minSdk 24
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.text.google.fonts)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.work.runtime)

    // FCM push notification (config: app/google-services.json dari Firebase)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.core.splashscreen)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)
    implementation(libs.androidx.lifecycle.process)

    coreLibraryDesugaring(libs.desugar.jdk)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
