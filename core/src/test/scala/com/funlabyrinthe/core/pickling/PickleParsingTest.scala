package com.funlabyrinthe.core.pickling

class PickleParsingTest extends munit.FunSuite:
  def testParse(input: String, expected: Pickle)(using munit.Location): Unit =
    val parsed = Pickle.fromString(input)
    assert(clue(parsed) == clue(expected))

  test("elementary") {
    testParse(" null ", NullPickle)
    testParse("\n  true  ", BooleanPickle(true))
    testParse("false", BooleanPickle(false))
    testParse("5", IntegerPickle(5))
    testParse("-65", IntegerPickle(-65))
    testParse("Infinity", DecimalPickle(Double.PositiveInfinity))
    testParse("  -Infinity", DecimalPickle(Double.NegativeInfinity))
    testParse("NaN", DecimalPickle(Double.NaN))
    testParse(""""foo"""", StringPickle("foo"))
  }

  test("list") {
    testParse(" [ 1, 2, false , ]", ListPickle(List(IntegerPickle(1), IntegerPickle(2), BooleanPickle(false))))
  }

  test("object") {
    testParse(
      s""" { "a":1, "f" ${'\n'}: false, } """,
      ObjectPickle(
        List(
          "a" -> IntegerPickle(1),
          "f" -> BooleanPickle(false),
        )
      )
    )
  }
end PickleParsingTest
