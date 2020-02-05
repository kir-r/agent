import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("kotlin-multiplatform")
}

kotlin {
    targets {
        if (isDevMode) {
            currentTarget("commonNative")
        } else {
            mingwX64()
            linuxX64()
            macosX64()
        }
    }

    sourceSets {

        val commonNativeMain: KotlinSourceSet = maybeCreate("commonNativeMain")
        with(commonNativeMain) {
            dependencies {
                implementation("com.epam.drill:common:$drillApiVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutinesVersion")
                implementation(project(":util"))
                implementation("com.epam.drill:drill-agent-part:$drillApiVersion")
            }
        }
        if (!isDevMode) {
            @Suppress("UNUSED_VARIABLE") val mingwX64Main by getting { dependsOn(commonNativeMain) }
            @Suppress("UNUSED_VARIABLE") val linuxX64Main by getting { dependsOn(commonNativeMain) }
            @Suppress("UNUSED_VARIABLE") val macosX64Main by getting { dependsOn(commonNativeMain) }
        }

    }
}

tasks.withType<KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
}