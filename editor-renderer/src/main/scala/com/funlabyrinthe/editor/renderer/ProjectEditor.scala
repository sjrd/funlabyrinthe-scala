package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
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

  val openSourceEditors = Var[List[SourceEditor]](Nil)
  val selectedTab = Var[SelectedTab](if project.isLibrary then SelectedTab.Settings else SelectedTab.Universe)

  val selectedSourceEditor: Signal[Option[SourceEditor]] =
    openSourceEditors.signal.combineWith(selectedTab.signal).mapN { (openEditors, selected) =>
      selected match
        case SelectedTab.Source(sourceName) => openEditors.find(_.sourceName == sourceName)
        case _                              => None
    }

  val universeLoadingState: Var[UniverseLoadingState] = Var({
    if project.isLibrary then
      UniverseLoadingState.NoUniverse
    else
      UniverseLoadingState.Loading
  })

  locally {
    if !project.isLibrary then
      project.loadUniverse().onComplete {
        case Success((universe, Nil)) =>
          project.installUniverse(universe)
          universeLoadingState.set(UniverseLoadingState.Loaded(universe, withErrors = false))
        case Success((universe, errors)) =>
          universeLoadingState.set(UniverseLoadingState.ErrorsToConfirm(universe, errors))
        case Failure(exception) =>
          universeLoadingState.set(UniverseLoadingState.FatalErrors(List(ErrorHandler.exceptionToString(exception))))
      }
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
        _.events.onItemClick.compose(_.withCurrentValueOf(selectedSourceEditor)) --> { (event, editor) =>
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
        _.events.onItemClick --> { event =>
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

  private lazy val tabIdentifierAttr = new HtmlAttr[SelectedTab]("data-tabidentifier", new {
    def encode(selectedTab: SelectedTab): String = selectedTab.serialized
    def decode(serialized: String): SelectedTab = SelectedTab.deserialize(serialized)
  })

  private lazy val tabs =
    ui5.TabContainer(
      cls := "main-tab-container",
      universeEditorTab,
      projectSettingsEditorTab,
      children <-- openSourceEditors.signal.split(_.sourceName) { (sourceName, initial, sig) =>
        val mySelectedTab = SelectedTab.Source(sourceName)
        ui5.Tab(
          tabIdentifierAttr := mySelectedTab,
          _.text <-- initial.isModified.map(modified => sourceName + (if modified then " ●" else "")),
          _.selected <-- selectedTab.signal.map(_ == mySelectedTab),
          initial.topElement,
        )
      },
      _.events.onTabSelect.map(_.detail.tab.dataset("tabidentifier")) -->
        selectedTab.writer.contramap(SelectedTab.deserialize(_)),
    )
  end tabs

  private lazy val universeEditorTab: Element =
    def universeEditor(universe: Universe): UniverseEditor =
      val editor = new UniverseEditor(
        universe,
        projectModifications,
      )
      project.onResourceLoaded = { () =>
        editor.refreshUI()
      }
      editor
    end universeEditor

    ui5.Tab(
      tabIdentifierAttr := SelectedTab.Universe,
      _.text <-- projectIsModified.signal.map(modified => if modified then "Maps ●" else "Maps"),
      _.selected <-- selectedTab.signal.map(_ == SelectedTab.Universe),
      child <-- universeLoadingState.signal.map { state =>
        state match
          case UniverseLoadingState.NoUniverse =>
            ui5.Text("No maps in a library project")
          case UniverseLoadingState.Loading =>
            ui5.BusyIndicator(
              _.size := BusyIndicatorSize.L,
              _.active := true,
            )
          case UniverseLoadingState.Loaded(universe, withErrors) =>
            universeEditor(universe).topElement
          case UniverseLoadingState.ErrorsToConfirm(universe, errors) =>
            ???
          case UniverseLoadingState.FatalErrors(errors) =>
            ui5.NotificationList(
              errors.map { error =>
                ui5.NotificationList.item(
                  _.titleText := error,
                  _.state := ValueState.Negative,
                )
              },
            )
      },
    )
  end universeEditorTab

  private lazy val projectSettingsEditorTab: Element =
    ui5.Tab(
      tabIdentifierAttr := SelectedTab.Settings,
      _.text <-- projectIsModified.signal.map(modified => if modified then "Settings ●" else "Settings"),
      _.selected <-- selectedTab.signal.map(_ == SelectedTab.Settings),
      new ProjectSettingsEditor(project, projectModifications).topElement
    )
  end projectSettingsEditorTab

  private def save(selectedEditor: Option[SourceEditor]): Unit =
    ErrorHandler.handleErrors {
      selectedEditor match
        case None =>
          doSaveProject()
        case Some(editor) =>
          editor.saveContent()
    }
  end save

  private def doSaveProject(): Future[Unit] =
    val preserveOriginalUniverseFileContent = project.universe.isEmpty
    project.save(preserveOriginalUniverseFileContent).map(_ => projectIsModified.set(false))

  private def doSaveAll(): Future[Unit] =
    val editors = openSourceEditors.now()

    def loop(editors: List[SourceEditor]): Future[Unit] = editors match
      case Nil            => Future.successful(())
      case editor :: rest => editor.saveContent().flatMap(_ => loop(rest))
    end loop

    loop(editors).flatMap(_ => doSaveProject())
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
      _ <- fileService.saveSourceFile(project.projectID.id, sourceName, content).toFuture
    yield
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
    ErrorHandler.handleErrors {
      doSaveAll()
        .flatMap(_ => doCompileSources())
    }
  end compileSources

  private def doCompileSources(): Future[Unit] =
    compilerLogVar.set(List("Compiling ..."))

    for
      result <- compilerService.compileProject(project.projectID.id).toFuture
    yield
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
    end for
  end doCompileSources

  private def openSourceFile(name: String): Unit =
    if openSourceEditors.now().exists(_.sourceName == name) then
      selectedTab.set(SelectedTab.Source(name))
    else
      ErrorHandler.handleErrors {
        val highlightingInitializedFuture = ScalaSyntaxHighlightingInit.initialize()
        for
          content <- fileService.loadSourceFile(project.projectID.id, name).toFuture
          highlightingInitialized <- highlightingInitializedFuture
        yield
          openSourceEditors.update { prev =>
            if prev.exists(_.sourceName == name) then prev
            else
              val problems = compilerProblems.map(_.filter(_.sourceName == name))
              val newEditor = new SourceEditor(project, name, content, highlightingInitialized, problems)
              prev :+ newEditor
          }
          selectedTab.set(SelectedTab.Source(name))
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
