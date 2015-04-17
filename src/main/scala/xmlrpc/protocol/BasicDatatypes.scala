package xmlrpc.protocol

import java.util.Date

import xmlrpc.protocol.Deserializer.{DeserializationError, Deserialized}

import scala.language.postfixOps
import scala.xml.{Null, Node, NodeSeq}
import scalaz.Scalaz._

trait BasicDatatypes extends Protocol {
  import Deserializer.StringToError

  implicit object Base64Xmlrpc extends Datatype[Array[Byte]] {
    override def serialize(value: Array[Byte]): Node = <base64>{value.map(_.toChar).mkString}</base64>.inValue

    override def deserialize(from: NodeSeq): Deserialized[Array[Byte]] =
      from \\ "param" \ "value" headOption match {
        case Some(<value><base64>{content}</base64></value>) => content.text.getBytes.success
        case _ => s"Expected base64 structure in $from".toError.failureNel
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
            case e: Exception => DeserializationError(s"The date ${from.text} has not been parsed correctly", Some(e)).failureNel
          }
        case _ => s"Expected datetime structure in $from".toError.failureNel
      }
  }

  implicit object DoubleXmlrpc extends Datatype[Double] {
    override def serialize(value: Double): Node = <double>{value}</double>.inValue

    override def deserialize(from: NodeSeq): Deserialized[Double] =
      from \\ "value" headOption match {
        case Some(<value><double>{double}</double></value>) =>
          makeNumericConversion(_.toDouble, double.text)

        case _ => "Expected double structure in $from".toError.failureNel
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

        case _ => s"Expected int structure in $from".toError.failureNel
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
            case _ => "No logical value in boolean structure".toError.failureNel
          }
        case _ => s"Expected boolean structure in $from".toError.failureNel
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
        case _ => s"Expected string structure in $from".toError.failureNel
      }
    }
  }
  
  object Void
  
  implicit object VoidXmlrpc extends Datatype[Void.type] {
    override def serialize(value: Void.type): NodeSeq = NodeSeq.Empty

    // If there is no param tag, then it is a void
    override def deserialize(from: NodeSeq): Deserialized[Void.type] =
      from \\ "param" headOption match {
        case Some(_) => s"Expected void (without any param tag) in $from".toError.failureNel
        case _ => Void.success
      }
  }

  implicit object NilXmlrpc extends Datatype[Null.type] {
    override def serialize(value: Null.type): Node = <nil/>.inValue

    override def deserialize(from: NodeSeq): Deserialized[Null.type] =
      from \\ "value" headOption match {
        case Some(<value><nil/></value>) => Null.success
        case _ => s"Expected nil structure in $from".toError.failureNel
      }
  }
}

trait CollectionDatatypes extends Protocol {
  import Deserializer.StringToError

  // We only support array of the same type, if an array contains elements with different
  // types, we deserialize it with case classes
  implicit def ArrayXmlrpc[T: Datatype]: Datatype[Seq[T]] = new Datatype[Seq[T]] {
    override def serialize(value: Seq[T]): Node =
      <array><data>{for {elem <- value} yield toXmlrpc(elem)}</data></array>

    override def deserialize(from: NodeSeq): Deserialized[Seq[T]] =
      from \\ "array" headOption match {
        case Some(<array><data>{array @ _*}</data></array>) =>
          (for { value <- array}
            yield fromXmlrpc[T](value)).toList.sequence[Deserialized, T]

        case _ => "Expected array structure in $from".toError.failureNel
      }
  }

  implicit def StructXmlrpc[T: Datatype]: Datatype[Map[String, T]] = new Datatype[Map[String, T]] {
    override def serialize(map: Map[String, T]): Node = {
      def inName(name: String): Node = <name>{name}</name>
      def inMember(elems: NodeSeq): NodeSeq = <member>{elems}</member>

      lazy val struct: NodeSeq = (for {
        (key, value) <- map
      } yield inMember(inName(key) ++ toXmlrpc(value))).reduce(_ ++ _)

      <struct>{struct}</struct>
    }

    override def deserialize(from: NodeSeq): Deserialized[Map[String, T]] =
      from \\ "struct" headOption match {
        case Some(<struct>{members @ _*}</struct>) =>
          (for { member <- members }
            yield fromXmlrpc[T](member \ "value" head) map ((member \ "name" text) -> _))
            .toList
            .sequence[Deserialized, (String, T)]
            .map(_.toMap[String, T])

        case _ => s"Expected struct in:\n$from".toError.failureNel
      }
  }
}
