package net.marioosh.stooq;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.marioosh.stooq.stuff.HttpClient;
import net.marioosh.stooq.stuff.Index;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static long DEFAULT_DELAY = 30; // sekundy
    private final static String DELAY_KEY = "delay";
    private static final String DATA_URL = "http://stooq.pl/";

    private SharedPreferences prefs;

    private RecyclerView rv;
    private final List<Index> data = new ArrayList<Index>();

    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));

        /**
         * TODO
         * @see android.support.v7.widget.RecyclerView.ItemAnimator#animateChange
         * rv.setItemAnimator(...);
         */

        rv.setAdapter(new MyAdapter());

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                fetchData();
                handler.postDelayed(this, prefs.getLong(DELAY_KEY, DEFAULT_DELAY)*1000);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(runnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(DELAY_KEY)) {
            handler.removeCallbacks(runnable);
            handler.post(runnable);
        }
    }

    private void fetchData() {
        OkHttpClient client = HttpClient.getInstance();

        for(final Index.Type t: Index.Type.values()) {
            Request request = new Request.Builder()
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .url(t.getSrcUrl())
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, final IOException e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, e+"", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(!response.isSuccessful()) {
                        return;
                    }

                    Document document = Jsoup.parse(response.body().string());

                    Elements elements = document.select(t.getCssSelector());
                    String value = elements.get(0).childNode(0).toString();
                    Log.d("parsed", t +"="+value);

                    boolean found = false;
                    for(int i=0;i<data.size();i++) {
                        Index d = data.get(i);
                        d.setTime(System.currentTimeMillis());
                        if (d.getType() == t) {
                            found = true;
                            if (!d.getValue().equals(value)) {
                                d.setValue(value);
                                d.setUpdated(true);
                            }
                            break;
                        }
                    }
                    if(!found) {
                        data.add(new Index(t, value));
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            rv.getAdapter().notifyDataSetChanged();
                        }
                    });
                }
            });
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.rv_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Index index = data.get(position);
            holder.name.setText(index.getType().name());
            holder.time.setText(sdf.format(new Date(index.getTime())));
            holder.value.setText(index.getValue());
            if(index.isUpdated()) {
                index.setUpdated(false);
                ObjectAnimator.ofObject(holder.itemView, "backgroundColor", new ArgbEvaluator(),
                        android.R.color.transparent, getResources().getColor(R.color.colorAccent), android.R.color.transparent)
                        .setDuration(500)
                        .start();
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView value;
        private final TextView time;
        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            value = (TextView) itemView.findViewById(R.id.value);
            time = (TextView) itemView.findViewById(R.id.time);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        long s = prefs.getLong(DELAY_KEY, DEFAULT_DELAY);
        int menuResId = getResources().getIdentifier("s"+s,"id",getPackageName());
        if(menuResId != 0) {
            menu.findItem(menuResId).setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.s1:
                prefs.edit().putLong(DELAY_KEY, 1).apply();
                return true;
            case R.id.s10:
                prefs.edit().putLong(DELAY_KEY, 10).apply();
                return true;
            case R.id.s30:
                prefs.edit().putLong(DELAY_KEY, DEFAULT_DELAY).apply();
                return true;
            case R.id.s60:
                prefs.edit().putLong(DELAY_KEY, 60).apply();
                return true;
            case R.id.s300:
                prefs.edit().putLong(DELAY_KEY, 300).apply();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
}
