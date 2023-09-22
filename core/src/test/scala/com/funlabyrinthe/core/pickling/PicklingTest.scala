package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.Universe
import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.core.reflect.*

import PicklingData.*

class PicklingTest extends munit.FunSuite:
  def makeContext(): PicklingContext =
    val universe = new Universe(FakeEnvironment.Instance)
    PicklingContext.make(universe)
  end makeContext

  test("elementary pickling") {
    given PicklingContext = makeContext()

    val foo = new Foo
    foo.s += " world"
    foo.bar.y = 3.1415
    foo.pos = MyPos(543, 2345)
    foo.opt2 = None

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
          "opt" -> ListPickle(List(IntegerPickle(5))),
          "opt2" ->NullPickle,
        )
      )
    end expectedPickle

    assert(clue(pickle) == clue(expectedPickle))
  }

  test("pickling Painter") {
    given PicklingContext = makeContext()

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
    given PicklingContext = makeContext()

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
          "opt" -> ListPickle(List(IntegerPickle(123))),
          "opt2" -> NullPickle,
        )
      )
    end inputPickle

    val foo = new Foo
    InPlacePickleable.unpickle(foo, inputPickle)

    assert(clue(foo.s) == "hello world")
    assert(clue(foo.bar.y) == 3.1415)
    assert(clue(foo.pos) == MyPos(543, 2345))
    assert(clue(foo.opt) == Some(123))
    assert(clue(foo.opt2) == None)
  }
end PicklingTest
