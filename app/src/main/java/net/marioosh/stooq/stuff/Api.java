package net.marioosh.stooq.stuff;

import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * @author marioosh
 */

public interface Api {
    
    final String API_URL = "http://stooq.pl/";

    @GET("q")
    Observable<ResponseBody> getIndex(@Query("s") String index);
}
