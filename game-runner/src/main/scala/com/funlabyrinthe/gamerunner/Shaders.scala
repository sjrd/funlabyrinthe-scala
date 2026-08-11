package com.funlabyrinthe.gamerunner

import indigo.*
import indigo.scenes.*
import ultraviolet.syntax.*

import scala.annotation.nowarn

final case class StoreAlphaMaskBlendMaterial() extends BlendMaterial.SrcAndDst:
  def toShaderData: ShaderData =
    ShaderData(StoreAlphaMaskBlendShader.shader.id)

final case class ApplyAlphaMaskBlendMaterial() extends BlendMaterial.SrcAndDst:
  def toShaderData: ShaderData =
    ShaderData(ApplyAlphaMaskBlendShader.shader.id)

object StoreAlphaMaskBlendShader {
  val shader: ShaderProgram =
    UltravioletShader.blendFragment(
      ShaderId("store-alpha-mask-blend-shader"),
      BlendShader.fragment[BlendFragmentEnv](fragment, BlendFragmentEnv.reference)
    )

  @nowarn("msg=unused")
  inline def fragment: Shader[BlendFragmentEnv, Unit] = {
    Shader[BlendFragmentEnv] { env =>
      def fragment(color: vec4): vec4 =
        vec4(env.DST.r, env.DST.g, env.DST.b, env.SRC.a)
    }
  }
}

object ApplyAlphaMaskBlendShader {
  val shader: ShaderProgram =
    UltravioletShader.blendFragment(
      ShaderId("apply-alpha-mask-blend-shader"),
      BlendShader.fragment[BlendFragmentEnv](fragment, BlendFragmentEnv.reference)
    )

  @nowarn("msg=unused")
  inline def fragment: Shader[BlendFragmentEnv, Unit] = {
    Shader[BlendFragmentEnv] { env =>
      def fragment(color: vec4): vec4 =
        val mixed = mix(env.DST, env.SRC, env.DST.a * env.SRC.a)
        //vec4(env.DST.r, env.DST.g, env.DST.b, 1.0)
        vec4(mixed.r, mixed.g, mixed.b, 1.0)
    }
  }
}
