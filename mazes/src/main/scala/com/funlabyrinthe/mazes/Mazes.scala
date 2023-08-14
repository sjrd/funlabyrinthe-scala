package com.funlabyrinthe
package mazes

import scala.annotation.tailrec

import scala.collection.mutable

import core._
import std._

object Mazes:
  def mazes(using universe: Universe): Mazes =
    universe.module[Mazes]

  private object PosComponentOrderingByZIndex extends Ordering[PosComponent]:
    def compare(x: PosComponent, y: PosComponent): Int =
      if x eq y then 0
      else if x.zIndex != y.zIndex then Integer.compare(x.zIndex, y.zIndex)
      else if x.id != y.id then x.id.compareTo(y.id)
      else Integer.compare(x.##, y.##) // tie-break
  end PosComponentOrderingByZIndex
end Mazes

final class Mazes(universe: Universe) extends Module(universe) {
  import Mazes.*
  import universe._

  // Ordered list of PosComponent's

  private var _posComponentsBottomUp: List[PosComponent] = Nil
  private var _posComponentsTopDown: List[PosComponent] = Nil

  private[mazes] def registerPosComponent(posComponent: PosComponent): Unit =
    val builder = mutable.ListBuffer.empty[PosComponent]

    @tailrec def loop(item: PosComponent, xs: List[PosComponent]): List[PosComponent] = xs match
      case x :: xr if PosComponentOrderingByZIndex.compare(item, x) >= 0 =>
        builder += x
        loop(item, xr)
      case _ =>
        builder += item
        builder.prependToList(xs)
    end loop

    _posComponentsBottomUp = loop(posComponent, _posComponentsBottomUp)
    _posComponentsTopDown = _posComponentsBottomUp.reverse
  end registerPosComponent

  private[mazes] def unregisterPosComponent(posComponent: PosComponent): Unit =
    _posComponentsBottomUp = _posComponentsBottomUp.filter(_ ne posComponent)
    _posComponentsTopDown = _posComponentsBottomUp.reverse
  end unregisterPosComponent

  private[mazes] def changingPosComponentZIndex(posComponent: PosComponent)(op: => Unit): Unit =
    unregisterPosComponent(posComponent)
    op
    registerPosComponent(posComponent)
  end changingPosComponentZIndex

  def posComponentsBottomUp: List[PosComponent] = _posComponentsBottomUp

  def posComponentsTopDown: List[PosComponent] = _posComponentsTopDown

  // Dummies

  val NoEffect = new Effect {
    name = "(no effect)"

    override def drawIcon(context: DrawContext) =
      DefaultIconPainter.drawTo(context)
  }

  val NoTool = new Tool {
    name = "(no tool)"

    override def drawIcon(context: DrawContext) =
      DefaultIconPainter.drawTo(context)
  }

  val NoObstacle = new Obstacle {
    name = "(no obstacle)"

    override def drawIcon(context: DrawContext) =
      DefaultIconPainter.drawTo(context)
  }

  val NoItemDef = new ItemDef {
    name = "(no item def)"
  }

  // Map creator

  val MapCreator = new MapCreator

  // Fields

  val Grass = new Grass {
    name = "Grass"
  }

  val Water = new Water {
    name = "Water"
  }

  val Wall = new Wall {
    name = "Wall"
  }

  val Hole = new Hole {
    name = "Hole"
  }

  val Sky = new Sky {
    name = "Sky"
  }

  val Outside = new Outside {
    name = "Outside"
  }

  // Arrows and other transporting effects

  val NorthArrow = new Arrow {
    name = "North arrow"
    direction = Direction.North
    painter += "Arrows/NorthArrow"
  }
  val EastArrow = new Arrow {
    name = "East arrow"
    direction = Direction.East
    painter += "Arrows/EastArrow"
  }
  val SouthArrow = new Arrow {
    name = "South arrow"
    direction = Direction.North
    painter += "Arrows/SouthArrow"
  }
  val WestArrow = new Arrow {
    name = "West arrow"
    direction = Direction.East
    painter += "Arrows/WestArrow"
  }

  val Crossroads = new Crossroads {
    name = "Crossroads"
    painter += "Arrows/Crossroads"
  }

  val DirectTurnstile = new DirectTurnstile {
    name = "Direct turnstile"
    painter += "Arrows/DirectTurnstile"
  }

  val IndirectTurnstile = new IndirectTurnstile {
    name = "Indirect turnstile"
    painter += "Arrows/IndirectTurnstile"
  }

  DirectTurnstile.pairingTurnstile = IndirectTurnstile
  IndirectTurnstile.pairingTurnstile = DirectTurnstile

  // Stairs

  val UpStairs = new UpStairs {
    name = "Up stairs"
    painter += "Stairs/UpStairs"
  }

  val DownStairs = new DownStairs {
    name = "Down stairs"
    painter += "Stairs/DownStairs"
  }

  UpStairs.pairingStairs = DownStairs
  DownStairs.pairingStairs = UpStairs

  val Lift = new Lift

  // Other effects

  val Treasure = new Treasure {
    name = "Treasure"
    painter += "Chests/Treasure"
  }

  // Buoy

  val BuoyPlugin = new BuoyPlugin

  val Buoys = new Buoys {
    name = "Buoys"
    icon += "Objects/Buoy"
  }

  val Buoy = new ItemTool {
    name = "Buoy"
    painter += "Objects/Buoy"
    item = Buoys
    message = "You found a buoy. You can now go on water."
  }

  // Plank

  val PlankPlugin = new PlankPlugin

  val Planks = new Planks {
    name = "Planks"
    icon += "Objects/Plank"
  }

  val Plank = new ItemTool {
    name = "Plank"
    painter += "Objects/Plank"
    item = Planks
    message = "You found a plank. You can pass over holes and water."
  }

  // Keys

  val SilverKeys = new Keys {
    name = "Silver keys"
    icon += "Objects/SilverKey"
    lock = SilverLock
  }

  val SilverKey = new ItemTool {
    name = "Silver key"
    painter += "Objects/SilverKey"
    item = SilverKeys
    message = "You found a silver key. You can open a silver lock."
  }

  val GoldenKeys = new Keys {
    name = "Golden keys"
    icon += "Objects/GoldenKey"
    lock = GoldenLock
  }

  val GoldenKey = new ItemTool {
    name = "Golden key"
    painter += "Objects/GoldenKey"
    item = GoldenKeys
    message = "You found a golden key. You can open a golden lock."
  }

  // Blocks

  val SilverBlock = new Block {
    name = "Silver block"
    painter += "Blocks/SilverBlock"
    lock = SilverLock
    message = "You need a silver key to open that lock."
  }

  val GoldenBlock = new Block {
    name = "Golden block"
    painter += "Blocks/GoldenBlock"
    lock = GoldenLock
    message = "You need a golden key to open that lock."
  }

  // Vehicles

  val BoatCreator = new BoatCreator

  // Plugins

  val DefaultMessagesPlugin = new DefaultMessagesPlugin {
  }

  // Initialization

  def initialize(): Unit = ()
}
