plugins {
    id("com.android.application")
    jacoco
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.tupastilla"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tupastilla"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val keystore = System.getenv("TUPASTILLA_KEYSTORE")
            if (keystore != null) {
                storeFile = file(keystore)
                storePassword = System.getenv("TUPASTILLA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("TUPASTILLA_KEY_ALIAS")
                keyPassword = System.getenv("TUPASTILLA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            enableUnitTestCoverage = true
            buildConfigField("String", "API_URL", "\"${providers.gradleProperty("tupastilla.apiUrlDebug").getOrElse("http://10.0.2.2:3000/")}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_URL", "\"${providers.gradleProperty("tupastilla.apiUrl").getOrElse("https://api.tupastilla.example/")}\"")
            if (System.getenv("TUPASTILLA_KEYSTORE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        xmlReport = true
        htmlReport = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    val roomVersion = "2.7.2"

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Tests unitarios (JVM): ./gradlew testDebugUnitTest
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Tests instrumentados (emulador o dispositivo): ./gradlew connectedDebugAndroidTest
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

// Cobertura solo del paquete auth. Excluye Fragments y el almacen con Keystore
// porque necesitan dispositivo.
val coberturaAuth by tasks.registering(JacocoReport::class) {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val clases = fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
        include("com/tupastilla/auth/**")
        exclude("**/*Fragment*", "**/FormularioAuth*", "**/MensajesAuth*", "**/AlmacenSesionCifrado*", "**/AlmacenCuentaPrefs*")
    }
    classDirectories.setFrom(clases)
    sourceDirectories.setFrom(files("src/main/java"))
    // Archivo exacto: recorrer build/ buscando *.ec hacia fallar a Gradle al correr
    // cobertura y lint en el mismo comando del pipeline.
    executionData.setFrom(layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))
}

val verificarCoberturaAuth by tasks.registering(JacocoCoverageVerification::class) {
    dependsOn(coberturaAuth)
    classDirectories.setFrom(coberturaAuth.map { it.classDirectories })
    sourceDirectories.setFrom(coberturaAuth.map { it.sourceDirectories })
    executionData.setFrom(coberturaAuth.map { it.executionData })
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
