package net.marioosh.stooq.stuff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author marioosh
 */

public class ApiImpl {
    private static Api client;
    private ApiImpl (){}

    private static Gson provideObjectMapper() {
        return new GsonBuilder()
                .create();
    }

    public static Api getInstance() {
        if(client == null) {
            client = new Retrofit.Builder()
                    .baseUrl(Api.API_URL)
                    .addConverterFactory(GsonConverterFactory.create(provideObjectMapper()))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(HttpClient.getInstance())
                    .build()
                    .create(Api.class);
        }
        return client;
    }
}
