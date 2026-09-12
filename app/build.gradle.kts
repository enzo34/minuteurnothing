import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "fr.enzo.cachet45"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.enzo.cachet45"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Clé de signature partagée et versionnée : voir keystore/README.md.
    // Elle n'est pas secrète, elle sert uniquement à ce que chaque nouvelle
    // version s'installe par-dessus la précédente sans désinstaller.
    signingConfigs {
        create("partagee") {
            storeFile = rootProject.file("keystore/cachet45.jks")
            storePassword = "cachet45"
            keyAlias = "cachet45"
            keyPassword = "cachet45"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("partagee")
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("partagee")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Appli personnelle installée à la main : un avertissement de lint ne doit pas
    // empêcher de produire l'APK. `./gradlew lint` reste disponible à la demande.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
}
