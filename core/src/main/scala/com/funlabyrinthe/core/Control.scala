package com.funlabyrinthe.core

import scala.collection.mutable

import input.KeyEvent

import cps.*

sealed abstract class Control[+A]:
  def map[B](f: A => B): Control[B]
  def flatMap[B](f: A => Control[B]): Control[B]
end Control

object Control:
  final case class Done[+A](result: A) extends Control[A]:
    def map[B](f: A => B): Control[B] =
      Done(f(result))

    def flatMap[B](f: A => Control[B]): Control[B] =
      f(result)
  end Done

  final case class Sleep[+A](ms: Int, cont: Unit => Control[A]) extends Control[A]:
    def map[B](f: A => B): Control[B] =
      Sleep(ms, unit => cont(unit).map(f))

    def flatMap[B](f: A => Control[B]): Control[B] =
      Sleep(ms, unit => cont(unit).flatMap(f))
  end Sleep

  final case class WaitForKeyEvent[+A](cont: KeyEvent => Control[A]) extends Control[A]:
    def map[B](f: A => B): Control[B] =
      WaitForKeyEvent(event => cont(event).map(f))

    def flatMap[B](f: A => Control[B]): Control[B] =
      WaitForKeyEvent(event => cont(event).flatMap(f))
  end WaitForKeyEvent

  given ControlCpsMonad: CpsMonad[Control] with CpsMonadInstanceContext[Control] with
    def pure[A](a: A): Control[A] = Control.Done(a)

    def map[A, B](fa: Control[A])(f: A => B): Control[B] =
      fa.map(f)

    def flatMap[A, B](fa: Control[A])(f: A => Control[B]): Control[B] =
      fa.flatMap(f)
  end ControlCpsMonad

  inline transparent given ControlUnitValueDiscard: ValueDiscard[Control[Unit]] = AwaitValueDiscard[Control, Unit]

  //given ControlMemoization: CpsMonadMemoization.Default[Control] with {}
end Control
