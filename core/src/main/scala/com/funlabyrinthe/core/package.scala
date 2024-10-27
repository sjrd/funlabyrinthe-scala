package com.funlabyrinthe

package object core {
  type Reflector[T] = reflect.Reflector[T]
  val Reflector: reflect.Reflector.type = reflect.Reflector
}
