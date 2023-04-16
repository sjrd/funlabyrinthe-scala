package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.ResourceLoader
import com.funlabyrinthe.core.graphics.Image

class FakeResourceLoader extends ResourceLoader:
  import FakeResourceLoader.*

  override def loadImage(name: String): Option[Image] =
    Some(new FakeImage(30.0, 30.0))
end FakeResourceLoader

object FakeResourceLoader:
  class FakeImage(val width: Double, val height: Double) extends Image
end FakeResourceLoader
