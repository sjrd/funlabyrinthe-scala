package com.funlabyrinthe.editor.common

import scala.scalajs.js

trait CompilerService extends js.Object:
  import CompilerService.*

  def compileProject(
    projectID: String,
    dependencyClasspath: js.Array[String],
    fullClasspath: js.Array[String]
  ): js.Promise[Result]
end CompilerService

object CompilerService:
  trait Result extends js.Object:
    val logLines: js.Array[String]
    val success: Boolean
    val moduleClassNames: js.Array[String]
  end Result
end CompilerService
