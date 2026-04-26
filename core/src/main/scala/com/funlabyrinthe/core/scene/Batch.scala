package com.funlabyrinthe.core.scene

import scala.reflect.ClassTag
import scala.collection.immutable.ArraySeq
import scala.collection.mutable.Growable
import scala.collection.mutable

sealed abstract class Batch[+A](val size: Int) {
  import Batch.*

  @transient
  lazy val toIndexedSeq: IndexedSeq[A] =
    val b = IndexedSeq.newBuilder[A]
    b.sizeHint(size)
    this.writeTo(b)
    b.result()

  protected def writeTo(b: Growable[A]): Unit

  def ++[B >: A](that: Batch[B]): Batch[B] =
    if this eq Empty then that
    else if that eq Empty then this
    else Combine(this, that)

  def :+[B >: A](that: B): Batch[B] =
    this ++ Batch(that)

  def +:[B >: A](that: B): Batch[B] =
    Batch(that) ++ this

  def foreach(f: A => Unit): Unit =
    toIndexedSeq.foreach(f)

  def map[B](f: A => B)(using ClassTag[B]): Batch[B] =
    val a = new Array[B](size)
    for (elem, i) <- toIndexedSeq.iterator.zipWithIndex do
      a(i) = f(elem)
    Leaf(IArray.unsafeFromArray(a))
}

object Batch {
  val empty: Batch[Nothing] = Empty

  def apply[A](item: A): Batch[A] =
    Single(item)

  def apply[A](items: A*)(using ClassTag[A]): Batch[A] =
    Leaf(IArray.from(items))

  def from[A](items: IArray[A]): Batch[A] =
    Leaf(items)

  def from[A](items: Seq[A])(using ClassTag[A]): Batch[A] =
    Leaf(IArray.from(items))

  private case object Empty extends Batch[Nothing](0) {
    protected def writeTo(b: Growable[Nothing]): Unit = ()
  }

  private final case class Single[+A](value: A) extends Batch[A](1) {
    protected def writeTo(b: Growable[A]): Unit =
      b += value
  }

  private final case class Leaf[+A](values: IArray[A]) extends Batch[A](values.length) {
    protected def writeTo(b: Growable[A]): Unit =
      b ++= values
  }

  private final case class Combine[+A](left: Batch[A], right: Batch[A])
      extends Batch[A](left.size + right.size) {

    protected def writeTo(b: mutable.Growable[A]): Unit =
      left.writeTo(b)
      right.writeTo(b)
  }
}
