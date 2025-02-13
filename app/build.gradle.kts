plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.gridpics"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gridpics"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx.v2100)
    implementation(libs.material)
    //coil
    implementation(libs.coil)
    //retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.v230)
    //okhttp
    implementation(libs.okhttp)
    //gson
    implementation(libs.gson)
    implementation(libs.converter.gson)
    implementation(libs.photoview.v230)
    //splash
    implementation(libs.androidx.core.splashscreen)
    //compose
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.material3)
    implementation(libs.ui)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.library)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.coil.compose)
    implementation(libs.androidx.ui)
    implementation(libs.ui.tooling)
    implementation(libs.coil.network.okhttp)
    //zoomable && compose
    implementation(libs.zoomable)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.v100)
    implementation(libs.androidx.material)
    implementation(libs.androidx.navigation.compose.v240alpha01)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.transformations)
    //anim
    implementation(libs.orbital)
    implementation(libs.androidx.compose.animation)
}