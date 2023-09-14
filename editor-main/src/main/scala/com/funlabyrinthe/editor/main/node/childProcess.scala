package com.funlabyrinthe.editor.main.node

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object childProcess:
  @js.native
  @JSImport("child_process")
  def spawn(command: String, args: js.Array[String], options: SpawnOptions): ChildProcess = js.native

  trait SpawnOptions extends js.Object:
    var cwd: js.UndefOr[String] = js.undefined
    var stdio: js.UndefOr[js.Array[String]] = js.undefined
  end SpawnOptions

  @js.native
  @JSImport("child_process")
  class ChildProcess extends js.Object:
    def on(eventName: String)(callback: ChildProcess.EventType[eventName.type]): Unit = js.native

    def stdout: Readable = js.native
    def stderr: Readable = js.native
  end ChildProcess

  object ChildProcess:
    type EventType[N <: String] <: js.Function = N match
      case "close" => js.Function2[Int | Null, String | Null, Any]
      case "error" => js.Function1[js.Error, Any]
  end ChildProcess

  @js.native
  @JSImport("stream")
  class Readable extends js.Object:
    def setEncoding(encoding: String): Unit = js.native
    def on(eventName: String)(callback: Readable.EventType[eventName.type]): Unit = js.native
  end Readable

  object Readable:
    type EventType[N <: String] <: js.Function = N match
      case "close" => js.Function0[Any]
      case "data"  => js.Function1[String, Any]
  end Readable
end childProcess
