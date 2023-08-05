package com.funlabyrinthe.editor

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import com.funlabyrinthe.core.*

import javafx.scene.input.{ KeyEvent => jfxKeyEvent }
import javafx.scene.input.KeyCode._

import scalafx.application.Platform
import scalafx.Includes._
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.scene.input._
import scalafx.scene.shape._
import scalafx.geometry._

import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory

class SourceEditorPane(val universeFile: UniverseFile, sourceName: String) extends BorderPane:
  center = editorCodeArea

  loadContent()

  private lazy val editorCodeArea: CodeArea =
    val codeArea = new CodeArea()

    // add line numbers to the left of area
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea))

    codeArea
  end editorCodeArea

  private def loadContent(): Unit =
    val file = new File(universeFile.sourcesDirectory, sourceName)
    val content = Files.readString(file.toPath, StandardCharsets.UTF_8)
    editorCodeArea.replaceText(content)
  end loadContent
end SourceEditorPane
