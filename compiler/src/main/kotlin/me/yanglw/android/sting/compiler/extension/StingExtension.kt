package me.yanglw.android.sting.compiler.extension

import org.gradle.api.plugins.ExtensionAware

open class StingExtension {
  var enable = true
  var onClick: OnClickExtension
  var anno: AnnoExtension

  init {
    this as ExtensionAware
    anno = extensions.create("anno", AnnoExtension::class.java)
    onClick = extensions.create("onClick", OnClickExtension::class.java)
  }

  override fun toString(): String {
    return "StingExtension(enable=$enable, onClick=$onClick, anno=$anno)"
  }

  companion object {
    const val DEFAULT_INTERVAL: Long = 2000
  }
}