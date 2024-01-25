package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait EditUserActionResult extends js.Object:
  val kind: EditUserActionResult.Kind
end EditUserActionResult

object EditUserActionResult:
  type Kind = "done" | "unchanged" | "error" | "askConfirmation" | "sequence"

  trait Done extends EditUserActionResult:
    val kind: "done"
  end Done

  trait Unchanged extends EditUserActionResult:
    val kind: "unchanged"
  end Unchanged

  trait Error extends EditUserActionResult:
    val kind: "error"
    val message: String
  end Error

  trait AskConfirmation extends EditUserActionResult:
    val kind: "askConfirmation"
    val message: String
    val onConfirm: js.Function0[EditUserActionResult]
  end AskConfirmation

  trait Sequence extends EditUserActionResult:
    val kind: "sequence"
    val first: EditUserActionResult
    val second: js.Function0[EditUserActionResult]
  end Sequence
end EditUserActionResult
