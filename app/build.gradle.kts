plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.c9"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.c9"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.3.1")

        implementation ("net.sourceforge.jtds:jtds:1.3.1") // Revisa que sea la versión correcta


        implementation ("com.google.android.material:material:1.4.0")
        // Otras dependencias necesarias



    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.recyclerview:recyclerview:1.3.1") // O la versión más reciente
    implementation("androidx.cardview:cardview:1.0.0")


    // JUnit para pruebas unitarias
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Controlador JDBC para Microsoft SQL Server
    implementation("com.microsoft.sqlserver:mssql-jdbc:9.4.1.jre11")

    dependencies {
        implementation("org.xerial:sqlite-jdbc:3.40.0.0") // o la versión más reciente
    }

}
