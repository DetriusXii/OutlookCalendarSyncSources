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
import org.apache.http.client.methods.HttpGet
import ca.regina.calendarsync.Utilities.Office365HTTPClient
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import java.io.IOException
import java.io.ByteArrayOutputStream
import org.apache.http.HttpEntity
import org.apache.http.HttpStatus

object OutlookSyncAdapter {
  val TAG = "ca.regina.calendarsync.syncadapter.OutlookSyncAdapter"
  val MICROSOFT_EVENTS_URL = "https://outlook.office365.com/ews/odata/Me/Events"
}

case class CalendarEvent(val startTime: java.util.Calendar, 
    val endTime: java.util.Calendar,
    val isAllDay: Boolean,
    val subject: String,
    val location: String
)

case class CalendarColumnPosition(_idPosition: Int, namePosition: Int, ownerAccountPosition: Int) 
case class Calendar(_id: String, name: String, ownerAccount: String)
case class MicrosoftEvent(attendees: List[String], 
    bodyPreview: String, 
    dateTimeCreated: java.util.Calendar,
    dateTimeLastModified: java.util.Calendar,
    end: java.util.Calendar,
    hasAttachments: Boolean,
    id: String,
    importance: String,
    isAllDay: Boolean,
    isCancelled: Boolean,
    start: java.util.Calendar,
    subject: String,
    `type`: String)

class OutlookSyncAdapter(context: Context, autoInitialize: Boolean) extends AbstractThreadedSyncAdapter(context, autoInitialize) {
  private val accountManager = AccountManager.get(context)
  private val contentResolver = context.getContentResolver()
 
  private val deletions = new Deletions(context)
  private val insertions = new Insertions(context)
  
  def log = IO.writeToLogVerbose(OutlookSyncAdapter.TAG) _
  
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
  def grabCalendarRows(cursor: android.database.Cursor): 
	  ListT[({type Q[A] = ValidationT[IO, IllegalArgumentException, A]})#Q, Calendar] = {
    import LiftIO._
    
    val cursorProjectionValidationT = getCursorProjection(cursor)
    def getCalendar(cursorProjection: CalendarColumnPosition) = 
      Calendar(cursor.getString(cursorProjection._idPosition),
    		  cursor.getString(cursorProjection.namePosition),
    		  cursor.getString(cursorProjection.ownerAccountPosition))
    
    def mapCursorProjectionOnCursor(cursor: android.database.Cursor) = 
      for (cursorProjection <- cursorProjectionValidationT
      ) yield getCalendar(cursorProjection) 
    
    def getCalendars(calendars: ListT[({type Q[A] = ValidationT[IO, IllegalArgumentException, A]})#Q, Calendar], 
        movedToNextSuccessful: Boolean):
    	ListT[({type Q[A] = ValidationT[IO, IllegalArgumentException, A]})#Q, Calendar] = movedToNextSuccessful match {
      case true => {
        val newList = mapCursorProjectionOnCursor(cursor) :: calendars
        val moveSuccessful = cursor.moveToNext()
        getCalendars(newList, moveSuccessful)
      }
      case false => calendars
    }
    
    val emptyListT = ListT.emptyListTMonad[({type Q[A] = ValidationT[IO, IllegalArgumentException, A]})#Q, Calendar]
    
    cursor.moveToFirst() match {
      case true => getCalendars(emptyListT, true)
      case false => emptyListT
    }
  }
  
  def prepareQuery(context: Context): ListT[({type Q[A] = ValidationT[IO, IllegalArgumentException, A]})#Q, Calendar]  = {
    import LiftIO._
    import ValidationT._
    import ListT._
    
    val calendarURI = CalendarContract.Calendars.CONTENT_URI
    
    def customLiftIO = LiftIO.listTLiftIO[({type Q[A] = ValidationT[IO, IllegalArgumentException, A]})#Q].liftIO[android.database.Cursor] _
    
    for (cursor <- customLiftIO(IO(context.
    		getContentResolver().
    		query(calendarURI, calendarProjection, null, null, null)));
    		calendarRow <- grabCalendarRows(cursor)	
    ) yield calendarRow
  }
  
  private def getContentValuesForCalendarInsert(account: Account): ContentValues = {
    val contentValues = new ContentValues
    contentValues.put(CalendarContract.SyncColumns.ACCOUNT_NAME, account.name)
    contentValues.put(CalendarContract.SyncColumns.ACCOUNT_TYPE, account.`type`)
    contentValues.put(CalendarContract.Calendars.NAME, account.name)
    contentValues.put(CalendarContract.CalendarColumns.CALENDAR_DISPLAY_NAME, account.name)
    contentValues.put(CalendarContract.CalendarColumns.CALENDAR_COLOR, 14417920.asInstanceOf[Integer])
    contentValues.put(CalendarContract.CalendarColumns.CALENDAR_ACCESS_LEVEL, 700.asInstanceOf[Integer])
    contentValues.put(CalendarContract.CalendarColumns.OWNER_ACCOUNT, account.name)
    
    contentValues
  }
  
  private def deleteExtraCalendars(calendars: List[Calendar]): IO[(Int, Option[Calendar])] = {
    import ListExtensions._
    
    val baseURL = Calendars.CONTENT_URI
    
    def deleteTailCalendars(calendars: List[Calendar]): IO[Int] =  
      calendars.map(calendar =>
    	baseURL.buildUpon().appendQueryParameter(android.provider.BaseColumns._ID, calendar._id).build()
    ).map(deletions.deleteBasedOnUrl(_)).sequence.map(_.foldLeft(0)((u, v) => u + v))
    
    calendars match {
      case Nil => IO((0, None))
      case x :: Nil => IO((0, Some(x)))
      case x :: t => deleteTailCalendars(t).map((_, Some(x)))
    }
  }
  
  private def handleCalendarDeletion(calendars: List[Calendar]): OptionT[IO, Calendar] = {
    import OptionT._
    
    def log(message: String) = IO.writeToLogVerbose(OutlookSyncAdapter.TAG)(message)
    
    OptionT(deleteExtraCalendars(calendars).flatMap(_ match {
      case (0, None) => log("No calendars present in list").map(_ => None)
      case (0, Some(x)) => IO(Some(x))
      case (n, Some(x)) if n == calendars.length - 1 => IO(Some(x))
      case _ => log("There are still too many calendars for the application").map(_ => None)
    }))
  }
  
  private def handleCalendarDeletion(
      calendars: ListT[({type Q[A] = ValidationT[IO, IllegalArgumentException, A]})#Q, Calendar]): 
      OptionT[IO, Calendar] = {
    OptionT(calendars.run.getOrElse(exception => 
      IO.writeToLogVerbose(OutlookSyncAdapter.TAG)(exception.getMessage()).map(_ => Nil)).flatMap(calendars =>
      	handleCalendarDeletion(calendars).run
      ))
  }
  
  private def handleCalendarInsertion(account: Account): OptionT[IO, Calendar] = {
    import OptionT._
    
    val uriIO = insertions.insert(CalendarContract.Calendars.CONTENT_URI, getContentValuesForCalendarInsert(account))
    
    prepareQuery(context).headOption.getOrElse(exception =>
    	IO.writeToLogVerbose(OutlookSyncAdapter.TAG)(exception.getMessage()).map(_ => None)
    ).toOptionT
  }
  
  private def handleRightAmountOfCalendars(
	  calendars: ListT[({type Q[A] = ValidationT[IO, IllegalArgumentException, A]})#Q, Calendar]) = {
    import OptionT._
    
    calendars.run.getOrElse(exception =>
    	IO.writeToLogVerbose(OutlookSyncAdapter.TAG)(exception.getMessage()).map(_ => Nil)).map(calendars =>
    		calendars.headOption
    	).toOptionT
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
      
      val calendarsListT = prepareQuery(context)
      val uri = CalendarContract.Calendars.CONTENT_URI
      
      val workingCalendarOptionT = calendarsListT.length.map(_ match {
        case 0 => handleCalendarInsertion(account)
        case 1 => handleRightAmountOfCalendars(calendarsListT)
        case _ => handleCalendarDeletion(calendarsListT)
      })
    
      val calendarURI = uri.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true").
    	appendQueryParameter(CalendarContract.SyncColumns.ACCOUNT_NAME, "Outlook Calendar Sync").
    	appendQueryParameter(CalendarContract.SyncColumns.ACCOUNT_TYPE, account.`type`).
    	build()
    	
      
      
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
  	(dateTimeString: String): Validation[ParseException, java.util.GregorianCalendar] =
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
	
	private def readEventsFromURL(account: Account): ListT[IO, MicrosoftEvent] = {
	  val get = new HttpGet(OutlookSyncAdapter.MICROSOFT_EVENTS_URL)
	  
	  val username = account.name
	  val password = accountManager.getPassword(account)
	  
	  def processEntity(entity: HttpEntity): IO[String] = {
	    val byteArrayOutputStream = new ByteArrayOutputStream
	    
	    entity.writeTo(byteArrayOutputStream)
	    val jsonString = new String(byteArrayOutputStream.toByteArray())
	    
	    IO(jsonString)
	  }
	  
	  def parseDateTimeIntoCalendar(dateTimeString: String) = 
	    Validation.tryCatchExpression[ParseException, java.util.Calendar](() => {
	      val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-'T'HH:mm:ss'Z'")
	      
	      val dateTimeLong = simpleDateFormat.parse(dateTimeString).getTime
	      val calendar = new java.util.GregorianCalendar
	      calendar.setTimeInMillis(dateTimeLong)
	      
	      calendar
	    })
	  
	  def convertJSONString(jsonString: String): IO[JSONObject] = IO(new JSONObject(jsonString))
	  
	  def convertJSONTOMicrosoftEvent(jsonObject: JSONObject): Validation[ParseException, List[MicrosoftEvent]] = {
	    import ListExtensions._
	    import Validation._
	    
	    val values = jsonObject.getJSONArray("value")
	    val microsoftEventList = ((0 until values.length()) map (index => {
	      val jsonEventObject = values.getJSONObject(index)
	      
	      val attendeesArray = jsonEventObject.getJSONArray("Attendees")
	      val bodyPreview = jsonEventObject.getString("BodyPreview")
	      val dateTimeCreated = jsonEventObject.getString("DateTimeCreated")
	      val dateTimeLastModified = jsonEventObject.getString("DateTimeLastModified")
	      val start = jsonEventObject.getString("Start")
	      val end = jsonEventObject.getString("End")
	      val hasAttachments = jsonEventObject.getBoolean("HasAttachments")
	      val id = jsonEventObject.getString("Id")
	      val isAllDay = jsonEventObject.getBoolean("IsAllDay")
	      val isCancelled = jsonEventObject.getBoolean("IsCancelled")
	      val subject = jsonEventObject.getString("Subject")
	      val `type` = jsonEventObject.getString("Type")
	      val importance = jsonEventObject.getString("Importance")
	      
	      val dateTimeCreatedValidation = parseDateTimeIntoCalendar(dateTimeCreated)
	      val dateTimeLastModifiedValidation = parseDateTimeIntoCalendar(dateTimeLastModified)
	      val startValidation = parseDateTimeIntoCalendar(start)
	      val endValidation = parseDateTimeIntoCalendar(end)
	      
	      val attendeesList =((0 until attendeesArray.length()) map (index => {
	        val attendeesObject = attendeesArray.getJSONObject(index)
	        val emailAddressObject = attendeesObject.getJSONObject("EmailAddressObject")
	        
	        emailAddressObject.getString("Name")
	      })).toList
	      
	      for (dateTimeCreated <- dateTimeCreatedValidation;
	          dateTimeLastModified <- dateTimeLastModifiedValidation;
	          start <- startValidation;
	          end <- endValidation
	      ) yield MicrosoftEvent(attendeesList, 
	          bodyPreview, 
	          dateTimeCreated, 
	          dateTimeLastModified, 
	          end,
	          hasAttachments,
	          id,
	          importance,
	          isAllDay, 
	          isCancelled,
	          start,
	          subject,
	          `type`)
	      
	    })).toList
	    
	    
	    richList[({type Q[A] = Validation[ParseException, A]})#Q, MicrosoftEvent](microsoftEventList)(Validation.validationMonad).
	    	sequence
	  }
	  
	  ListT(Validation.tryCatchExpression[IOException, IO[List[MicrosoftEvent]]](() => {
	    val client = new Office365HTTPClient()
	    client.getCredentialsProvider().setCredentials(
	        new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
	        new UsernamePasswordCredentials(username, password))
	    val resp = client.execute(get)
	    
	    (resp.getStatusLine.getStatusCode, resp.getEntity) match {
	      case (HttpStatus.SC_OK, entity) => 
	        for (jsonString <-  processEntity(entity);
        		jsonObject <- convertJSONString(jsonString);
        		microsoftEventListValidation <- IO(convertJSONTOMicrosoftEvent(jsonObject));
        		microsoftEventList <- microsoftEventListValidation.map(IO(_)).getOrElse(parseException => log(parseException.getMessage).map(_ => Nil))
	        ) yield (microsoftEventList)
	      case _ => IO(Nil)
	    }
	  }).getOrElse(ioException => log(ioException.getMessage()).map(Nil)))
	}
}