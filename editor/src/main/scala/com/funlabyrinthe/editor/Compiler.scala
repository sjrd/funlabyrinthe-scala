package com.funlabyrinthe.editor

import java.io.File
import java.nio.charset.StandardCharsets

object Compiler:
  val ScalaVersion = "3.3.0"

  final class Result(
    val success: Boolean,
    val logLines: List[String],
  )

  def compileProject(sourceDirectory: File, destDirectory: File, classPath: List[File]): Result =
    val pb = new ProcessBuilder(
      "scala-cli",
      "compile",
      "--scala",
      ScalaVersion,
      "-cp",
      classPath.map(_.toString()).mkString(File.pathSeparator),
      "-d",
      destDirectory.toString(),
      ".",
    )
    pb.directory(sourceDirectory)
    pb.redirectErrorStream(true)
    val process = pb.start()
    val exitCode = process.waitFor()

    val logText: String =
      val logStream = process.getInputStream()
      try new String(logStream.readAllBytes(), StandardCharsets.UTF_8)
      finally logStream.close()

    val logLines = logText.linesIterator.toList

    Result(exitCode == 0, logLines)
  end compileProject
end Compiler
