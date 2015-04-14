package ca.regina.calendarsync.syncadapter

import ca.regina.calendarsync.monads._
import android.content.Context
import android.provider.CalendarContract._
import android.provider.BaseColumns

object EventsSync {
  val TAG: String = "ca.regina.calendarsync.syncadapter.EventsSync"
  val MICROSOFT_EVENTS_URL: String = "https://outlook.office365.com/ews/odata/Me/Events"
  val PROJECTION: Array[String] = Array(BaseColumns._ID, 
      EventsColumns.CALENDAR_ID,
      EventsColumns.DESCRIPTION,
      EventsColumns.TITLE,
      EventsColumns.ALL_DAY,
      EventsColumns.DTSTART,
      EventsColumns.DTEND,
      EventsColumns.HAS_EXTENDED_PROPERTIES,
      EventsColumns.HAS_ATTENDEE_DATA,
      EventsColumns.SYNC_DATA1)
      
  def log = IO.writeToLogVerbose(TAG) _
}

class EventsSync(context: Context) {
  def openCursor(uri: android.net.Uri, calendarProjection: Array[String], selection: String, 
		  selectionArgs: Array[String]): IO[Option[android.database.Cursor]] = {
    val cursor = context.getContentResolver().query(uri, calendarProjection, selection, selectionArgs, null)
    if (cursor != null) {
      IO(Some(cursor))
    } else {
      IO(None)
    }
  }
		    
  def iterateeCursor[A]: IterateeT[A, IO, List[A]] = {
    def step(listOfElements: List[A]): Input[A] => IterateeT[A, IO, List[A]] = {
      case Element(x) => IterateeT(IO(StepT.scont(step(x :: listOfElements))))
      case Empty() => IterateeT(IO(StepT.scont(step(listOfElements))))
      case Eof() => IterateeT(IO(StepT.sdone(listOfElements, Eof())))
    }
    
    IterateeT(IO(StepT.scont(step(Nil))))
  }
		    
  
  def abstractQuery[A](
      uri: android.net.Uri,
      projection: Array[String],
      sourceList: List[A],
      singleRowSelectionClause: A => String, 
      singleRowElementToValues: A => Array[String],
      singleRowValuesToElement: Array[String] => A): IO[Option[List[A]]] = {
    val singleRowSelectionClauses = sourceList map singleRowSelectionClause
    val multipleRowSelectionClause = singleRowSelectionClauses.foldLeft(("", true))((u, v) => 
      u._2 match {
        case true => (v, false)
        case false => ("%s and %s" format (u._1, v), false)
      })._1 
    
    val valuesList = sourceList.map(singleRowElementToValues(_)).flatten.toArray
    
    IO.using(openCursor(uri, projection, multipleRowSelectionClause, valuesList))(cursor =>
    	(iterateeCursor &= EnumeratorT.enumCursor(projection, singleRowValuesToElement)(cursor)).run
    )
  }
  
  def abstractQuery[A](uri: android.net.Uri, 
      projection: Array[String],
      sourceList: List[A], 
      singleRowSelectionClause: A => String,
      singleRowElementToValues: A => Array[String],
      singleRowValuesToElement: Array[String] => IO[Option[A]]): IO[Option[List[A]]] = {
    val singleRowSelectionClause = sourceList map singleRowSelectionClause
    val multipleRowSelectionClause = singleRowSelectionClauses.foldLeft(("", true))((u, v) =>
	  u._2 match {
	    case true => (v, false)
	    case false => ("%s and %s" format (u._1, v), false)
	  })._1
	 
    )
  }
  
  def singleRowValuesToElement(values: Array[String]): ValidationT[IO, NumberFormatException, MicrosoftEvent] = {
	  import ValidationT._
	  import LiftIO._
	  
	  val startTimeAsLong = IO(Validation.tryCatchExpression[NumberFormatException, Long](() => values(5).toLong)).
	  	toValidationT
	  val endTimeAsLong = IO(Validation.tryCatchExpression[NumberFormatException, Long](() => values(6).toLong)).
	  	toValidationT
	  val isAllDay = values(4).toBoolean
	  val subject = values(3)
	  val microsoftCalendarID = values(9)
	  val body = values(3)
	  	
	  def liftIO[A] = validationTLiftIO[IO, NumberFormatException].liftIO[A] _	
	  	
	  val microsoftEventValidationT = for (sLong <- startTimeAsLong;
		  eLong <- endTimeAsLong;
		  startTime <- liftIO(IO(new java.util.GregorianCalendar()));
		  endTime <- liftIO(IO(new java.util.GregorianCalendar()));
		  _ <- liftIO(IO(startTime.setTimeInMillis(sLong)));
		  _ <- liftIO(IO(endTime.setTimeInMillis(eLong)))
	  ) yield MicrosoftEvent(Nil, body, new java.util.GregorianCalendar,
	      new java.util.GregorianCalendar, endTime, false, microsoftCalendarID,
	      "low", isAllDay, false, startTime, subject, "")
	  
	  microsoftEventValidationT
  }
  
  def insertAndUpdate(microsoftEvents: ListT[IO, MicrosoftEvent]): IO[Unit] = {
    def singleRowSelectionClause(microsoftEvent: MicrosoftEvent): String = 
      "%s = ?" format EventsColumns.SYNC_DATA1
    def singleRowElementToValues(microsoftEvent: MicrosoftEvent): Array[String] =
      Array(microsoftEvent.id)
    
      
      
	microsoftEvents.run.map(microsoftEventsList => 
		abstractQuery(CONTENT_URI, EventsSync.PROJECTION, microsoftEventsList, microsoftEvent =>
			microsoftEvent.
		)
	) 
  }
}