package com.funlabyrinthe.core.shaders

import scala.quoted.*

import com.funlabyrinthe.core.*

sealed abstract class Shader(
  val owner: Module,
  val id: String,
) {
  override def toString(): String = id

  val fullID: String = owner.moduleID + ":" + id
}

final class BlendShader private (
  owner: Module,
  id: String,
  val vertex: Option[String],
  val fragment: Option[String],
) extends Shader(owner, id)

object BlendShader {
  inline def create(vertex: Option[String], fragment: Option[String])(
    using universe: Universe
  ): BlendShader = {
    createInternal(universe, ComponentInit.autoModuleOwner,
        ComponentInit.materializeID("a shader ID"),
        vertex, fragment)
  }

  private[core] def createInternal(
    universe: Universe,
    owner: Module,
    id: String,
    vertex: Option[String],
    fragment: Option[String],
  ): BlendShader = {
    universe.registerShader(owner, new BlendShader(owner, id, vertex, fragment))
  }
}
