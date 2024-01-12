package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.coreinterface.*

import com.funlabyrinthe.core.input.MouseEvent

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{ButtonDesign, IconName, ValueState}

import com.funlabyrinthe.editor.renderer.codemirror.ScalaSyntaxHighlightingInit
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.InspectedProperty
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.PropSetEvent
import com.funlabyrinthe.editor.renderer.electron.fileService
import com.funlabyrinthe.editor.renderer.electron.compilerService

class UniverseEditor(val universeFile: UniverseFile)(using ErrorHandler):
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

  val compilerLogVar = Var[String]("")
  val compilerLog = compilerLogVar.signal

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
    val openFileMenuBus: EventBus[dom.HTMLElement] = new EventBus
    val openSourcesMenuBus: EventBus[dom.HTMLElement] = new EventBus

    val openNewSourceDialogBus: EventBus[Boolean] = new EventBus

    div(
      newSourceDialog(openNewSourceDialogBus),
      ui5.Button("File", _.events.onClick.map(_.target) --> openFileMenuBus.writer),
      ui5.Menu(
        inContext { el =>
          openFileMenuBus.events.map(el.ref -> _) --> Observer[(ui5.Menu.Ref, dom.HTMLElement)](_.showAt(_))
        },
        _.item(_.text := "Save", _.icon := IconName.save),
        _.item(_.text := "Save all"),
        _.item(_.text := "Exit", _.icon := IconName.`journey-arrive`),
        _.events.onItemClick.compose(_.withCurrentValueOf(universeIntf, selectedSourceEditor)) --> { (event, intf, editor) =>
          event.detail.text match
            case "Save"     => save(editor)
            case "Save all" => saveAll()
            case "Exit"     => exit()
        },
      ),
      ui5.Button("Sources", _.events.onClick.map(_.target) --> openSourcesMenuBus.writer),
      ui5.Menu(
        inContext { el =>
          openSourcesMenuBus.events.map(el.ref -> _) --> Observer[(ui5.Menu.Ref, dom.HTMLElement)](_.showAt(_))
        },
        _.item(_.text := "New", _.icon := IconName.`add-document`),
        _.item(_.text := "Compile", _.icon := IconName.process),
        children <-- sourcesSignal.map(_.zipWithIndex).split(_._1) { (name, initial, sig) =>
          ui5.Menu.item(_.text := name, _.startsSection := initial._2 == 0, dataAttr("sourcesmenu") := "true")
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
              if name == ".scala" then ValueState.Error
              else if universeFile.sourceFiles.contains(name) then ValueState.Error
              else ValueState.Success
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
        universeModifications.onNext(())
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
    val sourceFile = universeFile.sourcesDirectory / sourceName

    val content = createContentForNewSource(sourceName)
    for
      _ <- fileService.createDirectories(universeFile.sourcesDirectory.path).toFuture
      _ <- sourceFile.writeString(content)
    yield
      universeModifications.onNext(())
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
      |
      |class $baseName(universe: Universe) extends Module(universe):
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
    val rootPath = universeFile.rootDirectory.path
    val sourceDir = universeFile.sourcesDirectory
    val targetDir = universeFile.targetDirectory
    val dependencyClasspath = universeFile.dependencyClasspath.map(_.path)
    val fullClasspath = universeFile.fullClasspath.map(_.path)

    compilerLogVar.set("Compiling ...")

    for
      _ <- sourceDir.createDirectories()
      _ <- targetDir.createDirectories()
      result <- compilerService.compileProject(rootPath, dependencyClasspath, fullClasspath).toFuture
    yield
      compilerLogVar.set(result.logLines.mkString("", "\n", "\n"))
      result.logLines.foreach(println(_))
      println("----------")
      for modClassName <- result.moduleClassNames do println(modClassName)
      if !result.success then
        throw UserErrorMessage(s"There were compile errors")
      universeFile.moduleClassNames = result.moduleClassNames.toList
    end for
  end doCompileSources

  private def openSourceFile(name: String): Unit =
    if openSourceEditors.now().exists(_.sourceName == name) then
      selectedSourceName.set(Some(name))
    else
      ErrorHandler.handleErrors {
        val highlightingInitializedFuture = ScalaSyntaxHighlightingInit.initialize()
        for
          content <- (universeFile.sourcesDirectory / name).readAsString()
          highlightingInitialized <- highlightingInitializedFuture
        yield
          openSourceEditors.update { prev =>
            if prev.exists(_.sourceName == name) then prev
            else prev :+ new SourceEditor(universeFile, name, content, highlightingInitialized)
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
        child.text <-- compilerLog.map(log => fansi.Str(log, fansi.ErrorMode.Strip).plainText),
      )
    )
  end compilerLogDisplay

end UniverseEditor
