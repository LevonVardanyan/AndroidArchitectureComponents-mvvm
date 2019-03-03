package dev.sololearn.test.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;

/**
 * Executor class provides the different executors e.g UI, DB, NETWORK
 */
public class MyExecutor {
	private static MyExecutor myExecutor;
	private Executor networkExecutor;
	private Executor dbExecutor;
	private Executor uiExecutor;

	private Handler refreshExecutor;

	public static MyExecutor getInstance() {
		if (myExecutor == null) {
			synchronized (MyExecutor.class) {
				if (myExecutor == null) {
					myExecutor = new MyExecutor();
				}
			}
		}
		return myExecutor;
	}

	private MyExecutor() {
		networkExecutor = Executors.newFixedThreadPool(5);
		dbExecutor = Executors.newFixedThreadPool(10);
		uiExecutor = new MainThreadExecutor();

		HandlerThread handlerThread = new HandlerThread("postCheckExecutor");
		handlerThread.start();
		refreshExecutor = new Handler(handlerThread.getLooper());
	}

	public void lunchOn(LunchOn lunchOn, Runnable runnable) {
		switch (lunchOn) {
			case NETWORK:
				networkExecutor.execute(runnable);
				break;
			case UI:
				uiExecutor.execute(runnable);
				break;
			case DB:
				dbExecutor.execute(runnable);
				break;
		}
	}

	public Handler getRefreshExecutor() {
		return refreshExecutor;
	}

	public void lunchPeriodic(Runnable runnable, long delay) {
		refreshExecutor.removeCallbacks(runnable);
		refreshExecutor.postDelayed(runnable, delay);
	}

	private static class MainThreadExecutor implements Executor {
		private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

		@Override
		public void execute(@NonNull Runnable command) {
			mainThreadHandler.post(command);
		}
	}

	public enum LunchOn {
		NETWORK,
		DB,
		UI
	}
}
