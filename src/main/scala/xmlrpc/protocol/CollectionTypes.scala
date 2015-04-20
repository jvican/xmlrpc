package xmlrpc.protocol

import xmlrpc.protocol.Deserializer.Deserialized

import scala.xml.{NodeSeq, Node}
import scala.language.postfixOps
import scalaz.Scalaz._

trait CollectionTypes extends Protocol {
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
