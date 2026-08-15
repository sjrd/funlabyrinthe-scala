package com.funlabyrinthe.core.sounds

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.pickling.Pickleable
import com.funlabyrinthe.core.inspecting.{Editor, Inspectable}

/** Volume for audio.
 *
 *  The value should be in the range 0.0 (minimum) to 1.0 (maximum).
 */
final case class Volume(value: Double) derives Pickleable

object Volume {
  val Min: Volume = Volume(0.0)
  val Max: Volume = Volume(1.0)

  given VolumeIsInspectable: Inspectable[Volume] {
    type EditorValueType = String

    override def editor(using Universe): Editor.Text.type = Editor.Text

    override def toEditorValue(value: Volume)(using Universe): EditorValueType =
      value.value.toString()

    override def fromEditorValue(editorValue: EditorValueType)(using Universe): Volume =
      if editorValue.isEmpty() then Max // default to maximum volume
      else Volume(editorValue.toDouble)
  }
}
