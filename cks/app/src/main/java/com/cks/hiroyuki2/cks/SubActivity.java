package com.cks.hiroyuki2.cks;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.mapapp.hiroyuki2.mapapp4.R.drawable.bikeonce256px;
import static com.mapapp.hiroyuki2.mapapp4.R.drawable.bikeper256px;
import static com.mapapp.hiroyuki2.mapapp4.R.drawable.motoonce256px;
import static com.mapapp.hiroyuki2.mapapp4.R.drawable.motoper256px;
import static com.mapapp.hiroyuki2.mapapp4.R.drawable.suica256px;
import static com.mapapp.hiroyuki2.mapapp4.R.drawable.umbrella256px;


public class SubActivity extends AppCompatActivity {


    private URL urljson = null;
    private HttpURLConnection connection;
    private int remainder;
    private String asyncoutput = null;
    StreetViewPanorama svp;
    StreetViewPanoramaFragment streetViewPanoramaFragment;
    private JSONObject json;
    private AlertDialog dialog;
    private int dialogWidth;

    private final TextView feetxt[] = new TextView[10];//料金部分は5行まで
    private final TextView othertxt[] = new TextView[14];//その他部分は7行まで
    private final ImageView imageView[] = new ImageView[10];//アイコンは10個まで
    final private int iconid[] = {bikeonce256px,bikeper256px,motoonce256px,motoper256px,umbrella256px,suica256px};
    final private String[] DialogItems = {"自転車・一時", "自転車・定期", "バイク・一時", "バイク・定期", "屋根あり","電子マネー可"};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        //ツールバー設定
        Toolbar toolbarSub = (Toolbar)findViewById(R.id.toolbar_sub);
        setSupportActionBar(toolbarSub);
        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle(R.string.app_name);
        }
        //ManifestでSubActivityの親にMapsActivityを指定しているので、戻るボタンを表示させるだけで動作してくれる。
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        executeGet();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        dialogWidth = (int) (metrics.widthPixels * 0.8);

        setDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sub, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()) {
            case R.id.legend:
                dialog.show();
                if (dialog.getWindow() != null) {
                    WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
                    lp.width = dialogWidth;
                    dialog.getWindow().setAttributes(lp);
                }
                break;

        }
        return false;
    }


    private AlertDialog setDialog(){

        //カスタムタイトル作成
        int paddingLeftRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        int paddingTopBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        TextView title = new TextView(SubActivity.this);
        title.setBackgroundColor(ContextCompat.getColor(SubActivity.this, R.color.colorAccent));
        title.setTextColor(Color.WHITE);
        title.setText("凡例");
        title.setPadding(paddingLeftRight, paddingTopBottom, paddingLeftRight, paddingTopBottom);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        //ダイアログ作成
        LinearLayout linearLayout = new LinearLayout(SubActivity.this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        for (int i=0; i < iconid.length; i++){
            getLayoutInflater().inflate(R.layout.dialog_legend, linearLayout);
            getLayoutInflater().inflate(R.layout.seam, linearLayout);

            LinearLayout ll = (LinearLayout) linearLayout.getChildAt(i*2);
            ImageView imageView = (ImageView)ll.getChildAt(0);
            imageView.setImageResource(iconid[i]);
            TextView textView = (TextView)ll.getChildAt(1);
            textView.setText(DialogItems[i]);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }

        dialog = new AlertDialog.Builder(this).setCustomTitle(title).setView(linearLayout).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).create();

        return dialog;
    }


    private void executeGet() {

        //インテント取得
        Intent i = this.getIntent();
        String[] intentarray = i.getStringArrayExtra("intentarray");


        //　url作成
        try {
            int quotient = Integer.parseInt(intentarray[0])/20;
            remainder = Integer.parseInt(intentarray[0])%20;
            urljson = new URL("http://wppsc.html.xdomain.jp/markerdata_packs/markerdata_pack"+quotient+".json");
            connection = null;
            MyAsyncTask task = new MyAsyncTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,urljson);//doInBackgroundメソッドが起動

        }catch(MalformedURLException e){
//            e.printStackTrace();
//            Log.v("executeGetメソッド内にて",urljson.toString()+"が間違ったurlです");
            Toast.makeText(getApplicationContext(),"通信に失敗しました。しばらくしてからもう一度やり直してください。",Toast.LENGTH_SHORT).show();
        }

    }


    //androidではメインスレッドから直接外部にhttp通信することができないので、backgroundでやることにする
    //後で知ったけど、jsonloaderっていうのが既にもうあるみたい。くそ！
    private class MyAsyncTask extends AsyncTask<URL, Long, String> implements OnStreetViewPanoramaReadyCallback {

        @Override
        protected String doInBackground(URL... params) {
//            Log.v("doInBackground開始！urlは→",urljson.toString());
            try {
                //urlに通信開始
                connection = (HttpURLConnection) urljson.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int status = connection.getResponseCode();
                if (status == HttpURLConnection.HTTP_OK) {
                    InputStream in = new BufferedInputStream(connection.getInputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(in),1000);
                    asyncoutput = br.readLine();
                }
            } catch(IOException e) {
                Toast.makeText(getApplicationContext(),"通信に失敗しました。しばらくしてからもう一度やり直してください。",Toast.LENGTH_SHORT).show();
//                e.printStackTrace();
//                Log.v("doInBackground","IOException　通信に失敗しました");
            } finally {
                if(connection != null) {
                    connection.disconnect();
                }
            }
            return asyncoutput;//もし通信がうまくいかなかったら、そのままasyncoutputが返ってくる
        }

        @Override
        protected void onPostExecute(String result){

            try {

                JSONArray jsonpack = new JSONArray(asyncoutput);
                json = jsonpack.getJSONObject(remainder);
//                Log.v("onPostExecute開始！jsonは→",json.toString());
                LinearLayout lld = (LinearLayout)findViewById(R.id.LLD);

                //駐輪場名を設置
                String name = json.getString("name");
                TextView tvname = (TextView)findViewById(R.id.tvname);
                tvname.setText(name);

                //アイコン設置
                JSONArray arprop = json.getJSONArray("prop");
                LinearLayout lla1 = (LinearLayout) findViewById(R.id.LLA1);
                int m = 0;

                for (int n = 0; n < arprop.length(); n++) {

                    if(arprop.getInt(n) == 1){
                        //もしprop=1ならm++して、imageviewを配置しgetChildAt(m)とする
                        getLayoutInflater().inflate(R.layout.icon, lla1);
                        imageView[m] = (ImageView)lla1.getChildAt(m);
                        imageView[m].setImageResource(iconid[n]);
                        m++;
                    }
                }


                if (json.getString("streetview").equals("null")) {

                    getLayoutInflater().inflate(R.layout.no_image, lld);
                    ImageView noImage = (ImageView) findViewById(R.id.noImage);
                    noImage.setImageResource(R.drawable.noimage);

                } else {

                    getLayoutInflater().inflate(R.layout.map_fragment, lld);
                    streetViewPanoramaFragment = (StreetViewPanoramaFragment) getFragmentManager().findFragmentById(R.id.fragment);
                    streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);

                }

//              id LLAは欠番。
                SetDetails(json,"fee",R.id.LLB,feetxt);

                TextView fee = (TextView)findViewById(R.id.fee);
                fee.setText("料金");

                SetDetails(json,"others",R.id.LLC,othertxt);

                TextView detail = (TextView)findViewById(R.id.detail);
                detail.setText("詳細");


            } catch (JSONException e){
                Toast.makeText(getApplicationContext(),"データを取得できませんでした。",Toast.LENGTH_SHORT).show();
//                e.printStackTrace();
//                Log.v("onPostExecute","json化に失敗しました");
            }

        }



        private void SetDetails(JSONObject js, String prop, int llid, TextView tv[]){

            try {

                JSONArray ja = js.getJSONObject("details").getJSONArray(prop);

                LinearLayout rootll = (LinearLayout) findViewById(llid);
                final int len = ja.length();

                //jsonのデータを一個ずつTextviewに配置していく
                for (int i = 0; i < len; i+=2) {

                    getLayoutInflater().inflate(R.layout.layout, rootll);
                    if (i != len-2) {
                        getLayoutInflater().inflate(R.layout.seam, rootll);
                    }
                    LinearLayout ll = (LinearLayout) rootll.getChildAt(i);//i=0,2,4と増えていくのでうまいこといく
                    tv[i] = (TextView) ll.getChildAt(0);
                    tv[i + 1] = (TextView) ll.getChildAt(1);

                    try {
                        tv[i].setText(ja.getString(i));
                        tv[i + 1].setText(ja.getString(i + 1));
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(),"データを取得できませんでした。",Toast.LENGTH_SHORT).show();
//                        e.printStackTrace();
//                        Log.e("SetDetailsメソッドのfor内にて", "jsonエラー");
                    }
                }

            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(),"データを取得できませんでした。",Toast.LENGTH_SHORT).show();
//                e.printStackTrace();
//                Log.e("SetDetailsメソッド内にて","jsonエラー");
            }
        }



        @Override
        public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {

            svp = panorama;
            svp.setZoomGesturesEnabled(true);
            svp.setUserNavigationEnabled(true);

            try {

                JSONObject svJO = json.getJSONObject("streetview");
                String location = json.getJSONObject("streetview").getJSONArray("location").toString();
                String location2[] = location.substring(1, location.length() - 1).split(",");

                svp.setPosition(new LatLng(Double.parseDouble(location2[0]), Double.parseDouble(location2[1])));
                StreetViewPanoramaCamera camera = new StreetViewPanoramaCamera.Builder()
                        .zoom(Float.parseFloat(svJO.getString("zoom")))
                        .bearing(Float.parseFloat(svJO.getString("heading")))
                        .tilt(Float.parseFloat(svJO.getString("pitch")))
                        .build();
                svp.animateTo(camera, 500);


            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(),"データを取得できませんでした。",Toast.LENGTH_SHORT).show();
//                e.printStackTrace();
//                Log.e("JSON化に失敗","onStreetViewPanoramaReadyメソッド内");
            }

        }
    }
}
