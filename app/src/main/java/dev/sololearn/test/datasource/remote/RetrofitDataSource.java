package dev.sololearn.test.datasource.remote;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import dev.sololearn.test.callback.GetDataCallback;
import dev.sololearn.test.datamodel.remote.ApiService;
import dev.sololearn.test.datamodel.remote.RequestConstants;
import dev.sololearn.test.datamodel.remote.RetrofitRequestBuilder;
import dev.sololearn.test.datamodel.remote.apimodel.ArticleResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * remote source implementation for Retrofit, this class implements BaseRemoteDataSource and gives API
 * for working with Retrofit
 */
public class RetrofitDataSource implements BaseRemoteDataSource {
    private static RetrofitDataSource remoteDataSource;

    private static final int PAGE_SIZE = 20;
    private static final int PAGE_SIZE_FROM_DATE = 50;

    private ApiService apiService;

    private MutableLiveData<String> newestArticleDate = new MutableLiveData<>();

    private int loadedPageCount;

    @NonNull
    public static RetrofitDataSource getInstance() {
        if (remoteDataSource == null) {
            synchronized (RetrofitDataSource.class) {
                if (remoteDataSource == null) {
                    remoteDataSource = new RetrofitDataSource();
                }
            }
        }
        return remoteDataSource;
    }

    private RetrofitDataSource() {
        apiService = RetrofitRequestBuilder.getInstance().getApiService();
        loadedPageCount = 0;
    }


    @Override
    public void getArticles(@Nullable GetDataCallback getDataCallback) {
        loadedPageCount++;
        getArticlesPage(loadedPageCount, getDataCallback);
    }

    @Override
    public void getArticlesFirstPage(@Nullable GetDataCallback getDataCallback) {
        getArticlesPage(1, getDataCallback);
    }

    @Override
    public boolean isNewerArticleExist(@Nullable String lastItemDate) {
        ArticleResponse articleResponse = getArticlesFromDateSync(lastItemDate, 1);
        return articleResponse != null && articleResponse.articlesData.articleList.isEmpty();
    }

    @Override
    public void resetPageCounter() {
        loadedPageCount = 0;
    }

    @Override
    public ArticleResponse getArticlesFromDateSync(String fromDate, int page) {
        ArticleResponse articleResponse = null;
        try {
            articleResponse = apiService.getArticlesFromDate(fromDate, RequestConstants.SHOW_FIELDS_VALUES, RequestConstants.API_KEY_VALUE,
                    page, PAGE_SIZE_FROM_DATE).execute().body();
            if (articleResponse != null && articleResponse.articlesData != null && articleResponse.articlesData.articleList != null) {
                if (page == 1) {
                    newestArticleDate.postValue(articleResponse.articlesData.articleList.get(0).publicationDate);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return articleResponse;
    }

    @Nullable
    @Override
    public MutableLiveData<String> getNewestArticle() {
        return newestArticleDate;
    }

    @Override
    public void reset() {
        loadedPageCount = 0;
    }

    @Override
    public void getArticlesPage(int page, GetDataCallback getDataCallback) {
        apiService.getArticles(RequestConstants.SHOW_FIELDS_VALUES,
                RequestConstants.API_KEY_VALUE, page, PAGE_SIZE)
                .enqueue(new Callback<ArticleResponse>() {
                    @Override
                    public void onResponse(Call<ArticleResponse> call, Response<ArticleResponse> response) {
                        ArticleResponse articleResponse = response.body();
                        if (articleResponse != null && articleResponse.articlesData != null &&
                                RequestConstants.RESULT_OK.equalsIgnoreCase(articleResponse.articlesData.status)) {
                            if (getDataCallback != null) {
                                getDataCallback.onSuccess(articleResponse.articlesData.articleList);
                            }
                            if (page == 1) {
                                newestArticleDate.postValue(articleResponse.articlesData.articleList.get(0).publicationDate);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ArticleResponse> call, Throwable t) {
                        if (getDataCallback != null) {
                            getDataCallback.onFailure(t.getMessage());
                        }

                    }
                });
    }
}
