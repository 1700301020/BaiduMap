package com.example.lbstest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/*定位方式显示不出来，非主线程更新UI导致
* 建立一个标志，定义一个全局
    StringBuilder，在获取到定位信息后新建一个Message携带1，然后主
    线程判断Message中的标志，然后再将StringBuilder传入setText()即可。*/

public class MainActivity extends AppCompatActivity {

    public LocationClient mLocationClient;
    private TextView positionText;
    public static final int UPDATE_TEXT=1;
    StringBuilder currentPosition;
    private MapView mapView;
    private BaiduMap baiduMap;
    private boolean isFirstLocate=true;

    @SuppressLint("HandlerLeak")
    private Handler handler=new Handler(){
        public void handleMessage(Message message){
            switch (message.what){
                case UPDATE_TEXT:
                    //更新UI
                    positionText.setText(currentPosition);
                    currentPosition.delete(0,currentPosition.length());
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(this.getApplication());//初始化SDK,一定要在setContentView方法之前调用
        mLocationClient=new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        setContentView(R.layout.activity_main);
        mapView=findViewById(R.id.bmapView);
        positionText=findViewById(R.id.position_text_view);
        baiduMap=mapView.getMap();
        baiduMap.setMyLocationEnabled(true);//设置我的位置可见
        List<String> permissionList=new ArrayList<>();//申请权限的数组

        //判断权限是否申请
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);//加入到权限申请数组
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()){//一次性申请
            String[] permissions=permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else{
            requestLocation();
        }
    }

    private void navigateTo(BDLocation location){
        if (isFirstLocate){
            LatLng ll=new LatLng(location.getLatitude(),location.getLongitude());
            MapStatusUpdate update= MapStatusUpdateFactory.newLatLng(ll);//移动到指定的经纬度
            baiduMap.animateMapStatus(update);
            update=MapStatusUpdateFactory.zoomTo(16f);//设置缩放级别
            baiduMap.animateMapStatus(update);
            isFirstLocate=false;
        }
        MyLocationData.Builder builder=new MyLocationData.Builder();
        builder.latitude(location.getLatitude());
        builder.longitude(location.getLongitude());
        MyLocationData locationData=builder.build();
        baiduMap.setMyLocationData(locationData);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    private void initLocation(){
        LocationClientOption option=new LocationClientOption();
        option.setScanSpan(5000);//设置时间间隔
        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);//定位模式设置成传感器模式
        option.setIsNeedAddress(true);//获取当前详细地址信息
        mLocationClient.setLocOption(option);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    for (int result:grantResults){
                        if (result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"必须同意所有权限才能使用本程序",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else {
                    Toast.makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(final BDLocation bdLocation) {
            if (bdLocation.getLocType()==BDLocation.TypeGpsLocation || bdLocation.getLocType()==BDLocation.TypeNetWorkLocation){
                navigateTo(bdLocation);
            }
            /*currentPosition=new StringBuilder();
            currentPosition.append("纬度：").append(bdLocation.getLatitude()).append("\n");//获取纬度
            currentPosition.append("经线：").append(bdLocation.getLongitude()).append("\n");//获取经度
            currentPosition.append("国家：").append(bdLocation.getCountry()).append("\n");//获取国家
            currentPosition.append("省：").append(bdLocation.getProvince()).append("\n");//获取省份
            currentPosition.append("市：").append(bdLocation.getCity()).append("\n");//获取市
            currentPosition.append("区：").append(bdLocation.getDistrict()).append("\n");//获取区
            currentPosition.append("街道：").append(bdLocation.getStreet()).append("\n");//获取街道
            currentPosition.append("定位方式：");//获取定位方式
            if (bdLocation.getLocType()==BDLocation.TypeGpsLocation){
                currentPosition.append("GPS");
            }else if(bdLocation.getLocType()==BDLocation.TypeNetWorkLocation){
                currentPosition.append("网络");
            }

            //使用Handler在主线程中跟新UI
            Message message=new Message();
            message.what=UPDATE_TEXT;
            handler.sendMessage(message);*/
        }


    }
}
