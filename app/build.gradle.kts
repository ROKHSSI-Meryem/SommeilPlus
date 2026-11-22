plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
// Firebase : le plugin n'est applique que si app/google-services.json est present
// (fichier telecharge depuis la console Firebase). Sans lui, l'app reste utilisable en mode local.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
android {
    namespace = "com.example.sommeilplus"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.sommeilplus"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { viewBinding = false }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}
