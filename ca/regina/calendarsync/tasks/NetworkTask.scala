package ca.regina.calendarsync.tasks

import android.os.AsyncTask
import ca.regina.calendarsync.Utilities.AuthenticationUtility
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.Calendar

/*case class CalendarEvent(val startTime: java.util.Calendar, 
    val endTime: java.util.Calendar,
    val isAllDay: Boolean,
    val subject: String,
    val location: String
)

class NetworkTask(username: String, password: String) extends 
	FixedAsyncTask[Object, Unit, Option[String]] {
	protected def doInBackgroundHelper(params: Array[AnyRef]): Option[String] = {
	  try {
		  AuthenticationUtility.authenticate(username, password)
	  } catch {
	    case ex: Throwable => None
	  }
	}
	
	private def fromParsedDateTimeValidation(simpleDateFormat: SimpleDateFormat)
		(dateTimeString: String): Validation[ParseException, Calendar] = {
	  try {
	    val dateTimeLong = simpleDateFormat.parse(dateTimeString).getTime()
		val calendar = new java.util.GregorianCalendar
		calendar.setTimeInMillis(dateTimeLong)
		Success(calendar)
	  } catch {
	    case ex: ParseException => Failure(ex)
	  }
		  
	}
	  
	private def parseJSONObjectIntoCalendarEvent(jsonObject: JSONObject): 
		Validation[ParseException, CalendarEvent] = {
	  val startTimeString = jsonObject.get("Start").toString()
	  val endTimeString = jsonObject.get("End").toString()
	  val location = jsonObject.getJSONObject("Location")
	  val displayName = location.get("DisplayName").toString()
	  val subject = jsonObject.get("Subject").toString()
	  val isAllDay = jsonObject.getBoolean("IsAllDay")
	  
	  val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
	  
	  def parser = fromParsedDateTimeValidation(simpleDateFormat) _
	  
	  for (startCalendar <- parser(startTimeString);
		endCalendar <- parser(endTimeString)
	  ) yield CalendarEvent(startCalendar, endCalendar, isAllDay, subject, displayName) 
	  
	}
	
	private implicit def fromJSONArrayToList(jsonArray: JSONArray): List[JSONObject] =
	  (0 until jsonArray.length()).map(index => jsonArray.getJSONObject(index)).toList
	
	override protected def onPostExecute(messageOption: Option[String]): Unit = {
	  val calendarEvents = messageOption.map(new JSONObject(_)).map(_.getJSONArray("value").
	      map(parseJSONObjectIntoCalendarEvent(_)))
	      
	  var q = 0;
	}
	override protected def onCancelled(): Unit = {
	  var q = 0;
	}
}*/