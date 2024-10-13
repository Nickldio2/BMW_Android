import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

val localPropertiesFile = rootProject.file("local.properties")
val placesAPIKey: String = if (localPropertiesFile.exists()) {
    val properties = Properties()
    properties.load(localPropertiesFile.inputStream())
    properties.getProperty("placesAPIKey") ?: ""
} else {
    println("local.properties file not found")
    ""
}

println("placesAPIKey loaded: $placesAPIKey")

android {
    namespace = "com.example.mapboxtest"
    compileSdk = 34

    buildFeatures {
        buildConfig = true  // Enable BuildConfig generation
    }

    defaultConfig {
        applicationId = "com.example.mapboxtest"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "placesAPIKey", "\"$placesAPIKey\"")

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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
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
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.tools.core)
    implementation(libs.androidx.transition.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.mapbox.maps:android:11.4.1")
    implementation("com.mapbox.navigationcore:android:3.2.0-rc.1")
    implementation("com.mapbox.navigationcore:ui-components:3.2.0-rc.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.places:places:3.5.0")
    implementation("androidx.fragment:fragment-ktx:1.8.0")
    implementation("androidx.transition:transition:1.5.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}