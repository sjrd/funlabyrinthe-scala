package com.funlabyrinthe.core.pickling

import scala.deriving.*
import scala.compiletime.{erasedValue, summonInline}

import scala.collection.Factory
import scala.collection.immutable.TreeSet

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
      case s: Mirror.SumOf[T] =>
        val elemNames = IArray.from(summonValues[m.MirroredElemLabels])
        derivedForSum(s, elemInstances, elemNames)
      case p: Mirror.ProductOf[T] =>
        derivedForProduct(p, elemInstances)
  end derived

  private def derivedForSum[T](m: Mirror.SumOf[T], elems: IArray[Pickleable[?]], elemNames: IArray[String]): Pickleable[T] =
    new Pickleable[T] {
      def pickle(value: T)(using PicklingContext): Pickle =
        val ord = m.ordinal(value)
        val elemName = elemNames(ord)
        val content = elems(ord) match
          case inner: Pickleable[u] => inner.pickle(value.asInstanceOf[u])
        if NullPickle == content then
          StringPickle(elemName)
        else
          ObjectPickle(List(elemName -> content))
      end pickle

      def unpickle(pickle: Pickle)(using PicklingContext): Option[T] =
        pickle match
          case StringPickle(elemName) =>
            val ord = elemNames.indexOf(elemName)
            if ord < 0 then
              PicklingContext.typeError(elemNames.mkString("one of ", ", ", ""), pickle)
            else
              summon[PicklingContext].withSubPath(elemName) {
                elems(ord).unpickle(NullPickle).map(_.asInstanceOf[T])
              }
          case ObjectPickle(List((elemName, contentPickle))) =>
            val ord = elemNames.indexOf(elemName)
            if ord < 0 then
              PicklingContext.typeError(elemNames.mkString("one of ", ", ", ""), pickle)
            else
              summon[PicklingContext].withSubPath(elemName) {
                elems(ord).unpickle(contentPickle).map(_.asInstanceOf[T])
              }
          case _ =>
            PicklingContext.typeError(
              elemNames.mkString("string or object with a single field that is one of ", ", ", ""),
              pickle
            )
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
        innerPickles.toList match
          case Nil           => NullPickle
          case single :: Nil => single
          case multiple      => ListPickle(multiple)
      end pickle

      def unpickle(pickle: Pickle)(using PicklingContext): Option[T] =
        elems.length match
          case 0 =>
            if NullPickle == pickle then Some(m.fromProduct(EmptyTuple))
            else PicklingContext.typeError("null", pickle)
          case 1 =>
            for elemValue <- elems(0).unpickle(pickle) yield
              m.fromProduct(Tuple1(elemValue))
          case elemCount =>
            pickle match
              case ListPickle(elemPickles) if elemPickles.sizeIs == elemCount =>
                val elemValues = elemPickles.iterator.zip(elems.iterator).map {
                  (elemPickle, elemPickleable) =>
                    elemPickleable.unpickle(elemPickle)
                }.toArray
                if (elemValues.forall(_.isDefined))
                  Some(m.fromProduct(Tuple.fromArray(elemValues.map(_.get))))
                else
                  None
              case _ =>
                PicklingContext.typeError(s"list with $elemCount elements", pickle)
      end unpickle
    }
  end derivedForProduct

  private inline def summonAll[T <: Tuple]: List[Pickleable[?]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonInline[Pickleable[t]] :: summonAll[ts]
  end summonAll

  private inline def summonValues[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[ValueOf[t]].value.asInstanceOf[String] :: summonValues[ts]

  given UnitPickleable: Pickleable[Unit] with
    def pickle(value: Unit)(using PicklingContext): Pickle =
      NullPickle

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Unit] =
      pickle match {
        case NullPickle => Some(())
        case _          => PicklingContext.typeError("null", pickle)
      }
  end UnitPickleable

  given StringPickleable: Pickleable[String] with
    def pickle(value: String)(using PicklingContext): Pickle =
      StringPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[String] =
      pickle match {
        case StringPickle(v) => Some(v)
        case _               => PicklingContext.typeError("string", pickle)
      }
  end StringPickleable

  given BooleanPickleable: Pickleable[Boolean] with
    def pickle(value: Boolean)(using PicklingContext): Pickle =
      BooleanPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Boolean] =
      pickle match {
        case BooleanPickle(v) => Some(v)
        case _                => PicklingContext.typeError("boolean", pickle)
      }
  end BooleanPickleable

  given CharPickleable: Pickleable[Char] with
    def pickle(value: Char)(using PicklingContext): Pickle =
      IntegerPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Char] =
      unpickleIntInRange(pickle, 0 to Char.MaxValue.toInt).map(_.toChar)
  end CharPickleable

  given BytePickleable: Pickleable[Byte] with
    def pickle(value: Byte)(using PicklingContext): Pickle =
      IntegerPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Byte] =
      unpickleIntInRange(pickle, Byte.MinValue to Byte.MaxValue).map(_.toByte)
  end BytePickleable

  given ShortPickleable: Pickleable[Short] with
    def pickle(value: Short)(using PicklingContext): Pickle =
      IntegerPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Short] =
      unpickleIntInRange(pickle, Short.MinValue to Short.MaxValue).map(_.toShort)
  end ShortPickleable

  given IntPickleable: Pickleable[Int] with
    def pickle(value: Int)(using PicklingContext): Pickle =
      IntegerPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Int] =
      unpickleIntInRange(pickle, Int.MinValue to Int.MaxValue)
  end IntPickleable

  private def unpickleIntInRange(pickle: Pickle, range: Range)(using PicklingContext): Option[Int] =
    pickle match
      case IntegerPickle.ofInt(intValue) if range.contains(intValue) =>
        Some(intValue)
      case IntegerPickle(value) =>
        PicklingContext.typeError(s"integer in the range $range", value)
      case _ =>
        PicklingContext.typeError(s"integer in the range $range", pickle)
  end unpickleIntInRange

  given LongPickleable: Pickleable[Long] with
    def pickle(value: Long)(using PicklingContext): Pickle =
      IntegerPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Long] =
      pickle match
        case IntegerPickle.ofLong(longValue) =>
          Some(longValue)
        case IntegerPickle(value) =>
          PicklingContext.typeError(s"integer in the range ${Long.MinValue} to ${Long.MaxValue}", value)
        case _ =>
          PicklingContext.typeError(s"integer in the range ${Long.MinValue} to ${Long.MaxValue}", pickle)
    end unpickle
  end LongPickleable

  given FloatPickleable: Pickleable[Float] with
    def pickle(value: Float)(using PicklingContext): Pickle =
      DecimalPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Float] =
      pickle match {
        case pickle: DecimalPickle => Some(pickle.floatValue)
        case _                     => PicklingContext.typeError("decimal", pickle)
      }
  end FloatPickleable

  given DoublePickleable: Pickleable[Double] with
    def pickle(value: Double)(using PicklingContext): Pickle =
      DecimalPickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Double] =
      pickle match {
        case pickle: DecimalPickle => Some(pickle.doubleValue)
        case _                     => PicklingContext.typeError("decimal", pickle)
      }
  end DoublePickleable

  given OptionPickleable[T](using Pickleable[T]): Pickleable[Option[T]] with
    def pickle(value: Option[T])(using PicklingContext): Pickle =
      value match
        case None    => NullPickle
        case Some(v) => ListPickle(List(summon[Pickleable[T]].pickle(v)))

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Option[T]] =
      pickle match
        case NullPickle =>
          Some(None)
        case ListPickle(elemPickle :: Nil) =>
          summon[Pickleable[T]].unpickle(elemPickle).map(Some(_))
        case _ =>
          PicklingContext.typeError("null or single-element list", pickle)
  end OptionPickleable

  given ListPickleable[T](using Pickleable[T]): Pickleable[List[T]] with
    def pickle(value: List[T])(using PicklingContext): Pickle =
      ListPickle(value.map(summon[Pickleable[T]].pickle(_)))

    def unpickle(pickle: Pickle)(using PicklingContext): Option[List[T]] =
      unpickleList[T](pickle)
  end ListPickleable

  given SetPickleable[E, T <: Set[E]](
    using elemPickleable: Pickleable[E],
    elemOrdering: Ordering[E],
    factory: Factory[E, T],
  ): Pickleable[T] with
    def pickle(value: T)(using PicklingContext): Pickle =
      // sorted for stability
      ListPickle(value.toList.sorted.map(elemPickleable.pickle(_)))

    def unpickle(pickle: Pickle)(using PicklingContext): Option[T] =
      unpickleList[E](pickle).map(list => factory.fromSpecific(list))
  end SetPickleable

  private def unpickleList[T](pickle: Pickle)(using PicklingContext, Pickleable[T]): Option[List[T]] =
    pickle match
      case ListPickle(elemPickles) =>
        val maybeElems = elemPickles.map(summon[Pickleable[T]].unpickle(_))
        val validElems = maybeElems.collect {
          case Some(elem) => elem
        }
        if validElems.isEmpty && elemPickles.nonEmpty then
          // something is deeply wrong with the inner elements; keep default list
          None
        else
          // at least preserve the valid elements
          Some(validElems)
      case _ =>
        PicklingContext.typeError("list", pickle)
  end unpickleList
end Pickleable
