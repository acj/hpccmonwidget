/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.linuxguy.HPCCMonWidget;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.linuxguy.HPCCMonWidget.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper methods.
 */
public class Helper {
    private static final String TAG = "Helper";
    
    /**
     * Regular expression that splits "Word of the day" entry into word
     * name, word type, and the first description bullet point.
     */
    public static final String HPCC_REGEX =
    	"(\\d+) Current.+\\n.+(\\d+ Finished)";
    
    /**
     * Partial URL to use when requesting an HPCC status.
     */
    private static String HPCCSTATS_PAGE = "";
    
    private static String HPCCSTATS_USER = "";

    /**
     * {@link StatusLine} HTTP status code when no server error has occurred.
     */
    private static final int HTTP_STATUS_OK = 200;

    /**
     * Shared buffer used by {@link #getUrlContent(String)} when reading results
     * from an API request.
     */
    private static byte[] sBuffer = new byte[512];
    
    /**
     * User-agent string to use when making requests. Should be filled using
     * {@link #prepareUserAgent(Context)} before making any other calls.
     */
    private static String sUserAgent = "HPCCMon";
    
    /**
     * Thrown when there were problems contacting the remote API server, either
     * because of a network error, or the server returned a bad status code.
     */
    public static class ApiException extends Exception {
        public ApiException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
        
        public ApiException(String detailMessage) {
            super(detailMessage);
        }
    }

    /**
     * Thrown when there were problems parsing the response to an API call,
     * either because the response was empty, or it was malformed.
     */
    public static class ParseException extends Exception {
        public ParseException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
    
    public static void prepareHPCCData(Context context) {
    	if (HPCCSTATS_USER == "" || getHPCCPage() == "") {
    		SharedPreferences prefs = context.getSharedPreferences("org.linuxguy.HPCCMonWidget", 0);
    		HPCCSTATS_USER = prefs.getString("hpccmonwidget_user", "");
    		setHPCCPage(prefs.getString("hpccmonwidget_url", ""));
    		Log.i("Status", "prepareHPCCData()");
    	}
    }
    
    /**
     * Prepare the internal User-Agent string for use. This requires a
     * {@link Context} to pull the package name and version number for this
     * application.
     */
    public static void prepareUserAgent(Context context) {
        try {
            // Read package name and version number from manifest
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            sUserAgent = "HPCCMonWidget/Android";
            
        } catch(NameNotFoundException e) {
            Log.e(TAG, "Couldn't find package information in PackageManager", e);
        }
    }
    
    /**
     * Read and return the content for HPCC runs owned by a specific user.
     * Because this call blocks until results are available, it should not be
     * run from a UI thread.
     * 
     * @return Exact content of page.
     * @throws ApiException If any connection or server error occurs.
     * @throws ParseException If there are problems parsing the response.
     */
    public static String getPageContent()
            throws ApiException, ParseException {
        // Query the API for content
    	String URI = getHPCCPage() + "?username=" + getHPCCUser();
    	Log.i("HPCCMonWidget", "Polling " + URI);
        return getUrlContent(URI);
    }

    /**
     * Pull the raw text content of the given URL. This call blocks until the
     * operation has completed, and is synchronized because it uses a shared
     * buffer {@link #sBuffer}.
     * 
     * @param url The exact URL to request.
     * @return The raw content returned by the server.
     * @throws ApiException If any connection or server error occurs.
     */
    protected static synchronized String getUrlContent(String url) throws ApiException {
        if (sUserAgent == null) {
            throw new ApiException("User-Agent string must be prepared");
        }
        
        // Create client and set our specific user-agent string
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", sUserAgent);

        try {
            HttpResponse response = client.execute(request);
            
            // Check if server response is valid
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != HTTP_STATUS_OK) {
                throw new ApiException("Invalid response from server: " +
                        status.toString());
            }
    
            // Pull content stream from response
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            
            // Read response into a buffered stream
            int readBytes = 0;
            while ((readBytes = inputStream.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            
            // Return result from buffered stream
            return new String(content.toByteArray());
        } catch (IOException e) {
            throw new ApiException("Problem communicating with API", e);
        }
    }

	public static void setHPCCUser(String hPCCSTATS_USER) {
		HPCCSTATS_USER = hPCCSTATS_USER;
	}

	public static String getHPCCUser() {
		return HPCCSTATS_USER;
	}

	public static void setHPCCPage(String hPCCSTATS_PAGE) {
		HPCCSTATS_PAGE = hPCCSTATS_PAGE;
	}

	public static String getHPCCPage() {
		return HPCCSTATS_PAGE;
	}
}
