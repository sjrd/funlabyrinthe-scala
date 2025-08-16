package com.funlabyrinthe.editor.renderer

import scala.util.{Failure, Success}

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.coreinterface.*

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{BusyIndicatorSize, ButtonDesign, IconName, ValueState}

import com.funlabyrinthe.editor.renderer.codemirror.{Problem, ScalaSyntaxHighlightingInit}
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.InspectedProperty
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.PropSetEvent
import com.funlabyrinthe.editor.renderer.electron.fileService
import com.funlabyrinthe.editor.renderer.electron.compilerService

class ProjectEditor(
  val project: Project,
  returnToProjectSelector: Observer[Unit],
)(using ErrorHandler, Dialogs):
  import ProjectEditor.*

  val projectIsModified = Var[Boolean](false)
  val projectModifications = projectIsModified.writer.contramap((u: Unit) => true)

  val sourcesVar = Var(project.sourceFiles.toList)
  private def updateSourcesVar(): Unit = sourcesVar.set(project.sourceFiles.toList)
  val sourcesSignal = sourcesVar.signal

  private val settingsEditor = ProjectSettingsEditor(project)
  private val universeEditor = UniverseLoadingEditor(project)

  val openEditors = Var[List[Editor]](List(settingsEditor, universeEditor))
  val selectedEditor = Var[Editor](if project.isLibrary then settingsEditor else universeEditor)

  val selectedSourceEditor: Signal[Option[SourceEditor]] =
    selectedEditor.signal.map {
      case sourceEditor: SourceEditor => Some(sourceEditor)
      case _                          => None
    }

  def markModified(): Unit =
    projectModifications.onNext(())

  val compilerLogVar = Var[List[String]](Nil)
  val compilerLog = compilerLogVar.signal
  val compilerLogNoANSI = compilerLog.map(_.map(stripANSICodes(_)))

  val compilerProblems = compilerLogNoANSI.map(Problem.parseFromLogs(_))

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
        _.events.onItemClick.compose(_.withCurrentValueOf(selectedEditor)) --> { (event, editor) =>
          ErrorHandler.handleErrors {
            event.detail.text match
              case "Save"          => editor.saveContent()
              case "Save all"      => saveAll()
              case "Close project" => returnToProjectSelector.onNext(())
              case "Exit"          => exit()
          }
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
        _.events.onItemClick --> { event =>
          ErrorHandler.handleErrors {
            if event.detail.item.dataset.contains("sourcesmenu") then
              openSourceFile(event.detail.text)
            else
              event.detail.text match
                case "New"     => openNewSourceDialogBus.emit(true)
                case "Compile" => compileSources()
          }
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
              else if project.sourceFiles.contains(name) then ValueState.Negative
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
              openDialogBus.writer.onNext(false)
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

  private lazy val tabIdentifierAttr = new HtmlAttr[SelectedTab]("data-tabidentifier", new {
    def encode(selectedTab: SelectedTab): String = selectedTab.serialized
    def decode(serialized: String): SelectedTab = SelectedTab.deserialize(serialized)
  })

  private lazy val tabs =
    ui5.TabContainer(
      cls := "main-tab-container",
      children <-- openEditors.signal.split(identity) { (_, initial, sig) =>
        ui5.Tab(
          _.text <-- initial.isModified.map(modified => initial.tabTitle + (if modified then " ●" else "")),
          _.selected <-- selectedEditor.signal.map(_ == initial),
          child <-- initial.topElement,
        )
      },
      _.events.onTabSelect.map(_.detail.tabIndex).compose(_.withCurrentValueOf(openEditors)) -->
        selectedEditor.writer.contramap { (tabIndexAndEditors: (Int, List[Editor])) =>
          val (tabIndex, editors) = tabIndexAndEditors
          editors(tabIndex)
        },
    )
  end tabs

  private def saveAll(): Unit =
    for editor <- openEditors.now() do
      editor.saveContent()
  end saveAll

  private def exit(): Unit =
    dom.window.close()
  end exit

  private def doCreateNewSource(sourceName: String): Unit =
    val content = createContentForNewSource(sourceName)
    JSPI.await(fileService.saveSourceFile(project.projectID.id, sourceName, content))
    markModified()
    project.sourceFiles += sourceName
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
    saveAll()
    doCompileSources()
  end compileSources

  private def doCompileSources(): Unit =
    compilerLogVar.set(List("Compiling ..."))

    val result = JSPI.await(compilerService.compileProject(project.projectID.id))
    compilerLogVar.set(result.logLines.toList)
    result.logLines.foreach(println(_))
    println("----------")
    if !result.success then
      throw UserErrorMessage(s"There were compile errors")
    val newModuleClassNames =
      result.moduleClassNames.toList.filter(_ != "com.funlabyrinthe.core.Core")
    if newModuleClassNames != project.moduleClassNames then
      project.moduleClassNames = newModuleClassNames
      markModified()
  end doCompileSources

  private def openSourceFile(name: String): Unit =
    val existingEditor = openEditors.now().collectFirst {
      case sourceEditor: SourceEditor if sourceEditor.sourceName == name =>
        sourceEditor
    }
    existingEditor match
      case Some(existing) =>
        selectedEditor.set(existing)
      case None =>
        ErrorHandler.handleErrors {
          // Start loading the file in parallel
          val contentPromise = fileService.loadSourceFile(project.projectID.id, name)
          val highlightingInitialized = ScalaSyntaxHighlightingInit.initialize()
          val content = JSPI.await(contentPromise)
          val problems = compilerProblems.map(_.filter(_.sourceName == name))
          val newEditor = new SourceEditor(project, name, content, highlightingInitialized, problems)
          openEditors.update { prev =>
            prev :+ newEditor
          }
          selectedEditor.set(newEditor)
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
end ProjectEditor

object ProjectEditor:
  enum SelectedTab:
    case Settings
    case Universe
    case Source(sourceName: String)

    def serialized: String = toString()
  end SelectedTab

  object SelectedTab:
    def deserialize(str: String): SelectedTab = str match
      case "Settings"             => Settings
      case "Universe"             => Universe
      case s"Source($sourceName)" => Source(sourceName)
  end SelectedTab

  enum UniverseLoadingState:
    case NoUniverse
    case Loading
    case Loaded(universe: Universe, withErrors: Boolean)
    case ErrorsToConfirm(universe: Universe, errors: List[String])
    case FatalErrors(errors: List[String])

  private def stripANSICodes(str: String): String =
    fansi.Str(str, fansi.ErrorMode.Strip).plainText
end ProjectEditor
