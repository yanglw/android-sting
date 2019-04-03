package me.yanglw.android.sting.compiler.hook

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.Modifier
import javassist.bytecode.annotation.Annotation
import me.yanglw.android.sting.compiler.*
import org.gradle.api.Project

/** 对指定类型的注解进行 hook 的 [Hook] 类。 */
abstract class AbsAnnotationHook : Hook {
  override fun hook(project: Project, pool: ClassPool, clz: CtClass): Boolean {
    return clz.declaredMethods
        .filter { checkMethod(pool,clz, it) }
        .map {
          val interval = getInterval(it.getAnnotation(getAnnotationName()) ?: return@map false)
          if (interval <= 0) {
            return@map false
          }
          val fieldName = getFieldName(clz, it)
          clz.hookMethod(it, fieldName, interval)
          project.log("    ${javaClass.simpleName} hook ${clz.name}.${it.name} , interval = $interval , fieldName = $fieldName")
          return@map true
        }
        .contains(true)
  }

  /**
   * 检查目标 method 是否需要进行 hook 。
   *
   * @return 若目标 class 需要进行 hook 则返回 true ，否则返回 false 。
   */
  protected open fun checkMethod(pool: ClassPool, clz: CtClass, method: CtMethod): Boolean = !Modifier.isAbstract(method.modifiers) && clz.checkField(method, getFieldName(clz, method))

  /** 获取当前 [Hook] 将要处理的注解的名称。*/
  protected abstract fun getAnnotationName(): String

  /** 获取当前 [Hook] 将要处理的方法对应的记录上次触发时间的字段名称。*/
  protected open fun getFieldName(clz: CtClass, method: CtMethod): String = clz.buildFieldName(method)

  /** 获取当前 [Hook] 将要处理的方法触发时间间隔。*/
  protected abstract fun getInterval(annotation: Annotation): Long
}