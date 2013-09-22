package com.funlabyrinthe.editor.reflect

import scala.reflect.runtime.universe._

class ReadWriteReflectedData(val instanceTpe: Type, val getter: MethodMirror,
    val setter: MethodMirror) extends WritableReflectedData {

}
