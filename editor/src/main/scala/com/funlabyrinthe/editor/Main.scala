package com.funlabyrinthe.editor

import com.funlabyrinthe._
import core._
import core.graphics._
import mazes._

import com.funlabyrinthe.core.reflect.*

import com.funlabyrinthe.graphics.{ jfx => gjfx }
import com.funlabyrinthe.jvmenv.ResourceLoader

import java.net._

import scalafx.Includes._
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.control._

final case class MyPos(x: Int, y: Int) derives pickling.Pickleable

class Foo extends Reflectable derives Reflector {
  var x: Int = 42
  var s: String = "hello"
  val bar: Bar = new Bar
  var pos: MyPos = MyPos(5, 4)
  val pos2: MyPos = MyPos(-6, -7)

  override def reflect() = autoReflect[Foo]
}
class Bar extends Reflectable derives Reflector {
  var y: Double = 32.5

  override def reflect() = autoReflect[Bar]
}

class PainterContainer(var painter: Painter) extends Reflectable derives Reflector {
  override def reflect() = autoReflect[PainterContainer]
}

object Main extends JFXApp3 {
  override def start(): Unit =
    stage = MainImpl.initialStage
}

object MainImpl {
  private val resourceLoader = new ResourceLoader(new URLClassLoader(
      Array(
          new java.io.File("C:/Users/Public/Documents/FunLabyrinthe/Projects/Temple de l'eau/Resources/").toURI.toURL,
          new java.io.File("C:/Users/Public/Documents/FunLabyrinthe/Library/Resources/").toURI.toURL),
      getClass.getClassLoader))

  private val environment = new UniverseEnvironment(
      gjfx.JavaFXGraphicsSystem, resourceLoader)

  class MyUniverse extends Universe(environment) with MazeUniverse

  implicit val universe: MyUniverse = new MyUniverse
  universe.initialize()
  import universe._
  import mazes._

  {
    val mainMap = new Map(Dimensions(13, 9, 1), mazes.Grass)
    for (pos <- mainMap.minRef until mainMap.maxRef by (2, 2)) {
      pos() = mazes.Wall
    }

    val player = new Player
    player.position = Some(SquareRef(mainMap, Position(1, 1, 0)))
  }

  locally {
    val specificPicklers = new pickling.flspecific.SpecificPicklers(universe)

    val registry = new pickling.PicklingRegistry
    specificPicklers.registerSpecificPicklers(registry)
    registry.registerSubType(InspectedType.AnyRef, { (_, _) =>
      new pickling.MutableMembersPickler {
        val tpe = InspectedType.AnyRef
      }
    }, 30)
    registry.registerPickleable[MyPos]()

    val foo = new Foo
    foo.s += " world"
    foo.bar.y = 3.1415
    foo.pos = MyPos(543, 2345)
    val pickle = registry.pickle(foo).get
    println("---")
    println(pickle)

    val foo2 = new Foo
    println("---")
    println(registry.pickle(foo2).get)
    println("---")
    registry.unpickle(foo2, pickle)
    println("---")
    println(foo2.s)
    println("---")
    println(foo2.bar.y)
    println("---")
    println(registry.pickle(foo2).get)
    println("---")

    val container = new PainterContainer(Grass.painter)
    println("---")
    println(registry.pickle(container).get)
    println("---")
    println(registry.pickle(Grass).get)
    println("---")
  }

  lazy val initialStage: JFXApp3.PrimaryStage = new JFXApp3.PrimaryStage { stage0 =>
    title = "FunLabyrinthe editor"
    width = 1000
    height = 800
    scene = new Scene { scene0 =>
      content = new UniverseEditor(stage0)(universe) {
        prefWidth <== scene0.width
        prefHeight <== scene0.height
      }
      stylesheets += Main.getClass.getResource("editor.css").toExternalForm()
      stylesheets += classOf[inspector.jfx.Inspector].getResource("inspector.css").toExternalForm()
    }
  }
}
