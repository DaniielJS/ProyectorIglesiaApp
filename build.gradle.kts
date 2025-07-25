// build.gradle.kts (root project)

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.6.0")
        classpath(kotlin("gradle-plugin", version = "1.9.10"))

    }
}
