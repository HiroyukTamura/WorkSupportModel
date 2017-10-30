package com.cks.hiroyuki2.cks;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.Manifest;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Address;


import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;


import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private final int REQUEST_PERMISSION = 1000;
    //    private final static String mLogTag = "GeoJsonDemo";
    private final String[] DialogItems = {"自転車・一時", "自転車・定期", "バイク・一時", "バイク・定期", "屋根あり"};
    private Toolbar toolbar;
    private Geocoder geocoder;
    private MenuItem select;
    private MenuItem backBtn;
    private int whichDlgItm = 5;
    private final int[] jsonFiles = {R.raw.bike_once, R.raw.bike_per, R.raw.moto_once, R.raw.moto_per, R.raw.yaneari, R.raw.markers};
    private GeoJsonLayer geoJsonLayer;
    private AlertDialog dialog;
    private int dialogWidth;
    private LocationManager locationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        //ダイアログをセッティング
        setDialog();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        dialogWidth = (int) (metrics.widthPixels * 0.8);

        Tracker t = Analytics.tracker();
        t.setScreenName("MapActivity");
        t.send(new HitBuilders.ScreenViewBuilder().build());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        backBtn = menu.findItem(R.id.backBtn);
        select = menu.findItem(R.id.select);

        //サーチビューの設定
        final MenuItem menuItemSV = menu.findItem(R.id.searchview);
        final SearchView actionView = (SearchView) MenuItemCompat.getActionView(menuItemSV);
        actionView.setIconifiedByDefault(true);//ToDo これの挙動がいまいちわからん。他のエミュレータとかでも試してみるべし
        actionView.setSubmitButtonEnabled(true);


        //サーチビューが選択されたら他のアイコンを隠す
        MenuItemCompat.setOnActionExpandListener(menuItemSV, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                select.setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                select.setVisible(true);
                return true;
            }
        });


        //サーチビューにテキストを入力したら
        actionView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {

                try {
                    List<Address> addressList = geocoder.getFromLocationName(query, 1);

                    if (!addressList.isEmpty()){
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));//Todo mapを呼び出すので、if文でmapがnullのときとかのために分岐したほうがいいかも？
                    } else {
                        Toast.makeText(getApplicationContext(), "地点が見つかりませんでした", Toast.LENGTH_SHORT).show();
                    }

                } catch (IOException e) {
//                    Log.e("IOException", "onQueryTextSubmitメソッド内にて、ジオコーディングに失敗");
                    Toast.makeText(getApplicationContext(), "地点が見つかりませんでした", Toast.LENGTH_SHORT).show();
                }

                actionView.clearFocus();
                menuItemSV.collapseActionView();
                return false;

            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);

    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            //絞り込みボタンクリックしたら
            case R.id.select:
                dialog.show();
                if (dialog.getWindow() != null) {
                    WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
                    lp.width = dialogWidth;
                    dialog.getWindow().setAttributes(lp);
                }
                break;

            case R.id.searchview:
                break;

            case R.id.backBtn:
                onClickDialogItem(5);
                break;
        }

        return false;
    }



    private AlertDialog setDialog(){

        //カスタムタイトルの設定
        //setpaddingとか普通にsetTextsizeすると、ユーザー側で表示が変わってしまったりpxでしか設定できなかったりするので
        int paddingLeftRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        int paddingTopBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        TextView title = new TextView(MapsActivity.this);
        title.setBackgroundColor(ContextCompat.getColor(MapsActivity.this, R.color.colorAccent));
        title.setTextColor(Color.WHITE);
        title.setText("しぼりこみ");
        title.setPadding(paddingLeftRight, paddingTopBottom, paddingLeftRight, paddingTopBottom);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        //ダイアログ作成
        dialog = new AlertDialog.Builder(this).setCustomTitle(title).setItems(DialogItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (geoJsonLayer == null) {
                    Toast.makeText(getApplicationContext(), "しばらくしてからもう一度やり直してください", Toast.LENGTH_SHORT).show();
//                    Log.e("onClickDialogItemメソッドにて", "layer = nullのためエラー");
                } else {
                    onClickDialogItem(which);
                }
                dialog.dismiss();
            }
        }).create();

        return dialog;
    }



    private void onClickDialogItem(int which) {

        if (which == 5) {
            toolbar.setTitle(R.string.app_name);
            if (backBtn != null){
                backBtn.setVisible(false);
            }

        } else {
            toolbar.setTitle(DialogItems[which]);
            if (backBtn != null){
                backBtn.setVisible(true);
            }
        }

        if(geoJsonLayer != null) {
            if (geoJsonLayer.isLayerOnMap()) {
                geoJsonLayer.removeLayerFromMap();
            }
        }
        retrieveFileFromResource(which);

        whichDlgItm = which;
    }



    private void checkPermission() {
        // 既にパーミッションを持っている
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);

            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {

                @Override
                public boolean onMyLocationButtonClick() {

                    locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

                    //gpsかインターネット通信か利用できるものを選択
                    if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        setLocation("gps");
                    }else if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                        setLocation("network");
                    }else {
                        Toast.makeText(getApplicationContext(),"現在地が取得できませんでした",Toast.LENGTH_SHORT).show();
                    }

                    return false;
                }
            });

        }
        // パーミッションを持っていないのでダイアログを表示（以前「今後表示しない」が選択されていた場合、ダイアログを表示せずにそのままonRequestPermissionsResultへ）
        else {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
        }
    }


    private void setLocation(String provider){
        //最後のロケーションを取得
        try {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null){
                LatLng myPosition = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLng(myPosition));
            } else {
                Toast.makeText(getApplicationContext(), "現在地が取得できませんでした", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e){
            Toast.makeText(getApplicationContext(), "現在地が取得できませんでした", Toast.LENGTH_SHORT).show();
        }
    }



    // 結果の受け取り
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            // ダイアログでパーミッションが許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermission();

                // ダイアログでパーミッションが拒否された
            } else {
                //パーミッション付与を拒否したことがあり、かつ、「今後表示しない」にチェックされていなかった
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    new AlertDialog.Builder(this).setTitle("パーミッション取得エラー")
                            .setMessage("現在地が取得できないと、マップを現在地へ移動させることができません。よろしいですか？")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener(){

                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    dialogInterface.dismiss();
                                }

                            })
                            .setNeutralButton("キャンセル", new DialogInterface.OnClickListener(){

                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    checkPermission();
                                }

                            })
                            .create().show();
                }
            }
        }
    }



    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putInt("whichDlgItm",whichDlgItm);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        whichDlgItm = savedInstanceState.getInt("whichDlgItm");
    }



    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        checkPermission();

        mMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng yokohama = new LatLng(35.465798, 139.622314);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(yokohama, 14));

        onClickDialogItem(whichDlgItm);
    }



    private void retrieveFileFromResource(int which) {

        //geojsonからlayer追加
        try {
            geoJsonLayer = new GeoJsonLayer(mMap, jsonFiles[which], this);
            geoJsonLayer.addLayerToMap();

            mMap.setOnMarkerClickListener(new OnMarkerClickListener() {

                @Override
                public boolean onMarkerClick(Marker marker) {
                    GeoJsonFeature feature = geoJsonLayer.getFeature(marker);
                    String featureGeo = feature.getGeometry().toString();
                    int featureGeoLen = featureGeo.length();
//                    Log.e("君も見た", featureGeo.substring(30,featureGeoLen-4));
                    String[] intentarray = {feature.getProperty("num"),featureGeo.substring(30,featureGeoLen-4)};
                    Intent i = new Intent(MapsActivity.this, com.cks.hiroyuki2.cks.SubActivity.class);
                    i.putExtra("intentarray", intentarray);
                    startActivity(i);
                    return false;
                }
            });

        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "データの読み込みに失敗しました", Toast.LENGTH_SHORT).show();
//            Log.e(mLogTag, "GeoJSON file could not be read");
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), "データの読み込みに失敗しました", Toast.LENGTH_SHORT).show();
//            Log.e(mLogTag, "GeoJSON file could not be converted to a JSONObject");
        }
    }
}