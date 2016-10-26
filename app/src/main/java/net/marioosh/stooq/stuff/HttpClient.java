package net.marioosh.stooq.stuff;

import net.marioosh.stooq.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * @author marioosh
 */
public class HttpClient {

    private static OkHttpClient instance;

    private HttpClient(){

    }

    public static OkHttpClient getInstance() {
        if(instance == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            if(BuildConfig.DEBUG) {
                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(loggingInterceptor);
            }

            instance = builder.build();
        }
        return instance;
    }

}
