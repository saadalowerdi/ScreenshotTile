package com.github.ipcjs.screenshottile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import static com.github.ipcjs.screenshottile.BuildConfig.APPLICATION_ID;
import static com.github.ipcjs.screenshottile.Utils.p;
import static com.github.ipcjs.screenshottile.UtilsKt.screenshot;

public class NoDisplayActivity extends Activity {

    private static final String EXTRA_SCREENSHOT = APPLICATION_ID + ".NoDisplayActivity.EXTRA_SCREENSHOT";
    private static final String EXTRA_PARTIAL = APPLICATION_ID + ".NoDisplayActivity.EXTRA_PARTIAL";

    /**
     * New Intent that takes a screenshot immediately if screenshot is true
     *
     * @param context    Context
     * @param screenshot Immediately start taking a screenshot
     * @return The intent
     */
    public static Intent newIntent(Context context, boolean screenshot) {
        Intent intent = new Intent(context, NoDisplayActivity.class);
        intent.putExtra(EXTRA_SCREENSHOT, screenshot);
        return intent;
    }

    /**
     * New Intent that opens the partial screenshot selector
     *
     * @param context Context
     * @return The intent
     */
    public static Intent newPartialIntent(Context context) {
        Intent intent = new Intent(context, NoDisplayActivity.class);
        intent.putExtra(EXTRA_PARTIAL, true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();

            if (intent.getBooleanExtra(EXTRA_PARTIAL, false)) {
                p("NoDisplayActivity.onCreate EXTRA_PARTIAL=true");
                screenshot(this, true);
            } else if (intent.getBooleanExtra(EXTRA_SCREENSHOT, false) || (action != null && action.equals(EXTRA_SCREENSHOT))) {
                p("NoDisplayActivity.onCreate EXTRA_SCREENSHOT=true");
                screenshot(this, false);
            } else {
                p("NoDisplayActivity.onCreate EXTRA_SCREENSHOT=false");
            }
        }
        finish();
    }
}
