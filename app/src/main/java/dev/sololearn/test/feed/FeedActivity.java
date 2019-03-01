package dev.sololearn.test.feed;

import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionInflater;
import android.view.View;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import dev.sololearn.test.R;
import dev.sololearn.test.feed.newitemscheck.CheckForNewItemsWorker;
import dev.sololearn.test.openarticle.OpenArticleFragment;
import dev.sololearn.test.util.Constants;
import dev.sololearn.test.util.ViewModelFactory;

import static androidx.core.view.ViewCompat.getTransitionName;

public class FeedActivity extends AppCompatActivity {

    private FeedFragment feedFragment;
    private OpenArticleFragment openArticleFragment;
    private FeedViewModel feedViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_activity);
        feedViewModel = obtainViewModel(this);
        initFeedFragment();
        initOpenArticleFragment();

        feedViewModel.getOpenArticleEvent().observe(this, clickArticleEvent -> {
            if (!clickArticleEvent.isHandled()) {
                openArticle(clickArticleEvent);
            }
        });
        if (!feedFragment.isAdded()) {
            getSupportFragmentManager().beginTransaction().replace(R.id.root_container,
                    feedFragment, FeedFragment.TAG).addToBackStack(null).commit();
        }

        feedViewModel.getCloseEvent().observe(this, aBoolean -> openFeedFragment());
    }

    private static FeedViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
        return ViewModelProviders.of(activity, factory).get(FeedViewModel.class);
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
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            finish();
            return;
        } else {
            getSupportFragmentManager().popBackStackImmediate();
        }
//        super.onBackPressed();
    }

    private void initFeedFragment() {
        feedFragment = (FeedFragment) getSupportFragmentManager().findFragmentByTag(FeedFragment.TAG);
        if (feedFragment == null) {
            feedFragment = new FeedFragment();
        }
    }

    private void initOpenArticleFragment() {
        openArticleFragment = (OpenArticleFragment) getSupportFragmentManager().findFragmentByTag(OpenArticleFragment.TAG);
        if (openArticleFragment == null) {
            openArticleFragment = new OpenArticleFragment();
        }
    }

    private void openFeedFragment() {
        if (openArticleFragment.isAdded()) {
            getSupportFragmentManager().popBackStackImmediate();
        } else {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.root_container, feedFragment, FeedFragment.TAG);
            fragmentTransaction.commit();
        }
    }

    private void openArticle(ClickArticleEvent clickArticleEvent) {
        View[] sharedViews = clickArticleEvent.getSharedViews();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Bundle arguments = openArticleFragment.getArguments();
        if (arguments == null) {
            arguments = new Bundle();
        }
        arguments.putParcelable(Constants.EXTRA_ARTICLE, clickArticleEvent.getArticle());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && sharedViews != null) {
            arguments.putString(Constants.EXTRA_TRANSITION_NAME_THUMB, getTransitionName(sharedViews[0]));
            fragmentTransaction.addSharedElement(sharedViews[0], getTransitionName(sharedViews[0]));
        }
        if (openArticleFragment.getArguments() == null) {
            openArticleFragment.setArguments(arguments);
        }
        fragmentTransaction.setReorderingAllowed(true).replace(R.id.root_container, openArticleFragment, OpenArticleFragment.TAG).addToBackStack(null).commit();
        clickArticleEvent.setAsHandled();
    }

}
