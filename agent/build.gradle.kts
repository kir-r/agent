import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.HostManager
import com.github.jengelman.gradle.plugins.shadow.tasks.*

plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
    distribution
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "5.1.0"
}
val gccIsNeeded = (project.property("gccIsNeeded") as String).toBoolean()

allprojects {

    repositories {
        mavenCentral()
        jcenter()
        mavenLocal()
        maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
        maven(url = "https://dl.bintray.com/kotlin/ktor/")
        maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions.allWarningsAsErrors = true
    }
    tasks.withType<KotlinNativeCompile> {
        kotlinOptions.allWarningsAsErrors = true
    }
}

val libName = "drill-agent"
val nativeTargets = mutableSetOf<KotlinNativeTarget>()

kotlin {
    targets {
        if (isDevMode) {
            currentTarget("nativeAgent") {
                binaries { sharedLib(libName, setOf(DEBUG)) }
            }.apply {
                nativeTargets.add(this)
            }
        } else {
            mingwX64 { binaries { sharedLib(libName, setOf(DEBUG)) } }.apply { nativeTargets.add(this) }
            macosX64 { binaries { sharedLib(libName, setOf(DEBUG)) } }.apply { nativeTargets.add(this) }
            linuxX64 {
                binaries {
                    if (!gccIsNeeded) sharedLib(libName, setOf(DEBUG))
                    else staticLib(libName, setOf(DEBUG))
                }
            }.apply {
                nativeTargets.add(this)
            }
        }
        jvm("javaAgent")

    }
    targets.filterIsInstance<KotlinNativeTarget>().forEach { it.compilations["test"].cinterops?.create("jvmapiStub") }

    sourceSets {
        val commonNativeMain = maybeCreate("nativeAgentMain")
        @Suppress("UNUSED_VARIABLE") val commonNativeTest = maybeCreate("nativeAgentTest")
        if (!isDevMode) {
            @Suppress("UNUSED_VARIABLE") val mingwX64Main by getting { dependsOn(commonNativeMain) }
            @Suppress("UNUSED_VARIABLE") val linuxX64Main by getting { dependsOn(commonNativeMain) }
            @Suppress("UNUSED_VARIABLE") val macosX64Main by getting { dependsOn(commonNativeMain) }
        }
        jvm("javaAgent").compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect")) //TODO jarhell quick fix for kotlin jvm apps
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation(project(":common"))
                implementation(project(":plugin-api:drill-agent-part"))
                implementation("com.alibaba:transmittable-thread-local:2.11.0")
            }
        }
        jvm("javaAgent").compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        named("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationRuntimeVersion")
                implementation(project(":common"))
                implementation(project(":plugin-api:drill-agent-part"))
            }
        }
        named("commonTest") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        named("nativeAgentMain") {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationRuntimeVersion")
                implementation("io.ktor:ktor-utils-native:$ktorUtilVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-io-native:$kotlinxIoVersion")
                implementation("com.epam.drill:jvmapi-native:$drillJvmApiLibVerison")
                implementation(project(":plugin-api:drill-agent-part"))
                implementation(project(":common"))
                implementation(project(":agent:util"))
            }
        }
    }

}

tasks.withType<KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.io.core.ExperimentalIoApi"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
    kotlinOptions.freeCompilerArgs += "-XXLanguage:+InlineClasses"
}

tasks {

    val agentShadow by registering(ShadowJar::class) {
        mergeServiceFiles()
        isZip64 = true
        relocate("kotlin", "kruntime")
        archiveFileName.set("drillRuntime.jar")
        from(javaAgentJar)
    }

    named<Jar>("javaAgentJar") {
        archiveFileName.set("drillRuntime-temp.jar")
        from(provider {
            kotlin.targets["javaAgent"].compilations["main"].compileDependencyFiles.map {
                if (it.isDirectory) it else zipTree(it)
            }
        })
    }
    if (gccIsNeeded)
        register("link${libName.capitalize()}DebugSharedLinuxX64", Exec::class) {
            mustRunAfter("link${libName.capitalize()}DebugStaticLinuxX64")
            group = LifecycleBasePlugin.BUILD_GROUP
            val linuxTarget = kotlin.targets["linuxX64"] as KotlinNativeTarget
            val linuxStaticLib = linuxTarget
                .binaries
                .findStaticLib(libName, NativeBuildType.DEBUG)!!
                .outputFile.toPath()

            val linuxBuildDir = linuxStaticLib.parent.parent
            val targetSo = linuxBuildDir.resolve("${libName}DebugShared").resolve("lib${libName.replace("-", "_")}.so")
            outputs.file(targetSo)
            doFirst {

                targetSo.parent.toFile().mkdirs()
                commandLine = listOf(
                    "docker-compose",
                    "run",
                    "--rm",
                    "gcc",
                    "-shared",
                    "-o",
                    "/home/project/${project.name}/${projectDir.toPath().relativize(targetSo)
                        .toString()
                        .replace(
                            "\\",
                            "/"
                        )}",
                    "-Wl,--whole-archive",
                    "/home/project/${project.name}/${projectDir.toPath().relativize(linuxStaticLib)
                        .toString()
                        .replace(
                            "\\",
                            "/"
                        )}",
                    "-Wl,--no-whole-archive",
                    "-static-libgcc",
                    "-static-libstdc++",
                    "-lstdc++"
                )

            }
        }
}


val javaAgentJar: Jar by tasks
val agentShadow: ShadowJar by tasks

afterEvaluate {
    val availableTarget = nativeTargets.filter { HostManager().isEnabled(it.konanTarget) }

    distributions {
        availableTarget.forEach {
            val name = it.name
            create(name) {
                baseName = name
                contents {
                    from(tasks.getByPath(":agent:proxy-agent:jar"))
                    from(agentShadow)
                    from(tasks.getByPath("link${libName.capitalize()}DebugShared${name.capitalize()}"))
                }
            }
        }
    }
    publishing {
        repositories {
            maven {

                url =
                    if (version.toString().endsWith("-SNAPSHOT"))
                        uri("http://oss.jfrog.org/oss-snapshot-local")
                    else uri("http://oss.jfrog.org/oss-release-local")
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

        publications {
            availableTarget.forEach {
                create<MavenPublication>("${it.name}Zip") {
                    artifactId = "$libName-${it.name}"
                    artifact(tasks["${it.name}DistZip"])
                }
            }
        }
    }
}