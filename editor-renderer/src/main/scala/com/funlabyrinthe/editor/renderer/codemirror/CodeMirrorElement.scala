package com.funlabyrinthe.editor.renderer.codemirror

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import org.scalajs.dom

import com.raquo.airstream.ownership.Owner
import com.raquo.laminar.api.L.{*, given}

import org.scalablytyped.runtime.StringDictionary

import typings.codemirrorAutocomplete.mod.*
import typings.codemirrorCommands.mod.*
import typings.codemirrorLanguage.mod.*
import typings.codemirrorLint.mod.*
import typings.codemirrorSearch.mod.*
import typings.codemirrorState.mod.*
import typings.codemirrorView.mod.{EditorView, *}

import typings.replitCodemirrorIndentationMarkers.mod.*

object CodeMirrorElement:
  private val indentationMarkersExtension: js.Array[Extension] =
    import typings.replitCodemirrorIndentationMarkers.anon.ActiveDark

    val indentationMarkersColors = ActiveDark()
      .setDark("#2a4c55")
      .setActiveDark("#3f5e66")

    indentationMarkers(
      IndentationMarkerConfiguration()
        .setColors(indentationMarkersColors)
        .setThickness(2.0)
    )
  end indentationMarkersExtension

  val BaseExtensions: List[Extension | LanguageSupport | js.Array[Extension]] = List(
    EditorView.theme(StringDictionary()),
    lineNumbers(),
    highlightSpecialChars(),
    history(),
    drawSelection(),
    dropCursor(),
    EditorState.allowMultipleSelections.of(true),
    indentOnInput(),
    bracketMatching(),
    closeBrackets(),
    rectangularSelection(),
    crosshairCursor(),
    highlightSelectionMatches(),
    indentationMarkersExtension,
    keymap.of(closeBracketsKeymap ++ defaultKeymap ++ historyKeymap ++ foldKeymap ++ completionKeymap ++ lintKeymap ++ searchKeymap),
    /*StateField
      .define(StateFieldSpec[Set[api.Instrumentation]](_ => props.instrumentations, (value, _) => value))
      .extension,*/
    //DecorationProvider(props),
    EditorState.tabSize.of(2),
    //Prec.highest(EditorKeymaps.keymapping(props)),
    //InteractiveProvider.interactive.of(InteractiveProvider(props).extension),
    SyntaxHighlightingTheme.highlightingTheme,
    lintGutter(),
  )

  def apply(
    highlightingInitialized: ScalaSyntaxHighlightingInit.Initialized,
    initialDoc: String,
    viewUpdatesObserver: Observer[ViewUpdate],
    problems: Signal[List[Problem]],
  ): Element =
    apply(highlightingInitialized, initialDoc, BaseExtensions, viewUpdatesObserver, problems)

  def apply(
    highlightingInitialized: ScalaSyntaxHighlightingInit.Initialized,
    initialDoc: String,
    extensions: List[Extension | LanguageSupport | js.Array[Extension]],
    viewUpdatesObserver: Observer[ViewUpdate],
    problems: Signal[List[Problem]],
  ): Element =
    val baseExtensions = (extensions: List[Any]).toJSArray
    new CodeMirrorElement(highlightingInitialized, initialDoc, baseExtensions, viewUpdatesObserver, problems).topElement
  end apply
end CodeMirrorElement

private class CodeMirrorElement(
  highlightingInitialized: ScalaSyntaxHighlightingInit.Initialized,
  initialDoc: String,
  baseExtensions0: js.Array[Any],
  viewUpdatesObserver: Observer[ViewUpdate],
  problems: Signal[List[Problem]],
):
  private val transactionsBus = new EventBus[EditorState => Transaction]
  private val updatesBus = new EventBus[ViewUpdate]

  private val baseExtensions =
    val scalaLanguage = ViewPlugin.define(
      editorView => new ScalaSyntaxHighlightingHandler(highlightingInitialized, editorView.state.doc.toString),
      PluginSpec[ScalaSyntaxHighlightingHandler]().setDecorations(_.decorations)
    ).extension

    val extraExtensions = List(
      scalaLanguage,
      EditorView.updateListener.of(updatesBus.emit(_)),
    )

    baseExtensions0 ++ extraExtensions
  end baseExtensions

  lazy val topElement: Element =
    div(
      cls := "source-editor-codemirror-parent",
      onMountUnmountCallbackWithState(
        mount = { ctx =>
          setupCodeMirror(ctx.thisNode.ref)(using ctx.owner)
        },
        unmount = { (ctx, editorViewOpt) =>
          editorViewOpt.foreach(_.destroy())
        }
      ),
      updatesBus.events --> viewUpdatesObserver,
    )
  end topElement

  private def enqueueTransaction(makeTransaction: EditorState => Transaction): Unit =
    transactionsBus.emit(makeTransaction)

  private def setupCodeMirror(parent0: dom.HTMLElement)(using Owner): EditorView =
    val editor = new EditorView(new {
      this.doc = initialDoc
      this.extensions = baseExtensions
      this.parent = parent0
    })

    transactionsBus.events.foreach { makeTransaction =>
      editor.dispatch(makeTransaction(editor.state))
    }

    problems.changes.foreach { problems =>
      transactionsBus.emit { state =>
        val spec = setDiagnostics(state, problems.flatMap(problemToDiagnostic(state, _)).toJSArray)
        state.update(spec)
      }
    }

    editor
  end setupCodeMirror

  private def problemToDiagnostic(state: EditorState, problem: Problem): Option[Diagnostic] =
    if problem.line > state.doc.lines then
      None
    else
      val lineFrom = state.doc.line(problem.line).from
      val from = lineFrom + (problem.startColumn - 1)
      val to = lineFrom + (problem.endColumn - 1)
      val severity = problem.severity match
        case Problem.Severity.Info    => Severity.info
        case Problem.Severity.Warning => Severity.warning
        case Problem.Severity.Error   => Severity.error

      Some(Diagnostic(from, problem.message, severity, to))
  end problemToDiagnostic
end CodeMirrorElement
