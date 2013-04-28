package com.funlabyrinthe.editor.inspector

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

  /** Enumerate the reflectable properties of an instance */
  def reflectableProperties(instance: InstanceMirror,
      tpe: Type): Iterable[ReflectableProperty] = {

    val result = new mutable.ListBuffer[ReflectableProperty]

    for (member <- tpe.members) {
      if (member.isPublic && member.isMethod) {
        val getter = member.asMethod

        getter.typeSignatureIn(tpe) match {
          case NullaryMethodType(propertyType) =>
            val setterName = newTermName(getter.name.toString+"_$eq")
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
      (propType, getter, maybeSetter) <- reflectableProperties(instance, tpe)
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
  def reflectingEditorsForProperties(inspector: Inspector,
      instance: InstanceMirror, tpe: Type): Iterable[Editor] = {

    for {
      data <- reflectedDataForProperties(instance, tpe)
      editor <- inspector.registry.createEditor(inspector, data)
    } yield {
      editor
    }
  }
}
