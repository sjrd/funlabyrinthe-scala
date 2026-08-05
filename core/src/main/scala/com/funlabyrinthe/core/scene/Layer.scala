package com.funlabyrinthe.core.scene

import com.funlabyrinthe.core.shaders.*

final class Layer private (
  val nodes: Batch[SceneNode],
  val blending: Option[Layer.Blending],
) {
  import Layer.*

  override def toString(): String =
    s"Layer($nodes, $blending)"

  def withNodes(nodes: Batch[SceneNode]): Layer =
    copy(nodes = nodes)

  def withBlending(blending: Option[Blending]): Layer =
    copy(blending = blending)

  private def copy(
    nodes: Batch[SceneNode] = nodes,
    blending: Option[Blending] = blending,
  ): Layer = {
    new Layer(nodes, blending)
  }
}

object Layer {
  def apply(nodes: Batch[SceneNode]): Layer =
    new Layer(nodes, blending = None)

  val empty: Layer = apply(Batch.empty)

  final class Blending private (
    val blendMaterial: BlendMaterial,
  ) {
    def withBlendMaterial(blendMaterial: BlendMaterial): Blending =
      copy(blendMaterial = blendMaterial)

    private def copy(
      blendMaterial: BlendMaterial = blendMaterial,
    ): Blending = {
      new Blending(blendMaterial)
    }
  }

  object Blending {
    val default: Blending =
      new Blending(BlendMaterial.Normal)
  }

  sealed abstract class BlendMaterial

  object BlendMaterial {
    object Normal extends BlendMaterial

    final case class CustomBlendMaterial(shaderData: ShaderData) extends BlendMaterial
  }
}
