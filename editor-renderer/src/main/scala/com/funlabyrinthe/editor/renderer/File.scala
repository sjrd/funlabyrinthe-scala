package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future

final class File(val path: String):
  override def toString(): String = path

  def parent: File = File(path.substring(0, path.lastIndexOf('/')))

  def /(child: String): File = File(path + "/" + child)

  def readAsString(): Future[String] = ???

  def writeString(content: String): Future[Unit] = ???
end File
