package ca.regina.calendarsync.activities

import android.app.Activity
import android.os.Bundle
import ca.regina.calendarsync.R
import android.widget.EditText
import android.view.View
import android.net.wifi.WifiManager
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.accounts.AccountAuthenticatorActivity
import ca.regina.calendarsync.tasks.LoginTask
import android.accounts.AccountManager
import android.accounts.Account
import ca.regina.calendarsync.Utilities.OutlookAuthenticator
import android.util.Log

object LoginActivity {
  val TAG = "LoginActivity"
}

class LoginActivity extends AccountAuthenticatorActivity {
  private var wifiManagerOption: Option[WifiManager] = None
  private var loginTaskOption: Option[LoginTask] = None
  private var accountManagerOption: Option[AccountManager] = None
  
  private def safeClassCast[B](a: Any): Option[B] = {
    try {
      Some(a.asInstanceOf[B])
    } catch {
      case _: ClassCastException => None
    }
  }
	
  override protected def onCreate(savedInstanceState: Bundle): Unit = {
	super.onCreate(savedInstanceState)
	setContentView(R.layout.activity_main)
	val usernameEdit = this.findViewById(R.id.username_edit).asInstanceOf[EditText]
	val passwordEdit = this.findViewById(R.id.password_edit).asInstanceOf[EditText]
	val signInButton = this.findViewById(R.id.login_button)
  
    wifiManagerOption = safeClassCast[WifiManager](getSystemService(Context.WIFI_SERVICE))
    
    val accountManager = AccountManager.get(this)
    if (accountManager != null) {
      accountManagerOption = Some(accountManager)
    }
	
	signInButton.setOnClickListener(createOnClickListener(usernameEdit, passwordEdit))
  }
  
  private def getExistingAccount(username: String): Option[Account] =
    for (accountManager <- accountManagerOption;
	  existingAccount<- accountManager.getAccountsByType(OutlookAuthenticator.ACCOUNT_TYPE).find(_.name.compareTo(username) != 0)
    ) yield existingAccount
	
  private def createOnClickListener(usernameEditText: EditText,
      passwordEditText: EditText): View.OnClickListener = new View.OnClickListener {
	  override def onClick(v: View): Unit = {
	    Log.v(LoginActivity.TAG, "Starting on click")
	    Log.v(LoginActivity.TAG, "wifiManagerOption: " + wifiManagerOption.toString())
	    Log.v(LoginActivity.TAG, "loginTaskOption: " + loginTaskOption.toString())
	    Log.v(LoginActivity.TAG, "accountManagerOption: " + accountManagerOption.toString())
	    
	    val username = usernameEditText.getText.toString  
	    val password = passwordEditText.getText.toString
	    
	    val hasExistingAccount = getExistingAccount(username).map(_ => true).getOrElse({false})
	    
	    loginTaskOption.map(_.cancel(true))
	    
	    loginTaskOption = for (wifiManager <- wifiManagerOption;
    		_ <- Some(wifiManager);
    		accountManager <- accountManagerOption;
    		newLoginTask <- Some(new LoginTask(username, password, accountManager, !hasExistingAccount, LoginActivity.this))
	    ) yield newLoginTask
	    
	    loginTaskOption.map(_.execute())
	    
	    Log.v(LoginActivity.TAG, "post loginTaskOption: " + loginTaskOption.toString())
	  }
  }
  
  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater().inflate(R.menu.main, menu)
    true
  }
  
  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    val id = item.getItemId();

    id == R.id.action_settings match {
      case true => true
      case false => super.onOptionsItemSelected(item)
    }
  }
}