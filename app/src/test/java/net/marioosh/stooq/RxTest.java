package net.marioosh.stooq;

import junit.framework.Assert;

import net.marioosh.stooq.stuff.Api;
import net.marioosh.stooq.stuff.ApiImpl;
import net.marioosh.stooq.stuff.Index;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;

/**
 * @author marioosh
 */
public class RxTest {

    private static Api api;

    @BeforeClass
    public static void setup() {
        api = ApiImpl.getInstance();
    }

    @Test
    public void test() {
        Observable.just(1,2,3,4,5)
            .subscribe(new Action1<Integer>() {
                @Override
                public void call(Integer integer) {
                    Assert.assertNotNull(integer);
                }
            });
    }

    @Test
    @Ignore
    public void test2() {
        Observable.interval(1, TimeUnit.SECONDS, Schedulers.immediate())
            .takeUntil(new Func1<Long, Boolean>() {
                @Override
                public Boolean call(Long aLong) {
                    return aLong == 5;
                }
            })
            .subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    System.out.println(aLong);
                }
            });
    }

    @Test
    public void apiCalls() {

        List<Observable<ResponseBody>> observables = new ArrayList<Observable<ResponseBody>>();
        final Index.Type[] types = Index.Type.values();
        for (final Index.Type t : types) {
            observables.add(api.getIndex(t.getsParam()));
        }

        Observable.zip(observables, new FuncN<List<Index>>() {
            @Override
            public List<Index> call(Object... args) {
                List<Index> l = new ArrayList<Index>();
                for(int i = 0;i<args.length; i++) {
                    Index.Type t = types[i];
                    ResponseBody body = (ResponseBody)args[i];
                    try {
                        Document document = Jsoup.parse(body.string());
                        Elements elements = document.select(t.getCssSelector());
                        String value = elements.get(0).childNode(0).toString();
                        l.add(new Index(t, value));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return l;
            }
        })
        .subscribe(new Subscriber<List<Index>>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(List<Index> indices) {
                System.out.println(indices);
            }
        });
    }
}
