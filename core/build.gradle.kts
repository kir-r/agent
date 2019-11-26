import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    id("kotlin-multiplatform")
}
repositories {
    mavenCentral()
    jcenter()
    mavenLocal()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
    maven(url = "https://dl.bintray.com/kotlin/ktor/")
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
                implementation("org.jetbrains.kotlinx:kotlinx-io-native:$kotlinxIoVersion")
                implementation("io.ktor:ktor-utils-native:$ktorUtilVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutinesVersion")
                implementation(project(":agent:util"))
                implementation(project(":plugin-api:drill-agent-part"))
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
    kotlinOptions.freeCompilerArgs += "-XXLanguage:+InlineClasses"
}