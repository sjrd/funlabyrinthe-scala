package com.funlabyrinthe.core.messages

import com.funlabyrinthe.core.*

final case class ShowMessage(message: String) extends Message[Unit]
