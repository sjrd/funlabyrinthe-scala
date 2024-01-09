package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.noinspect
import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.core.reflect.*

object PicklingData:

  final case class MyPos(x: Int, y: Int) derives Pickleable

  class Foo extends Reflectable derives Reflector {
    var x: Int = 42
    var s: String = "hello"
    @noinspect
    val bar: Bar = new Bar
    @noinspect
    var pos: MyPos = MyPos(5, 4)
    @transient @noinspect
    val pos2: MyPos = MyPos(-6, -7)
    @noinspect
    var opt: Option[Int] = Some(5)
    @noinspect
    var opt2: Option[Int] = Some(6)

    override def reflect() = autoReflect[Foo]
  }

  class Bar extends Reflectable derives Reflector {
    @noinspect
    var y: Double = 32.5

    override def reflect() = autoReflect[Bar]
  }

  class PainterContainer(var painter: Painter) extends Reflectable derives Reflector {
    override def reflect() = autoReflect[PainterContainer]
  }

end PicklingData
