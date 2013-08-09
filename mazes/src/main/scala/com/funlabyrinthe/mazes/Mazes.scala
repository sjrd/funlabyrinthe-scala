package com.funlabyrinthe
package mazes

import core._
import std._

class Mazes(implicit uni: MazeUniverse) {
  val universe: MazeUniverse = uni
  import universe._

  // Dummies

  object NoEffect extends Effect {
    name = "(no effect)"

    override def drawIcon(context: DrawContext) =
      DefaultIconPainter.drawTo(context)
  }

  object NoTool extends Tool {
    name = "(no tool)"

    override def drawIcon(context: DrawContext) =
      DefaultIconPainter.drawTo(context)
  }

  object NoObstacle extends Obstacle {
    name = "(no obstacle)"

    override def drawIcon(context: DrawContext) =
      DefaultIconPainter.drawTo(context)
  }

  object NoItemDef extends ItemDef {
    name = "(no item def)"
  }

  // Fields

  object Grass extends Grass {
    name = "Grass"
  }

  object Water extends Water {
    name = "Water"
  }

  object Wall extends Wall {
    name = "Wall"
  }

  object Hole extends Hole {
    name = "Hole"
  }

  object Sky extends Sky {
    name = "Sky"
  }

  object Outside extends Outside {
    name = "Outside"
  }

  // Arrows and other transporting effects

  object NorthArrow extends Arrow {
    name = "North arrow"
    direction = North
    painter += "Arrows/NorthArrow"
  }
  object EastArrow extends Arrow {
    name = "East arrow"
    direction = East
    painter += "Arrows/EastArrow"
  }
  object SouthArrow extends Arrow {
    name = "South arrow"
    direction = North
    painter += "Arrows/SouthArrow"
  }
  object WestArrow extends Arrow {
    name = "West arrow"
    direction = East
    painter += "Arrows/WestArrow"
  }

  object Crossroads extends Crossroads {
    name = "Crossroads"
    painter += "Arrows/Crossroads"
  }

  object DirectTurnstile extends DirectTurnstile {
    name = "Direct turnstile"
    painter += "Arrows/DirectTurnstile"
  }

  object IndirectTurnstile extends IndirectTurnstile {
    name = "Indirect turnstile"
    painter += "Arrows/IndirectTurnstile"
  }

  // Stairs

  object UpStairs extends UpStairs {
    name = "Up stairs"
    painter += "Stairs/UpStairs"
  }

  object DownStairs extends DownStairs {
    name = "Down stairs"
    painter += "Stairs/DownStairs"
  }

  // Other effects

  object Treasure extends Treasure {
    name = "Treasure"
    painter += "Chests/Treasure"
  }

  // Buoy

  object Buoys extends Buoys {
    name = "Buoys"
    icon += "Objects/Buoy"
  }

  object Buoy extends ItemTool {
    name = "Buoy"
    painter += "Objects/Buoy"
    item = Buoys
    message = "You found a buoy. You can now go on water."
  }

  // Keys

  object SilverKeys extends Keys {
    name = "Silver keys"
    icon += "Objects/SilverKey"
    lock = SilverLock
  }

  object SilverKey extends ItemTool {
    name = "Silver key"
    painter += "Objects/SilverKey"
    item = SilverKeys
    message = "You found a silver key. You can open a silver lock."
  }

  object GoldenKeys extends Keys {
    name = "Golden keys"
    icon += "Objects/GoldenKey"
    lock = GoldenLock
  }

  object GoldenKey extends ItemTool {
    name = "Golden key"
    painter += "Objects/GoldenKey"
    item = GoldenKeys
    message = "You found a golden key. You can open a golden lock."
  }

  // Blocks

  object SilverBlock extends Block {
    name = "Silver block"
    painter += "Blocks/SilverBlock"
    lock = SilverLock
    message = "You need a silver key to open that lock."
  }

  object GoldenBlock extends Block {
    name = "Golden block"
    painter += "Blocks/GoldenBlock"
    lock = GoldenLock
    message = "You need a golden key to open that lock."
  }

  // Plugins

  object DefaultMessagesPlugin extends DefaultMessagesPlugin

  // Initialization

  def initialize() {
    NoEffect
    NoTool
    NoObstacle
    NoItemDef

    Grass
    Water
    Wall
    Hole
    Sky
    Outside

    NorthArrow
    EastArrow
    SouthArrow
    WestArrow
    Crossroads
    DirectTurnstile.pairingTurnstile = IndirectTurnstile
    IndirectTurnstile.pairingTurnstile = DirectTurnstile

    UpStairs.pairingStairs = DownStairs
    DownStairs.pairingStairs = UpStairs

    Buoys
    Buoys.Plugin
    Buoy

    SilverKeys
    SilverKey
    GoldenKeys
    GoldenKey

    SilverBlock
    GoldenBlock
  }
}
