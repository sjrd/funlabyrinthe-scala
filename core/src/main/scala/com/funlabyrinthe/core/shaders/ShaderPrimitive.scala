package com.funlabyrinthe.core.shaders

enum ShaderPrimitive {
  case float(value: Float)
  case vec2(x: Float, y: Float)
  case vec3(x: Float, y: Float, z: Float)
  case vec4(x: Float, y: Float, z: Float, w: Float)
}
