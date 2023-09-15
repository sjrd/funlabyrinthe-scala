package com.funlabyrinthe.editor.common

import scala.scalajs.js

trait CompilerService extends js.Object:
  import CompilerService.*

  def compileProject(projectDir: String, classpath: js.Array[String]): js.Promise[Result]
end CompilerService

object CompilerService:
  trait Result extends js.Object:
    val logLines: js.Array[String]
    val success: Boolean
  end Result
end CompilerService
