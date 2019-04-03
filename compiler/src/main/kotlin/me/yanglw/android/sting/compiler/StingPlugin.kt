package me.yanglw.android.sting.compiler

import com.android.build.gradle.BaseExtension
import me.yanglw.android.sting.compiler.extension.StingExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class StingPlugin : Plugin<Project> {
  override fun apply(project: Project?) {
    if (project == null) {
      return
    }
    val android: BaseExtension = project.extensions.findByName("android") as BaseExtension?
        ?: throw IllegalStateException("not found android extension.")

    val bee: StingExtension = project.extensions.create("androidSting", StingExtension::class.java)
    android.registerTransform(StingTransform(project, android, bee))
  }
}
