package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.editor.reflect._

import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._

trait ConstructiblePickler extends MutableMembersPickler {
  override def pickle(data: InspectedData)(implicit ctx: Context): Pickle = {
    import ReflectionUtils._

    val instanceMirror = reflectInstance(data.value)
    val tpe = guessRuntimeTypeOf(instanceMirror, this.tpe)

    val pickledParams = for {
      fir <- reflectableFields(tpe)
      if fir.isParam
    } yield {
      if (fir.accessor.isEmpty)
        throw new UnsupportedOperationException(
            s"Ouch! Param ${fir.name} of $tpe cannot be accessed")
      val paramData = new FieldIRData(instanceMirror, fir) {
        override val isReadOnly = false
      }
      val optPickler = ctx.registry.createPickler(paramData)
      optPickler.fold[(String, Pickle)] {
        throw new UnsupportedOperationException(
            s"Ouch! Cannot find a pickler for param ${paramData.name} of $tpe")
      } { paramPickler =>
        (paramData.name, paramPickler.pickle(paramData))
      }
    }

    val paramNames = pickledParams.map(_._1).toSet

    val mutableFieldsPickle = super.pickle(data, exclude = paramNames)

    ObjectPickle(List(
        "tpe" -> StringPickle(data.value.getClass.getName),
        "params" -> ListPickle(pickledParams.map(_._2)),
        "fields" -> mutableFieldsPickle))
  }

  override def unpickle(data: InspectedData, pickle: Pickle)(
      implicit ctx: Context): Unit = {
    import ReflectionUtils._

    pickle match {
      case ObjectPickle(List(
          ("tpe", StringPickle(fullClassName)),
          ("params", ListPickle(paramPickles)),
          ("fields", mutableFieldsPickle))) =>

        val rm = runtimeMirror(ctx.getClass.getClassLoader)
        val classSym = rm.staticClass(fullClassName)
        val tpe = classSym.toTypeConstructor.erasure

        val paramFields = reflectableFields(tpe).filter(_.isParam)

        if (paramPickles.size != paramFields.size)
          return

        val params = for {
          (fir, paramPickle) <- paramFields zip paramPickles
        } yield {
          ctx.unpickleViaTempReadWrite(fir.name, fir.tpe,
              (name: String) => s"param $name of $tpe", paramPickle)
        }

        val ctor = tpe.declaration(
            nme.CONSTRUCTOR).asTerm.alternatives.head.asMethod
        val reflectCtor = rm.reflectClass(classSym).reflectConstructor(ctor)

        val instance = reflectCtor(params:_*)
        data.value = instance

        super.unpickle(data, mutableFieldsPickle)

      case _ =>
        ()
    }
  }
}
