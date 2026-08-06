package com.funlabyrinthe.scene

import indigo.scenegraph.Blending

final case class Layer(
  nodes: Batch[SceneNode],
  blending: Option[Blending]
)
