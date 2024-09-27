import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  kotlin("multiplatform") version "2.0.20"
  kotlin("plugin.serialization") version "2.0.20"
  idea
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
  jvm {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    mainRun {
      mainClass = "TtsGenKt"
      args(layout.buildDirectory.dir("audio").get().asFile.invariantSeparatorsPath)
    }
  }
  js {
    moduleName = project.name // https://youtrack.jetbrains.com/issue/KT-60569
    binaries.executable()
    browser {
      commonWebpackConfig {
        devServer?.open = false
      }
    }
  }
  sourceSets {
    commonMain {
      dependencies {
        implementation("de.comahe.i18n4k:i18n4k-core:0.8.1")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
      }
    }

    jvmMain {
      dependencies {
        implementation(project.dependencies.platform("com.google.cloud:libraries-bom:26.47.0"))
//  implementation("com.google.cloud:google-cloud-speech")
        implementation("com.google.cloud:google-cloud-texttospeech")
      }
    }
    jsMain {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-html:0.11.0")
        implementation(project.dependencies.platform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:1.0.0-pre.812"))
        implementation("org.jetbrains.kotlin-wrappers:kotlin-browser")
      }
    }
  }
}


// region https://youtrack.jetbrains.com/issue/KT-60569
tasks.named<ProcessResources>("jsProcessResources") {
  val kotlinJsModuleName = provider { kotlin.js().moduleName + ".js" }
  inputs.property("kotlinJsModuleName", kotlinJsModuleName)

  eachFile {
    if (file.name == "index.html") {
      // replace `kotlinJsModuleName.get()` with `kotlinJsModuleName` when
      // https://github.com/gradle/gradle/issues/24268 is fixed
      expand("kotlinJsModuleName" to kotlinJsModuleName.get())
      filter {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        if (it.trim() == "<!--suppress HtmlUnknownTarget -->") null else it
      }
    }
  }
}
//endregion

tasks.named("jsRun") {
  group = "run"
}

idea {
  module {
    excludeDirs.addAll(
      files(
        ".kotlin",
        ".idea",
      )
    )
  }
}
