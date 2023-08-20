package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSGlobal
class WeakMap[K <: AnyRef, V]() extends js.Object:
  def has(key: K): Boolean = js.native

  def get(key: K): js.UndefOr[V] = js.native

  def set(key: K, value: V): Unit = js.native

  def delete(key: K): Boolean = js.native
end WeakMap
