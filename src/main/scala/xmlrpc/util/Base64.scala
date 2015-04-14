package api.uji.xmlrpc.util

object Base64Symbols {
  lazy val symbols: String =
    (('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') ++ Seq('+', '/')).mkString

  def encodeSymbol(i: Int): Char = symbols.charAt(i)
  def decodeSymbol(c: Char): Int = c match {
    case cc if 'A' to 'Z' contains cc => cc - 'A'
    case cc if 'a' to 'z' contains cc => cc - 'G'
    case cc if '0' to '9' contains cc => cc + 4
    case '+' => 63
    case '/' => 64
    case _ => -1
  }
}

/**
 * Base64 implementation as in RFC 4648 and RFC2045
 * @author jvican
 */
object Base64 {
  val pads = Stream.continually('=')
  val zeroes = Stream.continually(0.toByte)
  
  def encode(plain: String): String = encode(plain.getBytes)

  def encode(bytes: Array[Byte]): String = {
    val pad = (3 - (bytes.length % 3)) % 3

    def makeMultipleOf3(b: Array[Byte]): Array[Byte] =
      b ++ zeroes.take(pad)

    def threeToFourSymbols(b: Array[Byte]): Array[Int] =
      Array(
        b(0) >> 2,
        ((b(0) & 0x03) << 4) + (b(1) >> 4),
        ((b(1) & 0x0F) << 2) + (b(2) >> 6),
        b(2) & 0x3F
      )

    def applyPadding(s: Vector[Char]): Vector[Char] =
      s.dropRight(pad) ++ pads.take(pad)

    def toBase64Symbols(b: Array[Byte]): Vector[Char] =
      makeMultipleOf3(b)
        .grouped(3)
        .flatMap(threeToFourSymbols)
        .map(Base64Symbols.encodeSymbol)
        .toVector

    applyPadding(toBase64Symbols(bytes)).mkString
  }

  // If padding missing, we add it
  def decode(bytes: Array[Byte]): String = {
    val pad = (4 - (bytes.length % 4)) % 4

    def makeMultipleOf4(b: Array[Byte]): Array[Char] =
      b.map(_.toChar) ++ pads.take(pad)

    def fourToThreeSymbols(b: Array[Int]): Array[Char] =
      Array(
        (b(0) << 2) + (b(1) >> 4),
        (b(1) << 4) + (b(2) >> 2),
        (b(2) << 4) + b(3)
      ).map(_.toChar)

    def toRaw(b: Array[Byte]): Vector[Char] =
      makeMultipleOf4(b)
        .map(Base64Symbols.decodeSymbol)
        .grouped(4)
        .flatMap(fourToThreeSymbols)
        .toVector

    def removePadding(c: Vector[Char]): Vector[Char] =
      c.dropRight(pad)

    removePadding(toRaw(bytes)).mkString
  }

  def decode(encoded: String): String = decode(encoded.getBytes)
}
