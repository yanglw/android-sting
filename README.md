# Sting
Sting 是一个基于 [Android Gradle Plugin Transform API](http://google.github.io/android-gradle-dsl/javadoc/current/com/android/build/api/transform/Transform.html) ，在代码编译阶段注入代码从而实现的方法防抖功能的插件。

Sting 可以实现 `OnClickListener.OnClick` 方法的防抖，也可以实现任意方法的防抖。

**注意**
> Sting **不支持** [lambda 表达式](https://developer.android.com/studio/write/java8-support#supported_features)的 `OnClickListener.onClick` 方法的防抖功能。

## 使用方式

本库已经上传至 jcenter 仓库中，添加 jcenter 仓库便可以通过远程依赖的方式使用本库。

### 引入插件
```groovy
buildscript {
   repositories {
       google()
       jcenter()
   }
   dependencies {
       classpath 'com.android.tools.build:gradle:3.3.2'
       // 引入插件
       classpath 'me.yanglw:android-sting-compiler:last-version'
   }
}

allprojects {
   repositories {
       google()
       jcenter()
   }
}
```

插件的引入可以在 Android Application 项目中进行，也可以在 Android Library 项目中进行。

对于需要开启防抖功能的项目，需要在该项目中的 build.gradle 文件中添加插件的引入。
```groovy
apply plugin: 'me.yanglw.android.sting'
```


### 配置 DSL
在默认的情况下，只会开启添加有 `Sting` 注解的方法的防抖，`OnClickListener.onClick` 方法的防抖没有开启。可以通过 DSL 的配置，设置功能的开关和防抖时间的间隔。

```groovy
androidSting {
   enable true
   onClick {
       enable true
       interval 2000
       whiteNameList = ['']
       blackNameList = ['']
   }
   anno {
       enable true
       interval 2000
   }
}
```

`androidSting.enable` 为全局功能的开关，若设置为 `false` ，则关闭所有的防抖功能。

`androidSting.onClick` 为 `OnClickListener.onClick` 方法防抖功能的配置模块。
- `onClick.enable` 为 `OnClickListener.onClick` 方法防抖功能的开关。

  若设置为 `false` ，则关闭 `OnClickListener.onClick` 方法防抖功能。
- `onClick.interval` 为 `OnClickListener.onClick` 方法的防抖时间间隔。

  单位为毫秒，默认值为 2000 毫秒。

  该值全局 `OnClickListener.onClick` 方法共享，若需要单独为某一个 `OnClickListener.onClick` 方法设置时间，请使用 `Sting` 注解进行。
- `onClick.whiteNameList` 为 `OnClickListener.onClick` 的白名单过滤列表。

  该字段为 List\<String\> ，每一项为目标类的全称的匹配项，使用正则表达式的方式进行匹配。

  若配置了 `onClick.whiteNameList` ，则所有实现 `OnClickListener` 接口的类，需要该类的类名**匹配** `onClick.whiteNameList` 中的任意一项才会进行防抖操作。
- `onClick.blackNameList` 为 `OnClickListener.onClick` 的黑名单过滤列表。

  该字段为 List\<String\> ，每一项为目标类的全称的匹配项，使用正则表达式的方式进行匹配。

  若配置了 `onClick.blackNameList` ，则所有实现 `OnClickListener` 接口的类，需要该类的类名**不匹配** `onClick.whiteNameList` 中的任意一项才会进行防抖操作。

> 若同时配置了 `onClick.whiteNameList` 和 `onClick.blackNameList` ，则所有实现 `OnClickListener` 接口的类，需要该类的类名**匹配** `onClick.whiteNameList` 中的任意一项**同时**需要该类的类名**不匹配** `onClick.whiteNameList` 中的任意一项才会进行防抖操作。

`androidSting.anno` 为 `Sting` 注解防抖功能的配置模块。

若要使用该功能，首先需要为项目引入 `Sting` 注解的依赖。
```groovy
dependencies {
    implementation 'me.yanglw:android-sting-annotation:last-version'
}
```
之后，便可以在任意非抽象方法上添加 `Sting` 注解。
```java
public class MainActivity extends Activity {

    @Sting(enable = true, interval = 3000)
    private void fun(int arg) {
        System.out.println("hello world");
    }
}
```

- `anno.enable` 为 `Sting` 注解防抖功能的开关。

  若设置为 `false` ，则关闭 `Sting` 注解的防抖功能。
- `anno.interval` 为 `Sting` 

  单位为毫秒，默认值为 2000 毫秒。

`Sting` 注解包含有两个参数：
- `enable` 用于表示当前方法是否开启防抖功能。

  若设置为 `false` ，则关闭当前方法的防抖功能。
- `interval` 用于表示方法的防抖间隔时间。

  单位为毫秒，默认值使用 `anno.interval` 中配置的值。

