package com.yet.plugins.miniapp

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class MiniAppConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
        pluginManager.apply("com.plugins.kotlinMultiplatformPlugin")
        pluginManager.apply("com.plugins.composeMultiplatform")
        pluginManager.apply("dev.zacsweers.metro")

        configureResources(extensions.getByType(), MiniAppResourcePackage.from(path))
        configureAndroidResources(extensions.getByType())
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                api(project(":miniapp:metro"))
                implementation(project(":miniapp:audio-presets"))
                implementation(catalog.findLibrary("compose-components-resources").get())
                implementation(catalog.findLibrary("decompose-compose").get())
            }
            sourceSets.getByName("commonTest").dependencies { implementation(project(":miniapp:testkit")) }
        }
        val projectPath = path
        val validate = tasks.register("validateMiniAppDependencies", ValidateMiniAppDependenciesTask::class.java) {
            violations.empty()
            sourceFiles.from(fileTree("src") { include("**/*.kt") })
            sourceRootPath.set(layout.projectDirectory.asFile.absolutePath)
            miniAppProjectPath.set(projectPath)
        }
        tasks.register("verifyMiniApp") {
            group = "verification"
            description = "Verifies this MiniApp's boundaries, tests, and Android/iOS compilation."
            dependsOn(validate)
            dependsOn("allTests")
            dependsOn("compileAndroidMain")
            dependsOn("compileKotlinIosSimulatorArm64")
        }
        validate.configure {
            violations.set(providers.provider {
                configurations.toList()
                .filter { it.isCanBeDeclared }
                .flatMap { configuration ->
                    val projectViolations = configuration.dependencies
                        .withType(org.gradle.api.artifacts.ProjectDependency::class.java)
                        .mapNotNull { dependency ->
                            MiniAppDependencyBoundary.violationFor(
                                projectPath,
                                configuration.name,
                                dependency.path,
                            )?.message()
                        }
                    val externalViolations = configuration.dependencies
                        .withType(org.gradle.api.artifacts.ExternalModuleDependency::class.java)
                        .mapNotNull { dependency ->
                            MiniAppDependencyBoundary.externalViolationFor(
                                projectPath = projectPath,
                                configuration = configuration.name,
                                group = dependency.group,
                                name = dependency.name,
                            )?.message()
                        }
                    projectViolations + externalViolations
                }
                .distinct()
                .sorted()
            })
        }
        tasks.matching { it.name == "check" || it.name == "allTests" }.configureEach { dependsOn(validate) }
    }

    private fun configureResources(compose: ComposeExtension, resourcePackage: String) {
        compose.extensions.configure<ResourcesExtension>("resources") {
            publicResClass = false
            packageOfResClass = resourcePackage
        }
    }

    private fun configureAndroidResources(kotlin: KotlinMultiplatformExtension) {
        kotlin.extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("android") {
            androidResources.enable = true
        }
    }
}
