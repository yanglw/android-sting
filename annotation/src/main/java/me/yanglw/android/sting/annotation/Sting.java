package me.yanglw.android.sting.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 用于标记设置方法执行间隔的注解。 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Sting {
  /** 是否开启。 */
  boolean enable() default true;

  /** 方法触发的时间间隔。单位：毫秒。 */
  long interval() default USE_GLOBAL_INTERVAL;

  /** 使用 gradle DSL 中 anno 的 interval 的值。默认为 2000 毫秒。 */
  long USE_GLOBAL_INTERVAL = -1;
}
