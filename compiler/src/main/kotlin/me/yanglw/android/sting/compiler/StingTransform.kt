package me.yanglw.android.sting.compiler

import com.android.build.api.transform.*
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.*
import me.yanglw.android.sting.annotation.Sting
import me.yanglw.android.sting.compiler.extension.StingExtension
import me.yanglw.android.sting.compiler.hook.Hook
import me.yanglw.android.sting.compiler.hook.OnClickListenerHook
import me.yanglw.android.sting.compiler.hook.StingAnnotationHook
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class StingTransform(private val project: Project, private val android: BaseExtension, private val sting: StingExtension)
  : Transform() {
  private lateinit var pool: ClassPool
  private lateinit var hookList: MutableList<Hook>

  override fun getName(): String = "androidSting"

  override fun getParameterInputs(): Map<String, Any> = Collections.singletonMap("androidSting", sting.toString())

  override fun getInputTypes(): Set<QualifiedContent.ContentType> = TransformManager.CONTENT_CLASS

  override fun getScopes(): MutableSet<in QualifiedContent.Scope> = TransformManager.SCOPE_FULL_PROJECT

  override fun isIncremental(): Boolean = false

  @Throws(TransformException::class, InterruptedException::class, IOException::class)
  override fun transform(transformInvocation: TransformInvocation) {
    transformInvocation.outputProvider.deleteAll()

    if (!sting.enable) {
      transformFiles(transformInvocation)
      return
    }

    pool = object : ClassPool(true) {
      override fun getClassLoader(): ClassLoader = Loader(ClassLoader.getSystemClassLoader(), this)
    }

    project.log("=====================load boot class path=====================")
    android.bootClasspath.forEach {
      project.log("load boot class path : ${it.absolutePath}")
      pool.appendClassPath(it.absolutePath)
    }
    project.log("load annotation Sting")
    pool.appendClassPath(ClassClassPath(Sting::class.java))

    hookList = mutableListOf()
    if (sting.onClick.enable) {
      hookList.add(OnClickListenerHook(sting.onClick))
    }
    if (sting.anno.enable) {
      hookList.add(StingAnnotationHook(sting.anno))
    }

    loadAllClasses(transformInvocation.inputs, transformInvocation.outputProvider)
    hookAllClasses(transformInvocation.inputs, transformInvocation.outputProvider)
  }

  /**
   * 复制所有的 class 输入至对应的输出文件。
   *
   * 此方法仅在功能关闭时调用。
   */
  private fun transformFiles(transformInvocation: TransformInvocation) {
    val inputs = transformInvocation.inputs
    val outputProvider = transformInvocation.outputProvider
    inputs.forEach {
      it.jarInputs.forEach { jar ->
        val outFile = outputProvider.getContentLocation(jar.name,
                                                        jar.contentTypes,
                                                        jar.scopes,
                                                        Format.JAR)
        FileUtils.copyFile(jar.file, outFile)
      }
      it.directoryInputs.forEach { dir ->
        val outDir = outputProvider.getContentLocation(dir.name,
                                                       dir.contentTypes,
                                                       dir.scopes,
                                                       Format.DIRECTORY)
        FileUtils.copyDirectory(dir.file, outDir)
      }
    }
  }

  /**
   * 加载项目所有的 jar 和 class 至 @link [pool] 中。
   *
   * 目的是为了解决判断类的继承关系时，有些类还没有加载导致的 [ClassNotFoundException] 。
   */
  private fun loadAllClasses(inputs: Collection<TransformInput>, outputProvider: TransformOutputProvider) {
    inputs.forEach {
      it.jarInputs.forEach { jar ->
        pool.appendClassPath(jar.file.absolutePath)
      }
      it.directoryInputs.forEach { dir ->
        pool.appendClassPath(dir.file.absolutePath)
      }
    }
  }

  /**
   * hook 所有的 class 。
   *
   * 如果 class 不需要处理则输出至对应的输出文件。
   */
  private fun hookAllClasses(inputs: Collection<TransformInput>, outputProvider: TransformOutputProvider) {
    inputs.forEach {
      it.jarInputs.forEach { jar ->
        project.log("load jar : ${jar.file.absolutePath}")
        val outFile = outputProvider.getContentLocation(jar.name,
                                                        jar.contentTypes,
                                                        jar.scopes,
                                                        Format.JAR)
        hookJar(jar.file, outFile)
      }

      it.directoryInputs.forEach { dir ->
        project.log("load file : ${dir.file.absolutePath}")
        val outDir = outputProvider.getContentLocation(dir.name,
                                                       dir.contentTypes,
                                                       dir.scopes,
                                                       Format.DIRECTORY)
        hookFile(dir.file, dir.file, outDir)
      }
    }
  }

  /**
   * hook jar 。
   *
   * 会遍历 jar 中的所有 class 文件，若该 class 需要 hook 则输出 hook 后的字节码，否则输出原始的字节码。
   *
   * @param file 输入的 jar 文件。
   * @param outFile jar 文件对应的输出文件。
   */
  private fun hookJar(file: File, outFile: File) {
    val originJarFile = JarFile(file)
    val jarOutputStream = JarOutputStream(FileOutputStream(outFile))

    originJarFile
        .stream()
        .forEach {
          jarOutputStream.putNextEntry(JarEntry(it.name))

          if (FilenameUtils.isExtension(it.name, "class")) {
            val inputStream = originJarFile.getInputStream(it)
            val clz = pool.makeClass(inputStream)
            inputStream.close()

            if (hookClass(clz)) {
              jarOutputStream.write(clz.toBytecode())
              jarOutputStream.closeEntry()
              return@forEach
            }
          }

          val inputStream = originJarFile.getInputStream(it)
          IOUtils.copy(inputStream, jarOutputStream)
          inputStream.close()

          jarOutputStream.closeEntry()
        }
    jarOutputStream.close()
    originJarFile.close()
  }

  /**
   * hook class 文件或者 class 目录。若 [file] 为目录，则将递归遍历所有子文件夹。
   *
   * 若该 class 需要 hook 则输出 hook 后的字节码，否则输出原始的字节码。
   *
   * @param file 当前需要处理的文件/文件夹。
   * @param inputDir [file] 的起始目录。
   * @param outDir [inputDir] 对应的输出目录。
   */
  private fun hookFile(file: File, inputDir: File, outDir: File) {
    if (file.isDirectory) {
      file.listFiles()?.forEach {
        hookFile(it, inputDir, outDir)
      }
    } else {
      if (FilenameUtils.isExtension(file.name, "class")) {
        val inputStream = file.inputStream()
        val clz = pool.makeClass(inputStream)
        inputStream.close()

        if (hookClass(clz)) {
          clz.writeFile(outDir.path)
          return
        }
      }

      val targetFile = File(outDir, file.relativeTo(inputDir).path)
      project.log("copy class $file to $targetFile")
      FileUtils.copyFile(file, targetFile)
    }
  }

  /**
   * hook class 。
   *
   * 首先会简单判断 class 是否需要进行 hook ，之后会使用 [hookList] 遍历 hook class 。
   *
   * @return 若该 class 被 hook 返回 true ，否则返回 false 。
   */
  private fun hookClass(clz: CtClass): Boolean {
    if (clz.simpleName.contains(Regex("^R\$|^R\\\$\\w+\$|^BuildConfig\$"))) {
      return false
    }

    if (Modifier.isInterface(clz.modifiers)) {
      return false
    }
    return hookList.map { it.hook(project, pool, clz) }.contains(true)
  }
}
