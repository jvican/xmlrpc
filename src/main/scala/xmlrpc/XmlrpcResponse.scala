package xmlrpc

import xmlrpc.protocol.Deserializer
import Deserializer.{AnyErrors, Deserialized}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._

case class XmlrpcResponse[R](underlying: Future[Deserialized[R]])(implicit ec: ExecutionContext) {
  def map[S](f: R => S): XmlrpcResponse[S] = flatMap(r => XmlrpcResponse.apply(f(r))) 
  
  def flatMap[S](f: R => XmlrpcResponse[S]): XmlrpcResponse[S] = XmlrpcResponse[S] {
    handleErrors flatMap (_ fold (e => Future.successful(e.failure), f(_).handleErrors))
  }
  
  def fold[S](failure: AnyErrors => XmlrpcResponse[S], success: R => S) =
    handleErrors map (_ fold (failure, success))

  private lazy val handleErrors: Future[Deserialized[R]] = underlying recover {
    case error: Throwable => ConnectionError.from("Error when processing the future response", error).failureNel
  }
}

object XmlrpcResponse {
  def apply[R](value: R)(implicit ec: ExecutionContext): XmlrpcResponse[R] = XmlrpcResponse[R] {
    Future.successful(value.success)
  }
}