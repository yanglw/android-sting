package me.yanglw.android.sting.compiler

import javassist.*
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.Descriptor
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.BooleanMemberValue
import javassist.bytecode.annotation.LongMemberValue
import me.yanglw.android.sting.annotation.Sting
import org.gradle.api.Project
import java.math.BigInteger
import java.security.MessageDigest

/** 打印日志。 */
internal fun Project.log(message: String) = this.logger.quiet(message)

/** 获取 [CtMethod] 的 [Sting] 注解。 */
internal fun CtMethod.getAnnotation(): Annotation? = getAnnotation(Sting::class.java.name)

/** 获取 [CtMethod] 指定的注解。 */
internal fun CtMethod.getAnnotation(name: String): Annotation? {
  val attribute = this.methodInfo2.getAttribute(AnnotationsAttribute.invisibleTag)
  if (attribute != null && attribute is AnnotationsAttribute) {
    return attribute.getAnnotation(name)
  }
  return null
}

/**
 * 获取 [Sting] 注解中的 [Sting.interval] 的值。
 *
 * @param defaultInterval 若 [Sting.interval] 没有设置值，则返回该值。
 *
 * @return
 * 1. 若 [Sting.enable] 为 false ，则返回一个负数。
 * 2. 若 [Sting.interval] 没有设置值，则返回 [defaultInterval] 。
 * 3. 返回设置的 [Sting.interval] 。
 */
internal fun Annotation.getInterval(defaultInterval: Long): Long {
  val enableMemberValue: BooleanMemberValue? = getMemberValue(Sting::enable.name) as BooleanMemberValue?
  val intervalMemberValue: LongMemberValue? = getMemberValue(Sting::interval.name) as LongMemberValue?
  if (enableMemberValue != null && !enableMemberValue.value) {
    return -1L
  }
  if (intervalMemberValue == null || intervalMemberValue.value == Sting.USE_GLOBAL_INTERVAL) {
    return defaultInterval
  }
  return intervalMemberValue.value
}

/**
 * hook 指定方法。
 *
 * 1. 添加一个名称为 [fieldName] 的 [java.long] 类型字段，用于记录上一次触发的时间。
 * 2. 在方法的起始位置插入时间判断的代码，若在 [interval] 时间段内触发方法则返回。
 * 3. 若方法有返回值，则返回默认值。
 *
 * @param method 需要 hook 的方法。
 * @param fieldName 创建的字段名称。
 * @param interval 方法触发的时间间隔。
 * @param method 需要 hook 的方法。
 */
internal fun CtClass.hookMethod(method: CtMethod, fieldName: String, interval: Long) {
  val isStatic = Modifier.isStatic(method.modifiers)
  val field = CtField(CtClass.longType, fieldName, this)
  field.modifiers = Modifier.PRIVATE or if (isStatic) Modifier.STATIC else 0
  addField(field, CtField.Initializer.constant(-1L))

  val returnValue = when (method.returnType) {
    CtClass.booleanType -> "false"
    CtClass.charType,
    CtClass.shortType,
    CtClass.byteType,
    CtClass.intType -> "0"
    CtClass.longType -> "0L"
    CtClass.floatType -> "0.0F"
    CtClass.doubleType -> "0.0D"
    CtClass.voidType -> ""
    else -> "null"
  }
  method.insertBefore("""
         |if (android.os.SystemClock.elapsedRealtime() - $fieldName < $interval) {
         |    return $returnValue;
         |}
         |$fieldName = android.os.SystemClock.elapsedRealtime();
         |""".trimMargin())
}

internal fun CtClass.checkField(method: CtMethod, fieldName: String):Boolean{
  return try {
    this.getDeclaredField(fieldName, Descriptor.ofParameters(arrayOf(CtClass.longType)))
    false
  } catch (e: NotFoundException) {
    true
  }
}
/** 根据 class 和 method 生成对应的 field 的名称。 */
internal fun CtClass.buildFieldName(method: CtMethod): String {
  return "me_yanglw_sting_lastTime_${"${Descriptor.of(this.name)}_${method.name}_${Descriptor.ofMethod(method.returnType, method.parameterTypes)}".sign()}"
}

private fun String.sign() = BigInteger(1, MessageDigest.getInstance("MD5").digest(toByteArray())).toString(36)
