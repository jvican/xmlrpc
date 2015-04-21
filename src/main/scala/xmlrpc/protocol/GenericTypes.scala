package xmlrpc.protocol

import shapeless._
import xmlrpc.protocol.Deserializer.Deserialized

import scala.xml.NodeSeq

trait GenericTypes {
  /**
   * This gives support for both case case classes and tuples. Since they can be
   * extracted with a Generic.Aux and converted to HLists, we make this conversion
   * and serialize/deserialize HLists.
   */
  implicit def genericXmlrpc[T, H](implicit gen: Generic.Aux[T, H], rd: Lazy[Datatype[H]]): Datatype[T] =
    new Datatype[T] {
      override def serialize(value: T): NodeSeq =
        rd.value.serialize(gen.to(value))

      override def deserialize(from: NodeSeq): Deserialized[T] =
        rd.value.deserialize(from).map(gen.from)
    }
}
