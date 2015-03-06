package ca.regina.calendarsync.syncadapter

import android.content.Context
import ca.regina.calendarsync.monads._

class Insertions(context: Context) {
	def insert(url: android.net.Uri, contentValues: android.content.ContentValues): 
		IO[android.net.Uri] = IO(
	      context.getContentResolver().insert(url, contentValues))
}