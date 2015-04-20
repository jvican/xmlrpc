package xmlrpc.protocol

import xmlrpc.protocol.Deserializer._
import shapeless._

import scala.xml.NodeSeq
import scalaz.Scalaz._

trait ShapelessTypes extends Protocol {
  // Both serialize and deserialize methods are recursive
  implicit def hconsXmlrpc[T, H <: HList](implicit hd: Lazy[Datatype[H]], td: Lazy[Datatype[T]]): Datatype[T :: H] = new Datatype[T :: H] {
    override def serialize(value: T :: H): NodeSeq = value match {
      case t :: h => td.value.serialize(t) ++ hd.value.serialize(h)
    }

    override def deserialize(from: NodeSeq): Deserialized[T :: H] =
      (td.value.deserialize(from.head) |@| hd.value.deserialize(from.tail)) { (t, h) => t :: h }
  }

  implicit object hnilXmlrpc extends Datatype[HNil] {
    override def serialize(value: HNil): NodeSeq = NodeSeq.Empty
    override def deserialize(from: NodeSeq): Deserialized[HNil] = HNil.success
  }
}
