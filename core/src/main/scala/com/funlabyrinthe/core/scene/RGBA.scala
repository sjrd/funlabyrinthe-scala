package com.funlabyrinthe.core.scene

final case class RGBA(red: Double, green: Double, blue: Double, alpha: Double) {
  import RGBA.*

  requireValidComponent(red)
  requireValidComponent(green)
  requireValidComponent(blue)
  requireValidComponent(alpha)

  /** Pack the color into an RGBA word-order 32-bit integer (R is in the most-significant bits). */
  def packToInt: Int =
    def toInt(v: Double): Int = (v * 255).toInt

    (toInt(red) << 24) | (toInt(green) << 16) | (toInt(blue) << 8) | toInt(alpha)
  end packToInt

  def toHexString: String =
    def toHex(v: Double): String =
      val s = (v * 255).toInt.toHexString
      if s.length() < 2 then "0" + s
      else s

    toHex(red) + toHex(green) + toHex(blue) + toHex(alpha)
  end toHexString

  def withAlpha(alpha: Double): RGBA = copy(alpha = alpha)
}

object RGBA {
  def apply(red: Double, green: Double, blue: Double): RGBA =
    RGBA(red, green, blue, alpha = 1.0)

  @inline private final def requireValidComponent(component: Double): Unit = {
    require(component >= 0 && component <= 1,
        s"$component is not a valid color component")
  }

  /** Unpack a color from an RGBA word-order 32-bit integer (R is in the most-significant bits). */
  def unpackFromInt(value: Int): RGBA =
    def toDouble(v: Int): Double = (v & 0xff).toDouble / 255

    RGBA(toDouble(value >> 24), toDouble(value >> 16), toDouble(value >> 8), toDouble(value))
  end unpackFromInt

  /** A fully transparent color with an ARGB value of #00000000. */
  val Transparent = RGBA(0f, 0f, 0f, 0f)

  /** The color alice blue with an RGB value of #F0F8FF. */
  val AliceBlue = RGBA(0.9411765f, 0.972549f, 1.0f)

  /** The color antique white with an RGB value of #FAEBD7. */
  val AntiqueWhite = RGBA(0.98039216f, 0.92156863f, 0.84313726f)

  /** The color aqua with an RGB value of #00FFFF. */
  val Aqua = RGBA(0.0f, 1.0f, 1.0f)

  /** The color aquamarine with an RGB value of #7FFFD4. */
  val Aquamarine = RGBA(0.49803922f, 1.0f, 0.83137256f)

  /** The color azure with an RGB value of #F0FFFF. */
  val Azure = RGBA(0.9411765f, 1.0f, 1.0f)

  /** The color beige with an RGB value of #F5F5DC. */
  val Beige = RGBA(0.9607843f, 0.9607843f, 0.8627451f)

  /** The color bisque with an RGB value of #FFE4C4. */
  val Bisque = RGBA(1.0f, 0.89411765f, 0.76862746f)

  /** The color black with an RGB value of #000000. */
  val Black = RGBA(0.0f, 0.0f, 0.0f)

  /** The color blanched almond with an RGB value of #FFEBCD. */
  val BlanchedAlmond = RGBA(1.0f, 0.92156863f, 0.8039216f)

  /** The color blue with an RGB value of #0000FF. */
  val Blue = RGBA(0.0f, 0.0f, 1.0f)

  /** The color blue violet with an RGB value of #8A2BE2. */
  val BlueViolet = RGBA(0.5411765f, 0.16862746f, 0.8862745f)

  /** The color brown with an RGB value of #A52A2A. */
  val Brown = RGBA(0.64705884f, 0.16470589f, 0.16470589f)

  /** The color burly wood with an RGB value of #DEB887. */
  val BurlyWood = RGBA(0.87058824f, 0.72156864f, 0.5294118f)

  /** The color cadet blue with an RGB value of #5F9EA0. */
  val CadetBlue = RGBA(0.37254903f, 0.61960787f, 0.627451f)

  /** The color chartreuse with an RGB value of #7FFF00. */
  val Chartreuse = RGBA(0.49803922f, 1.0f, 0.0f)

  /** The color chocolate with an RGB value of #D2691E. */
  val Chocolate = RGBA(0.8235294f, 0.4117647f, 0.11764706f)

  /** The color coral with an RGB value of #FF7F50. */
  val Coral = RGBA(1.0f, 0.49803922f, 0.3137255f)

  /** The color cornflower blue with an RGB value of #6495ED. */
  val CornflowerBlue = RGBA(0.39215687f, 0.58431375f, 0.92941177f)

  /** The color cornsilk with an RGB value of #FFF8DC. */
  val Cornsilk = RGBA(1.0f, 0.972549f, 0.8627451f)

  /** The color crimson with an RGB value of #DC143C. */
  val Crimson = RGBA(0.8627451f, 0.078431375f, 0.23529412f)

  /** The color cyan with an RGB value of #00FFFF. */
  val Cyan = RGBA(0.0f, 1.0f, 1.0f)

  /** The color dark blue with an RGB value of #00008B. */
  val DarkBlue = RGBA(0.0f, 0.0f, 0.54509807f)

  /** The color dark cyan with an RGB value of #008B8B. */
  val DarkCyan = RGBA(0.0f, 0.54509807f, 0.54509807f)

  /** The color dark goldenrod with an RGB value of #B8860B. */
  val DarkGoldenrod = RGBA(0.72156864f, 0.5254902f, 0.043137256f)

  /** The color dark gray with an RGB value of #A9A9A9. */
  val DarkGray = RGBA(0.6627451f, 0.6627451f, 0.6627451f)

  /** The color dark green with an RGB value of #006400. */
  val DarkGreen = RGBA(0.0f, 0.39215687f, 0.0f)

  /** The color dark grey with an RGB value of #A9A9A9. */
  val DarkGrey = DarkGray

  /** The color dark khaki with an RGB value of #BDB76B. */
  val DarkKhaki = RGBA(0.7411765f, 0.7176471f, 0.41960785f)

  /** The color dark magenta with an RGB value of #8B008B. */
  val DarkMagenta = RGBA(0.54509807f, 0.0f, 0.54509807f)

  /** The color dark olive green with an RGB value of #556B2F. */
  val DarkOliveGreen = RGBA(0.33333334f, 0.41960785f, 0.18431373f)

  /** The color dark orange with an RGB value of #FF8C00. */
  val DarkOrange = RGBA(1.0f, 0.54901963f, 0.0f)

  /** The color dark orchid with an RGB value of #9932CC. */
  val DarkOrchid = RGBA(0.6f, 0.19607843f, 0.8f)

  /** The color dark red with an RGB value of #8B0000. */
  val DarkRed = RGBA(0.54509807f, 0.0f, 0.0f)

  /** The color dark salmon with an RGB value of #E9967A. */
  val DarkSalmon = RGBA(0.9137255f, 0.5882353f, 0.47843137f)

  /** The color dark sea green with an RGB value of #8FBC8F. */
  val DarkSeaGreen = RGBA(0.56078434f, 0.7372549f, 0.56078434f)

  /** The color dark slate blue with an RGB value of #483D8B. */
  val DarkSlateBlue = RGBA(0.28235295f, 0.23921569f, 0.54509807f)

  /** The color dark slate gray with an RGB value of #2F4F4F. */
  val DarkSlateGray = RGBA(0.18431373f, 0.30980393f, 0.30980393f)

  /** The color dark slate grey with an RGB value of #2F4F4F. */
  val DarkSlateGrey = DarkSlateGray

  /** The color dark turquoise with an RGB value of #00CED1. */
  val DarkTurquoise = RGBA(0.0f, 0.80784315f, 0.81960785f)

  /** The color dark violet with an RGB value of #9400D3. */
  val DarkViolet = RGBA(0.5803922f, 0.0f, 0.827451f)

  /** The color deep pink with an RGB value of #FF1493. */
  val DeepPink = RGBA(1.0f, 0.078431375f, 0.5764706f)

  /** The color deep sky blue with an RGB value of #00BFFF. */
  val DeepSkyBlue = RGBA(0.0f, 0.7490196f, 1.0f)

  /** The color dim gray with an RGB value of #696969. */
  val DimGray = RGBA(0.4117647f, 0.4117647f, 0.4117647f)

  /** The color dim grey with an RGB value of #696969. */
  val DimGrey = DimGray

  /** The color dodger blue with an RGB value of #1E90FF. */
  val DodgerBlue = RGBA(0.11764706f, 0.5647059f, 1.0f)

  /** The color firebrick with an RGB value of #B22222. */
  val Firebrick = RGBA(0.69803923f, 0.13333334f, 0.13333334f)

  /** The color floral white with an RGB value of #FFFAF0. */
  val FloralWhite = RGBA(1.0f, 0.98039216f, 0.9411765f)

  /** The color forest green with an RGB value of #228B22. */
  val ForestGreen = RGBA(0.13333334f, 0.54509807f, 0.13333334f)

  /** The color fuchsia with an RGB value of #FF00FF. */
  val Fuchsia = RGBA(1.0f, 0.0f, 1.0f)

  /** The color gainsboro with an RGB value of #DCDCDC. */
  val Gainsboro = RGBA(0.8627451f, 0.8627451f, 0.8627451f)

  /** The color ghost white with an RGB value of #F8F8FF. */
  val GhostWhite = RGBA(0.972549f, 0.972549f, 1.0f)

  /** The color gold with an RGB value of #FFD700. */
  val Gold = RGBA(1.0f, 0.84313726f, 0.0f)

  /** The color goldenrod with an RGB value of #DAA520. */
  val Goldenrod = RGBA(0.85490197f, 0.64705884f, 0.1254902f)

  /** The color gray with an RGB value of #808080. */
  val Gray = RGBA(0.5019608f, 0.5019608f, 0.5019608f)

  /** The color green with an RGB value of #008000. */
  val Green = RGBA(0.0f, 0.5019608f, 0.0f)

  /** The color green yellow with an RGB value of #ADFF2F. */
  val GreenYellow = RGBA(0.6784314f, 1.0f, 0.18431373f)

  /** The color grey with an RGB value of #808080. */
  val Grey = Gray

  /** The color honeydew with an RGB value of #F0FFF0. */
  val Honeydew = RGBA(0.9411765f, 1.0f, 0.9411765f)

  /** The color hot pink with an RGB value of #FF69B4. */
  val HotPink = RGBA(1.0f, 0.4117647f, 0.7058824f)

  /** The color indian red with an RGB value of #CD5C5C. */
  val IndianRed = RGBA(0.8039216f, 0.36078432f, 0.36078432f)

  /** The color indigo with an RGB value of #4B0082. */
  val Indigo = RGBA(0.29411766f, 0.0f, 0.50980395f)

  /** The color ivory with an RGB value of #FFFFF0. */
  val Ivory = RGBA(1.0f, 1.0f, 0.9411765f)

  /** The color khaki with an RGB value of #F0E68C. */
  val Khaki = RGBA(0.9411765f, 0.9019608f, 0.54901963f)

  /** The color lavender with an RGB value of #E6E6FA. */
  val Lavender = RGBA(0.9019608f, 0.9019608f, 0.98039216f)

  /** The color lavender blush with an RGB value of #FFF0F5. */
  val LavenderBlush = RGBA(1.0f, 0.9411765f, 0.9607843f)

  /** The color lawn green with an RGB value of #7CFC00. */
  val LawnGreen = RGBA(0.4862745f, 0.9882353f, 0.0f)

  /** The color lemon chiffon with an RGB value of #FFFACD. */
  val LemonChiffon = RGBA(1.0f, 0.98039216f, 0.8039216f)

  /** The color light blue with an RGB value of #ADD8E6. */
  val LightBlue = RGBA(0.6784314f, 0.84705883f, 0.9019608f)

  /** The color light coral with an RGB value of #F08080. */
  val LightCoral = RGBA(0.9411765f, 0.5019608f, 0.5019608f)

  /** The color light cyan with an RGB value of #E0FFFF. */
  val LightCyan = RGBA(0.8784314f, 1.0f, 1.0f)

  /** The color light goldenrod yellow with an RGB value of #FAFAD2. */
  val LightGoldenrodYellow = RGBA(0.98039216f, 0.98039216f, 0.8235294f)

  /** The color light gray with an RGB value of #D3D3D3. */
  val LightGray = RGBA(0.827451f, 0.827451f, 0.827451f)

  /** The color light green with an RGB value of #90EE90. */
  val LightGreen = RGBA(0.5647059f, 0.93333334f, 0.5647059f)

  /** The color light grey with an RGB value of #D3D3D3. */
  val LightGrey = LightGray

  /** The color light pink with an RGB value of #FFB6C1. */
  val LightPink = RGBA(1.0f, 0.7137255f, 0.75686276f)

  /** The color light salmon with an RGB value of #FFA07A. */
  val LightSalmon = RGBA(1.0f, 0.627451f, 0.47843137f)

  /** The color light sea green with an RGB value of #20B2AA. */
  val LightSeaGreen = RGBA(0.1254902f, 0.69803923f, 0.6666667f)

  /** The color light sky blue with an RGB value of #87CEFA. */
  val LightSkyBlue = RGBA(0.5294118f, 0.80784315f, 0.98039216f)

  /** The color light slate gray with an RGB value of #778899. */
  val LightSlateGray = RGBA(0.46666667f, 0.53333336f, 0.6f)

  /** The color light slate grey with an RGB value of #778899. */
  val LightSlateGrey = LightSlateGray

  /** The color light steel blue with an RGB value of #B0C4DE. */
  val LightSteelBlue = RGBA(0.6901961f, 0.76862746f, 0.87058824f)

  /** The color light yellow with an RGB value of #FFFFE0. */
  val LightYellow = RGBA(1.0f, 1.0f, 0.8784314f)

  /** The color lime with an RGB value of #00FF00. */
  val Lime = RGBA(0.0f, 1.0f, 0.0f)

  /** The color lime green with an RGB value of #32CD32. */
  val LimeGreen = RGBA(0.19607843f, 0.8039216f, 0.19607843f)

  /** The color linen with an RGB value of #FAF0E6. */
  val Linen = RGBA(0.98039216f, 0.9411765f, 0.9019608f)

  /** The color magenta with an RGB value of #FF00FF. */
  val Magenta = RGBA(1.0f, 0.0f, 1.0f)

  /** The color maroon with an RGB value of #800000. */
  val Maroon = RGBA(0.5019608f, 0.0f, 0.0f)

  /** The color medium aquamarine with an RGB value of #66CDAA. */
  val MediumAquamarine = RGBA(0.4f, 0.8039216f, 0.6666667f)

  /** The color medium blue with an RGB value of #0000CD. */
  val MediumBlue = RGBA(0.0f, 0.0f, 0.8039216f)

  /** The color medium orchid with an RGB value of #BA55D3. */
  val MediumOrchid = RGBA(0.7294118f, 0.33333334f, 0.827451f)

  /** The color medium purple with an RGB value of #9370DB. */
  val MediumPurple = RGBA(0.5764706f, 0.4392157f, 0.85882354f)

  /** The color medium sea green with an RGB value of #3CB371. */
  val MediumSeaGreen = RGBA(0.23529412f, 0.7019608f, 0.44313726f)

  /** The color medium slate blue with an RGB value of #7B68EE. */
  val MediumSlateBlue = RGBA(0.48235294f, 0.40784314f, 0.93333334f)

  /** The color medium spring green with an RGB value of #00FA9A. */
  val MediumSpringGreen = RGBA(0.0f, 0.98039216f, 0.6039216f)

  /** The color medium turquoise with an RGB value of #48D1CC. */
  val MediumTurquoise = RGBA(0.28235295f, 0.81960785f, 0.8f)

  /** The color medium violet red with an RGB value of #C71585. */
  val MediumVioletRed = RGBA(0.78039217f, 0.08235294f, 0.52156866f)

  /** The color midnight blue with an RGB value of #191970. */
  val MidnightBlue = RGBA(0.09803922f, 0.09803922f, 0.4392157f)

  /** The color mint cream with an RGB value of #F5FFFA. */
  val MintCream = RGBA(0.9607843f, 1.0f, 0.98039216f)

  /** The color misty rose with an RGB value of #FFE4E1. */
  val MistyRose = RGBA(1.0f, 0.89411765f, 0.88235295f)

  /** The color moccasin with an RGB value of #FFE4B5. */
  val Moccasin = RGBA(1.0f, 0.89411765f, 0.70980394f)

  /** The color navajo white with an RGB value of #FFDEAD. */
  val NavajoWhite = RGBA(1.0f, 0.87058824f, 0.6784314f)

  /** The color navy with an RGB value of #000080. */
  val Navy = RGBA(0.0f, 0.0f, 0.5019608f)

  /** The color old lace with an RGB value of #FDF5E6. */
  val OldLace = RGBA(0.99215686f, 0.9607843f, 0.9019608f)

  /** The color olive with an RGB value of #808000. */
  val Olive = RGBA(0.5019608f, 0.5019608f, 0.0f)

  /** The color olive drab with an RGB value of #6B8E23. */
  val OliveDrab = RGBA(0.41960785f, 0.5568628f, 0.13725491f)

  /** The color orange with an RGB value of #FFA500. */
  val Orange = RGBA(1.0f, 0.64705884f, 0.0f)

  /** The color orange red with an RGB value of #FF4500. */
  val OrangeRed = RGBA(1.0f, 0.27058825f, 0.0f)

  /** The color orchid with an RGB value of #DA70D6. */
  val Orchid = RGBA(0.85490197f, 0.4392157f, 0.8392157f)

  /** The color pale goldenrod with an RGB value of #EEE8AA. */
  val PaleGoldenrod = RGBA(0.93333334f, 0.9098039f, 0.6666667f)

  /** The color pale green with an RGB value of #98FB98. */
  val PaleGreen = RGBA(0.59607846f, 0.9843137f, 0.59607846f)

  /** The color pale turquoise with an RGB value of #AFEEEE. */
  val PaleTurquoise = RGBA(0.6862745f, 0.93333334f, 0.93333334f)

  /** The color pale violet red with an RGB value of #DB7093. */
  val PaleVioletRed = RGBA(0.85882354f, 0.4392157f, 0.5764706f)

  /** The color papaya whip with an RGB value of #FFEFD5. */
  val PapayaWhip = RGBA(1.0f, 0.9372549f, 0.8352941f)

  /** The color peach puff with an RGB value of #FFDAB9. */
  val PeachPuff = RGBA(1.0f, 0.85490197f, 0.7254902f)

  /** The color peru with an RGB value of #CD853F. */
  val Peru = RGBA(0.8039216f, 0.52156866f, 0.24705882f)

  /** The color pink with an RGB value of #FFC0CB. */
  val Pink = RGBA(1.0f, 0.7529412f, 0.79607844f)

  /** The color plum with an RGB value of #DDA0DD. */
  val Plum = RGBA(0.8666667f, 0.627451f, 0.8666667f)

  /** The color powder blue with an RGB value of #B0E0E6. */
  val PowderBlue = RGBA(0.6901961f, 0.8784314f, 0.9019608f)

  /** The color purple with an RGB value of #800080. */
  val Purple = RGBA(0.5019608f, 0.0f, 0.5019608f)

  /** The color red with an RGB value of #FF0000. */
  val Red = RGBA(1.0f, 0.0f, 0.0f)

  /** The color rosy brown with an RGB value of #BC8F8F. */
  val RosyBrown = RGBA(0.7372549f, 0.56078434f, 0.56078434f)

  /** The color royal blue with an RGB value of #4169E1. */
  val RoyalBlue = RGBA(0.25490198f, 0.4117647f, 0.88235295f)

  /** The color saddle brown with an RGB value of #8B4513. */
  val SaddleBrown = RGBA(0.54509807f, 0.27058825f, 0.07450981f)

  /** The color salmon with an RGB value of #FA8072. */
  val Salmon = RGBA(0.98039216f, 0.5019608f, 0.44705883f)

  /** The color sandy brown with an RGB value of #F4A460. */
  val SandyBrown = RGBA(0.95686275f, 0.6431373f, 0.3764706f)

  /** The color sea green with an RGB value of #2E8B57. */
  val SeaGreen = RGBA(0.18039216f, 0.54509807f, 0.34117648f)

  /** The color sea shell with an RGB value of #FFF5EE. */
  val SeaShell = RGBA(1.0f, 0.9607843f, 0.93333334f)

  /** The color sienna with an RGB value of #A0522D. */
  val Sienna = RGBA(0.627451f, 0.32156864f, 0.1764706f)

  /** The color silver with an RGB value of #C0C0C0. */
  val Silver = RGBA(0.7529412f, 0.7529412f, 0.7529412f)

  /** The color sky blue with an RGB value of #87CEEB. */
  val SkyBlue = RGBA(0.5294118f, 0.80784315f, 0.92156863f)

  /** The color slate blue with an RGB value of #6A5ACD. */
  val SlateBlue = RGBA(0.41568628f, 0.3529412f, 0.8039216f)

  /** The color slate gray with an RGB value of #708090. */
  val SlateGray = RGBA(0.4392157f, 0.5019608f, 0.5647059f)

  /** The color slate grey with an RGB value of #708090. */
  val SlateGrey = SlateGray

  /** The color snow with an RGB value of #FFFAFA. */
  val Snow = RGBA(1.0f, 0.98039216f, 0.98039216f)

  /** The color spring green with an RGB value of #00FF7F. */
  val SpringGreen = RGBA(0.0f, 1.0f, 0.49803922f)

  /** The color steel blue with an RGB value of #4682B4. */
  val SteelBlue = RGBA(0.27450982f, 0.50980395f, 0.7058824f)

  /** The color tan with an RGB value of #D2B48C. */
  val Tan = RGBA(0.8235294f, 0.7058824f, 0.54901963f)

  /** The color teal with an RGB value of #008080. */
  val Teal = RGBA(0.0f, 0.5019608f, 0.5019608f)

  /** The color thistle with an RGB value of #D8BFD8. */
  val Thistle = RGBA(0.84705883f, 0.7490196f, 0.84705883f)

  /** The color tomato with an RGB value of #FF6347. */
  val Tomato = RGBA(1.0f, 0.3882353f, 0.2784314f)

  /** The color turquoise with an RGB value of #40E0D0. */
  val Turquoise = RGBA(0.2509804f, 0.8784314f, 0.8156863f)

  /** The color violet with an RGB value of #EE82EE. */
  val Violet = RGBA(0.93333334f, 0.50980395f, 0.93333334f)

  /** The color wheat with an RGB value of #F5DEB3. */
  val Wheat = RGBA(0.9607843f, 0.87058824f, 0.7019608f)

  /** The color white with an RGB value of #FFFFFF. */
  val White = RGBA(1.0f, 1.0f, 1.0f)

  /** The color white smoke with an RGB value of #F5F5F5. */
  val WhiteSmoke = RGBA(0.9607843f, 0.9607843f, 0.9607843f)

  /** The color yellow with an RGB value of #FFFF00. */
  val Yellow = RGBA(1.0f, 1.0f, 0.0f)

  /** The color yellow green with an RGB value of #9ACD32. */
  val YellowGreen = RGBA(0.6039216f, 0.8039216f, 0.19607843f)
}
