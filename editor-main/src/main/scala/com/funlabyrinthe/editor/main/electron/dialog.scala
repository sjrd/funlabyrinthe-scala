package com.funlabyrinthe.editor.main.electron

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("electron")
object dialog extends js.Object:
  def showSaveDialog(parent: BrowserWindow, options: SaveDialogOptions): js.Promise[SaveDialogResult] = js.native

  trait OpenSaveDialogOptions extends js.Object:
    var filters: js.UndefOr[js.Array[FileFilter]] = js.undefined
  end OpenSaveDialogOptions

  trait FileFilter extends js.Object:
    val name: String
    val extensions: js.Array[String]
  end FileFilter

  trait SaveDialogOptions extends OpenSaveDialogOptions:
  end SaveDialogOptions

  trait SaveDialogResult extends js.Object:
    val canceled: Boolean
    val filePath: js.UndefOr[String]
  end SaveDialogResult
end dialog
