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

  // Reified player for mazes

  registerReifiedPlayer(classOf[Player], new Player(_))

  // Dummies

  val NoEffect = new NoEffect
  val NoTool = new NoTool
  val NoObstacle = new NoObstacle

  // Map creator

  val MapCreator = new MapCreator

  // Fields

  val Grass = new Grass
  val Water = new Water
  val Wall = new Wall
  val Hole = new Hole
  val Sky = new Sky
  val Outside = new Outside

  // Arrows and other transporting effects

  val NorthArrow = Arrow.make("North arrow", Direction.North, "Arrows/NorthArrow")
  val EastArrow = Arrow.make("East arrow", Direction.East, "Arrows/EastArrow")
  val SouthArrow = Arrow.make("South arrow", Direction.South, "Arrows/SouthArrow")
  val WestArrow = Arrow.make("West arrow", Direction.West, "Arrows/WestArrow")

  val Crossroads = new Crossroads

  val DirectTurnstile = new DirectTurnstile
  val IndirectTurnstile = new IndirectTurnstile
  DirectTurnstile.pairingTurnstile = IndirectTurnstile
  IndirectTurnstile.pairingTurnstile = DirectTurnstile

  // Stairs

  val UpStairs = new UpStairs
  val DownStairs = new DownStairs
  UpStairs.pairingStairs = DownStairs
  DownStairs.pairingStairs = UpStairs

  val Lift = new Lift

  // Other effects

  val Treasure = new Treasure
  val SunkenButton = DecorativeEffect.make("Sunken button", "Buttons/SunkenButton")
  val InactiveTransporter = DecorativeEffect.make("Inactive transporter", "Transporters/Transporter")

  // Buoy

  val BuoyPlugin = new BuoyPlugin
  val Buoys = new Buoys
  val Buoy = ItemTool.make(
    "Buoy",
    Buoys,
    "You found a buoy. You can now go on water.",
  )

  // Plank

  val PlankPlugin = new PlankPlugin
  val Planks = new Planks
  val Plank = ItemTool.make(
    "Plank",
    Planks,
    "You found a plank. You can pass over holes and water.",
  )

  // Keys

  val SilverKeys = Keys.make("Silver keys", "Objects/SilverKey", SilverLock)
  val SilverKey = ItemTool.make(
    "Silver key",
    SilverKeys,
    "You found a silver key. You can open a silver lock.",
  )

  val GoldenKeys = Keys.make("Golden keys", "Objects/GoldenKey", GoldenLock)
  val GoldenKey = ItemTool.make(
    "Golden key",
    GoldenKeys,
    "You found a golden key. You can open a golden lock.",
  )

  // Obstacles

  val SilverBlock = Block.make(
    "Silver block",
    "Blocks/SilverBlock",
    SilverLock,
    "You need a silver key to open that lock.",
  )

  val GoldenBlock = Block.make(
    "Golden block",
    "Blocks/GoldenBlock",
    GoldenLock,
    "You need a golden key to open that lock.",
  )

  val SecretWay = new SecretWay

  // Vehicles

  val BoatCreator = new BoatCreator

  // Initialization

  def initialize(): Unit = ()
}
