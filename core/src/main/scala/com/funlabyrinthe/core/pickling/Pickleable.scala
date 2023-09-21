package com.funlabyrinthe.core.pickling

import scala.deriving.*
import scala.compiletime.{erasedValue, summonInline}

import com.funlabyrinthe.core.reflect.InspectedData

trait Pickleable[T]:
  def pickle(value: T)(using PicklingContext): Pickle

  def unpickle(pickle: Pickle)(using PicklingContext): Option[T]
end Pickleable

object Pickleable:
  def pickle[T](value: T)(using PicklingContext, Pickleable[T]): Pickle =
    summon[Pickleable[T]].pickle(value)

  def unpickle[T](pickle: Pickle)(using PicklingContext, Pickleable[T]): Option[T] =
    summon[Pickleable[T]].unpickle(pickle)

  inline given implicitDerived[T](using m: Mirror.Of[T]): Pickleable[T] = derived[T]

  inline def derived[T](using m: Mirror.Of[T]): Pickleable[T] =
    val elemInstances = IArray.from(summonAll[m.MirroredElemTypes])
    inline m match
      case s: Mirror.SumOf[T]     => derivedForSum(s, elemInstances)
      case p: Mirror.ProductOf[T] => derivedForProduct(p, elemInstances)
  end derived

  private def derivedForSum[T](m: Mirror.SumOf[T], elems: IArray[Pickleable[?]]): Pickleable[T] =
    new Pickleable[T] {
      def pickle(value: T)(using PicklingContext): Pickle =
        val ord = m.ordinal(value)
        val content = elems(ord) match
          case inner: Pickleable[u] => inner.pickle(value.asInstanceOf[u])
        ObjectPickle(List("ordinal" -> IntegerPickle(ord), "content" -> content))
      end pickle

      def unpickle(pickle: Pickle)(using PicklingContext): Option[T] =
        pickle match
          case pickle: ObjectPickle =>
            for
              case ordPickle: IntegerPickle <- pickle.getField("ordinal")
              content <- pickle.getField("content")
              value <- elems(ordPickle.intValue).unpickle(content)
            yield
              value.asInstanceOf[T]
          case _ =>
            None
      end unpickle
    }
  end derivedForSum

  private def derivedForProduct[T](m: Mirror.ProductOf[T], elems: IArray[Pickleable[?]]): Pickleable[T] =
    new Pickleable[T] {
      def pickle(value: T)(using PicklingContext): Pickle =
        val product = value.asInstanceOf[Product]
        val innerPickles = product.productIterator.zip(elems.iterator).map {
          (elem, elemPickleable) =>
            elemPickleable match
              case elemPickleable: Pickleable[u] => elemPickleable.pickle(elem.asInstanceOf[u])
        }
        ListPickle(innerPickles.toList)
      end pickle

      def unpickle(pickle: Pickle)(using PicklingContext): Option[T] =
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
    def pickle(value: String)(using PicklingContext): Pickle =
      StringPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[String] =
      pickle match {
        case StringPickle(v) => Some(v)
        case _                => None
      }
  end StringPickleable

  given BooleanPickleable: Pickleable[Boolean] with
    def pickle(value: Boolean)(using PicklingContext): Pickle =
      BooleanPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Boolean] =
      pickle match {
        case BooleanPickle(v) => Some(v)
        case _                => None
      }
  end BooleanPickleable

  given CharPickleable: Pickleable[Char] with
    def pickle(value: Char)(using PicklingContext): Pickle =
      IntegerPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Char] =
      pickle match {
        case pickle: IntegerPickle => Some(pickle.intValue.toChar)
        case _                     => None
      }
  end CharPickleable

  given BytePickleable: Pickleable[Byte] with
    def pickle(value: Byte)(using PicklingContext): Pickle =
      IntegerPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Byte] =
      pickle match {
        case pickle: IntegerPickle => Some(pickle.intValue.toByte)
        case _                     => None
      }
  end BytePickleable

  given ShortPickleable: Pickleable[Short] with
    def pickle(value: Short)(using PicklingContext): Pickle =
      IntegerPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Short] =
      pickle match {
        case pickle: IntegerPickle => Some(pickle.intValue.toShort)
        case _                     => None
      }
  end ShortPickleable

  given IntPickleable: Pickleable[Int] with
    def pickle(value: Int)(using PicklingContext): Pickle =
      IntegerPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Int] =
      pickle match {
        case pickle: IntegerPickle => Some(pickle.intValue)
        case _                     => None
      }
  end IntPickleable

  given LongPickleable: Pickleable[Long] with
    def pickle(value: Long)(using PicklingContext): Pickle =
      IntegerPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Long] =
      pickle match {
        case pickle: IntegerPickle => Some(pickle.longValue)
        case _                     => None
      }
  end LongPickleable

  given FloatPickleable: Pickleable[Float] with
    def pickle(value: Float)(using PicklingContext): Pickle =
      DecimalPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Float] =
      pickle match {
        case pickle: DecimalPickle => Some(pickle.floatValue)
        case _                     => None
      }
  end FloatPickleable

  given DoublePickleable: Pickleable[Double] with
    def pickle(value: Double)(using PicklingContext): Pickle =
      DecimalPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Double] =
      pickle match {
        case pickle: DecimalPickle => Some(pickle.doubleValue)
        case _                     => None
      }
  end DoublePickleable

  given ListPickleable[T](using Pickleable[T]): Pickleable[List[T]] with
    def pickle(value: List[T])(using PicklingContext): Pickle =
      ListPickle(value.map(summon[Pickleable[T]].pickle(_)))

    def unpickle(pickle: Pickle)(using PicklingContext): Option[List[T]] =
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
