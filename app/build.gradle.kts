plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.gridpics"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gridpics"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //coil
    implementation(libs.coil)
    //glide
    implementation (libs.glide)
    annotationProcessor(libs.compiler)
    kapt(libs.compiler)
    //koin
    implementation(libs.koin.android)
    //retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.v230)
    //okhttp
    implementation(libs.okhttp)
    //gson
    implementation(libs.gson)
    implementation(libs.converter.gson)
    implementation(libs.photoview.v230)
    //permissions
    implementation(libs.permissionsdispatcher)
    kapt(libs.permissionsdispatcher.processor)
    //picasso
    implementation(libs.picasso)
    implementation(libs.picasso2.okhttp3.downloader)
}