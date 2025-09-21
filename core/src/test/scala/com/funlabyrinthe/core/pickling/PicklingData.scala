package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.noinspect
import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.core.reflect.*

object PicklingData:

  final case class MyPos(x: Int, y: Int) derives Pickleable

  class Foo extends Reflectable {
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
  }

  class Bar extends Reflectable {
    @noinspect
    var y: Double = 32.5
  }

  class PainterContainer(var painter: Painter) extends Reflectable

end PicklingData
