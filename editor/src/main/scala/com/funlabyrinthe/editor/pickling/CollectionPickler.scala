package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._

import scala.collection.mutable.{ Builder, ListBuffer }

trait CollectionPickler[Repr] extends Pickler {
  val elemTpe: InspectedType

  def toSeq(coll: Repr): Seq[Any]
  def fromSeq(seq: Seq[Any]): Repr

  def pickle(data: InspectedData)(implicit ctx: Context): Pickle = {
    val seq = toSeq(data.value.asInstanceOf[Repr])

    val pickledElems = new ListBuffer[Pickle]

    for (elem <- seq) {
      val elemData = new TempReadWriteData("", elemTpe, elem)

      for (elemPickler <- ctx.registry.createPickler(elemData))
        pickledElems += elemPickler.pickle(elemData)
    }

    ListPickle(pickledElems.result())
  }

  def unpickle(data: InspectedData, pickle: Pickle)(
      implicit ctx: Context): Unit = {
    pickle match {
      case ListPickle(elemsPickles) =>
        val builder = Seq.newBuilder[Any]

        for (elemPickle <- elemsPickles) {
          val elem = ctx.unpickleViaTempReadWrite("", elemTpe,
              (name: String) => s"element of type $elemTpe of collection",
              elemPickle)
          builder += elem
        }

        data.asWritable.value = fromSeq(builder.result())

      case _ => ()
    }
  }
}

object CollectionPickler {
  def registerCollectionPicklers(registry: PicklingRegistry): Unit = {
    registry.registerSubTypeReadWrite(InspectedType.ListOfAny, ListPicklerFactory)
  }

  class ListPickler(val elemTpe: InspectedType) extends CollectionPickler[List[Any]] {
    def toSeq(coll: List[Any]) = coll
    def fromSeq(seq: Seq[Any]) = seq.toList
  }

  val ListPicklerFactory = { (ctx: Context, data: InspectedData) =>
    data.tpe match
      case InspectedType.ListOf(elemType) =>
        new ListPickler(elemType)
      case tpe =>
        throw UnsupportedOperationException(s"Cannot create a ListPickler for non-List type $tpe")
  }
}
