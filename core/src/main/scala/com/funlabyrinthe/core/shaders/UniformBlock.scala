package com.funlabyrinthe.core.shaders

import com.funlabyrinthe.core.scene.Batch

final case class UniformBlock(
  blockName: String,
  fields: Batch[(String, ShaderPrimitive)],
)
