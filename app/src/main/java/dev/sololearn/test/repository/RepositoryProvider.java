package dev.sololearn.test.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import dev.sololearn.test.datasource.local.LocalAppDataSource;
import dev.sololearn.test.datasource.remote.RemoteAppDataSource;


/**
 * this will provide Repository instance, if no provider we must call Repository's getInstance which have parameters
 */
public class RepositoryProvider {
	public static ArticlesRepository provideArticlesRepository(@NonNull Context context) {
		return ArticlesRepository.getInstance(LocalAppDataSource.getInstance(context),
				RemoteAppDataSource.getInstance());
	}
}
