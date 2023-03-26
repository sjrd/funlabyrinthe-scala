package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.editor.reflect._

object PrimitivePicklers {
  def registerPrimitiveEditors(registry: PicklingRegistry): Unit = {
    registry.registerExactTypeReadWrite(InspectedType.String, (_, _) => StringPickler)
    registry.registerExactTypeReadWrite(InspectedType.Boolean, (_, _) => BooleanPickler)
    registry.registerExactTypeReadWrite(InspectedType.Char, (_, _) => CharPickler)
    registry.registerExactTypeReadWrite(InspectedType.Byte, (_, _) => BytePickler)
    registry.registerExactTypeReadWrite(InspectedType.Short, (_, _) => ShortPickler)
    registry.registerExactTypeReadWrite(InspectedType.Int, (_, _) => IntPickler)
    registry.registerExactTypeReadWrite(InspectedType.Long, (_, _) => LongPickler)
    registry.registerExactTypeReadWrite(InspectedType.Float, (_, _) => FloatPickler)
    registry.registerExactTypeReadWrite(InspectedType.Double, (_, _) => DoublePickler)
  }

  object StringPickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      StringPickle(data.value.asInstanceOf[String])

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case StringPickle(v) => data.value = v
        case _ => ()
      }
    }
  }

  object BooleanPickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      BooleanPickle(data.value.asInstanceOf[Boolean])

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case BooleanPickle(v) => data.value = v
        case _ => ()
      }
    }
  }

  object CharPickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      CharPickle(data.value.asInstanceOf[Char])

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case CharPickle(v) => data.value = v
        case _ => ()
      }
    }
  }

  object BytePickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      BytePickle(data.value.asInstanceOf[Byte])

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case IntegerPickle(v) => data.value = v.toByte
        case _ => ()
      }
    }
  }

  object ShortPickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      ShortPickle(data.value.asInstanceOf[Short])

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case IntegerPickle(v) => data.value = v.toShort
        case _ => ()
      }
    }
  }

  object IntPickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      IntPickle(data.value.asInstanceOf[Int])

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case IntegerPickle(v) => data.value = v.toInt
        case _ => ()
      }
    }
  }

  object LongPickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      LongPickle(data.value.asInstanceOf[Long])

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case IntegerPickle(v) => data.value = v.toLong
        case _ => ()
      }
    }
  }

  object FloatPickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      FloatPickle(data.value.asInstanceOf[Float])

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case NumberPickle(v) => data.value = v.toFloat
        case _ => ()
      }
    }
  }

  object DoublePickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      DoublePickle(data.value.asInstanceOf[Double])

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case NumberPickle(v) => data.value = v.toDouble
        case _ => ()
      }
    }
  }
}
