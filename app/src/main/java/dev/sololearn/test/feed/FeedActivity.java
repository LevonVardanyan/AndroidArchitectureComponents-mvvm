package dev.sololearn.test.feed;

import android.os.Bundle;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import dev.sololearn.test.R;
import dev.sololearn.test.feed.newitemscheck.CheckForNewItemsWorker;

public class FeedActivity extends AppCompatActivity {

    private FeedFragment feedFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_activity);
        initFeedFragment();

        if (!feedFragment.isAdded()) {
            getSupportFragmentManager().beginTransaction().replace(R.id.feed_container,
                    feedFragment, FeedFragment.TAG).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        WorkManager.getInstance().cancelAllWorkByTag(CheckForNewItemsWorker.NAME);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Constraints.Builder builder = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true);
        PeriodicWorkRequest.Builder checkForNewArticlesWorkBuilder =
                new PeriodicWorkRequest.Builder(CheckForNewItemsWorker.class, 2, TimeUnit.HOURS);
        checkForNewArticlesWorkBuilder.setConstraints(builder.build());
        PeriodicWorkRequest checkForNewArticlesWorker = checkForNewArticlesWorkBuilder.build();
        WorkManager.getInstance().enqueueUniquePeriodicWork(CheckForNewItemsWorker.NAME,
                ExistingPeriodicWorkPolicy.REPLACE, checkForNewArticlesWorker);
    }

    @Override
    public void onBackPressed() {
        if (!feedFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    private void initFeedFragment() {
        feedFragment = (FeedFragment) getSupportFragmentManager().findFragmentByTag(FeedFragment.TAG);
        if (feedFragment == null) {
            feedFragment = new FeedFragment();
        }
    }

}
