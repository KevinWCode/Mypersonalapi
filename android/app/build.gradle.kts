plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kevinwcode.personalapi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kevinwcode.personalapi"
        minSdk = 29
        targetSdk = 35
        versionCode = 21
        versionName = "0.21"

        // Rework branch: a separate app so it installs next to the 0.21 app instead of replacing it.
        // Its own package means its own data, settings and Health Connect permissions.
        // Remove these lines (and the suffix) if the rework is ever meant to replace the main app.
        applicationIdSuffix = ".rework"
        versionNameSuffix = "-rework"
        manifestPlaceholders["appLabel"] = "Personal API Rework"
        manifestPlaceholders["appIcon"] = "@android:drawable/ic_menu_edit"
    }

    // The dashboard page is the PC server's own file, bundled so the app can show it without the server.
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/dashboard"))

    signingConfigs {
        // A fixed key so each new APK installs over the last one instead of needing an uninstall.
        // Only for sideloading onto Kevin's own phone; this app is never published.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

val copyDashboard = tasks.register<Copy>("copyDashboard") {
    from(rootProject.file("../app/static/dashboard.html"))
    into(layout.buildDirectory.dir("generated/dashboard"))
}
tasks.named("preBuild") { dependsOn(copyDashboard) }

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.health.connect:connect-client:1.1.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
