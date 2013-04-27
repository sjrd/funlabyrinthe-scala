package com.funlabyrinthe.editor.inspector

import scala.reflect.runtime.universe._

class ReadWriteReflectedData(val instanceTpe: Type, val getter: MethodMirror,
    val setter: MethodMirror) extends WritableReflectedData {

}
