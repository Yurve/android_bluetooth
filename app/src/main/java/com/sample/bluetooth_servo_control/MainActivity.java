package com.sample.bluetooth_servo_control;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView Status;
    Button Bluetooth_On;
    Button Bluetooth_Off;
    Button Bluetooth_Connection;
    Button Servo_Up;
    Button Servo_Left;
    Button Servo_Right;
    Button Servo_Down;

    BluetoothAdapter bluetoothAdapter;
    Set<BluetoothDevice> PairedBluetoothDevices;
    List<String> ListBluetoothDevices;

    Handler bluetoothHandler;
    ConnectedBluetoothThread threadConnectedBluetooth;
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    //final static int BT_CONNECTING_STATUS = 2;
    final static int BT_MESSAGE_READ = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //범용 고유 식별자





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Status = findViewById(R.id.Status);
        Bluetooth_On = findViewById(R.id.BluetoothOn);
        Bluetooth_Off = findViewById(R.id.BluetoothOff);
        Bluetooth_Connection = findViewById(R.id.connection);
        Servo_Up = findViewById(R.id.Up);
        Servo_Left = findViewById(R.id.Left);
        Servo_Right = findViewById(R.id.Right);
        Servo_Down = findViewById(R.id.Down);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //블루투스 키기
        Bluetooth_On.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOn();
            }
        });

        //블루투스 끄기
        Bluetooth_Off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOff();
            }
        });

        //블루투스 연결하기 (페어링이랑 다름, 이미 페어링 되었던 친구들이랑 연결하는 것)
        Bluetooth_Connection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listPairedDevices();
            }
        });

        //값 전송하기 위(1), 왼쪽(2), 오른쪽(3), 아래(4)
        Servo_Up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(threadConnectedBluetooth != null){
                    threadConnectedBluetooth.write(Integer.toString(1));
                }
            }
        });

        Servo_Left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(threadConnectedBluetooth != null){
                    threadConnectedBluetooth.write(Integer.toString(2));
                }
            }
        });

        Servo_Right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(threadConnectedBluetooth != null){
                    threadConnectedBluetooth.write(Integer.toString(3));
                }
            }
        });

        Servo_Down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(threadConnectedBluetooth != null){
                    threadConnectedBluetooth.write(Integer.toString(4));
                }
            }
        });
    }

//122~146 까지 블루투스 끄고 키기
    void bluetoothOn() throws SecurityException{
        if(bluetoothAdapter == null){
            Toast.makeText(this,"블루투스를 지원하지 않는 기기",Toast.LENGTH_SHORT).show();
        }else{
            if(bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "블루투스가 이미 활성화 됨", Toast.LENGTH_SHORT).show();
                Status.setText("활성화");
            }else{
                Toast.makeText(this, "블루투스가 활성화 되지 않음", Toast.LENGTH_SHORT).show();
                Intent intentBluetoothEnable =new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
                }
            }
        }

    void bluetoothOff() throws SecurityException{
        if(bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable();
            Toast.makeText(this,"블루투스 비활성화",Toast.LENGTH_SHORT).show();
            Status.setText("비활성화");
        }else{
            Toast.makeText(this,"블루투스가 이미 비활성화 됨",Toast.LENGTH_SHORT).show();
        }
    }

//블루투스 권환 확인하기
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == BT_REQUEST_ENABLE){
            if(resultCode == RESULT_OK){
                Toast.makeText(this,"블루투스 활성화",Toast.LENGTH_SHORT).show();
                Status.setText("활성화");
            }else if (resultCode == RESULT_CANCELED){
                Toast.makeText(this, "블루투스 비활성화",Toast.LENGTH_SHORT).show();
                Status.setText("비활성화");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


//페어링된 기기 알람창을 통해 보여주기 및 연결
    void listPairedDevices() throws SecurityException{
        if(bluetoothAdapter.isEnabled()){
            PairedBluetoothDevices = bluetoothAdapter.getBondedDevices();
            if(PairedBluetoothDevices.size() >0){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                ListBluetoothDevices = new ArrayList<>();
                for(BluetoothDevice device : PairedBluetoothDevices){
                    ListBluetoothDevices.add(device.getName());
                }
                final CharSequence[] items = ListBluetoothDevices.toArray(new CharSequence[ListBluetoothDevices.size()]);
                ListBluetoothDevices.toArray(new CharSequence[ListBluetoothDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        connectSelectDevice(items[item].toString());
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }else{
                Toast.makeText(this,"페어링 된 장치가 없습니다",Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this,"블루투스가 활성화 되지 않았습니다",Toast.LENGTH_SHORT).show();
        }
    }

//알람창으로 뜬 디바이스를 연결 및 새 쓰레드를 통해 특정 값 전송
    void connectSelectDevice (String selectedDeviceName) throws SecurityException{
            for(BluetoothDevice tempDevice : PairedBluetoothDevices){
                if(selectedDeviceName.equals(tempDevice.getName())){
                    bluetoothDevice = tempDevice;
                    break;
                }
            }
            try{
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
                bluetoothSocket.connect();
                threadConnectedBluetooth = new ConnectedBluetoothThread(bluetoothSocket);
                threadConnectedBluetooth.start();
                // record how many bytes we actually read
              //  bluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
                Toast.makeText(this,"연결 성공!",Toast.LENGTH_SHORT).show();
            }catch (IOException e){
                Toast.makeText(this,"블루투스 연결중 오류 발생",Toast.LENGTH_SHORT).show();
            }
    }

//블루투스 스레드 클래스
    public class ConnectedBluetoothThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }catch (IOException e){
                //쓰레드에는 액티비티가 없어서 this 말고 get~~ 이거씀.
                Toast.makeText(getApplicationContext(),"소켓 연결 중 오류 발생",Toast.LENGTH_SHORT).show();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

    public void run() {
        byte[] buffer =new byte[1024];
        int bytes;

        while (true){
            try{
                bytes = mmInStream.available();
                if(bytes != 0){
                    buffer = new byte[1024];
                    SystemClock.sleep(100);
                    bytes = mmInStream.available();
                    bytes = mmInStream.read(buffer,0,bytes);
                    // record how many bytes we actually read 쓰지는 않음.
                    bluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes,-1, buffer).sendToTarget();
                }
            }catch (IOException e){
                break;
            }
        }

    }

    //수정 가능할듯. 애초에 String -> Byte 객체로 보내면 될듯
    public void write(String  str){
            byte[] bytes = str.getBytes();
            try{
                mmOutStream.write(bytes);
            }catch (IOException e){
                //위와 같은 이유 얘는 액티비티가 없음
                Toast.makeText(getApplicationContext(),"데이터 전송 중 오류 발생",Toast.LENGTH_SHORT).show();
            }
    }

    //사실 안쓸 메소드, 강제종료하면 알아서 소켓도 종료
    public void cancel(){
            try{
                mmSocket.close();
            }catch (IOException e){
                Toast.makeText(getApplicationContext(),"소켓 닫는 중 오류 발생",Toast.LENGTH_SHORT).show();
            }
    }

}


}