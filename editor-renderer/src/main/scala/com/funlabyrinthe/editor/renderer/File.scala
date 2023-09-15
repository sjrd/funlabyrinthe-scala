package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future

import com.funlabyrinthe.editor.renderer.electron.fileService

final class File(val path: String):
  override def toString(): String = path

  def parent: File =
    require(path.contains('/'))
    File(path.substring(0, path.lastIndexOf('/')))

  def name: String =
    if path.contains('/') then path.substring(path.lastIndexOf('/') + 1)
    else path

  def /(child: String): File = File(path + "/" + child)

  def readAsString(): Future[String] =
    fileService.readFileToString(path).toFuture

  def writeString(content: String): Future[Unit] =
    fileService.writeStringToFile(path, content).toFuture

  def createDirectories(): Future[Unit] =
    fileService.createDirectories(path).toFuture
end File
