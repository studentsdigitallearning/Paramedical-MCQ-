import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.jacboard.mcqmaster"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.jacboard.mcqmaster"
    minSdk = 23
    targetSdk = 36
    versionCode = 2
    versionName = "1.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    multiDexEnabled = true
    multiDexKeepProguard = file("main-dex-rules.pro")
  }

  signingConfigs {
    create("release") {
      val keystorePropsFile = rootProject.file("keystore.properties")
      val keystoreProps = Properties()
      if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { stream ->
          keystoreProps.load(stream)
        }
      }

      val rawPath = System.getenv("KEYSTORE_PATH")
        ?: keystoreProps.getProperty("storeFile")
        ?: "jac-mcq-master-release.jks"

      val storePass = System.getenv("STORE_PASSWORD")
        ?: keystoreProps.getProperty("storePassword")
        ?: "Ravi@2005Rapuja"

      val aliasName = System.getenv("KEY_ALIAS")
        ?: keystoreProps.getProperty("keyAlias")
        ?: "jacmcqmaster"

      val keyPass = System.getenv("KEY_PASSWORD")
        ?: keystoreProps.getProperty("keyPassword")
        ?: "Ravi@2005Rapuja"

      val kFile = if (File(rawPath).isAbsolute) File(rawPath) else rootProject.file(rawPath)
      if (kFile.exists() && !storePass.isNullOrBlank() && !keyPass.isNullOrBlank()) {
        storeFile = kFile
        storePassword = storePass
        keyAlias = aliasName
        keyPassword = keyPass
      } else {
        // Fallback to debug keystore if release keystore is not provisioned on build agent
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Bridge environment secrets into gitignored .env if not already materialized on disk
// so Secrets Gradle Plugin injects real BuildConfig values into physical device APK/AAB builds.
val dotEnvFile = rootProject.file(".env")
if (!dotEnvFile.exists()) {
  val envSupabaseUrl = System.getenv("SUPABASE_URL")?.trim().orEmpty()
  val envSupabaseKey = System.getenv("SUPABASE_ANON_KEY")?.trim().orEmpty()
  if (envSupabaseUrl.isNotBlank() && envSupabaseKey.isNotBlank()) {
    dotEnvFile.writeText("SUPABASE_URL=$envSupabaseUrl\nSUPABASE_ANON_KEY=$envSupabaseKey\n")
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Google Sign-In via Credential Manager:
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.zxing)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)

  // Official Supabase Kotlin SDK is not used (custom OkHttp client is implemented)
  // implementation(platform(libs.supabase.bom))
  // implementation(libs.supabase.auth)
  // implementation(libs.supabase.postgrest)
  // implementation(libs.supabase.realtime)
  // implementation(libs.ktor.client.android)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
