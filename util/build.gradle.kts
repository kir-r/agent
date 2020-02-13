import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("kotlin-multiplatform")
    id("com.epam.drill.cross-compilation")
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