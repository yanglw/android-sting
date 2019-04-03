package me.yanglw.android.sting.compiler.extension

/** [StingAnnotationHook] 注解配置项。*/
open class AnnoExtension {
  /** 是否开启 [Sting] 注解的功能。 */
  var enable = true
  /** [Sting] 默认触发的间隔。 */
  var interval: Long = StingExtension.DEFAULT_INTERVAL

  override fun toString(): String {
    return "AnnoExtension(enable=$enable, interval=$interval)"
  }
}