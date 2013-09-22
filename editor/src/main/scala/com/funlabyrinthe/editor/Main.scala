package com.funlabyrinthe.editor

import com.funlabyrinthe._
import core._
import core.graphics._
import mazes._

import com.funlabyrinthe.graphics.{ jfx => gjfx }
import com.funlabyrinthe.jvmenv.ResourceLoader

import java.net._

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.control._

case class MyPos(x: Int, var y: Int) {
  var z: Int = 0
}
class Foo {
  var x: Int = 42
  var s: String = "hello"
  val bar: Bar = new Bar
  var pos: MyPos = MyPos(5, 4)
  val pos2: MyPos = MyPos(-6, -7)
}
class Bar {
  var y: Double = 32.5
}

object Main extends JFXApp {
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

  {
    import scala.reflect.runtime.universe._
    val registry = new pickling.PicklingRegistry
    registry.registerSubTypeReadOnly(typeOf[AnyRef], { (_, _) =>
      new pickling.MutableMembersPickler {
        val tpe = typeOf[AnyRef]
      }
    })
    registry.registerSubTypeReadWrite(typeOf[AnyRef], { (_, _) =>
      new pickling.ConstructiblePickler {
        val tpe = typeOf[AnyRef]
      }
    })
    val foo = new Foo
    foo.s += " world"
    foo.bar.y = 3.1415
    foo.pos = MyPos(543, 2345)
    foo.pos.z = -4
    val pickle = registry.pickle(foo).get
    println(pickle)

    val foo2 = new Foo
    println(registry.pickle(foo2).get)
    registry.unpickle(foo2, pickle)
    println(foo2.s)
    println(foo2.bar.y)
    println(registry.pickle(foo2).get)
  }

  stage = new JFXApp.PrimaryStage { stage0 =>
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
