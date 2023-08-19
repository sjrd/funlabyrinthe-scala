package com.funlabyrinthe.editor.main.electron

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("electron")
object dialog extends js.Object:
  def showOpenDialog(parent: BrowserWindow, options: OpenDialogOptions): js.Promise[OpenDialogResult] = js.native
  def showSaveDialog(parent: BrowserWindow, options: SaveDialogOptions): js.Promise[SaveDialogResult] = js.native

  trait OpenSaveDialogOptions extends js.Object:
    var filters: js.UndefOr[js.Array[FileFilter]] = js.undefined
  end OpenSaveDialogOptions

  trait FileFilter extends js.Object:
    val name: String
    val extensions: js.Array[String]
  end FileFilter

  trait OpenDialogOptions extends OpenSaveDialogOptions:
  end OpenDialogOptions

  trait SaveDialogOptions extends OpenSaveDialogOptions:
  end SaveDialogOptions

  trait OpenDialogResult extends js.Object:
    val canceled: Boolean
    val filePaths: js.Array[String]
  end OpenDialogResult

  trait SaveDialogResult extends js.Object:
    val canceled: Boolean
    val filePath: js.UndefOr[String]
  end SaveDialogResult
end dialog
