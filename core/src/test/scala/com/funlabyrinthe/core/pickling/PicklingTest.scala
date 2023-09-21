package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.Universe
import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.core.reflect.*

import PicklingData.*

class PicklingTest extends munit.FunSuite:
  def makeContext(): Context =
    val universe = new Universe(FakeEnvironment.Instance)
    Context.make(universe)
  end makeContext

  test("elementary pickling") {
    given Context = makeContext()

    val foo = new Foo
    foo.s += " world"
    foo.bar.y = 3.1415
    foo.pos = MyPos(543, 2345)

    val pickle = InPlacePickleable.pickle(foo)

    val expectedPickle: Pickle =
      ObjectPickle(
        List(
          "bar" -> ObjectPickle(
            List(
              "y" -> DecimalPickle(3.1415),
            )
          ),
          "pos" -> ListPickle(List(IntegerPickle(543), IntegerPickle(2345))),
          "s" -> StringPickle("hello world"),
          "x" -> IntegerPickle(42),
        )
      )
    end expectedPickle

    assert(clue(pickle) == clue(expectedPickle))
  }

  test("pickling Painter") {
    given Context = makeContext()

    var painter = new Painter(new FakeResourceLoader)
    painter += "Fields/Grass"

    val container = new PainterContainer(painter)
    val pickle = InPlacePickleable.pickle(container)

    val expectedPickle =
      ObjectPickle(
        List(
        )
      )
    end expectedPickle

    assert(clue(pickle) == clue(expectedPickle))
  }

  test("elementary unpickling") {
    given Context = makeContext()

    val inputPickle: Pickle =
      ObjectPickle(
        List(
          "pos2" -> ObjectPickle.empty,
          "bar" -> ObjectPickle(
            List(
              "y" -> DecimalPickle(3.1415),
            )
          ),
          "pos" -> ListPickle(List(IntegerPickle(543), IntegerPickle(2345))),
          "s" -> StringPickle("hello world"),
          "x" -> IntegerPickle(42),
        )
      )
    end inputPickle

    val foo = new Foo
    InPlacePickleable.unpickle(foo, inputPickle)

    assert(clue(foo.s) == "hello world")
    assert(clue(foo.bar.y) == 3.1415)
    assert(clue(foo.pos) == MyPos(543, 2345))
  }
end PicklingTest
