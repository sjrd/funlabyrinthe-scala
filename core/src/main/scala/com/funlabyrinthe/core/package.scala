package com.funlabyrinthe

import scala.util.continuations._

import core.input._

package object core {
  type control = cps[ControlResult]

  def sleep(ms: Int): Unit @control = {
    if (ms <= 0) {
      ()
    } else {
      shift { (cont: Unit => ControlResult) =>
        ControlResult.Sleep(ms, cont)
      }
    }
  }

  def waitForKeyEvent(): KeyEvent @control = {
    scala.util.continuations.shift { (cont: KeyEvent => ControlResult) =>
      ControlResult.WaitForKeyEvent(cont)
    }
  }

  implicit class TraversableOnceControl[A](val self: Iterable[A]) extends AnyVal {
    def cforeach[U](f: A => U @control): Unit @control = {
      val iterator = self.iterator
      while (iterator.hasNext) {
        f(iterator.next())
      }
    }
  }
}
