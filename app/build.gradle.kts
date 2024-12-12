plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"  // Wtyczka KSP
}

android {
    namespace = "com.example.healthmate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.healthmate"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(libs.play.services.tasks)
    implementation(libs.androidx.runtime.livedata)


    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    val room_version = "2.6.1"
    val vicoVersion = "2.0.0-beta.3"
    val composeCharts_version = "0.1.0"

    implementation ("co.yml:ycharts:2.1.0")

    implementation ("io.github.ehsannarmani:compose-charts:$composeCharts_version")

    // Zaktualizuj Room do wersji z obsługą KSP
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")  // Zamiast kapt, używamy ksp

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")

    // optional - RxJava2 support for Room
    implementation("androidx.room:room-rxjava2:$room_version")

    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:$room_version")

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation("androidx.room:room-guava:$room_version")

    // optional - Test helpers
    testImplementation("androidx.room:room-testing:$room_version")

    // optional - Paging 3 Integration
    implementation("androidx.room:room-paging:$room_version")

    // For Jetpack Compose.
    implementation("com.patrykandpatrick.vico:compose:$vicoVersion")

    // For Material 2 theming in Jetpack Compose.
    implementation("com.patrykandpatrick.vico:compose-m2:$vicoVersion")

    // For Material 3 theming in Jetpack Compose.
    implementation("com.patrykandpatrick.vico:compose-m3:$vicoVersion")
}
