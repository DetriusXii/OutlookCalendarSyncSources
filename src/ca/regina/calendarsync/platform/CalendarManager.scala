package ca.regina.calendarsync.platform

import android.content._
import android.accounts.Account
import android.provider._
import android.database.Cursor
import android.util.Log
import ca.regina.calendarsync.monads.Validation
import android.os.Build
import android.net.Uri
import ca.regina.calendarsync.Utilities.OutlookAuthenticator
import scala.collection.JavaConversions._
import android.provider.CalendarContract.Events
import ca.regina.calendarsync.syncadapter.OutlookSyncAdapter
import java.util.TimeZone

object CalendarManager {
  val TAG = "ca.regina.calendarsync.platform.CalendarManager"
    
  val projection = Array(CalendarContract.CalendarColumns.CAL_ACCESS_OWNER.toString, 
      CalendarContract.CalendarColumns.OWNER_ACCOUNT)
  
  
  
  def readCalendars(account: Account, context: Context): Unit = {
    Log.v(TAG, "starting readCalendars")
    Log.v(TAG, "checking the full calendar URI %s" format CalendarContract.Calendars.CONTENT_URI)
    
    /*val contentValues = new ContentValues
    contentValues.put("_id", 1.asInstanceOf[Integer])
    contentValues.put("_sync_account", account.name)
    contentValues.put("_sync_account_type", account.`type`)
    contentValues.put("name", account.name)
    contentValues.put("displayName", account.name)
    contentValues.put("color", 14417920.asInstanceOf[Integer])
    contentValues.put("access_level", 700.asInstanceOf[Integer])
    contentValues.put("selected", 1.asInstanceOf[Integer])
    contentValues.put("ownerAccount", account.name)
    contentValues.put("sync_events", 1.asInstanceOf[Integer])
    contentValues.put("timezone", "GMT")
    contentValues.put("hidden", 0.asInstanceOf[Integer])
    val calURI = 
      new android.net.Uri.Builder().
      	encodedPath("content://com.android.calendar/calendars?caller_is_syncadapter=true&account_name=local&account_type=LOCAL").build()
    */
    
    
    //val cv = new ContentValues
    //cv.put(CalendarContract.SyncColumns.ACCOUNT_NAME, account.name)
    //cv.put(CalendarContract.SyncColumns.ACCOUNT_TYPE, account.`type`)
    //cv.put(CalendarContract.Calendars.NAME, account.name)
    //cv.put(CalendarContract.CalendarColumns.CALENDAR_DISPLAY_NAME, account.name)
    //cv.put(CalendarContract.CalendarColumns.CALENDAR_COLOR, 14417920.asInstanceOf[Integer])
    //cv.put(CalendarContract.CalendarColumns.CALENDAR_ACCESS_LEVEL, 700.asInstanceOf[Integer])
    //cv.put(CalendarContract.CalendarColumns.OWNER_ACCOUNT, account.name)
    
    
    //cv.valueSet.map(kv => Log.v(TAG, "key name: %s value: %s" format (kv.getKey(), kv.getValue().toString)))
    
        
    
    val contentResolver = context.getContentResolver()
   
    val uri = CalendarContract.Calendars.CONTENT_URI
    
    val calendarURI = uri.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true").
    	appendQueryParameter(CalendarContract.SyncColumns.ACCOUNT_NAME, "Outlook Calendar Sync").
    	appendQueryParameter(CalendarContract.SyncColumns.ACCOUNT_TYPE, OutlookAuthenticator.ACCOUNT_TYPE).
    	build()
    	
    val beginTime = java.util.Calendar.getInstance()
    beginTime.set(2015, 1, 4, 7, 30)
    val endTime = java.util.Calendar.getInstance
    endTime.set(2015, 1, 4, 11, 30)
    val contentValues = new ContentValues
    contentValues.put(CalendarContract.EventsColumns.DTSTART, beginTime.getTimeInMillis().asInstanceOf[java.lang.Long])
    contentValues.put(CalendarContract.EventsColumns.DTEND, endTime.getTimeInMillis.asInstanceOf[java.lang.Long])
    contentValues.put(CalendarContract.EventsColumns.TITLE, "Jazzercize")
    contentValues.put(CalendarContract.EventsColumns.CALENDAR_ID, 1.asInstanceOf[Integer])
    contentValues.put(CalendarContract.EventsColumns.EVENT_TIMEZONE, TimeZone.getDefault().getID())
    
    Validation.tryCatchExpression[Exception, Unit](
        () => contentResolver.insert(CalendarContract.Events.CONTENT_URI, contentValues)).getOrElse(e =>
        	Log.v(OutlookSyncAdapter.TAG, e.getMessage())
        )
    
    //
    //for ( _ <- Validation.tryCatchExpression(() =>
    //  		contentResolver.insert(CalendarContract.Events.CONTENT_URI, contentValues));
    //  	cursor <- Validation.tryCatchExpression(() => contentResolver.query(uri, null, null, null, null))) yield cursor
  }
  

}