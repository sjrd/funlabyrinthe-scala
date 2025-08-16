package com.funlabyrinthe.corebridge

import com.funlabyrinthe.core
import com.funlabyrinthe.{coreinterface as intf}

private final class EditingServices(underlying: intf.EditingServices) extends core.EditingServices:
  def markModified(): Unit =
    underlying.markModified()

  def askConfirmation(message: String): Boolean =
    JSPI.await(underlying.askConfirmation(message))

  def cancel(): Nothing =
    underlying.cancel()

  def error(message: String): Nothing =
    underlying.error(message)
end EditingServices

object EditingServices:
  def withEditingServices[A](intfServices: intf.EditingServices)(
      op: core.EditingServices ?=> A): A =
    op(using new EditingServices(intfServices))
  end withEditingServices
end EditingServices
