package com.funlabyrinthe.core.pickling

import scala.collection.mutable

import com.funlabyrinthe.core.Universe
import com.funlabyrinthe.core.Component

final class PicklingContext private (val universe: Universe):
  private var currentComponent: Option[Component] = None
  private var currentReversedPath: List[String] = Nil

  private val _errors = mutable.ListBuffer.empty[PicklingError]

  def withComponent[A](component: Component)(body: => A): A =
    val savedComponent = currentComponent
    val savedPath = currentReversedPath
    currentComponent = Some(component)
    currentReversedPath = Nil
    try
      body
    finally
      currentComponent = savedComponent
      currentReversedPath = savedPath
  end withComponent

  def withSubPath[A](subPath: String)(body: => A): A =
    currentReversedPath ::= subPath
    try
      body
    finally
      currentReversedPath = currentReversedPath.tail
  end withSubPath

  def reportError(message: String): Unit =
    _errors += PicklingError(currentComponent, currentReversedPath.reverse, message)

  def errors: List[PicklingError] =
    _errors.toList
end PicklingContext

object PicklingContext:
  def make(universe: Universe): PicklingContext =
    new PicklingContext(universe)

  /** Reports an error in the current pickling context. */
  def reportError(message: String)(using PicklingContext): Unit =
    summon[PicklingContext].reportError(message)

  /** Reports an error in the current pickling context and returns `None`.
   *
   *  This is a convenience method for the common situation where we must
   *  report an error then return `None` to signal an unpickling failure.
   */
  def error(message: String)(using PicklingContext): None.type =
    reportError(message)
    None

  def typeError(expected: String, actual: String)(using PicklingContext): None.type =
    error(s"data type error: expected $expected but got $actual")

  def typeError(expected: String, actualPickle: Pickle)(using PicklingContext): None.type =
    typeError(expected, actualPickle.typeString)
end PicklingContext
