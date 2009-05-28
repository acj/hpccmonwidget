package org.linuxguy.HPCCMonWidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.TextView;

public class HPCCMonWidgetConfigure extends Activity {

	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        // Set the view layout resource to use.
        setContentView(R.layout.widget_config);
        
        // Bind the action for the save button.
        findViewById(R.id.btnSave).setOnClickListener(mOnClickListener);

        // Find the widget id from the intent. 
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Load previous values if they are available
        EditText txtUser = (EditText)findViewById(R.id.txtUsername);
        EditText txtUrl = (EditText)findViewById(R.id.txtURL);
                
        String username = loadPref(this, mAppWidgetId, "user");
        String URL = loadPref(this, mAppWidgetId, "url");

        txtUser.setText(username);
        txtUrl.setText(URL);
        
        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
	}
	
    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = HPCCMonWidgetConfigure.this;

            // When the button is clicked, save the string in our prefs and return that they
            // clicked OK.
            EditText txtUser = (EditText)findViewById(R.id.txtUsername);
            EditText txtUrl = (EditText)findViewById(R.id.txtURL);
            
            String username = txtUser.getText().toString();
            String URL = txtUrl.getText().toString();
            if (URL == "") {
            	URL = "http://www.cse.msu.edu/~connel42/diststats/index.php";
            }
            savePrefs(context, mAppWidgetId, username, URL);
            
            Helper.setHPCCUser(username);
            Helper.setHPCCPage(URL);

            // Push widget update to surface with newly set prefix
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            HPCCMonWidget.updateAppWidget(context, appWidgetManager,
                    mAppWidgetId, username, URL);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    static String loadPref(Context context, int appWidgetId, String key) {
    	SharedPreferences prefs = context.getSharedPreferences("org.linuxguy.HPCCMonWidget", 0);
    	return prefs.getString("hpccmonwidget_" + key, "");
    }
    // Write the prefix to the SharedPreferences object for this widget
    static void savePrefs(Context context, int appWidgetId, String username, String url) {
        SharedPreferences.Editor prefs = context.getSharedPreferences("org.linuxguy.HPCCMonWidget", 0).edit();
        prefs.putString("hpccmonwidget_user", username);
        prefs.putString("hpccmonwidget_url", url);
        prefs.commit();
    }


	@Override
	protected void onPause() {
		super.onPause();
	}

}
