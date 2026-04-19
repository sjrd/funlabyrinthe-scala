package com.funlabyrinthe.core.scene


final case class SceneUpdateFragment(
  nodes: Batch[SceneNode]
) {
  import SceneUpdateFragment.*

  def ++(that: SceneUpdateFragment): SceneUpdateFragment =
    if this eq empty then that
    else if that eq empty then this
    else SceneUpdateFragment(this.nodes ++ that.nodes)
}

object SceneUpdateFragment {
  val empty: SceneUpdateFragment =
    SceneUpdateFragment(Batch.empty)
}
