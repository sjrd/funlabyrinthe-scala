package com.funlabyrinthe.editor.main.electron

import scala.scalajs.js
import scala.scalajs.js.annotation.*

trait CommandLine extends js.Object:
  def appendSwitch(switch: String): Unit
  def appendSwitch(switch: String, value: String): Unit
end CommandLine
