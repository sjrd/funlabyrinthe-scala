package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.core.reflect.*

object PicklingData:

  final case class MyPos(x: Int, y: Int) derives Pickleable

  class Foo extends Reflectable derives Reflector {
    var x: Int = 42
    var s: String = "hello"
    val bar: Bar = new Bar
    var pos: MyPos = MyPos(5, 4)
    val pos2: MyPos = MyPos(-6, -7)
    var opt: Option[Int] = Some(5)
    var opt2: Option[Int] = Some(6)

    override def reflect() = autoReflect[Foo]
  }

  class Bar extends Reflectable derives Reflector {
    var y: Double = 32.5

    override def reflect() = autoReflect[Bar]
  }

  class PainterContainer(var painter: Painter) extends Reflectable derives Reflector {
    override def reflect() = autoReflect[PainterContainer]
  }

end PicklingData
