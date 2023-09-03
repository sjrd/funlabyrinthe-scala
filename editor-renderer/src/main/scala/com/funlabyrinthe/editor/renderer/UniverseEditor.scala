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

import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.InspectedProperty
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.PropSetEvent
import com.funlabyrinthe.editor.renderer.electron.fileService

class UniverseEditor(val universeFile: UniverseFile)(using ErrorHandler):
  val sourcesVar = Var(universeFile.sourceFiles.toList)
  private def updateSourcesVar(): Unit = sourcesVar.set(universeFile.sourceFiles.toList)
  val sourcesSignal = sourcesVar.signal

  val openSourceEditors = Var[List[SourceEditor]](Nil)
  val selectedSourceName = Var[Option[String]](None)

  val universeIntfVar =
    Var({
      val universe = universeFile.universe
      val mapID = universe.allEditableMaps().head.id
      val currentFloor = 0
      UniverseInterface(universe, mapID, currentFloor, None)
    })

  val universeIntf = universeIntfVar.signal

  val mapMouseClickBus = new EventBus[MouseEvent]
  val setPropertyBus = new EventBus[PropSetEvent]

  locally {
    // Work around the initial loading time for images
    js.timers.setTimeout(200) {
      universeIntfVar.now().updated.foreach(universeIntfVar.set(_))
    }
  }

  lazy val topElement: Element =
    div(
      cls := "fill-parent-height",
      menu,
      tabs,
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
        _.item(_.text := "Exit", _.icon := IconName.`journey-arrive`),
        _.events.onItemClick.compose(_.withCurrentValueOf(universeIntf)) --> { (event, intf) =>
          event.detail.text match
            case "Save" => save(intf)
            case "Exit" => exit()
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
      mapMouseClickBus.events.withCurrentValueOf(universeIntf) --> { (event, intf) =>
        for result <- intf.mouseClickOnMap(event) do
          universeIntfVar.set(result)
      },
      setPropertyBus.events.withCurrentValueOf(universeIntf) --> { (event, intf) =>
        event.prop.setStringRepr(event.newValue)
        for result <- intf.updated do
          universeIntfVar.set(result)
      },
      mapEditorTab,
      children <-- openSourceEditors.signal.split(_.sourceName) { (sourceName, initial, sig) =>
        ui5.Tab(
          dataAttr("sourcename") := sourceName,
          _.text := sourceName,
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
      mapMouseClickBus.writer,
      universeIntfVar.updater(_.withSelectedComponentID(_)),
      setPropertyBus.writer,
    )
  end mapEditor

  private lazy val mapEditorTab: Element =
    ui5.Tab(
      _.text := "Maps",
      _.selected <-- selectedSourceName.signal.map(_.isEmpty),
      mapEditor.topElement,
    )

  private def save(intf: UniverseInterface): Unit =
    universeFile.save().onComplete(println(_))
  end save

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
    ???

  private def openSourceFile(name: String): Unit =
    openSourceEditors.update { prev =>
      if prev.exists(_.sourceName == name) then prev
      else prev :+ new SourceEditor(universeFile, name)
    }
    selectedSourceName.set(Some(name))
  end openSourceFile

end UniverseEditor
