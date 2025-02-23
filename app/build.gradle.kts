import org.gradle.internal.declarativedsl.parsing.main

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.xxh.ringbones"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xxh.ringbones"
        minSdk = 24
        targetSdk = 35
        versionCode = 16
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {

        create("release") {
            storeFile = file(project.findProperty("KEYSTORE_FILE") as String)
            storePassword = project.findProperty("KEYSTORE_PASSWORD") as String
            keyAlias = project.findProperty("KEY_ALIAS") as String
            keyPassword = project.findProperty("KEY_PASSWORD") as String
        }
    }

    buildTypes {
        getByName("release") {


            isMinifyEnabled = true
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }


//    sourceSets {
//        // 配置 main 源集
//        getByName("main") {
//            // Kotlin 源代码文件目录
//            java.srcDirs("src/main/kotlin", "src/main/java")
//            // 资源文件目录
//            res.srcDirs("src/main/res")
//            // assets 目录
//            assets.srcDirs("src/main/assets")
//            // JNI 库目录
//            jniLibs.srcDirs("src/main/jniLibs")
//            // Manifest 文件目录
//            manifest.srcFile("src/main/AndroidManifest.xml")
//        }
//    }



}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.window.core.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.gson)

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)


    implementation(libs.material)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}