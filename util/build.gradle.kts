import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("kotlin-multiplatform")
}

kotlin {
    targets {
        if (isDevMode) {
            currentTarget()
        } else {
            mingwX64()
            linuxX64()
            macosX64()
        }
    }
    sourceSets {
        val commonNativeMain: KotlinSourceSet by creating {
            dependencies {
                implementation("com.epam.drill:jvmapi-native:$drillJvmApiLibVersion")
                implementation("com.epam.drill:drill-agent-part-native:$drillApiVersion")
            }
        }
        if (isDevMode) {
            with(getByName(preset + "Main")) {
                dependsOn(commonNativeMain)
            }
        } else {
            @Suppress("UNUSED_VARIABLE") val mingwX64Main by getting { dependsOn(commonNativeMain) }
            @Suppress("UNUSED_VARIABLE") val linuxX64Main by getting { dependsOn(commonNativeMain) }
            @Suppress("UNUSED_VARIABLE") val macosX64Main by getting { dependsOn(commonNativeMain) }
        }

    }
}

tasks.withType<KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
    kotlinOptions.freeCompilerArgs += "-XXLanguage:+InlineClasses"
}