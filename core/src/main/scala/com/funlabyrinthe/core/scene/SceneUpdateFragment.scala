package com.funlabyrinthe.core.scene


final case class SceneUpdateFragment(
  nodes: Batch[SceneNode]
)

object SceneUpdateFragment {
  val empty: SceneUpdateFragment =
    SceneUpdateFragment(Batch.empty)
}
