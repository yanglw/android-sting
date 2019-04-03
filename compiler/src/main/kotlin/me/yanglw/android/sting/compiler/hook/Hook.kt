package me.yanglw.android.sting.compiler.hook

import javassist.ClassPool
import javassist.CtClass
import org.gradle.api.Project

/** 用于 Hook class 的类。 */
interface Hook {
  /**
   * Hook 目标 class 。
   *
   * @clz 目标 class 。
   *
   * @return 若对 [clz] 进行了 hook 则返回 true ，否则返回 false 。
   */
  fun hook(project: Project, pool: ClassPool, clz: CtClass): Boolean
}