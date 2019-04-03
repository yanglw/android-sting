package me.yanglw.android.sting.compiler.hook

import javassist.bytecode.annotation.Annotation
import me.yanglw.android.sting.annotation.Sting
import me.yanglw.android.sting.compiler.extension.AnnoExtension
import me.yanglw.android.sting.compiler.getInterval

/** 用于处理 [Sting] 注解的 [Hook] 。 */
open class StingAnnotationHook(private val extension: AnnoExtension) : AbsAnnotationHook() {
  override fun getAnnotationName(): String = Sting::class.java.name

  override fun getInterval(annotation: Annotation): Long = annotation.getInterval(extension.interval)
}