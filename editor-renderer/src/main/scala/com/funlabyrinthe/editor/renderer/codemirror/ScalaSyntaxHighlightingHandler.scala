package com.funlabyrinthe.editor.renderer.codemirror

import typings.codemirrorState.mod.ChangeSet
import typings.codemirrorState.mod._
import typings.codemirrorView.mod._
import typings.webTreeSitter.mod._

import scala.collection.mutable.ListBuffer

import scala.scalajs.js

// Copied from Scastie's SyntaxHighlightingHandler
class ScalaSyntaxHighlightingHandler(highlightingInitialized: ScalaSyntaxHighlightingInit.Initialized, initialState: String) extends js.Object {
  val query = highlightingInitialized.scalaQuery
  val parser = highlightingInitialized.newParser()
  parser.setLanguage(highlightingInitialized.scalaLanguage)

  val queryCaptureNames = query.captureNames

  var tree = parser.parse(initialState)
  var decorations: DecorationSet = computeDecorations()

  private def computeDecorations(): DecorationSet = {
    val rangeSetBuilder = new RangeSetBuilder[Decoration]()
    val captures = query.captures(tree.rootNode)

    captures.foldLeft(Option.empty[QueryCapture]){ (previousCapture, currentCapture) =>
      if (!previousCapture.exists(_ == currentCapture)) {
        val startPosition = currentCapture.node.startIndex
        val endPosition = currentCapture.node.endIndex

        val mark = Decoration.mark(
          MarkDecorationSpec()
            .setInclusive(true)
            .setClass(currentCapture.name.replace(".", "-")))
        rangeSetBuilder.add(startPosition, endPosition, mark)

      }
      Some(currentCapture)
    }

    rangeSetBuilder.finish()
  }

  private def indexToTSPoint(text: Text, index: Double): Point = {
    val line = text.lineAt(index)
    Point(index - line.from, line.number - 1)
  }

  private def mapChangesToTSEdits(changes: ChangeSet, originalText: Text, newText: Text): List[Edit] = {
    val editBuffer = new ListBuffer[Edit]()

    changes.iterChanges { (fromA: Double, toA: Double, _, toB: Double, _) => {
      val oldEndPosition = indexToTSPoint(originalText, toA)
      val newEndPosition = indexToTSPoint(newText, toB)
      val startPosition = indexToTSPoint(originalText, fromA)
      editBuffer.addOne(Edit(toB, newEndPosition, toA, oldEndPosition, fromA, startPosition))
    }}

    editBuffer.toList
  }

  var update: js.Function1[ViewUpdate, Unit] = { viewUpdate =>
    if (viewUpdate.docChanged) {
      val newText = viewUpdate.state.doc.toString
      val edits = mapChangesToTSEdits(viewUpdate.changes, viewUpdate.startState.doc, viewUpdate.state.doc)
      edits.foreach(tree.edit)
      tree = parser.parse(newText, tree)
      decorations = computeDecorations()
    }
  }
}
