package com.funlabyrinthe.editor.main.node

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object fsPromises:
  @js.native
  @JSImport("fs/promises")
  def writeFile(file: String, data: String, encoding: String): js.Promise[Unit] = js.native
end fsPromises
