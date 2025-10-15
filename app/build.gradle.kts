import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.jetbrainsKotlinAndroid)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.sonar)
  alias(libs.plugins.gms)
  id("jacoco")
}

android {
  namespace = "com.android.wildex"
  compileSdk = 34

  // Load the API key from local.properties
  val localProperties = Properties()
  val localPropertiesFile = rootProject.file("local.properties")
  if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
  }

  val adApiKey: String = localProperties.getProperty("ANIMALDETECT_API_KEY") ?: ""

  defaultConfig {
    applicationId = "com.android.wildex"
    minSdk = 28
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables { useSupportLibrary = true }
    buildConfigField("String", "ANIMALDETECT_API_KEY", "\"$adApiKey\"")
  }

  buildTypes {
    signingConfigs {
      create("release") {
        storeFile = file("upload-keystore.jks")
        storePassword = localProperties.getProperty("KEYSTORE_PASSWORD")
        keyAlias = localProperties.getProperty("KEY_ALIAS")
        keyPassword = localProperties.getProperty("KEY_PASSWORD")
      }
    }

    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      enableUnitTestCoverage = true
    }

    debug {
      isMinifyEnabled = false
    }

    debug {
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
    }
  }

  testCoverage { jacocoVersion = "0.8.8" }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  composeOptions { kotlinCompilerExtensionVersion = "1.4.2" }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions { jvmTarget = "11" }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      merges += "META-INF/LICENSE.md"
      merges += "META-INF/LICENSE-notice.md"
      excludes += "META-INF/LICENSE-notice.md"
      excludes += "META-INF/LICENSE.md"
      excludes += "META-INF/LICENSE"
      excludes += "META-INF/LICENSE.txt"
      excludes += "META-INF/NOTICE"
      excludes += "META-INF/NOTICE.txt"
    }
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
    packagingOptions {
      jniLibs {
        useLegacyPackaging = true
      }
    }
  }

  // Robolectric needs to be run only in debug. But its tests are placed in the shared source set
  // (test)
  // The next lines transfers the src/test/* from shared to the testDebug one
  //
  // This prevent errors from occurring during unit tests
  sourceSets.getByName("testDebug") {
    val test = sourceSets.getByName("test")

    java.setSrcDirs(test.java.srcDirs)
    res.setSrcDirs(test.res.srcDirs)
    resources.setSrcDirs(test.resources.srcDirs)
  }

  sourceSets.getByName("test") {
    java.setSrcDirs(emptyList<File>())
    res.setSrcDirs(emptyList<File>())
    resources.setSrcDirs(emptyList<File>())
  }
}

sonar {
  properties {
    property("sonar.projectKey", "wildex-swent_wildex-app")
    property("sonar.projectName", "wildex-app")
    property("sonar.organization", "wildex-swent")
    property("sonar.host.url", "https://sonarcloud.io")
    // Comma-separated paths to the various directories containing the *.xml JUnit report files.
    // Each path may be absolute or relative to the project base directory.
    property(
        "sonar.junit.reportPaths",
        "${project.layout.buildDirectory.get()}/test-results/testDebugUnitTest," +
                "${project.layout.buildDirectory.get()}/test-results/testReleaseUnitTest",
    )
    // Paths to xml files with Android Lint issues. If the main flavor is changed, this file will
    // have to be changed too.
    property(
        "sonar.androidLint.reportPaths",
        "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml",
    )
    // Paths to JaCoCo XML coverage report files.
    property(
        "sonar.coverage.jacoco.xmlReportPaths",
        "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml",
    )
  }
}

// When a library is used both by robolectric and connected tests, use this function
fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
  androidTestImplementation(dep)
  testImplementation(dep)
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(platform(libs.compose.bom))
  testImplementation(libs.junit)
  globalTestImplementation(libs.androidx.junit)
  globalTestImplementation(libs.androidx.espresso.core)
  implementation(libs.kotlinx.serialization.json)

  // ------------- Jetpack Compose ------------------
  val composeBom = platform(libs.compose.bom)
  implementation(composeBom)
  globalTestImplementation(composeBom)

  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  // Material Design 3
  implementation(libs.compose.material3)
  // Integration with activities
  implementation(libs.compose.activity)
  // Integration with ViewModels
  implementation(libs.compose.viewmodel)
  // Android Studio Preview support
  implementation(libs.compose.preview)
  debugImplementation(libs.compose.tooling)

  // Firebase
  implementation(libs.firebase.database.ktx)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth.ktx)
  implementation(libs.firebase.auth)

  // Credential Manager (for Google Sign-In)
  implementation(libs.credentials)
  implementation(libs.credentials.play.services.auth)
  implementation(libs.googleid)

  // Navigation
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui.ktx)

  // UI Tests
  globalTestImplementation(libs.compose.test.junit)
  debugImplementation(libs.compose.test.manifest)

  // --------- Kaspresso test framework ----------
  globalTestImplementation(libs.kaspresso)
  globalTestImplementation(libs.kaspresso.compose)

  // ----------       Robolectric     ------------
  testImplementation(libs.robolectric)

  // Networking with OkHttp
  implementation(libs.okhttp)

  // Mock testing
  testImplementation(libs.mockwebserver)
  testImplementation(libs.mockk)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.kotlin)
  androidTestImplementation(libs.mockk)
  androidTestImplementation(libs.mockk.android)
  androidTestImplementation(libs.mockk.agent)
  androidTestImplementation(libs.mockito.android)
  androidTestImplementation(libs.mockito.kotlin)
  testImplementation("junit:junit:4.13.2")
  testImplementation("io.mockk:mockk:1.13.12")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

  // Google Identity Services (Credential Manager - Google ID Token)
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

  // AndroidX Credential Manager
  //implementation("androidx.credentials:credentials:1.3.0")
  //implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

  // Coil
  implementation("io.coil-kt:coil-compose:2.6.0")
}

tasks.withType<Test> {
  // Configure Jacoco for each tests
  configure<JacocoTaskExtension> {
    isIncludeNoLocationClasses = true
    excludes = listOf("jdk.internal.*")
  }
  if (name.contains("Release")) {
    exclude("**/ui/**")
    exclude("**/*Compose*")
    exclude("**/*ProfileScreenTest*")
  }
}

// kotlin
tasks.register("jacocoTestReport", JacocoReport::class) {
  mustRunAfter("testDebugUnitTest", "connectedDebugAndroidTest", "testReleaseUnitTest")

  reports {
    xml.required = true
    html.required = true
  }

  val fileFilter =
    listOf(
      "**/R.class",
      "**/R$*.class",
      "**/BuildConfig.*",
      "**/Manifest*.*",
      "**/*Test*.*",
      "android/**/*.*",
    )

  // Classes compilées (ajout uniquement si le dossier existe)
  val classDirs = mutableListOf<FileTree>()
  val debugDir = file("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug")
  val releaseDir = file("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/release")
  if (debugDir.exists()) classDirs += fileTree(debugDir) { exclude(fileFilter) }
  if (releaseDir.exists()) classDirs += fileTree(releaseDir) { exclude(fileFilter) }
  classDirectories.setFrom(classDirs)

  // Sources
  val srcDirs = listOf(
    file("${project.layout.projectDirectory}/src/main/java"),
    file("${project.layout.projectDirectory}/src/main/kotlin")
  ).filter { it.exists() }
  sourceDirectories.setFrom(files(if (srcDirs.isEmpty()) file("${project.layout.projectDirectory}/src/main/java") else srcDirs))

  // Données de couverture: ne garder que les fichiers existants
  val execFiles = fileTree(project.layout.buildDirectory.get()) {
    include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    include("outputs/unit_test_code_coverage/releaseUnitTest/testReleaseUnitTest.exec")
    include("outputs/unit_test_code_coverage/*UnitTest/test*UnitTest.exec")
    include("outputs/code_coverage/**/connected/*/coverage.ec")
    include("jacoco/*.exec")
    include("**/*.ec")
  }.files.filter { it.exists() }
  executionData.setFrom(files(execFiles))

  // Ne génère un rapport que s'il y a des classes et des données de couverture
  onlyIf {
    classDirectories.files.isNotEmpty() && executionData.files.isNotEmpty()
  }

  doFirst {
    logger.lifecycle("JaCoCo exec files: ${executionData.files}")
    logger.lifecycle("JaCoCo class dirs: ${classDirectories.files}")
  }
}

tasks.matching { it.name == "testDebugUnitTest" || it.name == "testReleaseUnitTest" }
  .configureEach { finalizedBy("jacocoTestReport") }
tasks.named("sonarqube").configure { dependsOn("jacocoTestReport") }

configurations.forEach { configuration ->
  // Exclude protobuf-lite from all configurations
  // This fixes a fatal exception for tests interacting with Cloud Firestore
  configuration.exclude("com.google.protobuf", "protobuf-lite")
}
