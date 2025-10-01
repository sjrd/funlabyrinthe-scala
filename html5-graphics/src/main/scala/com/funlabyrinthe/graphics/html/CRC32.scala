package com.funlabyrinthe.graphics.html

import java.nio.ByteBuffer

/* Ported from
 * https://github.com/davidmz/apng-js/blob/52f6fab62ffabe2abac3467d6abf82fe98ba4018/src/library/crc32.js
 * Copyright (c) 2016 David Mzareulyan -- MIT License
 */

object CRC32:
  private val table = Array.tabulate(256) { i =>
    var c = i
    for k <- 0 until 8 do
      c = if (c & 1) != 0 then 0xEDB88320 ^ (c >>> 1) else c >>> 1
    c
  }

  def compute(buffer: ByteBuffer): Int =
    var crc = -1 // initialized to "all 1's"
    while buffer.hasRemaining() do
      crc = (crc >>> 8) ^ table((crc ^ (buffer.get() & 0xff)) & 0xff)
    ~crc
  end compute
end CRC32
