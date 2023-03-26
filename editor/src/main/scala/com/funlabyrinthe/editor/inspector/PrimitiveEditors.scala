package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.editor.reflect._

object PrimitiveEditors {
  def registerPrimitiveEditors(registry: InspectorRegistry) {
    registry.registerExactTypeReadWrite(InspectedType.String, new StringEditor(_, _))
    registry.registerExactTypeReadWrite(InspectedType.Boolean, new BooleanEditor(_, _))
    registry.registerExactTypeReadWrite(InspectedType.Char, new CharEditor(_, _))
    registry.registerExactTypeReadWrite(InspectedType.Byte, new ByteEditor(_, _))
    registry.registerExactTypeReadWrite(InspectedType.Short, new ShortEditor(_, _))
    registry.registerExactTypeReadWrite(InspectedType.Int, new IntEditor(_, _))
    registry.registerExactTypeReadWrite(InspectedType.Long, new LongEditor(_, _))
    registry.registerExactTypeReadWrite(InspectedType.Float, new FloatEditor(_, _))
    registry.registerExactTypeReadWrite(InspectedType.Double, new DoubleEditor(_, _))
  }

  class StringEditor(inspector: Inspector, data: InspectedData)
  extends Editor(inspector, data) with StringBasedEditor {

    override def stringToValue(v: String): String = v
  }

  class BooleanEditor(inspector: Inspector, data: InspectedData)
  extends Editor(inspector, data) with StringBasedEditor
                                  with ValueListBasedEditor {

    override def stringToValue(v: String): Boolean = {
      if (v.equalsIgnoreCase("true")) true
      else if (v.equalsIgnoreCase("false")) false
      else throw new ValueFormatException(s"'$v' is not a valid Boolean")
    }

    override def valueList: List[Boolean] = List(false, true)
  }

  class CharEditor(inspector: Inspector, data: InspectedData)
  extends Editor(inspector, data) with StringBasedEditor {

    override def stringToValue(v: String): Char = {
      if (v.length != 1)
        throw new ValueFormatException(s"'$v' is not a valid character")
      v.charAt(0)
    }
  }

  trait NumberEditor[A] extends StringBasedEditor {
    type NumberType = A

    /** Parser for the number, takes the string and a radix
     *  @throws java.lang.NumberFormatException
     */
    def parser(s: String, radix: Int): NumberType

    def typeNameForMessage: String =
      data.tpe.toString()

    /** Parse the radix of a number
     *  Currently this supports hexadecimal notation starting with 0x or 0X,
     *  and decimal notation otherwise.
     */
    def parseRadix(v: String): (String, Int) = {
      if ((v startsWith "0x") || (v startsWith "0X")) (v.substring(2), 16)
      else if ((v startsWith "-0x") || (v startsWith "-0X")) ("-"+v.substring(3), 16)
      else (v, 10)
    }

    override def stringToValue(v: String): NumberType = {
      val (s, radix) = parseRadix(v)
      try parser(s, radix)
      catch {
        case e: NumberFormatException =>
          throw new ValueFormatException(
              s"'v' is not a valid $typeNameForMessage", e)
      }
    }
  }

  class ByteEditor(inspector: Inspector, data: InspectedData)
  extends Editor(inspector, data) with NumberEditor[Byte] {

    override def parser(s: String, radix: Int) =
      java.lang.Byte.parseByte(s, radix)
  }

  class ShortEditor(inspector: Inspector, data: InspectedData)
  extends Editor(inspector, data) with NumberEditor[Short] {

    override def parser(s: String, radix: Int) =
      java.lang.Short.parseShort(s, radix)
  }

  class IntEditor(inspector: Inspector, data: InspectedData)
  extends Editor(inspector, data) with NumberEditor[Int] {

    override def parser(s: String, radix: Int) =
      java.lang.Integer.parseInt(s, radix)
  }

  class LongEditor(inspector: Inspector, data: InspectedData)
  extends Editor(inspector, data) with NumberEditor[Long] {

    override def parser(s: String, radix: Int) =
      java.lang.Long.parseLong(s, radix)
  }

  class FloatEditor(inspector: Inspector, data: InspectedData)
  extends Editor(inspector, data) with NumberEditor[Float] {

    override def parser(s: String, radix: Int) =
      java.lang.Float.parseFloat(s)
  }

  class DoubleEditor(inspector: Inspector, data: InspectedData)
  extends Editor(inspector, data) with NumberEditor[Double] {

    override def parser(s: String, radix: Int) =
      java.lang.Double.parseDouble(s)
  }
}
