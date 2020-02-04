package com.giyoon.widgetforyoutube.widget;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.giyoon.widgetforyoutube.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Subscription;
import com.google.api.services.youtube.model.SubscriptionListResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class WidgetConfigure extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    //구글인증 API


     GoogleAccountCredential mCredential;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { YouTubeScopes.YOUTUBE_READONLY };

    private static final Long MAX_REQUEST_RESULTS = 50L;

    private static final String TAG = "mWidget Configure";
    private static final String WIDGET_CLICK = "com.giyoon.widgetforyoutube.CLICK";


    //앱구성정보
    int appWidgetId;
    ProgressBar progressBar;
    androidx.appcompat.widget.Toolbar tb;
    androidx.recyclerview.widget.RecyclerView recyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        // 화면을 구성한다.
        setContentView(R.layout.widget_configure_layout);
        recyclerView = findViewById(R.id.recyclerview);
        progressBar = findViewById(R.id.progressBar1);
        tb = findViewById(R.id.toolbar);
        tb.setTitle(R.string.channel_select_toolbar);
        tb.setTitleTextColor(getResources().getColor(R.color.colorWhite));
        setSupportActionBar(tb);
        recyclerView.setLayoutManager(new LinearLayoutManager(getParent()));
        RecyclerView.RecyclerListener listener = new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {

            }
        };
        recyclerView.setRecyclerListener(listener);

        Log.d(TAG,"화면 구성 완료");


        //앱위젯매니저에서 구성하는 위젯의 아이디를 부여받는다
        Intent intent = getIntent();
        if(intent.hasExtra("from")) {
            //메인 액티비티에서 온 경우 아래 코드를 건너 뛴다.
        }else{
            // 위젯 추가 액티비티에서 온 경우 아래 코드를 한다.
            Bundle extras = intent.getExtras();
            if (extras != null) {
                appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                Log.d(TAG, "앱위젯 아이디 받아옴");
            }

            // 앱위젯매니저에서 아이디를 부여받지 못하면 구성화면을 종료시킨다 (위젯 생성되지 않음)
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.d(TAG, "앱위젯 아이디 받기 실패");
                finish();
            }
        }
        //구글 계정 인증 획득한다.
        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
        getResultsFromApi();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_configure,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_account :
                    // 계정 선택화면 호출
                    startActivityForResult(
                            mCredential.newChooseAccountIntent(),
                            REQUEST_ACCOUNT_PICKER);
                    // 계정
                    new MakeRequestTask(mCredential).execute();
                break;

            default:
                break;
        }
        return true;
    }

    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            Toast.makeText(this,"No network connection available",Toast.LENGTH_LONG).show();
        } else {
            new MakeRequestTask(mCredential).execute();
            Log.d(TAG,"MakeRequestTask가 execute 됨");
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
    int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this,"This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.",Toast.LENGTH_LONG).show();
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
    @NonNull String[] permissions,
    @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
    final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                WidgetConfigure.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the YouTube Data API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, SubscriptionListResponse> {
        com.google.api.services.youtube.YouTube mService;
        Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = new NetHttpTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                        mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("YouTube Data API Android Quickstart")
                    .build();
            Log.d(TAG,"MakeRequestTask 인스탄스 생성됨");
        }

        /**
         * Background task to call YouTube Data API.
         * @param params no parameters needed for this task.
         * @return SubscriptionListResponse
         */
        @Override
        protected SubscriptionListResponse doInBackground(Void... params) {
            try {
                Log.d(TAG,"doInBackground 호출됨");
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch information about the "GoogleDevelopers" YouTube channel.
         * @return List of Strings containing information about the channel.
         */
        private SubscriptionListResponse getDataFromApi() throws IOException{
            //유튜브 DATA API 로부터 구독정보를 불러온다
            YouTube.Subscriptions.List request;
            request = mService.subscriptions().list("snippet");
            SubscriptionListResponse response = request.setMaxResults(MAX_REQUEST_RESULTS).setMine(true).setOrder("alphabetical").execute();
            Log.d(TAG,"유튜브 API 쿼리전송 및 응답 완료");

            return response;
        }

        @Override
        protected void onPreExecute() {
            Log.d(TAG,"onPreExecute 호출됨");
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(final SubscriptionListResponse output) {
            progressBar.setVisibility(View.INVISIBLE);
            if(output == null | output.size() == 0){
                Toast.makeText(getApplicationContext(),"No Results returned",Toast.LENGTH_LONG).show();
            } else {
                onResume();
                Log.d(TAG,"백그라운드 완료 후, 메인 스레드 재시작");
                //리사이클러뷰에 구독정보를 붙인다.
                List<Subscription> channelList = output.getItems();
                final WidgetAdapter adapter = new WidgetAdapter(channelList, getApplicationContext());
                Log.d(TAG,"어댑터 생성 및 쿼리응답 바인딩 완료");
                recyclerView.setAdapter(adapter);
                Log.d(TAG,"리사이클러뷰에 어댑터 등록 완료");
                adapter.setOnItemClickListener(new WidgetAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int pos) {

                        if(getIntent().hasExtra("from")){
                            Intent intent = new Intent();
                            intent.putExtra("mChannelInfo","https://www.youtube.com/channel/"+adapter.getmItems(pos).getSnippet().getResourceId().getChannelId());
                            intent.putExtra("mThumbnailUri",adapter.getmItems(pos).getSnippet().getThumbnails().getDefault().getUrl());
                            intent.putExtra("mTitle",adapter.getmItems(pos).getSnippet().getTitle());
                            setResult(RESULT_OK,intent);
                            finish();
                        }else {

                            final Context context = WidgetConfigure.this;

                            Bitmap mBitmap = adapter.getBitmap(pos);
                            Bitmap mCroppedBitmap = null;
                            try {
                                mCroppedBitmap = getCroppedBitmap(mBitmap);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            String szAppWidgetId = String.valueOf(appWidgetId);
                            SharedPreferences pref = getSharedPreferences("YOUTUBE" + szAppWidgetId, MODE_PRIVATE);
                            SharedPreferences.Editor es = pref.edit();

                            es.putString("channelTitle", adapter.getmItems(pos).getSnippet().getTitle());
                            es.putString("channelThumbnail", adapter.getmItems(pos).getSnippet().getThumbnails().getDefault().getUrl());
                            es.putString("channelId", adapter.getmItems(pos).getSnippet().getResourceId().getChannelId());
                            es.putString("channelPublishedAt", adapter.getmItems(pos).getSnippet().getPublishedAt().toString());
                            es.putString("channelThumbnailBitmap", BitmapToString(mCroppedBitmap));

                            es.apply();


                            BroadcastReceiver br = new WidgetProvider();
                            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                            filter.addAction(WIDGET_CLICK);
                            getApplicationContext().registerReceiver(br, filter);

                            Intent intent = new Intent(WIDGET_CLICK);
                            intent.putExtra("Widget Id", appWidgetId);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_IMMUTABLE);


                            // 앱구성을 완료한다.
                            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
                            views.setImageViewBitmap(R.id.widget_layout, mCroppedBitmap);
                            views.setViewVisibility(R.id.widget_layout_notification, View.INVISIBLE);

                            appWidgetManager.updateAppWidget(appWidgetId, views);
                            Log.d(TAG, "앱구성을 완료함");

                            // 앱 구성 반환 인텐트를 만든다.
                            Intent resultValue = new Intent();
                            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                            setResult(RESULT_OK, resultValue);
                            Log.d(TAG, "앱 구성 반환 인텐트를 설정 완료하고 구성액티비티 종료함");
                            finish();
                        }
                    }
                });
                Log.d(TAG,"리사이클러뷰 리스너 등록 완료");
            }
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            WidgetConfigure.REQUEST_AUTHORIZATION);
                }
            }
        }
    }
    public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
    public static String BitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, baos);
        byte[] bytes = baos.toByteArray();
        String temp = Base64.encodeToString(bytes, Base64.DEFAULT);
        return temp;
    }
}