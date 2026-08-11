package com.funlabyrinthe.scene

import indigo.shaders.ShaderData
import indigo.scenegraph.materials.BlendMaterial

final case class CustomBlendMaterial(toShaderData: ShaderData) extends BlendMaterial.SrcAndDst
