package com.funlabyrinthe.core.pickling

private[pickling] object PickleParser:
  def parse(input: String): Pickle =
    new PickleParser(input).topLevel()
  end parse
end PickleParser

private[pickling] final class PickleParser(input: String):
  private var idx: Int = 0

  def topLevel(): Pickle =
    val result = pickle()
    space()
    if idx != input.length() then
      parseError(s"Expected EOF at $idx")
    result
  end topLevel

  private def pickle(): Pickle =
    space()

    curCharNotEOF() match
      case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
        number()

      case '"' => string()
      case '[' => list()
      case '{' => obj()
      case 'u' => constant("undefined", UnitPickle)
      case 'n' => constant("null", NullPickle)
      case 'f' => constant("false", BooleanPickle(false))
      case 't' => constant("true", BooleanPickle(true))
      case 'I' => constant("Infinity", DecimalPickle("Infinity"))
      case 'N' => constant("NaN", DecimalPickle("NaN"))
      case c =>
        parseError(s"Unexpected char '$c'")
  end pickle

  private def verbatim(kw: String): Unit =
    val endIdx = idx + kw.length()
    if endIdx > input.length() || input.substring(idx, endIdx) != kw then
      parseError(s"Expected '$kw'")
    idx = endIdx
  end verbatim

  private def constant[T](kw: String, value: T): T =
    verbatim(kw)
    value
  end constant

  private def number(): Pickle =
    val isNegative = curCharNotEOF() match
      case '-' =>
        idx += 1
        true
      case _ =>
        false
    end isNegative

    curCharNotEOF() match
      case 'I' =>
        constant("Infinity", DecimalPickle("-Infinity"))
      case _ =>
        val integral = digits()
        IntegerPickle(integral)
  end number

  private def digits(): String =
    val start = idx
    while !atEOF() && curChar() >= '0' && curChar() <= '9' do
      idx += 1
    if idx == start then
      parseError("Expected number")
    input.substring(start, idx)
  end digits

  private def string(): StringPickle =
    idx += 1
    val builder = new java.lang.StringBuilder()
    while curCharNotEOF() != '"' do
      builder.append(curChar())
      idx += 1
    idx += 1
    StringPickle(builder.toString())
  end string

  private def list(): ListPickle =
    val builder = List.newBuilder[Pickle]
    idx += 1
    space()
    while curCharNotEOF() != ']' do
      builder += pickle()
      space()
      verbatim(",")
      space()
    idx += 1
    ListPickle(builder.result())
  end list

  private def obj(): ObjectPickle =
    val builder = List.newBuilder[(String, Pickle)]
    idx += 1
    space()
    while curCharNotEOF() != '}' do
      val key = string().value
      space()
      verbatim(":")
      val value = pickle()
      builder += key -> value
      space()
      verbatim(",")
      space()
    idx += 1
    ObjectPickle(builder.result())
  end obj

  private def space(): Unit =
    while idx < input.length() && Character.isWhitespace(input.charAt(idx)) do
      idx += 1
  end space

  private def curCharNotEOF(): Char =
    if atEOF() then
      parseError(s"Unexpected EOF")
    input.charAt(idx)
  end curCharNotEOF

  private def curChar(): Char = input.charAt(idx)

  private def atEOF(): Boolean = idx == input.length()

  private def parseError(msg: String): Nothing =
    throw IllegalArgumentException(s"$msg at $idx")
end PickleParser
