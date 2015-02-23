package ca.regina.calendarsync.services

import android.os.IBinder
import android.app.Service
import android.content.Intent
import ca.regina.calendarsync.syncadapter.OutlookSyncAdapter

object OutlookSyncService {
  private var outlookSyncAdapter: OutlookSyncAdapter = null
}

class OutlookSyncService extends Service {  
  override def onCreate(): Unit = {
    this.synchronized({
      if (OutlookSyncService.outlookSyncAdapter == null) {
        OutlookSyncService.outlookSyncAdapter = new OutlookSyncAdapter(this.getApplicationContext(), true)
      }
    })
  }
  
  override def onBind(intent: Intent): IBinder = OutlookSyncService.outlookSyncAdapter .getSyncAdapterBinder()
}