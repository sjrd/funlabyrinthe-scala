package com.funlabyrinthe.editor.renderer

import scala.util.{Failure, Success, Try}

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.typedarray.*

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import com.funlabyrinthe.coreinterface.*
import com.funlabyrinthe.coreinterface as intf

import com.funlabyrinthe.editor.renderer.LaminarUtils.*
import com.funlabyrinthe.editor.renderer.scene

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{BusyIndicatorSize, IconName, MessageStripDesign, ToolbarAlign}

class ProjectRunner(val project: Project, returnToProjectSelector: Observer[Unit])(using ErrorHandler):
  import ProjectRunner.*

  val runningGame: Signal[Option[RunningGame]] =
    Signal.fromJsPromise(JSPI.async {
      val (universe, errors) = project.loadUniverse()
      if errors.nonEmpty then
        throw IllegalStateException(
          "There were errors while loading the game:"
          + errors.mkString("\n", "\n", "")
        )
      else
        universe.startGame()
    })
  end runningGame

  val topElement: Element =
    div(
      ui5.Toolbar(
        _.alignContent := ToolbarAlign.Start,
        _.button(
          _.icon(IconName.`sys-back`),
          _.text("Back to project selector"),
          _.events.onClick.mapToUnit --> returnToProjectSelector,
        ),
      ),
      child <-- runningGame.recoverToTry.map {
        case Success(None) =>
          ui5.BusyIndicator(
            _.size := BusyIndicatorSize.L,
            _.active := true,
          )
        case Success(Some(game)) =>
          indigoGameElement(game)
        case Failure(exception) =>
          ui5.MessageStrip(
            _.design := MessageStripDesign.Negative,
            ErrorHandler.exceptionToString(exception),
            _.hideCloseButton := true,
          )
      }
    )
  end topElement

  def indigoGameElement(game: RunningGame): Div = {
    div(
      idAttr := "indigo-container",
      onMountUnmountCallbackWithState[Div, IndigoWrapper]({ ctx =>
        val indigoUI = IndigoWrapper(game, game.players.head)
        indigoUI.launch(ctx.thisNode.ref.id)
        indigoUI
      }, { (thisNode, optIndigoUI) =>
        for indigoUI <- optIndigoUI do
          indigoUI.halt()
      }),
    )
  }

  def makeRunnerCanvas(game: RunningGame): CanvasElement =
    canvasTag(
      onMountUnmountCallbackWithState(
        mount = { ctx =>
          new PlayerCanvasState(game, game.players.head, ctx.thisNode.ref).init()
        },
        unmount = { (element, optState) =>
          for state <- optState do
            state.destroy()
        },
      ),
    )
  end makeRunnerCanvas

  final class PlayerCanvasState(game: RunningGame, player: Player, canvas: dom.HTMLCanvasElement):
    private var lastAnimationRequestHandle: Int = 0
    private var lastMillis = Double.NaN

    private val onKeyDownListener: js.Function1[dom.KeyboardEvent, Unit] = { event =>
      val normalizedPhysicalKey = physicalKeyNormalizations.getOrElse(event.code, event.code)

      val intfEvent = new KeyboardEvent {
        val physicalKey = normalizedPhysicalKey
        val keyString = event.key
        val repeat = event.repeat
        val shiftDown = event.shiftKey
        val controlDown = event.ctrlKey
        val altDown = event.altKey
        val metaDown = event.metaKey
      }

      player.keyDown(intfEvent)
    }

    private def scheduleRedraw(): Unit =
      lastAnimationRequestHandle = dom.window.requestAnimationFrame { currentMillis =>
        val currentMillis1 = Math.floor(currentMillis)
        if !lastMillis.isNaN then
          game.advanceTickCount(currentMillis1 - lastMillis)
        lastMillis = currentMillis1

        canvas.width = player.viewWidth.toInt
        canvas.height = player.viewHeight.toInt
        player.drawView(canvas)

        scheduleRedraw()
      }
    end scheduleRedraw

    def init(): this.type =
      scheduleRedraw()
      dom.document.addEventListener("keydown", onKeyDownListener)
      this
    end init

    def destroy(): Unit =
      if lastAnimationRequestHandle != 0 then
        dom.window.cancelAnimationFrame(lastAnimationRequestHandle)
      dom.document.removeEventListener("keydown", onKeyDownListener)
    end destroy
  end PlayerCanvasState
end ProjectRunner

object ProjectRunner:
  private val baseURL = "./Resources/"
  private inline val ImageNamePrefix = "Images/"

  private val physicalKeyNormalizations: Map[String, String] =
    Map(
      "VolumeDown" -> "AudioVolumeDown",
      "VolumeMute" -> "AudioVolumeMute",
      "VolumeUp" -> "AudioVolumeUp",
    )
  end physicalKeyNormalizations

  private final class IndigoWrapper(
      game: RunningGame,
      player: Player,
  ) extends indigo.IndigoDemo[Unit, Unit, Unit, Unit] {
    import IndigoWrapper.*
    import indigo.{mutable => _, *}

    private val imageCache = mutable.Map.empty[String, Option[Image]]

    private val pendingAssets: mutable.HashMap[AssetName, AssetType] =
      mutable.HashMap.empty
    private val knownAssets: mutable.HashSet[AssetName] =
      mutable.HashSet.empty
    private val loadedAssets: mutable.HashSet[AssetName] =
      mutable.HashSet.empty

    def extractNewAssetsToLoad(): Set[AssetType] = {
      if pendingAssets.isEmpty then
        Set.empty
      else
        val result = pendingAssets.valuesIterator.toSet
        pendingAssets.clear()
        result
    }

    private var lastBindingKey: Int = 0

    def boot(flags: Map[String, String]): Outcome[BootResult[Unit, Unit]] = {
      Outcome {
        val config = GameConfig(
          width = player.viewWidth.toInt,
          height = player.viewHeight.toInt,
        ).noResize
        BootResult(config, ())
          .addAssets(AssetType.Image(LoadingAssetName, AssetPath("./Resources/Images/Fields/Hole.png")))
      }
    }

    def eventFilters: EventFilters = EventFilters.AllowAll

    def initialModel(startupData: Unit): Outcome[Unit] = unitOutcome

    def initialViewModel(startupData: Unit, model: Unit): Outcome[Unit] = unitOutcome

    def present(context: Context[Unit], model: Unit, viewModel: Unit): Outcome[SceneUpdateFragment] =
      import scene.SceneSerializers.given

      /*return Outcome(
        SceneUpdateFragment(
          Shape.Box(Rectangle(200, 100), Fill.Color(RGBA.Blue))
        )
      )*/

      val baseOutcome = Outcome {
        try
          val serialized = player.presentView()
          val deserialized = upickle.readBinary[scene.SceneUpdateFragment](serialized.toArray)
          convertSceneUpdateFragment(deserialized)
        catch case th: Throwable =>
          th.printStackTrace()
          throw th
      }

      val pendingAssets = extractNewAssetsToLoad()
      if pendingAssets.isEmpty then
        baseOutcome
      else
        lastBindingKey += 1
        baseOutcome
          .addGlobalEvents(AssetEvent.LoadAssetBatch(pendingAssets, BindingKey(lastBindingKey.toString()), true))

    private def convertSceneUpdateFragment(fragment: scene.SceneUpdateFragment): SceneUpdateFragment = {
      SceneUpdateFragment(convertBatchOfSceneNodes(fragment.nodes))
    }

    private def convertBatchOfSceneNodes(batch: scene.Batch[scene.SceneNode]): Batch[SceneNode] =
      Batch(batch.map(convertSceneNode(_))*)

    private def convertSceneNode(node: scene.SceneNode): SceneNode = {
      node match
        case scene.Graphic(material, crop, position, ref) =>
          Graphic(convertRectange(crop), convertMaterial(material))
            .withRef(convertPoint(ref))
            .moveTo(convertPoint(position))
        case scene.Group(children, position, ref) =>
          Group(convertBatchOfSceneNodes(children))
            .withRef(convertPoint(ref))
            .moveTo(convertPoint(position))
    }

    private def convertRectange(rect: scene.Rectangle): Rectangle =
      Rectangle(convertPoint(rect.topLeft), convertSize(rect.size))

    private def convertPoint(point: scene.Point): Point =
      Point(point.x, point.y)

    private def convertSize(size: scene.Size): Size =
      Size(size.width, size.height)

    private def convertMaterial(material: scene.Material): Material.ImageEffects =
      Material.ImageEffects(convertImageAsset(material.asset), material.alpha)

    private def convertImageAsset(name: String): AssetName = {
      val relPath = ImageNamePrefix + name
      val assetName = AssetName(relPath)
      if loadedAssets.contains(assetName) then
        assetName
      else
        if knownAssets.add(assetName) then
          pendingAssets(assetName) = AssetType.Image(assetName, AssetPath(baseURL + relPath + ".png"))
        LoadingAssetName
    }

    def setup(bootData: Unit, assetCollection: AssetCollection, dice: Dice): Outcome[Startup[Unit]] =
      val s = assetCollection.images.map(_.name)
      if s.nonEmpty then println(s)
      loadedAssets ++= assetCollection.images.map(_.name)
      Outcome(Startup.Success(()))

    def updateModel(context: Context[Unit], model: Unit): GlobalEvent => Outcome[Unit] = {
      case FrameTick =>
        game.advanceTickCount(context.frame.time.delta.toMillis.toDouble)
        unitOutcome

      case AssetEvent.AssetBatchLoaded(a, b, c) =>
        unitOutcome

      case AssetEvent.AssetBatchLoadError(_, message) =>
        System.err.println(s"Error loading assets: $message")
        unitOutcome

      case e: KeyboardEvent =>
        val intfEvent: intf.KeyboardEvent = new intf.KeyboardEvent {
          val physicalKey = e.key.code.value
          val keyString = e.key.key
          val repeat = false
          val shiftDown = e.isShiftKeyDown
          val controlDown = e.isCtrlKeyDown
          val altDown = e.isAltKeyDown
          val metaDown = e.isMetaKeyDown
        }
        player.keyDown(intfEvent)
        unitOutcome

      case _ =>
        unitOutcome
    }

    def updateViewModel(context: Context[Unit], model: Unit, viewModel: Unit): GlobalEvent => Outcome[Unit] =
      _ => unitOutcome
  }

  object IndigoWrapper {
    import indigo.*

    val unitOutcome: Outcome[Unit] = Outcome(())

    val LoadingAssetName = AssetName("<loading>")
  }
end ProjectRunner
