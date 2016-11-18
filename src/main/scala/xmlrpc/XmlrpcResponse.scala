package xmlrpc

import xmlrpc.protocol.Datatype
import xmlrpc.protocol.Deserializer.{AnyErrors, Deserialized}
import xmlrpc.protocol.XmlrpcProtocol.readXmlResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.xml.NodeSeq
import scalaz.Scalaz._

case class XmlrpcResponse[R](underlying: Future[Deserialized[R]])(implicit ec: ExecutionContext) {
  import XmlrpcResponse.ToFutureDeserialized

  def map[S](f: R => S): XmlrpcResponse[S] = flatMap(r => XmlrpcResponse.apply(f(r))) 
  
  def flatMap[S](f: R => XmlrpcResponse[S]): XmlrpcResponse[S] = XmlrpcResponse[S] {
    handleErrors flatMap (_ fold (e => e.asFutureFailure, f(_).handleErrors))
  }
  
  def fold[S](failure: AnyErrors => XmlrpcResponse[S], success: R => S): XmlrpcResponse[S] =
    XmlrpcResponse[S] {
      handleErrors flatMap (_ fold (failure(_).handleErrors, r => success(r).asFutureSuccess))
    }

  private lazy val handleErrors: Future[Deserialized[R]] = underlying recover {
    case error: Throwable => ConnectionError("Error when processing the future response", Some(error)).failures
  }
}

object XmlrpcResponse {
  def apply[R](value: R)(implicit ec: ExecutionContext): XmlrpcResponse[R] = XmlrpcResponse(value.asFutureSuccess)

  def apply[R](value: Deserialized[R])(implicit ec: ExecutionContext): XmlrpcResponse[R] = XmlrpcResponse[R] {
    Future.successful(value)
  }

  implicit class AkkaHttpToXmlrpcResponse(underlying: Future[NodeSeq])(implicit ec: ExecutionContext) {
    def asXmlrpcResponse[R: Datatype]: XmlrpcResponse[R] = XmlrpcResponse[R](underlying map readXmlResponse[R])
  }

  implicit class WithRetry[R](f: () => XmlrpcResponse[R])(implicit ec: ExecutionContext) {
    /**
     * These methods runs the function until a success has been detected for `n` times
     * 
     * @param runSuccess executed once at least, to return the successful `XmlrpcResponse`
     * @param runFailure executed if the response has failed, at most `n` times
     * @param times maximum number of times to run the function
     * @tparam S type of output
     * @return successful `XmlrpcResponse[S]`
     */
    def retry[S](runFailure: AnyErrors => S, runSuccess: R => S, times: Int): XmlrpcResponse[S] = {
      def failureLogic(errors: AnyErrors, remaining: Int): XmlrpcResponse[S] =
        if(remaining == 0) XmlrpcResponse(runFailure(errors))
        else retry(runFailure, runSuccess, remaining - 1)

      def run(remaining: Int): XmlrpcResponse[S] = f() fold (failureLogic(_, remaining), runSuccess)

      if(times <= 0) throw new IllegalArgumentException("Retry must be executed at least one time.")
      else run(times)
    }

    def retry(times: Int): XmlrpcResponse[R] = {
      def failureLogic(errors: AnyErrors, remaining: Int): XmlrpcResponse[R] =
        if(remaining == 0) XmlrpcResponse(errors.asFutureFailure)
        else retry(remaining - 1)

      def run(remaining: Int): XmlrpcResponse[R] = f() fold (failureLogic(_, remaining), r => r)

      if(times <= 0) throw new IllegalArgumentException("Retry must be executed at least one time.")
      else run(times)
    }
  }

  implicit class ToFutureDeserialized[T](t: T) {
    def asFutureSuccess = Future.successful(t.success)
    def asFutureFailure = Future.successful(t.failure)
  }
}