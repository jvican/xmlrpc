package xmlrpc.protocol

import xmlrpc.protocol.Deserializer.Deserialized

import scala.xml.NodeSeq
import scalaz.Scalaz._

trait ScalaTypes extends Protocol {
  def asProduct1[S, T1: Datatype](apply: (T1) => S)(unapply: S => T1) =
    new Datatype[S] {
      override def serialize(value: S): NodeSeq = {
        val params = unapply(value)
        toXmlrpc(params)
      }

      override def deserialize(from: NodeSeq): Deserialized[S] = fromXmlrpc[T1](from) map apply
    }
  // We use case classes to represent responses from the server
  def asProduct2[S, T1: Datatype, T2: Datatype](apply: (T1, T2) => S)(unapply: S => Product2[T1, T2]) =
    new Datatype[S] {
      override def serialize(value: S): NodeSeq = {
        val params = unapply(value)
        toXmlrpc(params._1) ++ toXmlrpc(params._2)
      }

      override def deserialize(from: NodeSeq): Deserialized[S] = (
          fromXmlrpc[T1](from(0)) |@| fromXmlrpc[T2](from(1))
        ) {apply}
    }
}
