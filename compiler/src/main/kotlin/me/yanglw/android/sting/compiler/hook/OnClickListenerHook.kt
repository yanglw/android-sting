package me.yanglw.android.sting.compiler.hook

import javassist.ClassPool
import javassist.CtClass
import me.yanglw.android.sting.compiler.extension.OnClickExtension

/** 用于处理 OnClickListener 的 [Hook] 。 */
class OnClickListenerHook(private val extension: OnClickExtension) : AbsClassHook() {
  override fun checkClass(pool: ClassPool, clz: CtClass): Boolean {
    if (!super.checkClass(pool, clz)) {
      return false
    }
    if (extension.blackNameList != null) {
      if (extension.blackNameList!!.find { clz.name.matches(Regex(it)) } != null) {
        return false
      }
    }
    if (extension.whiteNameList != null) {
      if (extension.whiteNameList!!.find { clz.name.matches(Regex(it)) } == null) {
        return false
      }
    }
    return true
  }

  override fun getInterval(): Long = extension.interval

  override fun getClassName(): String = "android.view.View\$OnClickListener"

  override fun getMethodName(): String = "onClick"

  override fun getMethodParamClassNames(): List<String> = listOf("android.view.View")
}