package xmlrpc

import xmlrpc.protocol.Deserializer
import Deserializer.XmlrpcError

/**
 *
 * Wrapper if any exception occur when connecting to the server.
 *
 * @param reason The reason of the connection error
 * @param context Any exception occurred
 */
case class ConnectionError(reason: String, context: Option[Throwable] = None) extends XmlrpcError {
  override val friendlyMessage: String = s"Connection to the server has failed.\n" +
    s"More info:\n\tReason: $reason\n\t" +
    s"Throwable: ${if(context.isEmpty) "None" else "\n\t\t" + context.get.getMessage}"
}
