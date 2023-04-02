package com.funlabyrinthe.core.reflect

final class ReflectorImpl[T](properties: List[ReflectableProp[T]]) extends Reflector[T]:
  def reflectProperties(instance: T): List[InspectedData] =
    properties.map(_.reflect(instance))
end ReflectorImpl
