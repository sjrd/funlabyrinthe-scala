package com.funlabyrinthe.editor.main.node

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object fsPromises:
  @js.native
  @JSImport("fs/promises")
  def readFile(file: String, encoding: String): js.Promise[String] = js.native

  @js.native
  @JSImport("fs/promises")
  def writeFile(file: String, data: String, encoding: String): js.Promise[Unit] = js.native

  @js.native
  @JSImport("fs/promises")
  def readdir(path: String): js.Promise[js.Array[String]] = js.native

  @js.native
  @JSImport("fs/promises")
  def mkdir(path: String, options: MkDirOptions = js.native): js.Promise[Unit] = js.native

  trait MkDirOptions extends js.Object:
    var recursive: js.UndefOr[Boolean] = js.undefined
    var mode: js.UndefOr[String | Int] = js.undefined
  end MkDirOptions
end fsPromises
