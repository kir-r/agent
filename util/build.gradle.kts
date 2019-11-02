import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

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
                implementation("com.epam.drill:jvmapi-native:$drillJvmApiLibVerison")
                implementation(project(":plugin-api:drill-agent-part"))
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