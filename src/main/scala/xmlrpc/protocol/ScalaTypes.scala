package xmlrpc.protocol

import xmlrpc.protocol.Deserializer.Deserialized

import scala.language.{postfixOps, implicitConversions}
import scala.xml.NodeSeq

import scalaz.Scalaz._

trait ScalaTypes extends Protocol {
  implicit def optionXmlrpc[T: Datatype]: Datatype[Option[T]] = new Datatype[Option[T]] {
    override def serialize(value: Option[T]): NodeSeq = value match {
      case Some(a) => toXmlrpc[T](a)
      case None => NodeSeq.Empty
    }

    override def deserialize(from: NodeSeq): Deserialized[Option[T]] =
      from \\ "value" headOption match {
        case Some(a) => fromXmlrpc[T](a) map (Some(_))
        case None => None.success
      }
  }
}
