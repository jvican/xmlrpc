package xmlrpc.protocol

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

trait DatetimeSpec {
  /**
   * This is a pseudo ISO8601 timestamp because it lacks milliseconds and time zone
   * information. Specification in [[http://en.wikipedia.org/wiki/XML-RPC]].
   */
  val ISO8601Format = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss")

  // If you want to change the timezone of the dates, just override this
  val serverTimezone = TimeZone.getDefault

  ISO8601Format.setTimeZone(serverTimezone)
  ISO8601Format.setLenient(false)

  def withoutMillis(date: Date): Date = {
    val dateTime = date.getTime
    new Date(dateTime - (dateTime % 1000))
  }
}
