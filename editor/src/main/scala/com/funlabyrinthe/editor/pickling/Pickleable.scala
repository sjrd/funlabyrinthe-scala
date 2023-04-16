package com.funlabyrinthe.editor.pickling

import scala.deriving.*
import scala.compiletime.{erasedValue, summonInline}

import com.funlabyrinthe.core.reflect.InspectedData

trait Pickleable[T]:
  def pickle(value: T)(using Context): Pickle

  def unpickle(pickle: Pickle)(using Context): Option[T]

  private object pickler extends Pickler:
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      Pickleable.this.pickle(data.value.asInstanceOf[T])

    def unpickle(data: InspectedData, pickle: Pickle)(implicit ctx: Context): Unit =
      for newValue <- Pickleable.this.unpickle(pickle) do
        data.asWritable.value = newValue
  end pickler

  final def toPickler: Pickler = pickler
end Pickleable

object Pickleable:
  inline def derived[T](using m: Mirror.Of[T]): Pickleable[T] =
    val elemInstances = IArray.from(summonAll[m.MirroredElemTypes])
    inline m match
      case s: Mirror.SumOf[T]     => derivedForSum(s, elemInstances)
      case p: Mirror.ProductOf[T] => derivedForProduct(p, elemInstances)
  end derived

  private def derivedForSum[T](m: Mirror.SumOf[T], elems: IArray[Pickleable[?]]): Pickleable[T] =
    new Pickleable[T] {
      def pickle(value: T)(using Context): Pickle =
        val ord = m.ordinal(value)
        val content = elems(ord) match
          case inner: Pickleable[u] => inner.pickle(value.asInstanceOf[u])
        ObjectPickle(List("ordinal" -> IntPickle(ord), "content" -> content))
      end pickle

      def unpickle(pickle: Pickle)(using Context): Option[T] =
        pickle match
          case pickle: ObjectPickle =>
            for
              case IntPickle(ord) <- pickle.getField("ordinal")
              content <- pickle.getField("content")
              value <- elems(ord).unpickle(content)
            yield
              value.asInstanceOf[T]
          case _ =>
            None
      end unpickle
    }
  end derivedForSum

  private def derivedForProduct[T](m: Mirror.ProductOf[T], elems: IArray[Pickleable[?]]): Pickleable[T] =
    new Pickleable[T] {
      def pickle(value: T)(using Context): Pickle =
        val product = value.asInstanceOf[Product]
        val innerPickles = product.productIterator.zip(elems.iterator).map {
          (elem, elemPickleable) =>
            elemPickleable match
              case elemPickleable: Pickleable[u] => elemPickleable.pickle(elem.asInstanceOf[u])
        }
        ListPickle(innerPickles.toList)
      end pickle

      def unpickle(pickle: Pickle)(using Context): Option[T] =
        pickle match
          case ListPickle(elemPickles) if elemPickles.sizeIs == elems.size =>
            val elemValues = elemPickles.iterator.zip(elems.iterator).map {
              (elemPickle, elemPickleable) =>
                elemPickleable.unpickle(elemPickle)
            }.toArray
            if (elemValues.forall(_.isDefined))
              Some(m.fromProduct(Tuple.fromArray(elemValues.map(_.get))))
            else
              None
          case _ =>
            None
      end unpickle
    }
  end derivedForProduct

  private inline def summonAll[T <: Tuple]: List[Pickleable[_]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonInline[Pickleable[t]] :: summonAll[ts]
  end summonAll

  given StringPickleable: Pickleable[String] with
    def pickle(value: String)(using Context): Pickle =
      StringPickle(value)

    def unpickle(pickle: Pickle)(using Context): Option[String] =
      pickle match {
        case StringPickle(v) => Some(v)
        case _                => None
      }
  end StringPickleable

  given BooleanPickleable: Pickleable[Boolean] with
    def pickle(value: Boolean)(using Context): Pickle =
      BooleanPickle(value)

    def unpickle(pickle: Pickle)(using Context): Option[Boolean] =
      pickle match {
        case BooleanPickle(v) => Some(v)
        case _                => None
      }
  end BooleanPickleable

  given CharPickleable: Pickleable[Char] with
    def pickle(value: Char)(using Context): Pickle =
      CharPickle(value)

    def unpickle(pickle: Pickle)(using Context): Option[Char] =
      pickle match {
        case CharPickle(v) => Some(v)
        case _             => None
      }
  end CharPickleable

  given BytePickleable: Pickleable[Byte] with
    def pickle(value: Byte)(using Context): Pickle =
      BytePickle(value)

    def unpickle(pickle: Pickle)(using Context): Option[Byte] =
      pickle match {
        case IntegerPickle(v) => Some(v.toByte)
        case _                => None
      }
  end BytePickleable

  given ShortPickleable: Pickleable[Short] with
    def pickle(value: Short)(using Context): Pickle =
      ShortPickle(value)

    def unpickle(pickle: Pickle)(using Context): Option[Short] =
      pickle match {
        case IntegerPickle(v) => Some(v.toShort)
        case _                => None
      }
  end ShortPickleable

  given IntPickleable: Pickleable[Int] with
    def pickle(value: Int)(using Context): Pickle =
      IntPickle(value)

    def unpickle(pickle: Pickle)(using Context): Option[Int] =
      pickle match {
        case IntegerPickle(v) => Some(v.toInt)
        case _                => None
      }
  end IntPickleable

  given LongPickleable: Pickleable[Long] with
    def pickle(value: Long)(using Context): Pickle =
      LongPickle(value)

    def unpickle(pickle: Pickle)(using Context): Option[Long] =
      pickle match {
        case IntegerPickle(v) => Some(v)
        case _                => None
      }
  end LongPickleable

  given FloatPickleable: Pickleable[Float] with
    def pickle(value: Float)(using Context): Pickle =
      FloatPickle(value)

    def unpickle(pickle: Pickle)(using Context): Option[Float] =
      pickle match {
        case NumberPickle(v) => Some(v.toFloat)
        case _               => None
      }
  end FloatPickleable

  given DoublePickleable: Pickleable[Double] with
    def pickle(value: Double)(using Context): Pickle =
      DoublePickle(value)

    def unpickle(pickle: Pickle)(using Context): Option[Double] =
      pickle match {
        case NumberPickle(v) => Some(v)
        case _               => None
      }
  end DoublePickleable

  given ListPickleable[T](using Pickleable[T]): Pickleable[List[T]] with
    def pickle(value: List[T])(using Context): Pickle =
      ListPickle(value.map(summon[Pickleable[T]].pickle(_)))

    def unpickle(pickle: Pickle)(using Context): Option[List[T]] =
      pickle match
        case ListPickle(elemPickles) =>
          val maybeElems = elemPickles.map(summon[Pickleable[T]].unpickle(_))
          if maybeElems.forall(_.isDefined) then
            Some(maybeElems.map(_.get))
          else
            None
        case _ =>
          None
    end unpickle
  end ListPickleable
end Pickleable
