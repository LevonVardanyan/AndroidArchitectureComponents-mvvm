package dev.sololearn.test.util;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import dev.sololearn.test.feed.FeedViewModel;
import dev.sololearn.test.repository.ArticlesRepository;
import dev.sololearn.test.repository.RepositoryProvider;

/**
 * This is a Factory class for ViewModels, We don't use androids ViewModelFactory directly because in that
 * way we can't pass parameter to ViewModel's constructor, here we need pass Repository to viewModel
 */
public class ViewModelFactory extends ViewModelProvider.NewInstanceFactory {

    private static volatile ViewModelFactory viewModelFactory;

    private final ArticlesRepository articlesRepository;
    private Application application;

    public static ViewModelFactory getInstance(Application application) {

        if (viewModelFactory == null) {
            synchronized (ViewModelFactory.class) {
                if (viewModelFactory == null) {
                    viewModelFactory = new ViewModelFactory(
                            RepositoryProvider.provideArticlesRepository(application.getApplicationContext()), application);
                }
            }
        }
        return viewModelFactory;
    }

    private ViewModelFactory(ArticlesRepository repository, Application application) {
        articlesRepository = repository;
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(FeedViewModel.class)) {
            //noinspection unchecked
            return (T) new FeedViewModel(articlesRepository, application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
