package com.funlabyrinthe.core.scene


final case class SceneUpdateFragment(
  nodes: IArray[SceneNode]
)

object SceneUpdateFragment {
  val empty: SceneUpdateFragment =
    SceneUpdateFragment(IArray.empty)
}
