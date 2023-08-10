package com.funlabyrinthe.editor.renderer.domext

import scala.scalajs.js

import org.scalajs.dom.ImageBitmap

trait ImageBitmapRenderingContext extends js.Object:
  def transferFromImageBitmap(bitmap: ImageBitmap): Unit
end ImageBitmapRenderingContext
