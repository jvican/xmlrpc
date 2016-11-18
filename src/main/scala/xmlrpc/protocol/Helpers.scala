package xmlrpc.protocol

import xmlrpc.protocol.Deserializer.{DeserializationError, Deserialized}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, NodeSeq}
import scalaz.Scalaz._

trait Helpers {
  implicit class PimpedNode(node: NodeSeq) {
    def inValue = <value>{node}</value>
    def inParam = <param>{node}</param>
  }

  object FromRequestToResponse extends RewriteRule {
    override def transform(n: Node): Seq[Node] = n match {
      case e: Elem if e.label == "methodCall" =>
        e.copy(label="methodResponse",child=e.child.tail.tail)

      case _ => n
    }
  }

  object ToResponse extends RuleTransformer(FromRequestToResponse)

  implicit class RequestTransformer(request: Node) {
    val asResponse: Node = ToResponse(request)
  }

  def makeNumericConversion[T : Datatype : ClassTag](f: String => T, input: String): Deserialized[T] =
    Try(f(input)) match {
      case Success(convertedValue) => convertedValue.success
      case Failure(e) =>
        DeserializationError(s"The value $input couldn't be converted to a ${implicitly[ClassTag[T]].runtimeClass.getSimpleName}", Some(e)).failures
    }
}
