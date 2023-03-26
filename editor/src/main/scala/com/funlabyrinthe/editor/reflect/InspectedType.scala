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
  val Any: InspectedType = new InspectedType(typeOf[Any])
  val AnyRef: InspectedType = new InspectedType(typeOf[AnyRef])

  val ListOfAny: InspectedType = new InspectedType(typeOf[List[Any]])

  val String: InspectedType = new InspectedType(typeOf[String])
  val Boolean: InspectedType = new InspectedType(typeOf[Boolean])
  val Char: InspectedType = new InspectedType(typeOf[Char])
  val Byte: InspectedType = new InspectedType(typeOf[Byte])
  val Short: InspectedType = new InspectedType(typeOf[Short])
  val Int: InspectedType = new InspectedType(typeOf[Int])
  val Long: InspectedType = new InspectedType(typeOf[Long])
  val Float: InspectedType = new InspectedType(typeOf[Float])
  val Double: InspectedType = new InspectedType(typeOf[Double])

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
