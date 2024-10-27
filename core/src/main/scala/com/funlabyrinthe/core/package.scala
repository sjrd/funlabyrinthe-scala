package com.funlabyrinthe

import core.input._

import cps.customValueDiscard
import scala.annotation.implicitNotFound

package object core {
  type Reflector[T] = reflect.Reflector[T]
  val Reflector: reflect.Reflector.type = reflect.Reflector

  def control[R](value: R): Control[R] = Control.Done(value)

  def exec[R](c: Control[R]): R = c match
    case Control.Done(result) => result
    case _                    => throw NotImplementedError(c.toString())

  def doNothing(): Control[Unit] = Control.Done(())
}
