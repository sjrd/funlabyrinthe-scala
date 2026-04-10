package com.funlabyrinthe.corebridge

import com.funlabyrinthe.core
import com.funlabyrinthe.htmlenv.ResourceLoader
import com.funlabyrinthe.coreinterface as intf

import indigo.*

import IndigoWrapper.*
import indigo.scenes.SceneName
import indigo.scenes.Scene
import com.funlabyrinthe.core.{input => AssetBundleLoaderEvent}

private[corebridge] final class IndigoWrapper(
    resourceLoader: ResourceLoader,
    universe: Universe,
    controlledPlayer: Option[Player],
    onResourceLoaded: () => Unit,
) extends IndigoGame[BootData, StartUpData, Model, ViewModel] {
  private var lastBindingKey: Int = 0

  def boot(flags: Map[String, String]): Outcome[BootResult[Unit, Unit]] = {
    val (width, height) = controlledPlayer.fold((270.0, 270.0))(_.controller.viewSize)
    val config = GameConfig(
      width = width.toInt,
      height = height.toInt,
    )
    Outcome(BootResult(config, ()))
  }

  def eventFilters: EventFilters = EventFilters.AllowAll

  def initialModel(startupData: StartUpData): Outcome[Unit] = unitOutcome

  def initialScene(bootData: BootData): Option[SceneName] = Some(TheSceneName)

  def initialViewModel(startupData: StartUpData, model: Model): Outcome[Unit] = unitOutcome

  def present(context: Context[StartUpData], model: Model, viewModel: ViewModel): Outcome[SceneUpdateFragment] =
    controlledPlayer match {
      case Some(player) =>
        Outcome(player.controller.present())

      case None =>
        Outcome(SceneUpdateFragment.empty)
    }

  def scenes(bootData: BootData): NonEmptyBatch[Scene[Unit, Unit, Unit]] = ???

  def setup(bootData: BootData, assetCollection: AssetCollection, dice: Dice): Outcome[Startup[Unit]] =
    onResourceLoaded()
    Outcome(Startup.Success(()))

  def updateModel(context: Context[StartUpData], model: Model): GlobalEvent => Outcome[Unit] = {
    case FrameTick =>
      if !universe.underlying.isEditing then
        universe.underlying.advanceTickCount(context.frame.time.delta.toMillis.toLong)

      val pendingAssets = resourceLoader.extractNewAssetsToLoad()
      if pendingAssets.isEmpty then
        unitOutcome
      else
        lastBindingKey += 1
        unitOutcome
          .addGlobalEvents(AssetEvent.LoadAssetBatch(pendingAssets, BindingKey(lastBindingKey.toString()), false))

    case AssetEvent.AssetBatchLoaded(_, _, _) =>
      onResourceLoaded()
      unitOutcome

    case AssetEvent.AssetBatchLoadError(_, message) =>
      System.err.println(s"Error loading assets: $message")
      unitOutcome

    case e: KeyboardEvent =>
      controlledPlayer match {
        case Some(player) =>
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

        case None =>
          // ignore
          ()
      }

      unitOutcome

    case _ =>
      unitOutcome
  }

  def updateViewModel(context: Context[StartUpData], model: Model, viewModel: ViewModel): GlobalEvent => Outcome[Unit] =
    _ => unitOutcome
}

object IndigoWrapper {
  type BootData = Unit
  type StartUpData = Unit
  type Model = Unit
  type ViewModel = Unit

  val TheSceneName = SceneName("the-scene")

  val unitOutcome: Outcome[Unit] = Outcome(())
}
