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

  val noEffect = new NoEffect
  val noTool = new NoTool
  val noObstacle = new NoObstacle

  // Map creator

  val mapCreator = new MapCreator

  // Fields

  val grass = new Grass
  val water = new Water
  val wall = new Wall
  val hole = new Hole
  val sky = new Sky
  val outside = new Outside

  // Arrows and other transporting effects

  val northArrow = Arrow.make("North arrow", Direction.North, "Arrows/NorthArrow")
  val eastArrow = Arrow.make("East arrow", Direction.East, "Arrows/EastArrow")
  val southArrow = Arrow.make("South arrow", Direction.South, "Arrows/SouthArrow")
  val westArrow = Arrow.make("West arrow", Direction.West, "Arrows/WestArrow")

  val crossroads = new Crossroads

  val directTurnstile = new DirectTurnstile
  val indirectTurnstile = new IndirectTurnstile
  directTurnstile.pairingTurnstile = indirectTurnstile
  indirectTurnstile.pairingTurnstile = directTurnstile

  // Stairs

  val upStairs = new UpStairs
  val downStairs = new DownStairs
  upStairs.pairingStairs = downStairs
  downStairs.pairingStairs = upStairs

  val lift = new Lift

  // Transporters

  val transporterCreator = new TransporterCreator

  // Other effects

  val treasure = new Treasure
  val sunkenButton = DecorativeEffect.make("Sunken button", "Buttons/SunkenButton")
  val inactiveTransporter = DecorativeEffect.make("Inactive transporter", "Transporters/Transporter")

  // Buoy

  val buoyPlugin = new BuoyPlugin
  val buoys = new Buoys
  val buoy = ItemTool.make(
    "Buoy",
    buoys,
    "You found a buoy. You can now go on water.",
  )

  // Plank

  val plankPlugin = new PlankPlugin
  val planks = new Planks
  val plank = ItemTool.make(
    "Plank",
    planks,
    "You found a plank. You can pass over holes and water.",
  )

  // Keys

  val silverKeys = Keys.make("Silver keys", "Objects/SilverKey", SilverLock)
  val silverKey = ItemTool.make(
    "Silver key",
    silverKeys,
    "You found a silver key. You can open a silver lock.",
  )

  val goldenKeys = Keys.make("Golden keys", "Objects/GoldenKey", GoldenLock)
  val goldenKey = ItemTool.make(
    "Golden key",
    goldenKeys,
    "You found a golden key. You can open a golden lock.",
  )

  // Obstacles

  val silverBlock = Block.make(
    "Silver block",
    "Blocks/SilverBlock",
    SilverLock,
    "You need a silver key to open that lock.",
  )

  val goldenBlock = Block.make(
    "Golden block",
    "Blocks/GoldenBlock",
    GoldenLock,
    "You need a golden key to open that lock.",
  )

  val secretWay = new SecretWay

  // Vehicles

  val boatCreator = new BoatCreator

  // Initialization

  def initialize(): Unit = ()
}
