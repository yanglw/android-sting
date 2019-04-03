package me.yanglw.android.sting.compiler.extension

/** [OnClickListenerHook] 配置项。*/
open class OnClickExtension {
  /** 是否开启该功能。 */
  var enable = true
  /** 触发时间间隔。 */
  var interval: Long = StingExtension.DEFAULT_INTERVAL
  /** 类名白名单。若该字段不为 null ，则目标类的类名不在白名单中则不会处理该类。 */
  var whiteNameList: List<String>? = null
  /** 类名黑名单。若该字段不为 null ，则目标类的类名在黑名单中则不会处理该类。 */
  var blackNameList: List<String>? = null

  override fun toString(): String {
    return "OnClickExtension(enable=$enable, interval=$interval, whiteNameList=$whiteNameList, blackNameList=$blackNameList)"
  }
}