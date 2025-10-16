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

jacoco {
  toolVersion = "0.8.11"
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

  testCoverage { jacocoVersion = "0.8.11" }

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

  // Redirige src/test/* vers testDebug pour isoler Robolectric des builds Release
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
    property(
      "sonar.junit.reportPaths",
      "${project.layout.buildDirectory.get()}/test-results/testDebugUnitTest," +
              "${project.layout.buildDirectory.get()}/test-results/testReleaseUnitTest",
    )
    property(
      "sonar.androidLint.reportPaths",
      "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml",
    )
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
  implementation(libs.compose.material3)
  implementation(libs.compose.activity)
  implementation(libs.compose.viewmodel)
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

  // Coil
  implementation("io.coil-kt:coil-compose:2.6.0")
}

tasks.withType<Test> {
  // Configure Jacoco for each tests
  configure<JacocoTaskExtension> {
    isIncludeNoLocationClasses = true
    excludes = listOf("jdk.internal.*")
  }
  // Sécuriser Release: ignorer tout test UI/Compose s'il était découvert
  if (name.contains("Release")) {
    exclude("**/ui/**")
    exclude("**/*Compose*")
    exclude("**/*ProfileScreenTest*")
  }
}

// Rapport JaCoCo: choisir un seul variant (debug sinon release) et n'utiliser que ses *.exec
tasks.register("jacocoTestReport", JacocoReport::class) {
  // Exécute les tests si le CI invoque directement cette tâche
  dependsOn("testDebugUnitTest")

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
      "META-INF/*.kotlin_module",
      "META-INF/**",
    )

  val buildDirFile = project.layout.buildDirectory.get().asFile

  // Dossiers classes par variant
  val kotlinDebug = File(buildDirFile, "tmp/kotlin-classes/debug")
  val javaDebug = File(buildDirFile, "intermediates/javac/debug/classes")
  val kotlinRelease = File(buildDirFile, "tmp/kotlin-classes/release")
  val javaRelease = File(buildDirFile, "intermediates/javac/release/classes")

  // Choix du variant: debug si dispo, sinon release
  val useDebug = kotlinDebug.exists() || javaDebug.exists()
  val useRelease = !useDebug && (kotlinRelease.exists() || javaRelease.exists())

  val classTrees = mutableListOf<FileTree>()
  if (useDebug) {
    if (kotlinDebug.exists()) classTrees += fileTree(kotlinDebug) { include("**/*.class"); exclude(fileFilter) }
    if (javaDebug.exists()) classTrees += fileTree(javaDebug) { include("**/*.class"); exclude(fileFilter) }
  } else if (useRelease) {
    if (kotlinRelease.exists()) classTrees += fileTree(kotlinRelease) { include("**/*.class"); exclude(fileFilter) }
    if (javaRelease.exists()) classTrees += fileTree(javaRelease) { include("**/*.class"); exclude(fileFilter) }
    dependsOn("testReleaseUnitTest")
  }
  classDirectories.setFrom(classTrees)

  // Sources
  val srcDirs = listOf(
    file("${project.layout.projectDirectory}/src/main/java"),
    file("${project.layout.projectDirectory}/src/main/kotlin")
  ).filter { it.exists() }
  sourceDirectories.setFrom(files(srcDirs))

  // Données de couverture: seulement le variant choisi, exclut volontairement les .ec
  val execFiles = mutableListOf<File>()
  if (useDebug) {
    listOf(
      "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
      "jacoco/testDebugUnitTest.exec"
    ).mapTo(execFiles) { File(buildDirFile, it) }
  } else if (useRelease) {
    listOf(
      "outputs/unit_test_code_coverage/releaseUnitTest/testReleaseUnitTest.exec",
      "jacoco/testReleaseUnitTest.exec"
    ).mapTo(execFiles) { File(buildDirFile, it) }
  }
  executionData.setFrom(files(execFiles.filter { it.exists() && it.length() > 0 }))

  // Ne génère le rapport que s'il y a classes et données alignées
  onlyIf {
    classDirectories.files.isNotEmpty() && executionData.files.isNotEmpty()
  }

  doFirst {
    logger.lifecycle("Variant: ${if (useDebug) "debug" else if (useRelease) "release" else "none"}")
    logger.lifecycle("JaCoCo exec files: ${executionData.files}")
    logger.lifecycle("JaCoCo class dirs: ${classDirectories.files}")
  }
}

// Générer JaCoCo après les tests, sans changer le pipeline
tasks.matching { it.name == "testDebugUnitTest" || it.name == "testReleaseUnitTest" }
  .configureEach { finalizedBy("jacocoTestReport") }

// Sonar lit un rapport JaCoCo à jour
tasks.named("sonarqube").configure { dependsOn("jacocoTestReport") }

configurations.forEach { configuration ->
  // Exclude protobuf-lite from all configurations
  // This fixes a fatal exception for tests interacting with Cloud Firestore
  configuration.exclude("com.google.protobuf", "protobuf-lite")
}
