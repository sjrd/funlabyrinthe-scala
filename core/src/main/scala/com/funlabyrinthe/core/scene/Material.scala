package com.funlabyrinthe.core.scene

final case class Material(
  asset: String,
  alpha: Double,
  tint: RGBA,
  //overlay: Fill,
  //fillType: FillType,
)

object Material {
  def apply(asset: String): Material =
    Material(asset, 1.0, RGBA.White /*, Fill.None, FillType.Tile*/)
}
