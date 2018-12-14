package me.calebjones.spacelaunchnow.local.common;

import android.os.Bundle;

import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity;

import androidx.appcompat.app.AppCompatActivity;

import io.realm.Realm;
import me.calebjones.spacelaunchnow.utils.analytics.Analytics;
import timber.log.Timber;

public class BaseActivity extends CyaneaAppCompatActivity {

    public BaseActivity(){}

    public BaseActivity (String screenName){
        name = screenName;
    }

    private Realm realm;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");
        realm = Realm.getDefaultInstance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy");
        Analytics.getInstance().sendScreenView(name, "Activity destroyed.");
        realm.close();
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.d("onStart");
        Analytics.getInstance().sendScreenView(name, name + " started.");
        if (realm.isClosed()) {
            realm = Realm.getDefaultInstance();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");
        Analytics.getInstance().sendScreenView(name, name + " resumed.");
    }

    @Override
    public void onPause() {
        super.onPause();
        Timber.d("onPause");
        Analytics.getInstance().notifyGoneBackground();
    }


    public Realm getRealm() {
        if(realm.isClosed()){
            realm = Realm.getDefaultInstance();
        }
        return realm;
    }
}
