package ca.regina.calendarsync.Utilities

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpConnectionParams
import org.apache.http.conn.params.ConnManagerParams

object Office365HTTPClient {
  val HTTP_REQUEST_TIMEOUT =  30 * 1000
}

class Office365HTTPClient extends DefaultHttpClient {
  HttpConnectionParams.setConnectionTimeout(this.getParams, Office365HTTPClient.HTTP_REQUEST_TIMEOUT)
  HttpConnectionParams.setSoTimeout(this.getParams, Office365HTTPClient.HTTP_REQUEST_TIMEOUT)
  ConnManagerParams.setTimeout(this.getParams, Office365HTTPClient.HTTP_REQUEST_TIMEOUT)
}