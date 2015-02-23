package ca.regina.calendarsync.services

import android.os.IBinder
import android.app.Service
import android.content.Intent
import ca.regina.calendarsync.Utilities.OutlookAuthenticator
import android.util.Log

object OutlookAuthenticationService {
  var TAG = "AuthenticationService"
}

class OutlookAuthenticationService extends Service {
	private var outlookAuthenticator: OutlookAuthenticator = null
	
	override def onCreate(): Unit = {
	  if (Log.isLoggable(OutlookAuthenticationService.TAG, Log.VERBOSE)) {
	    Log.v(OutlookAuthenticationService.TAG, "Office 365 authentication service has started.")
	  }
	  
	  outlookAuthenticator = new OutlookAuthenticator(this)
	}
	
	override def onDestroy(): Unit = {
		if (Log.isLoggable(OutlookAuthenticationService.TAG, Log.VERBOSE)) {
		  Log.v(OutlookAuthenticationService.TAG,
			  "OutlookAuthenticationService has stopped.")
		}
	}
	
	def onBind(intent: Intent): IBinder = {
	  if (Log.isLoggable(OutlookAuthenticationService.TAG, Log.VERBOSE)) {
	    Log.v(OutlookAuthenticationService.TAG,
		  "getBinder()... returning the OutlookAuthenticator binder for intent " + intent
	    )
	  }
	  
	  outlookAuthenticator.getIBinder()
	}
}