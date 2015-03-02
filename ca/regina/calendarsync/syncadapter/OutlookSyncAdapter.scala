package ca.regina.calendarsync.syncadapter

import android.os.Bundle
import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content._
import android.accounts.AccountManager
import ca.regina.calendarsync.Utilities.AuthenticationUtility
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.Calendar
import ca.regina.calendarsync.platform.CalendarManager
import android.util.Log
import ca.regina.calendarsync.monads._
import android.provider._
import android.provider.CalendarContract.Calendars
import ca.regina.calendarsync.monads.ValidationT._

object OutlookSyncAdapter {
  val TAG = "ca.regina.calendarsync.syncadapter.OutlookSyncAdapter"
}

case class CalendarEvent(val startTime: java.util.Calendar, 
    val endTime: java.util.Calendar,
    val isAllDay: Boolean,
    val subject: String,
    val location: String
)

case class CalendarColumnPosition(_idPosition: Int, namePosition: Int, ownerAccountPosition: Int) 
case class Calendar(_id: String, name: String, ownerAccount: String)

class OutlookSyncAdapter(context: Context, autoInitialize: Boolean) extends AbstractThreadedSyncAdapter(context, autoInitialize) {
  private val accountManager = AccountManager.get(context)
  private val contentResolver = context.getContentResolver()
 
  
  private val calendarProjection: Array[String] = Array(BaseColumns._ID, 
      Calendars.NAME,
      CalendarContract.CalendarColumns.OWNER_ACCOUNT)
 
  
  
      
  /*private def calendarProjection(cursorIO: IO[android.database.Cursor]) = {
    for ( cursor <- cursorIO;
    	_ <- ;
    ) yield ()
  }*/
      
  private def getCursorProjection(cursor: android.database.Cursor) = {
    def getColumnIndexOrThrow(columnName: String) = 
      Validation.tryCatchExpression[IllegalArgumentException, Int](() => cursor.getColumnIndexOrThrow(columnName))
    
    val idPositionValidation = getColumnIndexOrThrow(BaseColumns._ID)
    val namePositionValidation = getColumnIndexOrThrow(Calendars.NAME)
    val ownerAccountPositionValidation = getColumnIndexOrThrow(CalendarContract.CalendarColumns.OWNER_ACCOUNT)
    
    val calendarColumnPositionValidation = for (idPosition <- idPositionValidation;
      namePosition <- namePositionValidation;
      ownerAccountPosition <- ownerAccountPositionValidation
    ) yield CalendarColumnPosition(idPosition, namePosition, ownerAccountPosition)
    
    IO(calendarColumnPositionValidation).toValidationT
  }
  
  /*private def queryCursor[A](projectionClassMapping: Array[String] => A)
  	(selectionClause: String)
  	(selectionArguments: Array[String])
    IO.using(context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, x$2, x$3, x$4, x$5))(f)*/
  def grabCalendarRows(cursor: android.database.Cursor) = {
    val cursorProjectionValidationT = getCursorProjection(cursor)
    def getCalendar(cursorProjection: CalendarColumnPosition) = 
      Calendar(cursor.getString(cursorProjection._idPosition),
    		  cursor.getString(cursorProjection.namePosition),
    		  cursor.getString(cursorProjection.ownerAccountPosition))
    
    def mapCursorProjectionOnCursor(cursor: android.database.Cursor) = 
      for (cursorProjection <- cursorProjectionValidationT
      ) yield getCalendar(cursorProjection)
    
     
    
    def getCalendars(calendars: IO[Validation[Exception, List[Calendar]]], movedToNextSuccessful: Boolean) = movedToNextSuccessful match {
      case true => getCalendars()
      case false => calendars
    }
    
    cursor.moveToFirst() match {
      case true => 
      case false => IO[List[Calendar]](Nil)
    }
  }
  
  def prepareQuery(context: Context) = {
    val calendarURI = CalendarContract.Calendars.CONTENT_URI
    
    IO(context.getContentResolver().query(calendarURI, calendarProjection, null, null, null)).map(cursor => )
  }
  
  override def onPerformSync(account: Account, 
      extras: Bundle, 
      authority: String,
      provider: ContentProviderClient,
      syncResult: SyncResult): Unit = {
    try {
      Log.v(OutlookSyncAdapter.TAG, "Starting onPerformSync")
      Log.v(OutlookSyncAdapter.TAG, "CalendarContractAuthority: %s" format CalendarContract.AUTHORITY)
      Log.v(OutlookSyncAdapter.TAG, "CalendarContractURI: %s" format CalendarContract.CONTENT_URI)
      
      val password = accountManager.getPassword(account)
      val authenticationInformationOption = AuthenticationUtility.authenticate(account.name, password, false)
      
      
      IO.using(() => context.getContentResolver().query(x$1, x$2, x$3, x$4, x$5))
      
      CalendarManager.readCalendars(account, context)
      /*cursorValidation.map(cursor => {
        Log.v(OutlookSyncAdapter.TAG, "Cursor row count: %d" format cursor.getCount())
        Log.v(OutlookSyncAdapter.TAG, "Column count: %d" format cursor.getColumnCount())
        cursor.getColumnNames().map(columnName => Log.v(OutlookSyncAdapter.TAG, "column name: %s" format columnName))
        
        val columnNames = cursor.getColumnNames()
        val nameIndex = columnNames.indexOf("name")
        val calendarLocationIndex = columnNames.indexOf("calendar_location")
        val accountNameIndex = columnNames.indexOf("account_name")
        val calendarDisplayNameIndex = columnNames.indexOf("calendar_displayName")
        val idIndex = columnNames.indexOf("_id")
        
        cursor.moveToFirst()
        cursor.moveToPrevious()
        while (cursor.moveToNext()) {
          Log.v(OutlookSyncAdapter.TAG, 
              "Id: %s Name column: %s Account Name: %s Calendar Display Name: %s Calendar Location: %s" format 
              	(cursor.getString(idIndex),
              	    cursor.getString(nameIndex), 
              	    cursor.getString(accountNameIndex),
              	    cursor.getString(calendarDisplayNameIndex),
              	    cursor.getString(calendarLocationIndex)))
        }
        
        
        
        cursor.close()
        
      }).getOrElse(exception => {
        Log.v(OutlookSyncAdapter.TAG, "Exception message: %s" format exception.getMessage())
        exception.getStackTrace().map(element => Log.v(OutlookSyncAdapter.TAG, "Exception method name: %s" format element.getMethodName()))
        
      })*/
      
      
    } catch {
      case ex: Throwable => Log.v(OutlookSyncAdapter.TAG, ex.getMessage())
    }
  }
  
  	
  private def fromParsedDateTimeValidation(simpleDateFormat: SimpleDateFormat)
  	(dateTimeString: String): Validation[ParseException, Calendar] =
    try {
      val dateTimeLong = simpleDateFormat.parse(dateTimeString).getTime()
      val calendar = new java.util.GregorianCalendar
      calendar.setTimeInMillis(dateTimeLong)
      Success(calendar)
    } catch {
	  case ex: ParseException => Failure(ex)
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
}