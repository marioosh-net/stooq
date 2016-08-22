package net.marioosh.stooq;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import net.marioosh.stooq.stuff.HttpClient;
import net.marioosh.stooq.stuff.Index;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static long DEFAULT_DELAY = 30; // sekundy
    private final static String DELAY_KEY = "delay";
    private static final String DATA_URL = "http://stooq.pl/";

    private SharedPreferences prefs;

    private RecyclerView rv;
    private final List<Index> data = new ArrayList<Index>();

    private Subscription subscription;
    private Observer observer;
    private Observable<Map<Index.Type, String>> observable;

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

        observable = Observable.interval(prefs.getLong(DELAY_KEY, DEFAULT_DELAY), TimeUnit.SECONDS, Schedulers.io())
                .startWith(0l) // pierwsze requesty od razu
            .map(new Func1<Long, Map<Index.Type, String>>() {
                @Override
                public Map<Index.Type, String> call(Long aLong) {
                    final OkHttpClient client = HttpClient.getInstance();
                    Map<Index.Type, String> m = new HashMap<Index.Type, String>();
                    for (final Index.Type t : Index.Type.values()) {
                        Request request = new Request.Builder()
                                .cacheControl(CacheControl.FORCE_NETWORK)
                                .url(t.getSrcUrl())
                                .build();
                        try {
                            Response response = client.newCall(request).execute();
                            Document document = Jsoup.parse(response.body().string());
                            Elements elements = document.select(t.getCssSelector());
                            String value = elements.get(0).childNode(0).toString();
                            Log.d("parsed", t + "=" + value);
                            m.put(t, value);
                        } catch (IOException e) {
                        }
                    }
                    return m;
                }
            });

        observer = new Subscriber<Map<Index.Type, String>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e("error", e+"");
            }

            @Override
            public void onNext(Map<Index.Type, String> map) {
                for(Index.Type t: map.keySet()) {
                    String value = map.get(t);
                    boolean found = false;
                    for (int i = 0; i < data.size(); i++) {
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
                    if (!found) {
                        data.add(new Index(t, value));
                    }
                }
                rv.getAdapter().notifyDataSetChanged();
            }
        };

        subscription = observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        subscription.unsubscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(DELAY_KEY)) {
            // TODO
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
