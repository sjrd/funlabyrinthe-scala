package com.funlabyrinthe.editor.reflect

import scala.reflect.runtime.universe._

import scala.collection.mutable

object ReflectionUtils {
  type ReflectableProperty = (Type, MethodSymbol, Option[MethodSymbol])

  def reflectInstance(instance: Any): InstanceMirror =
    runtimeMirror(instance.getClass.getClassLoader).reflect(instance)

  def guessRuntimeTypeOf(instanceMirror: InstanceMirror,
      bestKnownSuperType: Type = typeOf[Any]): Type = {
    /* In theory we could intersect this with bestKnownSuperType, but
     * currently it seems not to give a better result.
     * Or we could use something like propagateKnownTypes.
     * But currently we do none of these things, and just do not use
     * `bestKnownSuperType`.
     */
    instanceMirror.symbol.toTypeConstructor.erasure
  }

  def guessRuntimeTypeOfValue[A : TypeTag](value: A): Type = {
    val instanceMirror = ReflectionUtils.reflectInstance(value)
    guessRuntimeTypeOf(instanceMirror, typeOf[A])
  }

  def reflectableFields(tpe: Type): List[FieldIR] = {
    val ctor = tpe.decl(termNames.CONSTRUCTOR) match {
      // NOTE: primary ctor is always the first in the list
      case overloaded: TermSymbol => overloaded.alternatives.head.asMethod
      case primaryCtor: MethodSymbol => primaryCtor
      case NoSymbol => NoSymbol
    }
    val ctorParams =
      if (ctor != NoSymbol) ctor.asMethod.paramLists.flatten.map(_.asTerm)
      else Nil

    val allAccessors = tpe.members collect {
      case meth: MethodSymbol if meth.isAccessor || meth.isParamAccessor => meth
    }
    val (paramAccessors, otherAccessors) =
      allAccessors.partition(_.isParamAccessor)

    def mkFieldIR(sym: TermSymbol, param: Option[TermSymbol],
        accessor: Option[MethodSymbol]) = {
      val (quantified, rawTp) = tpe match {
        case ExistentialType(quantified, tpe) => (quantified, tpe)
        case tpe => (Nil, tpe)
      }
      val rawSymTp = accessor.getOrElse(sym).typeSignatureIn(rawTp) match {
        case NullaryMethodType(tpe) => tpe
        case tpe => tpe
      }
      val symTp = internal.existentialAbstraction(quantified, rawSymTp)
      FieldIR(sym.name.toString.trim, symTp, param, accessor)
    }

    val paramFields = ctorParams map {
      sym => mkFieldIR(sym, Some(sym), paramAccessors.find(_.name == sym.name))
    }
    val valAndVarGetters = otherAccessors collect {
      case meth if meth.isGetter && meth.accessed != NoSymbol => meth
    }
    val valAndVarFields = valAndVarGetters map {
      sym => mkFieldIR(sym, None, Some(sym))
    }
    paramFields ++ valAndVarFields
  }

  /** Enumerate the reflectable properties of an instance */
  def reflectableProperties(tpe: Type): Iterable[ReflectableProperty] = {
    val result = new mutable.ListBuffer[ReflectableProperty]

    for (member <- tpe.members) {
      if (member.isPublic && member.isMethod) {
        val getter = member.asMethod

        getter.typeSignatureIn(tpe) match {
          case NullaryMethodType(propertyType) =>
            val setterName = TermName(getter.name.toString+"_$eq")
            val setters = tpe.member(setterName).filter { sym =>
              sym.isPublic && sym.isMethod && (sym.typeSignatureIn(tpe) match {
                case MethodType(List(param), _) =>
                  param.typeSignatureIn(tpe) =:= propertyType
                case _ => false
              })
            }
            val setter =
              if (setters.isMethod) Some(setters.asMethod.alternatives.head.asMethod)
              else None

            result += ((propertyType, getter, setter))

          case _ => ()
        }
      }
    }

    result.toList
  }

  /** Enumerate the reflected data for properties of an instance */
  def reflectedDataForProperties(instance: InstanceMirror,
      tpe: Type): Iterable[ReflectedData] = {

    for {
      (propType, getter, maybeSetter) <- reflectableProperties(tpe)
    } yield {
      maybeSetter match {
        case None =>
          new ReadOnlyReflectedData(tpe,
              instance.reflectMethod(getter))

        case Some(setter) =>
          new ReadWriteReflectedData(tpe,
              instance.reflectMethod(getter),
              instance.reflectMethod(setter))
      }
    }
  }

  /** Enumerate the reflected data for properties of an instance */
  def reflectedDataForFields(instance: InstanceMirror,
      tpe: Type): Iterable[FieldIRData] = {

    for {
      fir <- reflectableFields(tpe)
      if fir.field.isDefined
      if !hasTransientAnnot(fir.field.get)
    } yield {
      println(fir)
      new FieldIRData(instance, fir)
    }
  }

  def hasTransientAnnot(sym: Symbol): Boolean =
    sym.annotations.exists(_.tree.tpe.typeSymbol == transientClass)

  lazy val transientClass: ClassSymbol =
    reflect.runtime.universe.rootMirror.staticClass("scala.transient")
}
