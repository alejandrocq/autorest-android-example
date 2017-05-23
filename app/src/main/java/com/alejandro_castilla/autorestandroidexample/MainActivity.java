package com.alejandro_castilla.autorestandroidexample;

import static com.intendia.gwt.autorest.client.CollectorResourceVisitor.Param.expand;
import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.newThread;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import com.alejandro_castilla.autorestandroidexample.shared.services.MyRestService;
import com.alejandro_castilla.autorestandroidexample.shared.services.MyRestService.Data;
import com.alejandro_castilla.autorestandroidexample.shared.services.MyRestService_RestServiceModel;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.intendia.gwt.autorest.client.CollectorResourceVisitor;
import com.intendia.gwt.autorest.client.ResourceVisitor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

public class MainActivity extends AppCompatActivity {
    private TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.data);
    }

    @Override protected void onResume() {
        super.onResume();
        MyRestService restService = new MyRestService_RestServiceModel(new ResourceVisitor.Supplier() {
            @Override public ResourceVisitor get() {
                return new MyResourceVisitor()
                        .header("Content-Type", "application/json");
            }
        });

        restService.onlinePlayers().subscribeOn(newThread()).observeOn(mainThread()).doOnNext(new Action1<Data>() {
            @Override public void call(Data data) {
                text.setText("Players online: " + data.pc.count);
            }
        }).subscribe();
    }

    class MyResourceVisitor extends CollectorResourceVisitor {
        private String encode(String key) {
            try {
                return URLEncoder.encode(key, "UTF-8");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String uri() {
            String uri = "";
            for (String path : paths) uri += path;
            return uri + query();
        }

        private String query() {
            String q = "";
            for (Param p : expand(queryParams)) {
                q += (q.isEmpty() ? "" : "&") + encode(p.k) + "=" + encode(p.v.toString());
            }
            return q.isEmpty() ? "" : "?" + q;
        }

        @Override
        public <T> T as(Class<? super T> container, final Class<?> type) {
            if (!Observable.class.equals(container)) {
                throw new IllegalArgumentException("unsupported type " + container);
            }

            //noinspection unchecked
            return (T) request(type);
        }

        private <T> Observable<T> request(final Class<?> type) {
            return Observable.defer(new Func0<Observable<T>>() {
                @Override
                public Observable<T> call() {
                    HttpURLConnection req;
                    try {
                        // Open connection and set headers
                        req = (HttpURLConnection) new URL(uri()).openConnection();
                        req.setRequestMethod(method);
                        for (Param e : headerParams) req.setRequestProperty(e.k, Objects.toString(e.v));
                    } catch (IOException e) {
                        throw new RuntimeException("open connection error: " + e, e);
                    }

                    // Get response (only responses with 200 as code are processed)
                    try (InputStream inputStream = req.getInputStream()) {
                        int rc = req.getResponseCode();
                        if (rc != 200) throw new RuntimeException("unexpected response code " + rc);
                        final Gson gson = new Gson();
                        JsonElement json = new JsonParser().parse(new InputStreamReader(inputStream));
                        //noinspection unchecked
                        return (Observable<T>) (json.isJsonObject() ?
                                Observable.just(gson.fromJson(json, type)) :
                                Observable.from(json.getAsJsonArray()).map(new Func1<JsonElement, Object>() {
                                    public Object call(JsonElement e) {
                                        return gson.fromJson(e, type);
                                    }
                                }));
                    } catch (IOException e) {
                        throw new RuntimeException("receiving response error: " + e, e);
                    }
                }
            });
        }
    }
}
