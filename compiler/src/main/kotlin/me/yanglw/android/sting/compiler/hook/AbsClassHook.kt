package me.yanglw.android.sting.compiler.hook

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.Modifier
import me.yanglw.android.sting.compiler.*
import org.gradle.api.Project

/** 对指定类型的 class 进行 hook 的 [Hook] 类。 */
abstract class AbsClassHook : Hook {
  private lateinit var targetClass: CtClass

  override fun hook(project: Project, pool: ClassPool, clz: CtClass): Boolean {
    if (!checkClass(pool, clz)) {
      return false
    }

    val method: CtMethod
    try {
      method = clz.getDeclaredMethod(getMethodName(), getMethodParamClassNames().map { pool.get(it) }.toTypedArray())
    } catch (e: Exception) {
      return false
    }
    if (!checkMethod(pool, clz, method)) {
      return false
    }

    val interval = getInterval()
    val fieldName = getFieldName(clz, method)
    clz.hookMethod(method, fieldName, interval)
    project.log("    ${javaClass.simpleName} hook ${clz.name}.${getMethodName()} , interval = $interval , filedName = $fieldName")
    return true
  }

  /**
   * 检查目标 class 是否需要进行 hook 。
   *
   * @return 若目标 class 需要进行 hook 则返回 true ，否则返回 false 。
   */
  protected open fun checkClass(pool: ClassPool, clz: CtClass): Boolean {
    if (!::targetClass.isInitialized) {
      targetClass = pool.get(getClassName())
    }
    if (!clz.subtypeOf(targetClass)) {
      return false
    }
    return true
  }

  /**
   * 检查目标 method 是否需要进行 hook 。
   *
   * @return 若目标 class 需要进行 hook 则返回 true ，否则返回 false 。
   */
  protected open fun checkMethod(pool: ClassPool, clz: CtClass, method: CtMethod): Boolean {
    if (Modifier.isAbstract(method.modifiers)) {
      return false
    }
    if (Modifier.isStatic(method.modifiers)) {
      return false
    }
    if (method.getAnnotation() != null) {
      return false
    }
    if (!clz.checkField(method, getFieldName(clz, method))) {
      return false
    }
    return true
  }

  /** 获取当前 [Hook] 将要处理的 class 的名称。*/
  protected abstract fun getClassName(): String

  /** 获取当前 [Hook] 将要处理的 class 的方法名称。*/
  protected abstract fun getMethodName(): String

  /** 获取当前 [Hook] 将要处理的 class 的方法参数类型名称列表。*/
  protected abstract fun getMethodParamClassNames(): List<String>

  /** 获取当前 [Hook] 将要处理的 class 的方法对应的记录上次触发时间的字段名称。*/
  protected open fun getFieldName(clz: CtClass, method: CtMethod): String = clz.buildFieldName(method)

  /** 获取当前 [Hook] 将要处理的 class 的方法触发时间间隔。*/
  protected abstract fun getInterval(): Long
}