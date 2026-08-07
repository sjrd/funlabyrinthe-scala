package com.funlabyrinthe.core.scene

import scala.annotation.targetName

final case class SceneUpdateFragment(
  layers: Batch[Layer]
) {
  import SceneUpdateFragment.*

  def ++(that: SceneUpdateFragment): SceneUpdateFragment =
    if this eq empty then that
    else if that eq empty then this
    else SceneUpdateFragment(this.layers ++ that.layers)
}

object SceneUpdateFragment {
  val empty: SceneUpdateFragment =
    SceneUpdateFragment(Batch.empty: Batch[Layer])

  @targetName("fromSceneNodes")
  def apply(nodes: Batch[SceneNode]): SceneUpdateFragment =
    SceneUpdateFragment(Batch(Layer(nodes)))
}
