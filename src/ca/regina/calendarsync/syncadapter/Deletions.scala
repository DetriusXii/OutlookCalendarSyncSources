package ca.regina.calendarsync.syncadapter

import android.content.Context
import ca.regina.calendarsync.monads.IO

class Deletions(context: Context) {
	def deleteBasedOnUrl(url: android.net.Uri): IO[Int] = IO(context.getContentResolver().delete(url, null, null))
}