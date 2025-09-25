package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.ResourceLoader
import com.funlabyrinthe.core.graphics.Image

class FakeResourceLoader extends ResourceLoader:
  import FakeResourceLoader.*

  override def loadImage(name: String): Option[Image] =
    Some(new FakeImage(30, 30))
end FakeResourceLoader

object FakeResourceLoader:
  class FakeImage(val width: Int, val height: Int) extends Image:
    def isComplete: Boolean = true

    def isAnimated: Boolean = false
    def time: Int = 0
    def frames: IArray[Image] = IArray()
  end FakeImage
end FakeResourceLoader
