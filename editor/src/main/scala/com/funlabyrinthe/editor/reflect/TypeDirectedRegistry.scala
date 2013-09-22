package com.funlabyrinthe.editor.reflect

import scala.collection.mutable

import scala.reflect.runtime.universe._

abstract class TypeDirectedRegistry {
  import TypeDirectedRegistry._
  import Entry._

  type Entry <: TypeDirectedRegistry.Entry

  private var entries: List[Entry] = Nil

  def register(entry: Entry) {
    entries = entry :: entries
  }

  def findEntry(data: InspectedData): Option[Entry] = {
    val allMatches = entries.filter(_.matches(data))
    if (allMatches.isEmpty) None
    else Some(allMatches.max(makeOrdering(data)))
  }
}

object TypeDirectedRegistry {
  abstract class Entry {
    def matches(data: InspectedData): Boolean
    def matchPercent(data: InspectedData): Int

    def compareTo(that: Entry, data: InspectedData): Int = {
      assert(this.matches(data) && that.matches(data))
      this.matchPercent(data) - that.matchPercent(data)
    }
  }

  object Entry {
    def makeOrdering(data: InspectedData): Ordering[Entry] = {
      new EntryOrdering(data)
    }

    private class EntryOrdering(val data: InspectedData) extends Ordering[Entry] {
      def compare(left: Entry, right: Entry): Int =
        left.compareTo(right, data)
    }

    trait ReadWriteOnly extends Entry {
      abstract override def matches(data: InspectedData) =
        !data.isReadOnly && super.matches(data)
    }

    trait ReadOnlyOnly extends Entry {
      abstract override def matches(data: InspectedData) =
        data.isReadOnly && super.matches(data)
    }

    trait ExactType extends Entry {
      val tpe: Type
      protected val matchPercent0: Int = 90

      override def matches(data: InspectedData) = data.tpe =:= tpe
      override def matchPercent(data: InspectedData) = matchPercent0
    }

    trait SubType extends Entry {
      val tpe: Type
      protected val matchPercent0: Int = 50

      override def matches(data: InspectedData) = data.tpe <:< tpe
      override def matchPercent(data: InspectedData) = matchPercent0
    }
  }
}
