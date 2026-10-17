plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  val resolvedVersionCode: Int = run {
    val rawProp = if (providers.gradleProperty("versionCode").isPresent) {
      providers.gradleProperty("versionCode").get()
    } else {
      findProperty("versionCode")?.toString()
    }

    if (rawProp == null) {
      1
    } else {
      val code = rawProp.toIntOrNull()
        ?: throw GradleException("Invalid versionCode '$rawProp': must be a positive integer within 1..2147483647.")
      if (code <= 0) {
        throw GradleException("Invalid versionCode '$rawProp': must be a positive integer within 1..2147483647.")
      }
      code
    }
  }

  val resolvedVersionName: String = run {
    val rawProp = if (providers.gradleProperty("versionName").isPresent) {
      providers.gradleProperty("versionName").get()
    } else {
      findProperty("versionName")?.toString()
    }

    if (rawProp == null) {
      "1.0.0-dev"
    } else {
      val trimmed = rawProp.trim()
      if (trimmed.isEmpty()) {
        throw GradleException("Invalid versionName '$rawProp': must not be empty or blank.")
      }
      trimmed
    }
  }

  defaultConfig {
    applicationId = "com.aistudio.flipp.zkxqpv"
    minSdk = 26
    targetSdk = 36
    versionCode = resolvedVersionCode
    versionName = resolvedVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "my-upload-key.jks"
      storeFile = rootProject.file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = rootProject.file("debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}


dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.itextg)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.withType(Test::class.java).configureEach {
    systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
    systemProperty("robolectric.dependency.repo.id", "google-maven-central")
}
