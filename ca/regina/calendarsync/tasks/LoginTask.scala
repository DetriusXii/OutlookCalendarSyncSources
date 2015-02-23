package ca.regina.calendarsync.tasks

import ca.regina.calendarsync.Utilities.AuthenticationUtility
import android.accounts.AccountManager
import android.accounts.Account
import ca.regina.calendarsync.Utilities._
import android.accounts.AccountAuthenticatorActivity
import android.app.Activity
import android.content.Intent
import android.util.Log

object LoginTask {
  val TAG = "LoginTask"
}

class LoginTask(username: String, 
    password: String, 
    accountManager: AccountManager,
    isNewAccountRequested: Boolean,
    activity: AccountAuthenticatorActivity) extends ca.regina.calendarsync.tasks.FixedAsyncTask[Object, Unit, Option[AuthenticationInformation]] {
	protected def doInBackgroundHelper(params: Array[AnyRef]): Option[AuthenticationInformation] = {
	  Log.v(LoginTask.TAG, "Starting doInBackgroundHelper for LoginTask")
	  try {
		  AuthenticationUtility.authenticate(username, password, isNewAccountRequested)
	  } catch {
	    case ex: Throwable => None
	  }
	}
	
	private def generateFinishLoginIntent(authenticationInformation: AuthenticationInformation): Intent = {
	  val intent = new Intent;
	  
	  intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, authenticationInformation.username)
	  intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, OutlookAuthenticator.ACCOUNT_TYPE)
	  
	  intent
	}
	
	private def setActivityResult(authenticationInformation: AuthenticationInformation): Unit = {
	  
	  val intent = generateFinishLoginIntent(authenticationInformation)
	  activity.setAccountAuthenticatorResult(intent.getExtras())
	  activity.setResult(Activity.RESULT_OK, intent)
	  activity.finish
	}
	
	private def accountManagement(authenticationInformation: AuthenticationInformation): Unit = {
	  val account = new Account(authenticationInformation.username, OutlookAuthenticator.ACCOUNT_TYPE)
	  if (authenticationInformation.isNewAccountRequested) {
	 
	    accountManager.addAccountExplicitly(account, authenticationInformation.password, null)
	  } else {
	    accountManager.setPassword(account, authenticationInformation.password)
	  }
	}
	
	override protected def onPostExecute(authenticationOption: Option[AuthenticationInformation]): Unit =
	  authenticationOption.map(authenticationInformation => {
	    accountManagement(authenticationInformation)
	    setActivityResult(authenticationInformation)
	  })
	
}