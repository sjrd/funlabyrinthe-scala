package com.funlabyrinthe.mazes

import scala.annotation.tailrec

import scala.collection.mutable

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.Color

import com.funlabyrinthe.mazes.generic.*
import com.funlabyrinthe.mazes.std.*

object Mazes extends Module:
  private object PosComponentOrderingByZIndex extends Ordering[PosComponent]:
    def compare(x: PosComponent, y: PosComponent): Int =
      if x eq y then 0
      else if x.zIndex != y.zIndex then Integer.compare(x.zIndex, y.zIndex)
      else if x.id != y.id then x.id.compareTo(y.id)
      else Integer.compare(x.##, y.##) // tie-break
  end PosComponentOrderingByZIndex

  // Ordered list of PosComponent's
  // TODO Move this out of the singleton object somehow

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

  override protected def preInitialize()(using Universe): Unit =
    // Reified player for mazes

    registerReifiedPlayer(classOf[Player], new Player(_))
  end preInitialize

  override protected def initialize()(using Universe): Unit =
    directTurnstile.pairingTurnstile = indirectTurnstile
    indirectTurnstile.pairingTurnstile = directTurnstile

    upStairs.pairingStairs = downStairs
    downStairs.pairingStairs = upStairs
  end initialize
end Mazes

export Mazes.{posComponentsBottomUp, posComponentsTopDown}

// Dummies

@definition def noEffect(using Universe) = new NoEffect
@definition def noTool(using Universe) = new NoTool
@definition def noObstacle(using Universe) = new NoObstacle

// Map creator

@definition def mapCreator(using Universe) = new MapCreator

// Fields

@definition def grass(using Universe) = new Grass
@definition def water(using Universe) = new Water
@definition def wall(using Universe) = new Wall
@definition def hole(using Universe) = new Hole
@definition def sky(using Universe) = new Sky
@definition def outside(using Universe) = new Outside

// Arrows and other transporting effects

@definition def northArrow(using Universe) = Arrow.make(Direction.North, "Arrows/NorthArrow")
@definition def eastArrow(using Universe) = Arrow.make(Direction.East, "Arrows/EastArrow")
@definition def southArrow(using Universe) = Arrow.make(Direction.South, "Arrows/SouthArrow")
@definition def westArrow(using Universe) = Arrow.make(Direction.West, "Arrows/WestArrow")

@definition def crossroads(using Universe) = new Crossroads

@definition def directTurnstile(using Universe) = new DirectTurnstile
@definition def indirectTurnstile(using Universe) = new IndirectTurnstile

// Stairs

@definition def upStairs(using Universe) = new UpStairs
@definition def downStairs(using Universe) = new DownStairs

@definition def lift(using Universe) = new Lift

// Transporters

@definition def transporterCreator(using Universe) = new TransporterCreator

// Other effects

@definition def treasure(using Universe) = new Treasure
@definition def sunkenButton(using Universe) = DecorativeEffect.make("Buttons/SunkenButton")
@definition def inactiveTransporter(using Universe) = DecorativeEffect.make("Transporters/Transporter")

// Buoy

@definition def buoyPlugin(using Universe) = new BuoyPlugin
@definition def buoys(using Universe) = new Buoys
@definition def buoy(using Universe) = ItemTool.make(
  buoys,
  "You found a buoy. You can now go on water.",
)

// Plank

@definition def plankPlugin(using Universe) = new PlankPlugin
@definition def planks(using Universe) = new Planks
@definition def plank(using Universe) = ItemTool.make(
  planks,
  "You found a plank. You can pass over holes and water.",
)

// Keys

@definition def silverKeys(using Universe) = Keys.make("Objects/SilverKey", Lock(Color.Silver))
@definition def silverKey(using Universe) = ItemTool.make(
  silverKeys,
  "You found a silver key. You can open a silver lock.",
)

@definition def goldenKeys(using Universe) = Keys.make("Objects/GoldenKey", Lock(Color.Gold))
@definition def goldenKey(using Universe) = ItemTool.make(
  goldenKeys,
  "You found a golden key. You can open a golden lock.",
)

// Obstacles

@definition def silverBlock(using Universe) = Block.make(
  "Blocks/SilverBlock",
  Lock(Color.Silver),
  "You need a silver key to open that lock.",
)

@definition def goldenBlock(using Universe) = Block.make(
  "Blocks/GoldenBlock",
  Lock(Color.Gold),
  "You need a golden key to open that lock.",
)

@definition def secretWay(using Universe) = new SecretWay

// Vehicles

@definition def boatCreator(using Universe) = new BoatCreator

// Simple component creators

@definition def simpleEffectCreator(using Universe) = new SimpleEffectCreator
@definition def simplePushButtonCreator(using Universe) = new SimplePushButtonCreator
@definition def simpleSwitchCreator(using Universe) = new SimpleSwitchCreator
@definition def simpleObstacleCreator(using Universe) = new SimpleObstacleCreator
