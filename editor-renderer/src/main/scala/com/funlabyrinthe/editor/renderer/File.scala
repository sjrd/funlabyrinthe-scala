package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future

import com.funlabyrinthe.editor.renderer.electron.fileService

final class File(val path: String):
  override def toString(): String = path

  def parent: File = File(path.substring(0, path.lastIndexOf('/')))

  def /(child: String): File = File(path + "/" + child)

  def readAsString(): Future[String] =
    fileService.readFileToString(path).toFuture

  def writeString(content: String): Future[Unit] =
    fileService.writeStringToFile(path, content).toFuture
end File
