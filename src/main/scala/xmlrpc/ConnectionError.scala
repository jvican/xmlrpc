package xmlrpc

import xmlrpc.protocol.Deserializer
import Deserializer.XmlrpcError

/**
 * Wrapper to allow easier handling with XML-RPC responses.
 *
 * @param code Error code. If this code is positive it is from spray.http.StatusCodes.
 *             If it is negative, it represents a hand-rolled code error defined in the
 *             companion object.
 * @param reason The reason of the connection error
 * @param context Any exception occurred
 */
case class ConnectionError(code: Int, reason: String, context: Option[Throwable] = None) extends XmlrpcError {
  override val friendlyMessage: String = s"Connection to the server has failed.\n" +
    s"More info:\n\tCode: $code\n\tReason: $reason\n\t" +
    s"Throwable: ${if(context.isEmpty) "None" else "\n\t\t" + context.get.getMessage}"
}

object ConnectionError {
  val UnexpectedError = -1
  def from(reason: String) = ConnectionError(UnexpectedError, reason, None)
  def from(reason: String, context: Throwable) = ConnectionError(UnexpectedError, reason, Some(context))
}
