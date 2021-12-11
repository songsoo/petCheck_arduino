package com.example.firebase_test;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MyService extends Service {
    String TAG = "movmov";
    ConnectedThread connectedThread;
    TextView contentText;

    private Messenger mClient = null;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mMessenger.getBinder();    }


    public void onCreate(){
        super.onCreate();
        Log.d(TAG,"onCreate");

    }

    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG,"onStartCommand() called");

        if(intent == null){
            return Service.START_STICKY;
        }else{
            String command = intent.getStringExtra("command");
            String name = intent.getStringExtra("name");
            Log.d(TAG,"데이터: "+command+","+name);
            String btString = intent.getStringExtra("btSocket");

            connectedThread = new ConnectedThread(BluetoothConnect.btSocket);
            connectedThread.start();
        }

        return super.onStartCommand(intent,flags,startId);
    }

    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG,"onDestroy() called");
    }

    final Handler handler = new Handler(){
        public void handleMessage(Message msg){
            Bundle bundle = msg.getData();
            Log.d(TAG,bundle.getString("result"));
            sendMsgToActivity(0,bundle.getString("result"));
        }
    };

    private final Messenger mMessenger = new Messenger(new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.w(TAG,"ControlService - message what : "+msg.what +" , msg.obj "+ msg.obj);
            mClient = msg.replyTo;  // activity로부터 가져온
            return false;
        }
    }));

    private void sendMsgToActivity(int sendValue,String value) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString("test",value);
            Message msg = Message.obtain(null, 0);
            msg.setData(bundle);
            mClient.send(msg);
            Log.d(TAG,"sent/"+value);
        } catch (RemoteException e) {
        }
    }
    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        TextView readContent;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()


            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if (bytes != 0) {

                        buffer = new byte[1024];
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        String result = new String(buffer, 0, bytes);
                        Message msg = handler.obtainMessage();
                        Bundle bundle = new Bundle();
                        bundle.putString("result",result);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

}