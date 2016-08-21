package net.marioosh.stooq.stuff;

import okhttp3.OkHttpClient;

/**
 * @author marioosh
 */
public class HttpClient {

    private static OkHttpClient instance;

    private HttpClient(){

    }

    public static OkHttpClient getInstance() {
        if(instance == null) {
            instance = new OkHttpClient();
        }
        return instance;
    }

}
