package com.funlabyrinthe.editor.inspector

import scala.collection.mutable

trait EditorWithLazyMembers extends Editor {
  override val hasChildren = true

  protected val childEditors = new mutable.ListBuffer[Editor]
  private var _completed = false
  protected def completed = _completed

  /** To be implemented in subclasses */
  protected def complete() {
  }

  override def children: List[Editor] = {
    if (!completed) {
      complete()
      _completed = true
    }
    childEditors.toList
  }
}
