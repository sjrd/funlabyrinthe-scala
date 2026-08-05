package com.funlabyrinthe.gamerunner.scene

final case class Layer(
  nodes: Batch[SceneNode],
  blending: Option[indigo.Blending]
)
