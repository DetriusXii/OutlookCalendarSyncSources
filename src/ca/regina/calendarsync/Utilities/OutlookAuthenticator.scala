package ca.regina.calendarsync.Utilities

import android.os.Bundle
import android.accounts.AbstractAccountAuthenticator
import android.content.Context
import android.accounts.AccountAuthenticatorResponse
import android.content.Intent
import android.accounts.AccountManager
import android.accounts.Account
import android.util.Log
import ca.regina.calendarsync.activities.LoginActivity

object OutlookAuthenticator {
  val TAG = "OutlookAuthenticator"
  val ACCOUNT_TYPE = "ca.regina.calendarsync"
}

class OutlookAuthenticator(context: Context) extends AbstractAccountAuthenticator(context) {
	def editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle = {
	  throw new UnsupportedOperationException
	}
	
	def addAccount(response: AccountAuthenticatorResponse,
			accountType: String,
			authTokenType: String,
			requiredFeatures: Array[String],
			options: Bundle): Bundle = {
	  android.util.Log.v(OutlookAuthenticator.TAG, "addAccount()")
	  val intent = new Intent(context, classOf[LoginActivity])
	  intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
	  val bundle = new Bundle
	  bundle.putParcelable(AccountManager.KEY_INTENT, intent)
	  bundle
	}
	
	def confirmCredentials(response: AccountAuthenticatorResponse,
			account: Account,
			options: Bundle): Bundle = {
	  android.util.Log.v(OutlookAuthenticator.TAG, "confirmCredentials()")
	  null
	}
	
	def getAuthToken(response: AccountAuthenticatorResponse,
			account: Account,
			authTokenType: String,
			options: Bundle): Bundle = {
	  def repromptCredentials(): Bundle = {
	    val intent = new Intent(context, classOf[LoginActivity])
	    intent.putExtra(AuthenticationUtility.USERNAME_KEY, account.name)
	    intent.putExtra(AuthenticationUtility.PASSWORD_KEY , authTokenType)
	    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
	    val bundle = new Bundle
	    bundle.putParcelable(AccountManager.KEY_INTENT, intent)
	    bundle
	  }
	  
	  Log.v(OutlookAuthenticator.TAG, "getAuthToken()")
	  if (authTokenType.compareTo(OutlookAuthenticator.ACCOUNT_TYPE) != 0) {
	    val result = new Bundle
	    result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType")
	    result
	  } else {
	    val am = AccountManager.get(context)
	    val password = am.getPassword(account)
	    val passwordOption = if (password != null) Some(password) else None
	    val bundleOption = for (password <- passwordOption;
	    		authToken <- AuthenticationUtility.authenticate(account.name, password, false)
	    ) yield {
	      val result = new Bundle
	      result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
	      result.putString(AccountManager.KEY_ACCOUNT_TYPE, OutlookAuthenticator.ACCOUNT_TYPE)
	      result.putString(AccountManager.KEY_AUTHTOKEN, authToken.password)
	      result
	    }
	    
	    bundleOption.getOrElse({repromptCredentials()})
	  }
	}
	
	override def getAuthTokenLabel(authTokenType: String): String = {
	  Log.v(OutlookAuthenticator.TAG, "getAuthTokenLabel()")
	  return null
	}
	
	def updateCredentials(response: AccountAuthenticatorResponse, 
	    account: Account, 
	    authTokenType: String,
	    options: Bundle): Bundle = {
	  Log.v(OutlookAuthenticator.TAG, "updateCredentials()")
	  null
	}
	
	override def hasFeatures(response: AccountAuthenticatorResponse,
					account: Account,
					features: Array[String]): Bundle = {
	  Log.v(OutlookAuthenticator.TAG, "hasFeatures()")
	  val result = new Bundle
	  result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
	  return result
	}
	
	
}