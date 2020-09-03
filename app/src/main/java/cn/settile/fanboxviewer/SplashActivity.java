package cn.settile.fanboxviewer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cn.settile.fanboxviewer.Network.Common;
import cn.settile.fanboxviewer.Network.RESTfulClient.FanboxAPI;
import cn.settile.fanboxviewer.Network.RESTfulClient.FanboxParser;
import cn.settile.fanboxviewer.Util.Constants;
import retrofit2.Retrofit;

import static cn.settile.fanboxviewer.Network.Common.initClient;
import static cn.settile.fanboxviewer.Network.Common.isLoggedIn;

public class SplashActivity extends AppCompatActivity implements Runnable {
    public static SharedPreferences sp;
    final String TAG = "Main";
    Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.loading);

        thread = new Thread(this);
        thread.start();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        switch (request) {
            case Constants.requestCodes.LOGIN:
                recreate();
                break;
            case Constants.requestCodes.EXIT:
                finish();
                break;
            default:
                break;
        }
        super.onActivityResult(request, result, data);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    public void run() {
        sp = getSharedPreferences("Configs", MODE_PRIVATE);
        boolean firstRun = false;

        if (sp.getBoolean("FirstRun", true)) {
            firstRun = true;
            sp.edit().putBoolean("FirstRun", false)
                    .apply();
        }

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        boolean isConnected = (networkInfo != null && networkInfo.isConnected());

        if (!isConnected) {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("isLoggedIn", false);
            i.putExtra("NO_NETWORK", true);
            startActivity(i);
            return;
        }

        if (firstRun) {
            Log.d(TAG, "First Run");
            Intent i = new Intent(this, LoginActivity.class);
            startActivityForResult(i, Constants.requestCodes.LOGIN);
            return;
        }

        CookieManager cm = CookieManager.getInstance();
        Constants.Cookie = cm.getCookie(getString(R.string.index));
        if(Constants.Cookie == null){
            Intent i = new Intent(this, LoginActivity.class);
            startActivityForResult(i, Constants.requestCodes.LOGIN);
            return;
        }

        initClient();

        if(Common.singleton == null) {
            Common.singleton = new Picasso.Builder(this)
                    .downloader(new OkHttp3Downloader(Common.client))
                    .build();
            Picasso.setSingletonInstance(Common.singleton);
        }

        Intent i = new Intent(this, MainActivity.class);
        Future<Boolean> loginFuture = isLoggedIn();
        while (!loginFuture.isDone()) {
        }
        try {
            if(!loginFuture.get()){
                Intent i1 = new Intent(this, LoginActivity.class);
                startActivityForResult(i1, Constants.requestCodes.LOGIN);
                return;
            }
            i.putExtra("isLoggedIn", true);

            Future<Object> tmp = Executors.newSingleThreadExecutor().submit(() -> FanboxParser.getMessages(true));
            while (!tmp.isDone()) {
            }
            tmp.get();
        } catch (Exception ex) {
            i.putExtra("isLoggedIn", false);
        }

        Uri url = getIntent().getData();
        Log.d(TAG, "Main_ocCreate_run: " + url);
        while (!Objects.equals(url, null)){                     // no extra function calls
            String username = url.getHost().split("\\.")[0];
            String path = url.getPath();                            // main=/  posts=/posts/<id>
            if (username.equals("www")){
                if (url.getPath().contains("@")){
                    path = url.getPath().split("@")[1];
                    Log.d(TAG, "run: " + path);
                    if (!path.contains("/")){
                        username = new StringBuilder(path).toString(); // Copy
                        path = "/";
                    }else{
                        username = path.split("/posts/")[0];
                    }
                    Log.d(TAG, "Main_ocCreate_run: " + username + "  ---  " + path);
                }else{
                    break;
                }
            }
            Log.d(TAG, "onCreate: " + path);
            if (path.equals("/")){
                i = new Intent(this, UserDetailActivity.class);
                i.putExtra("isURL", true);
                i.putExtra("CID", username);
            }else if (path.contains("/posts/")){
                String postId = path.split("/posts/")[1];
                i = new Intent(this, PostDetailActivity.class);
                i.putExtra("isURL", true);
                i.putExtra("CID", username);
                i.putExtra("URL", postId);
            }
            break;
        }

        startActivity(i);
        finish();
    }
}
