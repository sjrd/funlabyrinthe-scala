package com.funlabyrinthe.core.input

/** Physical key location as if on a QWERTY keyboard.
 *
 *  Even if the user has their keyboard layout configured differently, a
 *  physical key represents a physical location on the keyboard.
 *
 *  For example, if the user has an AZERTY keyboard, the key that is labeled
 *  `A` for them is encoded as `PhysicalKey.KeyQ`.
 */
enum PhysicalKey:
  case AltLeft
  case AltRight
  case ArrowDown
  case ArrowLeft
  case ArrowRight
  case ArrowUp
  case AudioVolumeDown // "VolumeDown" in Firefox
  case AudioVolumeMute // "VolumeMute" in Firefox
  case AudioVolumeUp // "VolumeUp" in Firefox
  case Backquote
  case Backslash
  case Backspace
  case BracketLeft
  case BracketRigth
  case BrowserHome
  case CapsLock
  case Comma
  case ContextMenu
  case ControlLeft
  case ControlRight
  case Convert
  case Delete
  case Digit0
  case Digit1
  case Digit2
  case Digit3
  case Digit4
  case Digit5
  case Digit6
  case Digit7
  case Digit8
  case Digit9
  case End
  case Enter
  case Equal
  case Escape
  case F1
  case F2
  case F3
  case F4
  case F5
  case F6
  case F7
  case F8
  case F9
  case F10
  case F11
  case F12
  case F13
  case F14
  case F15
  case F16
  case F17
  case F18
  case F19
  case F20
  case F21
  case F22
  case F23
  case F24
  case Home
  case Insert

  /** The key located between ShiftLeft and Y, which does not exist on a QWERTY keyboard. */
  case IntlBackslash

  case IntlRo
  case IntlYen
  case KanaMode
  case KeyA
  case KeyB
  case KeyC
  case KeyD
  case KeyE
  case KeyF
  case KeyG
  case KeyH
  case KeyI
  case KeyJ
  case KeyK
  case KeyL
  case KeyM
  case KeyN
  case KeyO
  case KeyP
  case KeyQ
  case KeyR
  case KeyS
  case KeyT
  case KeyU
  case KeyV
  case KeyW
  case KeyX
  case KeyY
  case KeyZ
  case Lang1
  case Lang2
  case LaunchApp2
  case MediaPlayPause
  case MediaSelect
  case MediaStop
  case MediaTrackNext
  case MediaTrackPrevious
  case MetaLeft
  case MetaRight
  case Minus
  case NonConvert
  case NumLock
  case Numpad0
  case Numpad1
  case Numpad2
  case Numpad3
  case Numpad4
  case Numpad5
  case Numpad6
  case Numpad7
  case Numpad8
  case Numpad9
  case NumpadAdd
  case NumpadComma
  case NumpadDecimal
  case NumpadDivide
  case NumpadEnter
  case NumpadEqual
  case NumpadMultiply
  case NumpadSubtract
  case PageDown
  case PageUp
  case Pause
  case Period
  case PrintScreen
  case Quote
  case ScrollLock
  case Semicolon
  case ShiftLeft
  case ShiftRight
  case Slash
  case Space
  case Tab
  case Unidentified
end PhysicalKey
