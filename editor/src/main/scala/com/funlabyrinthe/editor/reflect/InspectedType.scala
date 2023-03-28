package com.funlabyrinthe.editor.reflect

import scala.reflect.runtime.universe._

final class InspectedType(private val underlying: Type) {
  def isEquiv(that: InspectedType): Boolean =
    this.underlying =:= that.underlying

  def isSubtype(that: InspectedType): Boolean =
    this.underlying <:< that.underlying

  def isValue(v: Any): Boolean = {
    val m = runtimeMirror(v.getClass.getClassLoader)
    val valueTpe = m.reflect(v).symbol.toType
    isPrimitiveValueClass || (valueTpe <:< underlying)
  }

  private def isPrimitiveValueClass: Boolean =
    definitions.ScalaPrimitiveValueClasses.contains(underlying)

  override def toString(): String = underlying.toString()
}

object InspectedType {
  val Any: InspectedType = new InspectedType(TypeTag.Any.tpe)
  val AnyRef: InspectedType = new InspectedType(TypeTag.AnyRef.tpe)

  val ListOfAny: InspectedType = new InspectedType(???) // typeOf[List[Any]]

  val String: InspectedType = staticMonoClass[String]
  val Boolean: InspectedType = new InspectedType(TypeTag.Boolean.tpe)
  val Char: InspectedType = new InspectedType(TypeTag.Char.tpe)
  val Byte: InspectedType = new InspectedType(TypeTag.Byte.tpe)
  val Short: InspectedType = new InspectedType(TypeTag.Short.tpe)
  val Int: InspectedType = new InspectedType(TypeTag.Int.tpe)
  val Long: InspectedType = new InspectedType(TypeTag.Long.tpe)
  val Float: InspectedType = new InspectedType(TypeTag.Float.tpe)
  val Double: InspectedType = new InspectedType(TypeTag.Double.tpe)

  def staticMonoClass[T <: AnyRef](implicit ct: scala.reflect.ClassTag[T]): InspectedType =
    new InspectedType(scala.reflect.runtime.universe.rootMirror.classSymbol(ct.runtimeClass).toType)

  def listItemOf(tpe: InspectedType): InspectedType = {
    tpe.underlying match {
      case reflect.runtime.universe.TypeRef(pre, tpeSym, List(tparam)) =>
        new InspectedType(tparam)

      case _ =>
        println(s"Warning! $tpe of class ${tpe.underlying.getClass} not a TypeRef")
        InspectedType.Any
    }
  }
}
