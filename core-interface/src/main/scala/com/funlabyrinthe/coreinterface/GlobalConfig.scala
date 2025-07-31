package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait GlobalConfig extends js.Object:
  var isEditing: js.UndefOr[Boolean] = js.undefined
  var onResourceLoaded: js.UndefOr[js.Function0[Unit]] = js.undefined
end GlobalConfig
