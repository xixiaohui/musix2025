import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    alias(libs.plugins.hilt)

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.xxh.ringbones"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xxh.ringbones"
        minSdk = 24
        targetSdk = 35
        versionCode = 21
        versionName = "3.3.0"

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
        getByName("debug"){
            isDebuggable = true

            buildConfigField(
                "boolean",
                "ENABLE_FEATURE",
                "true"
            )
        }

        getByName("release") {

            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true

            // 保留原生代码的调试符号表，用于上传到 Play Console 以符号化原生崩溃和 ANR
            ndk {
                debugSymbolLevel = "symbol_table"
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")

            buildConfigField("boolean", "ENABLE_FEATURE", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }

    buildFeatures{
        buildConfig = true
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

    // 强制 kotlin-stdlib 版本与项目 Kotlin 编译器版本一致，防止传递依赖升级到不兼容版本
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib") {
            version {
                strictly("2.3.21")
            }
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib-common") {
            version {
                strictly("2.3.21")
            }
        }
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.window.core.android)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.gson)

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)


    implementation(libs.material)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)


    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx) // Kotlin 扩展库，支持协程和 Flow
    ksp(libs.androidx.room.compiler) // 注解处理器
    // 如果需要使用数据库迁移支持
    implementation(libs.androidx.room.paging)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // DataStore
    implementation(libs.datastore.preferences)
}