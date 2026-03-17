plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ethiostat.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ethiostat.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // BuildConfig fields from gradle.properties
        val properties = java.util.Properties()
        properties.load(project.rootProject.file("gradle.properties").inputStream())
        
        buildConfigField("String", "DEFAULT_TELECOM_SENDER", "\"${properties.getProperty("DEFAULT_TELECOM_SENDER")}\"")
        buildConfigField("String", "DEFAULT_TELEBIRR_SENDER", "\"${properties.getProperty("DEFAULT_TELEBIRR_SENDER")}\"")
        buildConfigField("String", "DEFAULT_USSD_BALANCE", "\"${properties.getProperty("DEFAULT_USSD_BALANCE")}\"")
        buildConfigField("String", "DEFAULT_USSD_PACKAGES", "\"${properties.getProperty("DEFAULT_USSD_PACKAGES")}\"")
        buildConfigField("String", "DEFAULT_USSD_DATA_CHECK", "\"${properties.getProperty("DEFAULT_USSD_DATA_CHECK")}\"")
        buildConfigField("String", "DEFAULT_BANK_SENDERS", "\"${properties.getProperty("DEFAULT_BANK_SENDERS")}\"")
        buildConfigField("String", "DEFAULT_LANGUAGE", "\"${properties.getProperty("DEFAULT_LANGUAGE")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    ksp("androidx.room:room-compiler:2.6.0")
    
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
