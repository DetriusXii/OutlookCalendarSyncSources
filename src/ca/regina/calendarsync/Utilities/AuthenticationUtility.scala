package ca.regina.calendarsync.Utilities

import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import scala.collection.JavaConversions._
import org.apache.http.client.methods.HttpPost
import org.apache.http.HttpStatus
import android.util.Log
import java.io.IOException
import android.util.Base64
import org.apache.http.client.methods.HttpGet
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import java.io.InputStream
import org.apache.http.HttpEntity
import java.io.ByteArrayOutputStream

class AuthenticationInformation(val username: String, 
    val password: String,
    val isNewAccountRequested: Boolean,
    val jsonString: String
)

object AuthenticationUtility {
  val USERNAME_KEY = "login"
  val PASSWORD_KEY = "passwd"
  val URL = "https://outlook.office365.com/ews/odata/Me/Events"
  
  def using[M <: java.io.Closeable, A](f: M => A, m: M): A = {
	try {
	  f(m)
	} finally {
	  if (m != null) {
	    m.close()
	  }
	}
  }
  
  def processInputStream(entity: HttpEntity): String = {
    val byteArrayOutputStream = new ByteArrayOutputStream()
    
    entity.writeTo(byteArrayOutputStream)
    val string = new String(byteArrayOutputStream.toByteArray())
    
    string
  }
	
  def authenticate(username: String, password: String, isNewAccountRequested: Boolean): Option[AuthenticationInformation] = {
    val get = new HttpGet(URL)
    
    try {
      val client = new Office365HTTPClient()
      client.getCredentialsProvider().setCredentials(
          new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), 
          new UsernamePasswordCredentials(username, password))
      val resp = client.execute(get)
      (resp.getStatusLine.getStatusCode, resp.getEntity()) match {
        case (HttpStatus.SC_OK, entity) =>  
          Some(new AuthenticationInformation(
        		  	username, 
        		  	password,
        		  	isNewAccountRequested,
        		  	processInputStream(entity)))
        case _ => None
      }
    } catch {
      case e: IOException => {
        Log.e("AuthenticationUtility", "Error authenticating " + e)
        None
      }
    }
  }
}