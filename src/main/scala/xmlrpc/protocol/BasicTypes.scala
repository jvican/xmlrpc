package xmlrpc.protocol

import java.util.Date

import xmlrpc.protocol.Deserializer.{DeserializationError, Deserialized}

import scala.language.postfixOps
import scala.xml.{Node, NodeSeq}
import scalaz.Scalaz._

trait BasicTypes extends Protocol {
  import Deserializer.StringToError

  /**
   * In all the deserialize methods, we query \\ "value" instead of \\ "param" \ "value"
   * because the array and struct contains elements only inside a value tag
   */

  implicit object Base64Xmlrpc extends Datatype[Array[Byte]] {
    override def serialize(value: Array[Byte]): Node = <base64>{value.map(_.toChar).mkString}</base64>.inValue

    override def deserialize(from: NodeSeq): Deserialized[Array[Byte]] =
      from \\ "value" headOption match {
        case Some(<value><base64>{content}</base64></value>) => content.text.getBytes.success
        case _ => s"Expected base64 structure in $from".toError.failures
      }
  }

  implicit object DatetimeXmlrpc extends Datatype[Date] {
    override def serialize(value: Date): Node = <dateTime.iso8601>{ISO8601Format.format(value)}</dateTime.iso8601>.inValue

    override def deserialize(from: NodeSeq): Deserialized[Date] =
      from \\ "value" headOption match {
        case Some(<value><dateTime.iso8601>{date}</dateTime.iso8601></value>) =>
          try {
            ISO8601Format.parse(date.text).success
          } catch {
            case e: Exception => DeserializationError(s"The date ${from.text} has not been parsed correctly", Some(e)).failures
          }
        case _ => s"Expected datetime structure in $from".toError.failures
      }
  }

  implicit object DoubleXmlrpc extends Datatype[Double] {
    override def serialize(value: Double): Node = <double>{value}</double>.inValue

    override def deserialize(from: NodeSeq): Deserialized[Double] =
      from \\ "value" headOption match {
        case Some(<value><double>{double}</double></value>) =>
          makeNumericConversion(_.toDouble, double.text)

        case _ => "Expected double structure in $from".toError.failures
      }
  }

  implicit object IntegerXmlrpc extends Datatype[Int] {
    override def serialize(value: Int): Node = <int>{value}</int>.inValue

    override def deserialize(from: NodeSeq): Deserialized[Int] =
      from \\ "value" headOption match {
        case Some(<value><int>{integer}</int></value>) =>
          makeNumericConversion(_.toInt, integer.text)

        case Some(<value><i4>{integer}</i4></value>) =>
          makeNumericConversion(_.toInt, integer.text)

        case _ => s"Expected int structure in $from".toError.failures
      }
  }

  implicit object LogicalValueXmlrpc extends Datatype[Boolean] {
    override def serialize(value: Boolean): Node = <boolean>{if(value) 1 else 0}</boolean>.inValue

    override def deserialize(from: NodeSeq): Deserialized[Boolean] =
      from \\ "value" headOption match {
        case Some(<value><boolean>{logicalValue}</boolean></value>) =>
          logicalValue.text match {
            case "1" => true.success
            case "0" => false.success
            case _ => "No logical value in boolean structure".toError.failures
          }
        case _ => s"Expected boolean structure in $from".toError.failures
      }
  }

  implicit object StringXmlrpc extends Datatype[String] {
    override def serialize(value: String): Node = {
      def encodeSpecialCharacters(content: String) =
        content.replace("&", "&amp").replace("<", "&lt")

      <string>{encodeSpecialCharacters(value)}</string>.inValue
    }

    override def deserialize(from: NodeSeq): Deserialized[String] = {
      def decodeSpecialCharacters(content: String) =
        content.replace("&amp", "&").replace("&lt", "<")

      from \\ "value" headOption match {
        case Some(<value><string>{content}</string></value>) => decodeSpecialCharacters(content.text).success
        case Some(<value>{content}</value>) => decodeSpecialCharacters(content.text).success
        case _ => s"Expected string structure in $from".toError.failures
      }
    }
  }

  object Void
  type Empty = Void.type

  implicit object VoidXmlrpc extends Datatype[Empty] {
    override def serialize(value: Empty): NodeSeq = NodeSeq.Empty

    // If there is no param tag, then it is a void
    override def deserialize(from: NodeSeq): Deserialized[Empty] =
      from \\ "param" headOption match {
        case Some(_) => s"Expected void (without any param tag) in $from".toError.failures
        case _ => Void.success
      }
  }

  type Null = scala.xml.Null.type

  implicit object NilXmlrpc extends Datatype[Null] {
    override def serialize(value: Null): Node = <nil/>.inValue

    override def deserialize(from: NodeSeq): Deserialized[Null] =
      from \\ "value" headOption match {
        case Some(<value><nil/></value>) => scala.xml.Null.success
        case _ => s"Expected nil structure in $from".toError.failures
      }
  }
}


