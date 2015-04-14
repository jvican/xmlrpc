package xmlrpc

import java.text.SimpleDateFormat

trait ProtocolSpec {
  /**
   * This is a pseudo ISO8601 timestamp because it lacks milliseconds and time zone
   * information. Specification in [[http://en.wikipedia.org/wiki/XML-RPC]].
   */
  val ISO8601Format = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss")
  ISO8601Format.setLenient(true)
}
