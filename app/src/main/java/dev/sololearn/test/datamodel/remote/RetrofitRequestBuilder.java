package dev.sololearn.test.datamodel.remote;

import androidx.annotation.Nullable;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitRequestBuilder {


	private static RetrofitRequestBuilder instance;
	private Retrofit retrofit;
	private ApiService apiService;

	public static RetrofitRequestBuilder getInstance() {
		if (instance == null) {
			instance = new RetrofitRequestBuilder();
		}
		return instance;
	}

	private RetrofitRequestBuilder() {
		retrofit = new Retrofit.Builder().baseUrl(RequestConstants.ARTICLES_BASE_URL).
				addConverterFactory(GsonConverterFactory.create()).build();

		apiService = retrofit.create(ApiService.class);
	}

	@Nullable
	public ApiService getApiService() {
		return apiService;
	}


}
