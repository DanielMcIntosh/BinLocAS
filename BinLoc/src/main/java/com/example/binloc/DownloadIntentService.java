package com.example.binloc;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class DownloadIntentService extends IntentService {

    private static final String TAG = DownloadIntentService.class.getSimpleName();

    public static final String PENDING_RESULT_EXTRA = "pendingResult";
    public static final String URL_EXTRA = "url";
    public static final String BIN_NUMBER = "binNumber";

    public static final String BYTE_ARR_EXTRA = "byteArray";

    public static final int RESULT_CODE = 0;
    public static final int INVALID_URL_CODE = 1;
    public static final int ERROR_CODE = 2;

    public DownloadIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        PendingIntent reply = intent.getParcelableExtra(PENDING_RESULT_EXTRA);
        try {
            try {
                //URL url = new URL(intent.getStringExtra(URL_EXTRA));
                HttpClient httpClient = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();
                HttpPost httpPost = new HttpPost(intent.getStringExtra(URL_EXTRA));
                HttpResponse response = httpClient.execute(httpPost, localContext);
                InputStream in = response.getEntity().getContent();

                LinkedList<Integer> linkedList = new LinkedList<>();
                for (int next = in.read(); next != -1; next = in.read()) {linkedList.add(next);}
                Integer [] byteArr = new Integer[linkedList.size()];
                linkedList.toArray(byteArr);

                Intent result = new Intent();
                result.putExtra(BYTE_ARR_EXTRA, byteArr);

                reply.send(this, RESULT_CODE, result);
            } catch (MalformedURLException exc) {
                reply.send(INVALID_URL_CODE);
            } catch (Exception e) {
                // could do better by treating the different sax/xml exceptions individually
                reply.send(ERROR_CODE);
            }
        } catch (PendingIntent.CanceledException exc) {
            Log.i(TAG, "reply cancelled", exc);
        }
    }
}