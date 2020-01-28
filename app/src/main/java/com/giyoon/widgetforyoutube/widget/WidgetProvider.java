package com.giyoon.widgetforyoutube.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.giyoon.widgetforyoutube.R;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.ChannelListResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import static android.content.Context.MODE_PRIVATE;


public class WidgetProvider extends AppWidgetProvider {

    private static final String TAG = "mWidget Provider";
    private static final String WIDGET_CLICK = "com.giyoon.widgetforyoutube.CLICK";
    private static final String[] SCOPES = {YouTubeScopes.YOUTUBE_READONLY};

    GoogleAccountCredential mCredential;


    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled");
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");

        for (int appWidgetId : appWidgetIds) {
            Intent intent = new Intent(context, WidgetProvider.class);
            intent.setAction(WIDGET_CLICK);
            intent.putExtra("Widget Id",appWidgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,appWidgetId,intent,PendingIntent.FLAG_IMMUTABLE);

            String szAppWidgetId = String.valueOf(appWidgetId);
            SharedPreferences pref = context.getSharedPreferences("YOUTUBE"+szAppWidgetId, MODE_PRIVATE);
            Bitmap mCroppedBitmap = StringToBitmap(pref.getString("channelThumbnailBitmap",""));

            // 앱구성을 완료한다.
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setOnClickPendingIntent(R.id.widget_layout,pendingIntent);
            views.setImageViewBitmap(R.id.widget_layout, mCroppedBitmap);
            views.setViewVisibility(R.id.widget_layout_notification,View.INVISIBLE);

            appWidgetManager.updateAppWidget(appWidgetId,views);

            UpdateWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public void UpdateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "onUpdateWidget");
        mCredential = GoogleAccountCredential.usingOAuth2(context, Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
        new MakeRequestTask(mCredential, appWidgetId, context).execute();
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        if(intent.getAction().equals(WIDGET_CLICK)){
            int appWidgetId = intent.getIntExtra("Widget Id",0);
            String szAppWidgetId = String.valueOf(appWidgetId);
            SharedPreferences pref = context.getSharedPreferences("YOUTUBE"+szAppWidgetId,MODE_PRIVATE);
            String channelId = pref.getString("channelId","");
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.widget_layout);
            remoteViews.setViewVisibility(R.id.widget_layout_notification,View.INVISIBLE);
            Intent youtube = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/channel/"+channelId));
            context.startActivity(youtube.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

        }else{
            super.onReceive(context,intent);
        }


    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "onDeleted");
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            String szAppWidgetId = String.valueOf(appWidgetId);
            SharedPreferences pref = context.getSharedPreferences("YOUTUBE"+szAppWidgetId,MODE_PRIVATE);
            pref.edit().clear().apply();
        }

    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }


    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled");
        super.onDisabled(context);
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
    }

    private class MakeRequestTask extends AsyncTask<String, Void, ChannelListResponse> {
        com.google.api.services.youtube.YouTube mService;
        Exception mLastError = null;
        String mChannelId;
        Context context;
        int appWidgetId;

        MakeRequestTask(GoogleAccountCredential credential, int appWidgetId, Context context) {
            HttpTransport transport = new NetHttpTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("YouTube Data API")
                    .build();
            SharedPreferences pref = context.getSharedPreferences(String.valueOf(appWidgetId), MODE_PRIVATE);
            mChannelId = pref.getString("channelId", "");
            this.context = context;
            this.appWidgetId = appWidgetId;
        }

        @Override
        protected ChannelListResponse doInBackground(String... params) {
            try {
                Log.d(TAG, "doInBackground 호출됨");
                return getDataFromApi(mChannelId);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPostExecute(ChannelListResponse channelListResponse) {
            //채널 비디오 숫자를 불러온다
            BigInteger bUpdatedVideoCount = channelListResponse.getItems().get(0).getStatistics().getVideoCount();
            long updatedVideoCount = bUpdatedVideoCount.longValue();

            //저장된 비디오 숫자를 불러온다. 기본값은 0이다.
            String szAppWidgetId = String.valueOf(appWidgetId);
            SharedPreferences pref = context.getSharedPreferences("YOUTUBE"+szAppWidgetId,MODE_PRIVATE);
            long videoCount = pref.getInt("videoCount",0);

            //저장된 비디오 숫자가 0이면, 새로운 숫자를 저장만한다.
            if(videoCount == 0){
                SharedPreferences.Editor es = pref.edit();
                es.putLong("videoCount",updatedVideoCount);
                es.apply();
            //저장된 비디오 숫자보다 새로운 비디오 숫자가 크면 업데이트 표시기를 켜고 앱을 업데이트 한다.
            }else if(updatedVideoCount > videoCount){
                pref.edit().putLong("videoCount",updatedVideoCount);
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                views.setViewVisibility(R.id.widget_layout_notification, View.VISIBLE);
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId,views);
            //저장된 비디오 수보다 새로운 비디오 수가 크지 않으면 아무것도 하지 않는다.
            }else{

            }

        }

        private ChannelListResponse getDataFromApi(String id) throws IOException {
            //유튜브 DATA API 로부터 구독정보를 불러온다
            YouTube.Channels.List request;
            request = mService.channels().list("statistics");
            ChannelListResponse response = request.setId(id).execute();
            return response;
        }

    }
    public static Bitmap StringToBitmap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

}