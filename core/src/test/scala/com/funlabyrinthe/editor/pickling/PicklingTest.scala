package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.core.reflect.*

import PicklingData.*

class PicklingTest extends munit.FunSuite:
  def makeRegistry(): PicklingRegistry =
    val registry = new PicklingRegistry
    registry.registerPickleable[MyPos]()
    registry
  end makeRegistry

  test("elementary pickling") {
    val registry = makeRegistry()

    val foo = new Foo
    foo.s += " world"
    foo.bar.y = 3.1415
    foo.pos = MyPos(543, 2345)

    val pickle = registry.pickle(foo)

    val expectedPickle: Pickle =
      ObjectPickle(
        List(
          "bar" -> ObjectPickle(
            List(
              "y" -> DoublePickle(3.1415),
            )
          ),
          "pos" -> ListPickle(List(IntPickle(543), IntPickle(2345))),
          "s" -> StringPickle("hello world"),
          "x" -> IntPickle(42),
        )
      )
    end expectedPickle

    assert(clue(pickle) == clue(expectedPickle))
  }

  test("pickling Painter") {
    val registry = makeRegistry()

    var painter = new Painter(new FakeResourceLoader)
    painter += "Fields/Grass"

    val container = new PainterContainer(painter)
    val pickle = registry.pickle(container)

    val expectedPickle =
      ObjectPickle(
        List(
        )
      )
    end expectedPickle

    assert(clue(pickle) == clue(expectedPickle))
  }

  test("elementary unpickling") {
    val registry = makeRegistry()

    val inputPickle: Pickle =
      ObjectPickle(
        List(
          "pos2" -> ObjectPickle.empty,
          "bar" -> ObjectPickle(
            List(
              "y" -> DoublePickle(3.1415),
            )
          ),
          "pos" -> ListPickle(List(IntPickle(543), IntPickle(2345))),
          "s" -> StringPickle("hello world"),
          "x" -> IntPickle(42),
        )
      )
    end inputPickle

    val foo = new Foo
    registry.unpickle(foo, inputPickle)

    assert(clue(foo.s) == "hello world")
    assert(clue(foo.bar.y) == 3.1415)
    assert(clue(foo.pos) == MyPos(543, 2345))
  }
end PicklingTest
