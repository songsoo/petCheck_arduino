package com.example.firebase_test;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnect extends AppCompatActivity implements OnChartValueSelectedListener , SwipeRefreshLayout.OnRefreshListener{

    String TAG = "movmov";
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    TextView textStatus,stressText,RMSSDText;
    Button btnMenu;
    ListView listView1, listView2;
    LineChart bpmChart,RMSSDChart;
    ImageView stressImg;
    NavigationView navigationview;

    int count = 20;
    public static int i = 0;

    private Messenger mServiceMessenger = null;

    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> btArrayAdapter1,btArrayAdapter2;
    ArrayList<String> deviceAddressArray1, deviceAddressArray2;
    DrawerLayout drawerLayout;
    SwipeRefreshLayout swipeRefreshLayout;

    public static ArrayList<Entry> bpm_values = new ArrayList<>();
    public static ArrayList<Entry> RMSSD_values = new ArrayList<>();

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("dog Check");

    private final static int REQUEST_ENABLE_BT = 1;
    public static BluetoothSocket btSocket = null;
    public static boolean flag = false;
    public static String name;
    public static double RMSSD=-1;
    public static int stressStatus=-1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        Log.d(TAG,"protocol: ");
        Log.d(TAG,"protocol: ");
        Log.d(TAG,"protocol: ");
        setVariables();
        setBluetooth();
        createChart();

    }



    @SuppressLint("UseCompatLoadingForDrawables")
    public void setVariables(){
        textStatus = (TextView) findViewById(R.id.text_status);
        stressText = (TextView) findViewById(R.id.stressText);
        RMSSDText = findViewById(R.id.RMSSDText);
        btnMenu = (Button) findViewById(R.id.btn_menu);
        listView1 = (ListView) findViewById(R.id.listview1);
        listView2 = (ListView) findViewById(R.id.listview2);
        stressImg = findViewById(R.id.stressImg);

        bpmChart = findViewById(R.id.bpm_chart);
        RMSSDChart = findViewById(R.id.RMSSD_chart);
        drawerLayout = findViewById(R.id.bluetooth_Layout);

        btnMenu.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                pairedDevices();
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        swipeRefreshLayout = findViewById(R.id.swipeLayout);
        swipeRefreshLayout.setOnRefreshListener(this);

        NavigationView navigationview = findViewById(R.id.navigationView);
        navigationview.setItemIconTintList(null);

        if(RMSSD!=-1){
            RMSSDText.setText(Double.toString(RMSSD));
        }
        if(stressStatus!=-1){
            switch(stressStatus){
                case 0:
                    stressImg.setImageResource(R.drawable.happy);
                    stressText.setText("Low");
                    break;
                case 1:
                    stressImg.setImageResource(R.drawable.normal);
                    stressText.setText("Normal");
                    break;
                case 2:
                    stressImg.setImageResource(R.drawable.sad);
                    stressText.setText("High");
                    break;
            }
        }

    }

    public void setBluetooth(){

        String[] permission_list = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions(BluetoothConnect.this, permission_list, 1);

        // Show paired devices
        btArrayAdapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        btArrayAdapter2 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        deviceAddressArray1 = new ArrayList<>();
        deviceAddressArray2= new ArrayList<>();

        listView1.setAdapter(btArrayAdapter1);
        listView2.setAdapter(btArrayAdapter2);

        listView1.setOnItemClickListener(new myOnItemClickListener());
        listView2.setOnItemClickListener(new myOnItemClickListener());

        // Enable bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        if(flag){
            textStatus.setText("connected to "+name);
            Intent intent = new Intent(getApplicationContext(),MyService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

    }

    final Handler handler = new Handler(){
        public void handleMessage(Message msg){
            Bundle bundle = msg.getData();
        }
    };

    public void pairedDevices(){
        Log.d(TAG,"paired!");
        btArrayAdapter1.clear();
        if(deviceAddressArray1!=null && !deviceAddressArray1.isEmpty()){ deviceAddressArray1.clear(); }
        pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                btArrayAdapter1.add(deviceName);
                deviceAddressArray1.add(deviceHardwareAddress);
            }
        }
    }


    public void searchDevices(){
        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        } else {
            if (btAdapter.isEnabled()) {
                btAdapter.startDiscovery();
                btArrayAdapter2.clear();
                if (deviceAddressArray2 != null && !deviceAddressArray2.isEmpty()) {
                    deviceAddressArray2.clear();
                }
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(receiver, filter);
            } else {
                Toast.makeText(getApplicationContext(), "bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();

                if(deviceName!=null) {
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    btArrayAdapter2.add(deviceName);
                    deviceAddressArray2.add(deviceHardwareAddress);
                    btArrayAdapter2.notifyDataSetChanged();
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    @Override
    public void onRefresh() {
        searchDevices();
        swipeRefreshLayout.setRefreshing(false);
    }

    public class myOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Toast.makeText(getApplicationContext(), btArrayAdapter1.getItem(position), Toast.LENGTH_SHORT).show();


            name = btArrayAdapter1.getItem(position); // get name
            final String address = deviceAddressArray1.get(position); // get address
            if (address.length() != 0){
                textStatus.setText("try...");
                flag = true;
                BluetoothDevice device = btAdapter.getRemoteDevice(address);

                // create & connect socket
                try {
                    btSocket = createBluetoothSocket(device);
                    btSocket.connect();
                } catch (IOException e) {
                    flag = false;
                    textStatus.setText("connection failed!");
                    e.printStackTrace();
                }

                // start bluetooth communication
                if (flag) {
                    textStatus.setText("connected to " + name);
                    Intent intent = new Intent(getApplicationContext(), MyService.class);
                    startService(intent);
                    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            }
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    private Messenger mMessenger = new Messenger(new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String value = msg.getData().getString("test");
            String protoArr[] = value.split("#");
            for(String protocols : protoArr) {
                Log.d(TAG,protocols);
                String resultArr[] = protocols.split("/");
                switch (resultArr[0]) {
                    case "beat":
                        int bpm = Integer.valueOf(resultArr[1]);
                        Log.d(TAG, "BPM: " + bpm);
                        setData(count, bpm,0);
                        bpmChart.invalidate();
                        break;
                    case "RMSSD":
                        double PreRMSSD = Double.valueOf(resultArr[1]);
                        double CurRMSSD = Double.valueOf(resultArr[2]);
                        Log.d(TAG, "RMSSD: " + CurRMSSD);
                        RMSSD = CurRMSSD;
                        setData(count, (int)CurRMSSD,1);
                        RMSSDChart.invalidate();
                        if (CurRMSSD > 500) {
                            stressImg.setImageResource(R.drawable.sad);
                            stressText.setText("HIGH");
                            RMSSDText.setText(Double.toString(CurRMSSD));
                            stressStatus = 2;
                        } else if (CurRMSSD > 200) {
                            stressImg.setImageResource(R.drawable.normal);
                            stressText.setText("Normal");
                            RMSSDText.setText(Double.toString(CurRMSSD));
                            stressStatus = 1;
                        } else {
                            stressImg.setImageResource(R.drawable.happy);
                            stressText.setText("LOW");
                            RMSSDText.setText(Double.toString(CurRMSSD));
                            stressStatus = 0;
                        }
                        break;
                    case "GYRO":
                        break;
                }
            }
            return false;
        }
    }));

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mServiceMessenger = new Messenger(iBinder);
            try {
                Message msg = Message.obtain(null,0);
                msg.replyTo = mMessenger;
                mServiceMessenger.send(msg);
            }
            catch (RemoteException e) {
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    private void createChart(){
        {   // // Chart Style // //
            bpmChart.setBackgroundColor(Color.WHITE);
            bpmChart.getDescription().setEnabled(false);
            bpmChart.setTouchEnabled(true);
            bpmChart.setOnChartValueSelectedListener(this);
            bpmChart.setDrawGridBackground(false);
            bpmChart.setDragEnabled(true);
            bpmChart.setScaleEnabled(true);
            bpmChart.setPinchZoom(true);

            RMSSDChart.setBackgroundColor(Color.WHITE);
            RMSSDChart.getDescription().setEnabled(false);
            RMSSDChart.setTouchEnabled(true);
            RMSSDChart.setOnChartValueSelectedListener(this);
            RMSSDChart.setDrawGridBackground(false);
            RMSSDChart.setDragEnabled(true);
            RMSSDChart.setScaleEnabled(true);
            RMSSDChart.setPinchZoom(true);
        }

        XAxis bpm_xAxis = bpmChart.getXAxis();
        bpm_xAxis.setEnabled(false);

        YAxis bpm_yAxis;
        {
            bpm_yAxis = bpmChart.getAxisLeft();
            bpmChart.getAxisRight().setEnabled(false);
            bpm_yAxis.enableGridDashedLine(10f, 10f, 0f);
            bpm_yAxis.setAxisMaximum(250f);
            bpm_yAxis.setAxisMinimum(0f);
        }

        XAxis RMSSD_xAxis = RMSSDChart.getXAxis();
        RMSSD_xAxis.setEnabled(false);

        YAxis RMSSD_yAxis;
        {
            RMSSD_yAxis = RMSSDChart.getAxisLeft();
            RMSSDChart.getAxisRight().setEnabled(false);
            RMSSD_yAxis.enableGridDashedLine(10f, 10f, 0f);
            RMSSD_yAxis.setAxisMaximum(1000f);
            RMSSD_yAxis.setAxisMinimum(0f);
        }

        if(bpm_values.size() == 0) {
            setData(count, 0, 0);
            setData(count, 10, 0);
            setData(count, 20, 0);
            setData(count, 180, 0);

            setData(count, 0, 1);
            setData(count, 10, 1);
            setData(count, 20, 1);
            setData(count, 180, 1);
        }else{
            setData(count, -1, 0);
            setData(count, -1, 1);
        }

        bpmChart.animateX(1500);

        Legend bpm_l = bpmChart.getLegend();
        bpm_l.setForm(Legend.LegendForm.LINE);

        RMSSDChart.animateX(1500);
        Legend RMSSD_l = RMSSDChart.getLegend();
        RMSSD_l.setForm(Legend.LegendForm.LINE);
    }

    private void setChart() {
        LineChart lineChart = bpmChart;
        lineChart.invalidate(); //차트 초기화 작업
        lineChart.clear();
        ArrayList<Entry> values = new ArrayList<>();
    }

    private void setData(int count, int val, int chartNum) {

        ArrayList<Entry> values = new ArrayList<>();
        LineChart chart;
        String Label;
        int line_color;
        Drawable graph_color;

        if(chartNum ==0) {
            chart = bpmChart;
            values = bpm_values;
            Label = "BPM";
            line_color = ContextCompat.getColor(this,R.color.red);
            graph_color=ContextCompat.getDrawable(this, R.drawable.fade_red);
        }else{
            chart = RMSSDChart;
            values = RMSSD_values;
            Label = "RMSSD";
            line_color = ContextCompat.getColor(this,R.color.teal_line);
            graph_color=ContextCompat.getDrawable(this, R.drawable.fade_teal);

        }
        if(val != -1) {
            values.add(new Entry(i++, val, getResources().getDrawable(R.drawable.star)));
            if (values.size() > count) {
                values.remove(0);
            }
        }

        LineDataSet set1;

        if (chart.getData() != null &&
                chart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            set1.notifyDataSetChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {

            set1 = new LineDataSet(values, Label);

            set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set1.setCubicIntensity(0.1f);
            set1.setDrawIcons(false);

            set1.setColor(line_color);
            set1.setCircleColor(line_color);

            // line thickness and point size
            set1.setLineWidth(2f);
            set1.setCircleRadius(2f);



            // draw points as solid circles
            set1.setDrawCircleHole(false);

            // customize legend entry
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);

            // text size of values
            set1.setValueTextSize(9f);
            set1.setValueTextColor(Color.BLACK);

            set1.enableDashedHighlightLine(10f, 5f, 0f);

            set1.setDrawFilled(true);
            set1.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return chart.getAxisLeft().getAxisMinimum();
                }
            });


            if (Utils.getSDKInt() >= 18) {
                Drawable drawable = graph_color;
                set1.setFillDrawable(drawable);
            } else {
                set1.setFillColor(Color.BLACK);
            }

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);

            LineData data = new LineData(dataSets);

            chart.setData(data);
        }
    }

}