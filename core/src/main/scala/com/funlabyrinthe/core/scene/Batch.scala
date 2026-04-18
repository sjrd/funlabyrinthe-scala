package com.funlabyrinthe.core.scene

/*import scala.reflect.ClassTag

sealed abstract class Batch[+A] /*{
  @transient
  lazy val toArray(using ClassTag[A]): IArray[A] =
    computeArray

  protected def computeArray(using ClassTag[A]): IArray[A]

  def ++[B >: A](that: Batch[B]): Batch[B] =
    Batch(this.inner ++ that.inner)
}*/

object Batch {
  final case class Leaf[+A](values: IArray[A]) extends Batch[A]
}
*/
type Batch[+A] = IArray[A]
