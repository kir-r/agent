import org.jetbrains.kotlin.gradle.tasks.*
import java.net.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.hierynomus.license")
    `maven-publish`
}

val scriptUrl: String by extra

val serializationRuntimeVersion: String by extra
val coroutinesVersion: String by extra
val uuidVersion: String by extra

val drillHttpInterceptorVersion: String by extra
val drillApiVersion: String by extra
val drillLoggerVersion: String by extra
val drillLoggerApiVersion: String by extra
val drillTransportLibVerison: String by extra
val kxCollection: String by extra

allprojects {
    apply(from = "$scriptUrl/git-version.gradle.kts")

    repositories {
        mavenLocal()
        mavenCentral()
        apply(from = "$scriptUrl/maven-repo.gradle.kts")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.allWarningsAsErrors = true
    }
}

kotlin {
    targets {
        setOf(
            mingwX64 { binaries.all { linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock") } },
            macosX64(),
            linuxX64()
        ).forEach {
            it.compilations["main"].addCInterop()
        }
        jvm {
            val main by compilations
            main.defaultSourceSet {
                dependencies {
                    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationRuntimeVersion")
                }
            }
        }
    }

    sourceSets {
        listOf(
            "kotlin.ExperimentalStdlibApi",
            "kotlin.ExperimentalUnsignedTypes",
            "kotlin.time.ExperimentalTime",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlinx.serialization.InternalSerializationApi",
        ).let { annotations ->
            all { annotations.forEach(languageSettings::optIn) }
        }
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationRuntimeVersion")
                implementation("com.epam.drill.logger:logger-api:$drillLoggerApiVersion")
            }
        }
        val commonNative by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationRuntimeVersion")
                implementation("com.benasher44:uuid:$uuidVersion")
                implementation("com.epam.drill:transport:$drillTransportLibVerison")
                implementation("com.epam.drill.interceptor:http:$drillHttpInterceptorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kxCollection")
                implementation("com.epam.drill:drill-agent-part:$drillApiVersion")
                implementation("com.epam.drill:common:$drillApiVersion")
                implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core") {
                    version { strictly("$coroutinesVersion-native-mt") }
                }
            }
        }

        val posixNative by creating {
            dependsOn(commonNative)
        }

        val linuxX64Main by getting {
            dependsOn(posixNative)
        }
        val mingwX64Main by getting {
            dependsOn(commonNative)
        }
        val macosX64Main by getting {
            dependsOn(posixNative)
        }
    }
}

tasks.withType<KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.InternalSerializationApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=io.ktor.util.InternalAPI"
}


fun org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation.addCInterop() {
    cinterops.create("zstd_bindings").includeDirs(rootProject.file("lib").resolve("include"))
}

val licenseFormatSettings by tasks.registering(com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir).also {
        include("**/*.kt", "**/*.java", "**/*.groovy")
        exclude("**/.idea")
    }.asFileTree
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
}

tasks["licenseFormat"].dependsOn(licenseFormatSettings)
