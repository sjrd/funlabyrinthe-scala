package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import com.funlabyrinthe.core
import com.funlabyrinthe.core.shaders.BlendShader
import com.funlabyrinthe.coreinterface as intf

object RunningGame:
  def startGame(universe: Universe, coreUniverse: core.Universe): RunningGame =
    Errors.protect {
      val players = coreUniverse.players.map(new Player(universe, _)) // attaches controllers
      coreUniverse.startGame()
      players.foreach(_.processQueueItem()) // process items from startGame()
      RunningGame(coreUniverse, players.toJSArray)
    }
  end startGame
end RunningGame

final class RunningGame private (
  underlying: core.Universe,
  val players: js.Array[intf.Player],
) extends intf.RunningGame:
  def allShaders(): js.Array[intf.Shader] = {
    Errors.protect {
      underlying.allRegisteredShadersIterator.map {
        case shader: BlendShader =>
          new intf.Shader {
            val fullID: String = shader.fullID
            val tpe = intf.Shader.TypeBlend

            vertex = shader.vertex.orUndefined
            fragment = shader.fragment.orUndefined
          }
      }.toJSArray
    }
  }

  def advanceTickCount(delta: Double): Unit =
    Errors.protect {
      underlying.advanceTickCount(delta.toLong)
    }
end RunningGame
