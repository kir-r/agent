plugins {
    id("com.epam.drill.version.plugin")
}

subprojects {

    repositories {
        mavenCentral()
        jcenter()
        mavenLocal()
        maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
        maven(url = "https://dl.bintray.com/kotlin/ktor/")
        maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
    }

    apply(plugin = "com.epam.drill.version.plugin")
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.allWarningsAsErrors = true
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
        kotlinOptions.allWarningsAsErrors = true
    }

}