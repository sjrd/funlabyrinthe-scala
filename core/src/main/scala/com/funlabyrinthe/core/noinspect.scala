package com.funlabyrinthe.core

import scala.annotation.meta.*

/** Properties annotated with `@noinspect` are not displayed in the object inspector of the editor. */
@field
final class noinspect extends scala.annotation.StaticAnnotation
