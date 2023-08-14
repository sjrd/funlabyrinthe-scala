package com.funlabyrinthe.core.reflect

import scala.reflect.{ClassTag, classTag}

final class InspectedType private (private val underlying: InspectedType.Repr):
  import InspectedType.*

  def isEquiv(that: InspectedType): Boolean =
    this.underlying == that.underlying

  def isSubtype(that: InspectedType): Boolean =
    Repr.isSubRepr(this.underlying, that.underlying)

  override def toString(): String =
    s"InspectedType($underlying)"
end InspectedType

object InspectedType:
  private enum Repr:
    case Any, AnyRef, String, Boolean, Char, Byte, Short, Int, Long, Float, Double
    case List(elemType: Repr)
    case MonoClass(cls: Class[?])
    case Enum[A](cls: Class[A], values: scala.List[A])
  end Repr

  private object Repr:
    def isSubRepr(lhs: Repr, rhs: Repr): Boolean =
      (lhs, rhs) match
        case _ if lhs == rhs                        => true
        case (_, Any)                               => true
        case (MonoClass(_) | List(_), AnyRef)       => true
        case (List(lhsElem), List(rhsEleme))        => isSubRepr(lhsElem, rhsEleme)
        case (MonoClass(lhsCls), MonoClass(rhsCls)) => rhsCls.isAssignableFrom(lhsCls)
        case (Enum(lhsCls, _), Enum(rhsCls, _))     => lhsCls == rhsCls
        case (Enum(lhsCls, _), MonoClass(rhsCls))   => rhsCls.isAssignableFrom(lhsCls)
        case _ => false
    end isSubRepr
  end Repr

  val Any: InspectedType = InspectedType(Repr.Any)
  val AnyRef: InspectedType = InspectedType(Repr.AnyRef)
  val String: InspectedType = InspectedType(Repr.String)
  val Boolean: InspectedType = InspectedType(Repr.Boolean)
  val Char: InspectedType = InspectedType(Repr.Char)
  val Byte: InspectedType = InspectedType(Repr.Byte)
  val Short: InspectedType = InspectedType(Repr.Short)
  val Int: InspectedType = InspectedType(Repr.Int)
  val Long: InspectedType = InspectedType(Repr.Long)
  val Float: InspectedType = InspectedType(Repr.Float)
  val Double: InspectedType = InspectedType(Repr.Double)

  val ListOfAny: InspectedType = listOf(Any)

  def listOf(elemType: InspectedType): InspectedType =
    InspectedType(Repr.List(elemType.underlying))

  def monoClass(cls: Class[?]): InspectedType =
    InspectedType(Repr.MonoClass(cls))

  def staticMonoClass[T <: AnyRef](using ClassTag[T]): InspectedType =
    monoClass(classTag[T].runtimeClass)

  def enumClass[T](cls: Class[T], values: List[T]): InspectedType =
    InspectedType(Repr.Enum[T](cls, values))

  object ListOf:
    def apply(elemType: InspectedType): InspectedType =
      listOf(elemType)

    def unapply(tpe: InspectedType): Option[InspectedType] = tpe.underlying match
      case Repr.List(elemType) => Some(InspectedType(elemType))
      case _                   => None
  end ListOf

  object EnumClass:
    def unapply(tpe: InspectedType): Option[List[Any]] = tpe.underlying match
      case Repr.Enum(_, values) => Some(values)
      case _                    => None
  end EnumClass
end InspectedType
