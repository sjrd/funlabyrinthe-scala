package com.funlabyrinthe.core

import scala.reflect.{ClassTag, classTag}

final class ModuleDesc private (val runtimeClass: Class[? <: Module]):
end ModuleDesc

object ModuleDesc:
  def apply[M <: Module](using ClassTag[M]): ModuleDesc =
    new ModuleDesc(classTag[M].runtimeClass.asInstanceOf[Class[? <: Module]])
end ModuleDesc
