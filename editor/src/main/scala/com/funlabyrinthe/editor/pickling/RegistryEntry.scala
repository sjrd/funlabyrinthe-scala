package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.editor.reflect._

abstract class RegistryEntry extends TypeDirectedRegistry.Entry {
  def createPickler(data: InspectedData)(implicit ctx: Context): Pickler
}

trait RegistryEntryWithFactory extends RegistryEntry {
  protected val picklerFactory: RegistryEntry.PicklerFactory

  override def createPickler(data: InspectedData)(implicit ctx: Context) =
    picklerFactory(ctx, data)
}

object RegistryEntry {
  import TypeDirectedRegistry.{ Entry => BaseEntry }

  type PicklerFactory = (Context, InspectedData) => Pickler

  class ExactType(val tpe: InspectedType, protected val picklerFactory: PicklerFactory,
      override protected val matchPercent0: Int = 90)
  extends RegistryEntryWithFactory with BaseEntry.ExactType

  class SubType(val tpe: InspectedType, protected val picklerFactory: PicklerFactory,
      override protected val matchPercent0: Int = 50)
  extends RegistryEntryWithFactory with BaseEntry.SubType
}
