package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.coreinterface.*

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{ButtonDesign, IconName, ValueState}

import com.funlabyrinthe.editor.renderer.codemirror.{Problem, ScalaSyntaxHighlightingInit}
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.InspectedProperty
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.PropSetEvent
import com.funlabyrinthe.editor.renderer.electron.fileService
import com.funlabyrinthe.editor.renderer.electron.compilerService

class UniverseEditor(
  val universeFile: UniverseFile, returnToProjectSelector: Observer[Unit]
)(using ErrorHandler, Dialogs):
  val universeIsModified = Var[Boolean](false)
  val universeModifications = universeIsModified.writer.contramap((u: Unit) => true)

  val sourcesVar = Var(universeFile.sourceFiles.toList)
  private def updateSourcesVar(): Unit = sourcesVar.set(universeFile.sourceFiles.toList)
  val sourcesSignal = sourcesVar.signal

  val openSourceEditors = Var[List[SourceEditor]](Nil)
  val selectedSourceName = Var[Option[String]](None)

  val selectedSourceEditor: Signal[Option[SourceEditor]] =
    openSourceEditors.signal.combineWith(selectedSourceName.signal).mapN { (openEditors, selected) =>
      selected.flatMap(name => openEditors.find(_.sourceName == name))
    }

  val universeIntfUIState: Var[UniverseInterface.UIState] =
    Var(UniverseInterface.UIState.defaultFor(universeFile.universe))

  val universeIntf = universeIntfUIState.signal.map(UniverseInterface(universeFile.universe, _))

  def updateUniverseIntf(): Unit =
    universeIntfUIState.update(identity)

  def markModified(): Unit =
    universeModifications.onNext(())

  val compilerLogVar = Var[List[String]](Nil)
  val compilerLog = compilerLogVar.signal
  val compilerLogNoANSI = compilerLog.map(_.map(stripANSICodes(_)))

  val compilerProblems = compilerLogNoANSI.map(Problem.parseFromLogs(_))

  val setPropertyBus = new EventBus[PropSetEvent[?]]

  locally {
    universeFile.onResourceLoaded = { () =>
      updateUniverseIntf()
    }
  }

  lazy val topElement: Element =
    div(
      cls := "fill-parent-height",
      menu,
      tabs,
      compilerLogDisplay,
    )
  end topElement

  private lazy val menu =
    // feed the bus to open the menu at the fed element
    val openFileMenuBus: EventBus[Boolean] = new EventBus
    val openSourcesMenuBus: EventBus[Boolean] = new EventBus

    val openNewSourceDialogBus: EventBus[Boolean] = new EventBus

    div(
      newSourceDialog(openNewSourceDialogBus),
      ui5.Button("File", idAttr := "menu-button-file", _.events.onClick.mapTo(true) --> openFileMenuBus.writer),
      ui5.Menu(
        _.open <-- openFileMenuBus.events,
        _.openerId := "menu-button-file",
        _.item(_.text := "Save", _.icon := IconName.save),
        _.item(_.text := "Save all"),
        _.item(_.text := "Close project", _.icon := IconName.`sys-cancel`),
        _.item(_.text := "Exit", _.icon := IconName.`journey-arrive`),
        _.events.onItemClick.compose(_.withCurrentValueOf(universeIntf, selectedSourceEditor)) --> { (event, intf, editor) =>
          event.detail.text match
            case "Save"          => save(editor)
            case "Save all"      => saveAll()
            case "Close project" => returnToProjectSelector.onNext(())
            case "Exit"          => exit()
        },
      ),
      ui5.Button("Sources", idAttr := "menu-button-sources", _.events.onClick.mapTo(true) --> openSourcesMenuBus.writer),
      ui5.Menu(
        _.open <-- openSourcesMenuBus.events,
        _.openerId := "menu-button-sources",
        _.item(_.text := "New", _.icon := IconName.`add-document`),
        _.item(_.text := "Compile", _.icon := IconName.process),
        _.separator(),
        children <-- sourcesSignal.map(_.zipWithIndex).split(_._1) { (name, initial, sig) =>
          ui5.Menu.item(_.text := name, dataAttr("sourcesmenu") := "true")
        },
        _.events.onItemClick.compose(_.withCurrentValueOf(universeIntf)) --> { (event, intf) =>
          if event.detail.item.dataset.contains("sourcesmenu") then
            openSourceFile(event.detail.text)
          else
            event.detail.text match
              case "New"     => openNewSourceDialogBus.emit(true)
              case "Compile" => compileSources()
        },
      ),
    )
  end menu

  private def newSourceDialog(openDialogBus: EventBus[Boolean]): Element =
    val sourceName = Var[String]("")

    ui5.Dialog(
      _.showFromEvents(openDialogBus.events.filter(x => x).mapTo(())),
      _.closeFromEvents(openDialogBus.events.filter(x => !x).mapTo(())),
      _.headerText := "New source file",
      sectionTag(
        div(
          ui5.Label(_.forId := "sourcename", _.required := true, "Source name:"),
          ui5.Input(
            _.id := "sourcename",
            _.valueState <-- sourceName.signal.map(_ + ".scala").map { name =>
              if name == ".scala" then ValueState.Negative
              else if universeFile.sourceFiles.contains(name) then ValueState.Negative
              else ValueState.Positive
            },
            _.value <-- sourceName.signal,
            _.events.onChange.mapToValue --> sourceName.writer,
          ),
        ),
      ),
      _.slots.footer := div(
        div(flex := "1"),
        ui5.Button(
          _.design := ButtonDesign.Emphasized,
          "New",
          _.events.onClick.compose(_.sample(sourceName.signal)) --> { name =>
            ErrorHandler.handleErrors {
              doCreateNewSource(name + ".scala")
                .map(_ => openDialogBus.writer.onNext(false))
            }
          },
        ),
        ui5.Button(
          _.design := ButtonDesign.Negative,
          "Cancel",
          _.events.onClick.mapTo(false) --> openDialogBus.writer,
        ),
      )
    )
  end newSourceDialog

  private lazy val tabs =
    ui5.TabContainer(
      cls := "main-tab-container",
      setPropertyBus.events.withCurrentValueOf(universeIntf) --> { (event, intf) =>
        event.prop.setEditorValue(event.newValue)
        markModified()
        updateUniverseIntf()
      },
      mapEditorTab,
      children <-- openSourceEditors.signal.split(_.sourceName) { (sourceName, initial, sig) =>
        ui5.Tab(
          dataAttr("sourcename") := sourceName,
          _.text <-- initial.isModified.map(modified => sourceName + (if modified then " ●" else "")),
          _.selected <-- selectedSourceName.signal.map(_.contains(sourceName)),
          initial.topElement,
        )
      },
      _.events.onTabSelect.map(_.detail.tab.dataset.get("sourcename")) --> selectedSourceName.writer,
    )
  end tabs

  private lazy val mapEditor =
    new MapEditor(
      universeIntf,
      universeIntfUIState,
      setPropertyBus.writer,
      universeModifications,
    )
  end mapEditor

  private lazy val mapEditorTab: Element =
    ui5.Tab(
      _.text <-- universeIsModified.signal.map(modified => if modified then "Maps ●" else "Maps"),
      _.selected <-- selectedSourceName.signal.map(_.isEmpty),
      mapEditor.topElement,
    )

  private def save(selectedEditor: Option[SourceEditor]): Unit =
    ErrorHandler.handleErrors {
      selectedEditor match
        case None =>
          doSaveUniverse()
        case Some(editor) =>
          editor.saveContent()
    }
  end save

  private def doSaveUniverse(): Future[Unit] =
    universeFile.save().map(_ => universeIsModified.set(false))

  private def doSaveAll(): Future[Unit] =
    val editors = openSourceEditors.now()

    def loop(editors: List[SourceEditor]): Future[Unit] = editors match
      case Nil            => Future.successful(())
      case editor :: rest => editor.saveContent().flatMap(_ => loop(rest))
    end loop

    loop(editors).flatMap(_ => doSaveUniverse())
  end doSaveAll

  private def saveAll(): Unit =
    ErrorHandler.handleErrors {
      doSaveAll()
    }
  end saveAll

  private def exit(): Unit =
    dom.window.close()
  end exit

  private def doCreateNewSource(sourceName: String): Future[Unit] =
    val content = createContentForNewSource(sourceName)
    for
      _ <- fileService.saveSourceFile(universeFile.projectID.id, sourceName, content).toFuture
    yield
      markModified()
      universeFile.sourceFiles += sourceName
      updateSourcesVar()
      openSourceFile(sourceName)
  end doCreateNewSource

  private def createContentForNewSource(sourceName: String): String =
    val baseName = sourceName.stripSuffix(".scala")
    s"""package myfunlaby
      |
      |import com.funlabyrinthe.core.*
      |import com.funlabyrinthe.mazes.*
      |import com.funlabyrinthe.mazes.std.*
      |
      |object $baseName extends Module:
      |  override protected def createComponents()(using Universe): Unit =
      |    ()
      |  end createComponents
      |end $baseName
      |""".stripMargin
  end createContentForNewSource

  private def compileSources(): Unit =
    ErrorHandler.handleErrors {
      doSaveAll()
        .flatMap(_ => doCompileSources())
    }
  end compileSources

  private def doCompileSources(): Future[Unit] =
    compilerLogVar.set(List("Compiling ..."))

    for
      result <- compilerService.compileProject(universeFile.projectID.id).toFuture
    yield
      compilerLogVar.set(result.logLines.toList)
      result.logLines.foreach(println(_))
      println("----------")
      if !result.success then
        throw UserErrorMessage(s"There were compile errors")
      val newModuleClassNames =
        result.moduleClassNames.toList.filter(_ != "com.funlabyrinthe.core.Core")
      if newModuleClassNames != universeFile.moduleClassNames then
        universeFile.moduleClassNames = newModuleClassNames
        markModified()
    end for
  end doCompileSources

  private def openSourceFile(name: String): Unit =
    if openSourceEditors.now().exists(_.sourceName == name) then
      selectedSourceName.set(Some(name))
    else
      ErrorHandler.handleErrors {
        val highlightingInitializedFuture = ScalaSyntaxHighlightingInit.initialize()
        for
          content <- fileService.loadSourceFile(universeFile.projectID.id, name).toFuture
          highlightingInitialized <- highlightingInitializedFuture
        yield
          openSourceEditors.update { prev =>
            if prev.exists(_.sourceName == name) then prev
            else
              val problems = compilerProblems.map(_.filter(_.sourceName == name))
              val newEditor = new SourceEditor(universeFile, name, content, highlightingInitialized, problems)
              prev :+ newEditor
          }
          selectedSourceName.set(Some(name))
      }
  end openSourceFile

  private lazy val compilerLogDisplay: Element =
    div(
      cls := "compiler-log-container",
      textArea(
        cls := "compiler-log",
        readOnly := true,
        child.text <-- compilerLogNoANSI.map(_.mkString("", "\n", "\n")),
      )
    )
  end compilerLogDisplay

  private def stripANSICodes(str: String): String =
    fansi.Str(str, fansi.ErrorMode.Strip).plainText

end UniverseEditor
