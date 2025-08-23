package com.funlabyrinthe.core

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.reflect.InspectedData

final class TimerQueue[M] private (universe: Universe, onMessage: M => Unit):
  import TimerQueue.*

  def schedule(delay: Long, message: M): Unit =
    scheduleAt(universe.tickCount + delay, message)

  def scheduleAt(deadline: Long, message: M): Unit =
    universe.scheduleTimerEntry(Entry(this, deadline, message))

  private[core] def dispatch(message: M): Unit =
    onMessage(message)
end TimerQueue

object TimerQueue:
  def apply[M](onMessage: M => Unit)(using universe: Universe): TimerQueue[M] =
    new TimerQueue(universe, onMessage)

  private[core] final case class Entry[M](queue: TimerQueue[M], deadline: Long, message: M)

  given TimerQueuePickleable[M](using Pickleable[M]): InPlacePickleable[TimerQueue[M]] with
    def storeDefaults(queue: TimerQueue[M]): Unit = ()

    def pickle(queue: TimerQueue[M])(using PicklingContext): Option[Pickle] =
      val entries = summon[PicklingContext].universe.getAllTimerEntriesOf(queue)
      val pickledEntries =
        for Entry(_, deadline, message) <- entries
        yield (deadline, message)
      Some(Pickleable.pickle(pickledEntries))

    def unpickle(queue: TimerQueue[M], pickle: Pickle)(using PicklingContext): Unit =
      for pickledEntries <- Pickleable.unpickle[List[(Long, M)]](pickle) do
        for (deadline, message) <- pickledEntries do
          queue.scheduleAt(deadline, message)
  end TimerQueuePickleable
end TimerQueue
