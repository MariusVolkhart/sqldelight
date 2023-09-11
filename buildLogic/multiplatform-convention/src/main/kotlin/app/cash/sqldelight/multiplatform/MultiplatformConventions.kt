package app.cash.sqldelight.multiplatform

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class MultiplatformConventions : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.apply("org.jetbrains.kotlin.multiplatform")

    (project.kotlinExtension as KotlinMultiplatformExtension).apply {
      jvm()
    }
  }
}
