package com.funlabyrinthe.editor.inspector

import scala.reflect.runtime.universe._

import scala.collection.mutable

class Inspector(val registry: InspectorRegistry) {
  def this() = this(new InspectorRegistry)

  private var _inspectedObject: Option[AnyRef] = None
  def inspectedObject = _inspectedObject
  def inspectedObject_=(v: Option[AnyRef]) {
    _inspectedObject = v
    clearDescriptors()
    v foreach populateDescriptors
    onChange()
  }

  private var _onChange: () => Unit = () => ()
  def onChange = _onChange
  def onChange_=(body: => Unit): Unit = _onChange = () => body

  val descriptors = new mutable.ArrayBuffer[Editor]

  private def clearDescriptors() {
    descriptors.clear()
  }

  private def populateDescriptors(instance: AnyRef) {
    val m = runtimeMirror(instance.getClass.getClassLoader)
    val im = m.reflect(instance)
    val tpe = im.symbol.toType
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
              if (setters.isMethod) setters.asMethod.alternatives.head
              else NoSymbol

            val data: ReflectedData = {
              if (setter == NoSymbol) {
                new ReadOnlyReflectedData(tpe,
                    im.reflectMethod(getter))
              } else {
                new ReadWriteReflectedData(tpe,
                    im.reflectMethod(getter),
                    im.reflectMethod(setter.asMethod))
              }
            }

            val editor = registry.createEditor(this, data)
            if (editor.isDefined)
              descriptors += editor.get

          case _ => ()
        }
      }
    }
  }
}
