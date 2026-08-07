package com.funlabyrinthe.core.messages

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

final case class Padding(top: Int, right: Int, bottom: Int, left: Int)
    derives Pickleable, Inspectable
