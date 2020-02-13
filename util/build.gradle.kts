import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("kotlin-multiplatform")
    id("com.epam.drill.cross-compilation")
    `maven-publish`
}

kotlin {
    crossCompilation{
        common{
            defaultSourceSet{
                dependencies {
                    implementation("com.epam.drill:jvmapi-native:$drillJvmApiLibVersion")
                    implementation("com.epam.drill:drill-agent-part:$drillApiVersion")
                }
            }
        }
    }

    mingwX64()
    linuxX64()
    macosX64()

}

tasks.withType<KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
}

publishing {
    repositories {
        maven {
            url = uri("http://oss.jfrog.org/oss-release-local")
            credentials {
                username =
                    if (project.hasProperty("bintrayUser"))
                        project.property("bintrayUser").toString()
                    else System.getenv("BINTRAY_USER")
                password =
                    if (project.hasProperty("bintrayApiKey"))
                        project.property("bintrayApiKey").toString()
                    else System.getenv("BINTRAY_API_KEY")
            }
        }
    }
}