package dev.sololearn.test.feed.newitemscheck;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import dev.sololearn.test.R;
import dev.sololearn.test.feed.FeedActivity;
import dev.sololearn.test.repository.ArticlesRepository;
import dev.sololearn.test.repository.RepositoryProvider;
import dev.sololearn.test.util.Constants;

/**
 * Perioudic worker for checking new articles and send notification
 */
public class CheckForNewItemsWorker extends Worker {

    public final static String NAME = "new_article_check_worker";
    private final static String CHANNEL_ID = "news_notification";
    private final static int NOTIFICATION_ID = 0;


    public CheckForNewItemsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ArticlesRepository articlesRepository = RepositoryProvider.provideArticlesRepository(getApplicationContext());
        String lastArticleDate = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(Constants.PREF_NEWEST_ARTICLE_PUBLICATION_DATE, "");
        if (articlesRepository.isNewerArticleExist(lastArticleDate)) {
            Intent intent = new Intent(getApplicationContext(), FeedActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(getApplicationContext().getString(R.string.notification_title))
                    .setContentText(getApplicationContext().getString(R.string.notification_content))
                    .setContentIntent(pendingIntent);


            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "news_notification", NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }

            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
        return Result.retry();
    }

}
