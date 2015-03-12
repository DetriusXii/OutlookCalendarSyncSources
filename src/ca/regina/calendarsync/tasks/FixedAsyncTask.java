package ca.regina.calendarsync.tasks;

import android.os.AsyncTask;

public abstract class FixedAsyncTask<T, U, V> extends AsyncTask<T, U, V> {
	protected V doInBackground(T... ts) {
		return doInBackgroundHelper(ts);
	}
	
	protected abstract V doInBackgroundHelper(T[] ts);
}
