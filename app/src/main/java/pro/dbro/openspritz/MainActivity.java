package pro.dbro.openspritz;

import android.app.ActionBar;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import pro.dbro.openspritz.events.ChapterSelectRequested;
import pro.dbro.openspritz.events.ChapterSelectedEvent;
import pro.dbro.openspritz.events.WpmSelectedEvent;
import pro.dbro.openspritz.formats.SpritzerBook;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    public static final String SPRITZ_FRAG_TAG = "spritzfrag";
    private static final String PREFS = "ui_prefs";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;

    private int mWpm;
    private Bus mBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int theme = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt("THEME", 0);
        switch (theme) {
            case THEME_LIGHT:
                setTheme(R.style.Light);
                break;
            case THEME_DARK:
                setTheme(R.style.Dark);
                break;
        }
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new SpritzFragment(), SPRITZ_FRAG_TAG)
                .commit();

        OpenSpritzApplication app = (OpenSpritzApplication) getApplication();
        this.mBus = app.getBus();
        this.mBus.register(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getIntent().getAction().equals(Intent.ACTION_VIEW) && getIntent().getData() != null) {
            SpritzFragment frag = getSpritzFragment();
            frag.feedEpubToSpritzer(getIntent().getData());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBus != null) {
            mBus.unregister(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_speed) {
            if (mWpm == 0) {
                if (getSpritzFragment().getSpritzer() != null) {
                    mWpm = getSpritzFragment().getSpritzer().mWPM;
                } else {
                    mWpm = 500;
                }
            }
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            DialogFragment newFragment = WpmDialogFragment.newInstance(mWpm);
            newFragment.show(ft, "dialog");
            return true;
        } else if (id == R.id.action_theme) {
            int theme = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getInt("THEME", THEME_LIGHT);
            if (theme == THEME_LIGHT) {
                applyDarkTheme();
            } else {
                applyLightTheme();
            }
        } else if (id == R.id.action_open) {
            getSpritzFragment().chooseEpub();
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onWpmSelected(WpmSelectedEvent event) {
        if (getSpritzFragment() != null) {
            getSpritzFragment().getSpritzer()
                    .setWpm(event.getWpm());
        }
        mWpm = event.getWpm();
    }

    private void applyDarkTheme() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("THEME", THEME_DARK)
                .commit();
        recreate();

    }

    private void applyLightTheme() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("THEME", THEME_LIGHT)
                .commit();
        recreate();
    }

    @Subscribe
    public void onChapterSelected(ChapterSelectedEvent event) {
        SpritzFragment frag = getSpritzFragment();
        if (frag != null && frag.getSpritzer() != null) {
            frag.getSpritzer().printChapter(event.getChapter());
            frag.updateMetaUi();
        } else {
            Log.e(TAG, "SpritzFragment not available to apply chapter selection");
        }
    }

    @Subscribe
    public void onChapterSelectRequested(ChapterSelectRequested ignored) {
        SpritzFragment frag = getSpritzFragment();
        if (frag != null && frag.getSpritzer() != null) {
            SpritzerBook book = frag.getSpritzer().getBook();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            DialogFragment newFragment = TocDialogFragment.newInstance(book);
            newFragment.show(ft, "dialog");
        } else {
            Log.e(TAG, "SpritzFragment not available for chapter selection");
        }
    }

    private SpritzFragment getSpritzFragment() {
        return ((SpritzFragment) getSupportFragmentManager().findFragmentByTag(SPRITZ_FRAG_TAG));
    }
}
