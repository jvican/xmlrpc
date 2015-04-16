package xmlrpc

import java.text.SimpleDateFormat
import java.util.Date

trait DatetimeSpec {
  /**
   * This is a pseudo ISO8601 timestamp because it lacks milliseconds and time zone
   * information. Specification in [[http://en.wikipedia.org/wiki/XML-RPC]].
   */
  val ISO8601Format = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss")
  ISO8601Format.setLenient(false)

  //def toServerTimezone(date: Date)(implicit timezone: TimeZone = TimeZone.getDefault) =
  def withoutMillis(date: Date): Date = {
    val dateTime = date.getTime
    new Date(dateTime - (dateTime % 1000))
  }

}
