package com.funlabyrinthe.mazes

import scala.annotation.tailrec

import scala.collection.mutable
import scala.reflect.TypeTest

import com.funlabyrinthe.core.*
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

  override protected def createComponents()(using Universe): Unit =
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

    val northArrow = Arrow.make(Direction.North, "Arrows/NorthArrow")
    val eastArrow = Arrow.make(Direction.East, "Arrows/EastArrow")
    val southArrow = Arrow.make(Direction.South, "Arrows/SouthArrow")
    val westArrow = Arrow.make(Direction.West, "Arrows/WestArrow")

    val crossroads = new Crossroads

    val directTurnstile = new DirectTurnstile
    val indirectTurnstile = new IndirectTurnstile

    // Stairs

    val upStairs = new UpStairs
    val downStairs = new DownStairs

    val lift = new Lift

    // Transporters

    val transporterCreator = new TransporterCreator

    // Other effects

    val treasure = new Treasure
    val sunkenButton = DecorativeEffect.make("Buttons/SunkenButton")
    val inactiveTransporter = DecorativeEffect.make("Transporters/Transporter")

    // Buoy

    val buoyPlugin = new BuoyPlugin
    val buoys = new Buoys
    val buoy = ItemTool.make(
      buoys,
      "You found a buoy. You can now go on water.",
    )

    // Plank

    val plankPlugin = new PlankPlugin
    val planks = new Planks
    val plank = ItemTool.make(
      planks,
      "You found a plank. You can pass over holes and water.",
    )

    // Keys

    val silverKeys = Keys.make("Objects/SilverKey", SilverLock)
    val silverKey = ItemTool.make(
      silverKeys,
      "You found a silver key. You can open a silver lock.",
    )

    val goldenKeys = Keys.make("Objects/GoldenKey", GoldenLock)
    val goldenKey = ItemTool.make(
      goldenKeys,
      "You found a golden key. You can open a golden lock.",
    )

    // Obstacles

    val silverBlock = Block.make(
      "Blocks/SilverBlock",
      SilverLock,
      "You need a silver key to open that lock.",
    )

    val goldenBlock = Block.make(
      "Blocks/GoldenBlock",
      GoldenLock,
      "You need a golden key to open that lock.",
    )

    val secretWay = new SecretWay

    // Vehicles

    val boatCreator = new BoatCreator

    // Simple component creators

    val simpleEffectCreator = new SimpleEffectCreator
    val simplePushButtonCreator = new SimplePushButtonCreator
    val simpleSwitchCreator = new SimpleSwitchCreator
  end createComponents

  override protected def initialize()(using Universe): Unit =
    directTurnstile.pairingTurnstile = indirectTurnstile
    indirectTurnstile.pairingTurnstile = directTurnstile

    upStairs.pairingStairs = downStairs
    downStairs.pairingStairs = upStairs
  end initialize

  // Dummies

  def noEffect(using Universe): NoEffect = myComponentByID("noEffect")
  def noTool(using Universe): NoTool = myComponentByID("noTool")
  def noObstacle(using Universe): NoObstacle = myComponentByID("noObstacle")

  // Map creator

  def mapCreator(using Universe): MapCreator = myComponentByID("mapCreator")

  // Fields

  def grass(using Universe): Grass = myComponentByID("grass")
  def water(using Universe): Water = myComponentByID("water")
  def wall(using Universe): Wall = myComponentByID("wall")
  def hole(using Universe): Hole = myComponentByID("hole")
  def sky(using Universe): Sky = myComponentByID("sky")
  def outside(using Universe): Outside = myComponentByID("outside")

  // Arrows and other transporting effects

  def northArrow(using Universe): Arrow = myComponentByID("northArrow")
  def eastArrow(using Universe): Arrow = myComponentByID("eastArrow")
  def southArrow(using Universe): Arrow = myComponentByID("southArrow")
  def westArrow(using Universe): Arrow = myComponentByID("westArrow")

  def crossroads(using Universe): Crossroads = myComponentByID("crossroads")

  def directTurnstile(using Universe): DirectTurnstile = myComponentByID("directTurnstile")
  def indirectTurnstile(using Universe): IndirectTurnstile = myComponentByID("indirectTurnstile")

  // Stairs

  def upStairs(using Universe): UpStairs = myComponentByID("upStairs")
  def downStairs(using Universe): DownStairs = myComponentByID("downStairs")

  def lift(using Universe): Lift = myComponentByID("lift")

  // Transporters

  def transporterCreator(using Universe): TransporterCreator = myComponentByID("transporterCreator")

  // Other effects

  def treasure(using Universe): Treasure = myComponentByID("treasure")
  def sunkenButton(using Universe): DecorativeEffect = myComponentByID("sunkenButton")
  def inactiveTransporter(using Universe): DecorativeEffect = myComponentByID("inactiveTransporter")

  // Buoy

  def buoyPlugin(using Universe): BuoyPlugin = myComponentByID("buoyPlugin")
  def buoys(using Universe): Buoys = myComponentByID("buoys")
  def buoy(using Universe): ItemTool = myComponentByID("buoy")

  // Plank

  def plankPlugin(using Universe): PlankPlugin = myComponentByID("plankPlugin")
  def planks(using Universe): Planks = myComponentByID("planks")
  def plank(using Universe): ItemTool = myComponentByID("plank")

  // Keys

  def silverKeys(using Universe): Keys = myComponentByID("silverKeys")
  def silverKey(using Universe): ItemTool = myComponentByID("silverKey")

  def goldenKeys(using Universe): Keys = myComponentByID("goldenKeys")
  def goldenKey(using Universe): ItemTool = myComponentByID("goldenKey")

  // Obstacles

  def silverBlock(using Universe): Block = myComponentByID("silverBlock")
  def goldenBlock(using Universe): Block = myComponentByID("goldenBlock")

  def secretWay(using Universe): SecretWay = myComponentByID("secretWay")

  // Vehicles

  def boatCreator(using Universe): BoatCreator = myComponentByID("boatCreator")

  // Simple component creators

  def simpleEffectCreator(using Universe): SimpleEffectCreator = myComponentByID("simpleEffectCreator")
  def simplePushButtonCreator(using Universe): SimplePushButtonCreator = myComponentByID("simplePushButtonCreator")
  def simpleSwitchCreator(using Universe): SimpleSwitchCreator = myComponentByID("simpleSwitchCreator")
end Mazes

export Mazes.*
