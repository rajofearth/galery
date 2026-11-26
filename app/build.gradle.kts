plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "rs.rajofearth.galery"
    compileSdk = 37

    defaultConfig {
        applicationId = "rs.rajofearth.galery"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            optimization { enable = true }
            proguardFiles("proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs.useLegacyPackaging = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-video:3.6.3")

    val media3Version = "1.9.0"
    implementation("androidx.media3:media3-exoplayer:$media3Version")

    testImplementation("junit:junit:4.13.2")
}
