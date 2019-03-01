package dev.sololearn.test.datamodel.remote;

import dev.sololearn.test.datamodel.remote.apimodel.ArticleResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * request api service
 */
public interface ApiService {

	/**
	 * get articles without any optional parameters, this will return pageSize count articles
	 * @param show_fields indicate that need get thumbnail, (from response structure)
	 * @param api_key registered api key
	 * @param pageNumber which page currently requesting
	 * @param pageSize page size for getting
	 * @return
	 */
	@GET("/search")
	Call<ArticleResponse> getArticles(@Query(RequestConstants.SHOW_FIELDS_QUERY_PARAM) String show_fields,
                                      @Query(RequestConstants.API_KEY_QUERY_PARAM) String api_key,
                                      @Query("page") long pageNumber, @Query("page-size") long pageSize);

	/**
	 * get articles published since fromDate
	 * @param fromDate
	 * @param show_fields
	 * @param api_key
	 * @param pageNumber
	 * @param pageSize
	 * @return
	 */
	@GET("/search")
	Call<ArticleResponse> getArticlesFromDate(@Query(RequestConstants.FROM_DATE_QUERY_PARAM) String fromDate,
                                              @Query(RequestConstants.SHOW_FIELDS_QUERY_PARAM) String show_fields,
                                              @Query(RequestConstants.API_KEY_QUERY_PARAM) String api_key,
                                              @Query("page") long pageNumber, @Query("page-size") long pageSize);

}
