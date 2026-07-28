package com.funlabyrinthe.gamerunner

import scala.util.{Failure, Success, Try}

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.scalajs.js.typedarray.*

import org.scalajs.dom

import com.funlabyrinthe.coreinterface.*
import com.funlabyrinthe.coreinterface as intf

import com.funlabyrinthe.graphics.html.PNGImage

import com.funlabyrinthe.gamerunner.scene

trait GameRunner extends js.Object {
  def halt(): Unit
}

object GameRunner {
  @JSExportTopLevel("start")
  def start(container: dom.HTMLElement, runningGame: RunningGame): GameRunner = {
    val wrapper = new IndigoWrapper(runningGame, runningGame.players.head)
    val indigoElem = dom.document.createElement("div").asInstanceOf[dom.HTMLElement]
    container.appendChild(indigoElem)
    wrapper.start(indigoElem)

    new GameRunner {
      def halt(): Unit = wrapper.halt()
    }
  }

  private val baseURL = "./Resources/"
  private inline val ImageNamePrefix = "Images/"

  private final class IndigoWrapper(
      runningGame: RunningGame,
      player: Player,
  ) extends indigo.BasicGameRuntime[Unit] {
    import indigo.{mutable => _, *}
    import tyrian.{GlobalMsg, Result}

    def start(container: dom.HTMLElement): Unit = {
      launch(container, Map(
        "width" -> player.viewWidth.toInt.toString(),
        "height" -> player.viewHeight.toInt.toString(),
      ))
    }

    val game: Game[?, ?, ?] = IndigoGame(runningGame, player)

    def halt(): Unit = game.system.halt()

    def settings: Settings = Settings.default

    def eventMapping: PartialIso[GlobalMsg, GlobalEvent] = PartialIso.none

    def init(flags: Map[String, String]): Result[Unit] = Result(())

    def update(model: Unit): GlobalMsg => Result[Unit] = {
      case _ => Result(())
    }

    // Hack the super view to use Extent.Fill for the container
    override def view(model: Unit): tyrian.HtmlRoot = {
      import tyrian.*
      import tyrian.ui.*

      val inherited = super.view(model)

      given Theme = Theme.None

      val surround: Batch[Elem[GlobalMsg]] => Html[GlobalMsg] =
        elems =>
          Container(
            Column(
              HtmlElement.many(elems)
            ).fillWidth.fillHeight
          )
            .withSize(
              Extent.Fill,
              Extent.Fill
            )
            .toHtml

      val fragment: HtmlFragment = inherited.fragment

      HtmlRoot(surround, fragment)
    }
}

  private final class IndigoGame(
      game: RunningGame,
      player: Player,
  ) extends indigo.Game[Unit, Unit, Unit] {
    import IndigoWrapper.*
    import indigo.{mutable => _, *}

    private var tickCount: Long = 0L

    private val defaultFontKey = fonts.DefaultFont.fontKey
    private val defaultFontAsset = AssetName("default-font-material")

    private val imageInfos: mutable.HashMap[String, ImageInfo] =
      mutable.HashMap.empty
    private val newAssetsToLoad: mutable.HashSet[AssetType] =
      mutable.HashSet.empty
    private val loadingAssetNames: mutable.HashMap[AssetName, ImageInfo] =
      mutable.HashMap.empty

    def gameId: GameId = GameId("indigo-game")

    def scenes(bootData: Unit): NonEmptyBatch[Scene[Unit]] =
      NonEmptyBatch(Scene.empty[Unit])

    def initialScene(bootData: Unit): Option[SceneName] =
      None

    def extractNewAssetsToLoad(): Set[AssetType] = {
      if newAssetsToLoad.isEmpty then
        Set.empty
      else
        val result = newAssetsToLoad.toSet
        newAssetsToLoad.clear()
        result
    }

    private var lastBindingKey: Int = 0

    def boot(flags: Map[String, String]): Outcome[BootResult[Unit, Unit]] = {
      Outcome {
        val config = EngineConfig.default
        BootResult(config, ())
          .addAssets(AssetType.Image(LoadingAssetName, AssetPath("./Resources/Images/Fields/Hole.png")))
          .addAssets(AssetType.Image(defaultFontAsset, AssetPath("../game-runner/fonts/DefaultFont.png")))
          .addFonts(fonts.DefaultFont.fontInfo)
      }
    }

    def eventFilters: EventFilters = EventFilters.AllowAll

    def initialModel(startupData: Unit): Outcome[Unit] = unitOutcome

    def initialViewModel(startupData: Unit, model: Unit): Outcome[Unit] = unitOutcome

    def present(context: Context, model: Unit): Outcome[SceneUpdateFragment] = {
      import scene.SceneSerializers.given

      var outcome: Outcome[SceneUpdateFragment] = Outcome {
        try
          val serialized = player.presentView()
          val deserialized = upickle.readBinary[scene.SceneUpdateFragment](serialized.toArray)
          convertSceneUpdateFragment(deserialized)
        catch case th: Throwable =>
          th.printStackTrace()
          throw th
      }

      val pendingAssets = extractNewAssetsToLoad()
      if pendingAssets.nonEmpty then
        lastBindingKey += 1
        outcome = outcome
          .addGlobalEvents(AssetEvent.LoadAssets(pendingAssets, BindingKey(lastBindingKey.toString()), true))

      val requestedSize = Size(player.viewWidth.toInt, player.viewHeight.toInt)
      if context.frame.viewport.size != requestedSize then
        val canvas = dom.document.getElementById(s"indigo-container").asInstanceOf[dom.HTMLElement | Null]
        println(s"$canvas ${canvas.style.toString()} ${requestedSize.width}")
        if canvas != null then
          canvas.style.width = s"${requestedSize.width}px"
          canvas.style.height = s"${requestedSize.height}px"
          //canvas.style = s"width: ${requestedSize.width}; height: ${requestedSize.height};"
          //canvas.width = requestedSize.width
          //canvas.height = requestedSize.height

      outcome
    }

    private def convertSceneUpdateFragment(fragment: scene.SceneUpdateFragment): SceneUpdateFragment = {
      SceneUpdateFragment(convertBatchOfSceneNodes(fragment.nodes))
    }

    private def convertBatchOfSceneNodes(batch: scene.Batch[scene.SceneNode]): Batch[SceneNode] =
      Batch(batch.map(convertSceneNode(_))*)

    private def convertBatchOfPoints(points: scene.Batch[scene.Point]): Batch[Point] =
      Batch(points.map(convertPoint(_))*)

    private def convertSceneNode(node: scene.SceneNode): SceneNode = {
      node match
        case scene.Graphic(material, crop, position, ref) =>
          val crop1 = convertRectange(crop)
          Graphic(crop1.size, convertMaterial(material))
            .withCrop(crop1)
            .withRef(convertPoint(ref))
            .moveTo(convertPoint(position))
        case scene.Group(children, position, ref) =>
          Group(convertBatchOfSceneNodes(children))
            .withRef(convertPoint(ref))
            .moveTo(convertPoint(position))
        case scene.Shape.Box(dimensions, fill, stroke, ref) =>
          Shape.Box(convertRectange(dimensions), convertFill(fill), convertStroke(stroke))
            .withRef(convertPoint(ref))
        case scene.Shape.Circle(circle, fill, stroke, ref) =>
          Shape.Circle(convertCircle(circle), convertFill(fill), convertStroke(stroke))
            .withRef(convertPoint(ref))
        case scene.Shape.Line(start, end, stroke, ref) =>
          Shape.Line(convertPoint(start), convertPoint(end), convertStroke(stroke))
            .withRef(convertPoint(ref))
        case scene.Shape.Polygon(vertices, fill, stroke, ref) =>
          Shape.Polygon(convertBatchOfPoints(vertices), convertFill(fill), convertStroke(stroke))
            .withRef(convertPoint(ref))
        case scene.Text(pos, text, font, textColor, ref) =>
          val material = Material.ImageEffects(defaultFontAsset).withTint(convertRGBA(textColor))
          Text(text, pos.x, pos.y, defaultFontKey, material)
            .withRef(convertPoint(ref))
    }

    private def convertRectange(rect: scene.Rectangle): Rectangle =
      Rectangle(convertPoint(rect.topLeft), convertSize(rect.size))

    private def convertCircle(circle: scene.Circle): Circle =
      Circle(convertPoint(circle.center), circle.radius)

    private def convertPoint(point: scene.Point): Point =
      Point(point.x, point.y)

    private def convertSize(size: scene.Size): Size =
      Size(size.width, size.height)

    private def convertFill(fill: scene.Fill): Fill =
      fill match
        case scene.Fill.Color(color) => Fill.Color(convertRGBA(color))

    private def convertStroke(stroke: scene.Stroke): Stroke =
      Stroke(stroke.width, convertRGBA(stroke.color))

    private def convertRGBA(rgba: scene.RGBA): RGBA =
      RGBA(rgba.red, rgba.green, rgba.blue, rgba.alpha)

    private def convertMaterial(material: scene.Material): Material.ImageEffects =
      Material.ImageEffects(convertImageAsset(material.asset), material.alpha)
        .withTint(convertRGBA(material.tint))

    private def convertImageAsset(name: String): AssetName = {
      imageInfos.get(name) match {
        case Some(info) =>
          info.frameAssetNameAt(tickCount)

        case None =>
          val relPath = ImageNamePrefix + name
          val info = new ImageInfo(relPath, baseURL + relPath + ".png")
          imageInfos += name -> info
          startLoadingImage(info)
          LoadingAssetName
      }
    }

    private def startLoadingImage(info: ImageInfo): Unit = {
      import scala.concurrent.ExecutionContext.Implicits.global

      println("start load " + info.relPath)

      newAssetsToLoad += AssetType.Image(info.baseAssetName, info.baseAssetPath)
      loadingAssetNames += info.baseAssetName -> info

      for
        response <- dom.fetch(info.basePath).toFuture
        buffer <- response.arrayBuffer().toFuture
        pngImage <- new PNGImage(buffer).future
      do
        val totalFrameCount = if pngImage.isAnimated then pngImage.frameBlobs.length else 1
        info.totalFrameCount = totalFrameCount
        info.pngImage = Some(pngImage)

        info.frameAssetNames = IArray.tabulate(totalFrameCount) { i =>
          if i == 0 then
            info.baseAssetName
          else
            val frameAssetName = AssetName(info.relPath + "/" + i)
            val frameAssetPath = AssetPath(dom.URL.createObjectURL(pngImage.frameBlobs(i)))
            newAssetsToLoad += AssetType.Image(frameAssetName, frameAssetPath)
            loadingAssetNames += frameAssetName -> info
            frameAssetName
        }
    }

    def setup(bootData: Unit, assetCollection: AssetCollection, dice: Dice): Outcome[Startup[Unit]] =
      for image <- assetCollection.images do
        loadingAssetNames.remove(image.name).foreach(_.oneLoaded(image.name))
      Outcome(Startup.Success(()))

    def updateModel(context: Context, model: Unit): GlobalEvent => Outcome[Unit] = {
      case FrameTick =>
        game.advanceTickCount(context.frame.time.delta.toMillis.toDouble)
        tickCount += context.frame.time.delta.toMillis.toLong
        unitOutcome

      /*case AssetEvent.AssetBatchLoaded(_, _, _) =>
        unitOutcome*/

      case AssetEvent.AssetBatchLoadError(_, message) =>
        System.err.println(s"Error loading assets: $message")
        unitOutcome

      case e: KeyboardEvent.KeyDown =>
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
  }

  object IndigoWrapper {
    import indigo.*

    val unitOutcome: Outcome[Unit] = Outcome(())

    val LoadingAssetName = AssetName("<loading>")

    private val EmptyAssetNameArray = IArray.empty[AssetName]

    final class ImageInfo(val relPath: String, val basePath: String) {
      val baseAssetName = AssetName(relPath)
      val baseAssetPath = AssetPath(basePath)
      var baseLoaded: Boolean = false
      var pngImage: Option[PNGImage] = None
      var frameAssetNames: IArray[AssetName] = EmptyAssetNameArray
      private var loadedFrameCount: Int = 0
      var totalFrameCount: Int = -1 // -1 while unknown

      def oneLoaded(assetName: AssetName): Unit =
        loadedFrameCount += 1
        if assetName == baseAssetName then
          baseLoaded = true
          println("base loaded for " + relPath)
        if allFramesLoaded then
          println("all  loaded for " + relPath)

      def frameAssetNameAt(tickCount: Long): AssetName = {
        if !baseLoaded then
          LoadingAssetName
        else if !allFramesLoaded then
          baseAssetName
        else
          frameAssetNames(pngImage.get.frameIndexAt(tickCount))
      }

      def allFramesLoaded: Boolean = loadedFrameCount == totalFrameCount
    }
  }
}
