package xmlrpc

import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, NodeSeq}

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
}
