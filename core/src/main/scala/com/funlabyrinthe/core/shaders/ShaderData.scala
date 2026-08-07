package com.funlabyrinthe.core.shaders

import com.funlabyrinthe.core.scene.Batch

final class ShaderData(
  val shader: Shader,
  val blocks: Batch[UniformBlock],
)
