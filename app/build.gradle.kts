plugins {
    id("com.android.application")

}

android {
    namespace = "com.example.mlrecognition"
    compileSdk = 34
        aaptOptions {
            noCompress("tflite")
        }

    defaultConfig {
        applicationId = "com.example.mlrecognition"
        minSdk = 26
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
    buildFeatures {
        mlModelBinding = true
    }


}


dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // TensorFlow dependencies


    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")


    // MediaPipe dependencies
    implementation("com.google.mediapipe:tasks-vision:latest.release")

    // AndroidX Camera dependencies
    implementation("androidx.camera:camera-view:1.4.1") // Updated version
    implementation("androidx.camera:camera-camera2:1.4.1") // Updated version
    implementation("androidx.camera:camera-lifecycle:1.4.1") // Updated version


    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.13.0")  // Optional for GPU acceleration




    // Play Services TFLite Acceleration Service
    implementation("com.google.android.gms:play-services-tflite-acceleration-service:16.4.0-beta01")

    // Unit and Instrumentation tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
