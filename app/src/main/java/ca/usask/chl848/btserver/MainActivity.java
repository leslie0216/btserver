package ca.usask.chl848.btserver;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;


public class MainActivity extends Activity implements View.OnClickListener {

    private MainView m_mainView;

    private ServerThread m_serverThread = null;
    private ArrayList<ConnectedThread> m_connectedThreadList = new ArrayList<>();

    private BluetoothAdapter m_bluetoothAdapter = null;

    private BluetoothServerSocket m_serverSocket = null;
    private ArrayList<BluetoothSocket> m_socketList = new ArrayList<>();

    private UUID m_UUID = UUID.fromString("8bb345b0-712a-400a-8f47-6a4bda472638");

    private static int REQUEST_ENABLE_BLUETOOTH = 1;

    private ArrayList m_messageList = new ArrayList();

    private boolean m_isOn;

    long startTime = 0;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            m_mainView.invalidate();
            if (m_messageList.size() == 0) {
                addMessage(m_mainView.cookMessage());
            }

            sendMessage();
            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setStatus("");

        Button btn=(Button)this.findViewById(R.id.btn_switch);
        btn.setOnClickListener(this);

        m_mainView = new MainView(this);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.addContentView(m_mainView, new LinearLayout.LayoutParams(displayMetrics.widthPixels, (int) (displayMetrics.heightPixels * 0.7f)));

        m_isOn = false;

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setStatus(String status){
        TextView v = (TextView)this.findViewById(R.id.txt_status_cnt);
        v.setText(status + " : " + m_connectedThreadList.size());
    }

    @Override
    protected void onResume() {
        if (m_isOn) {
            setupThread();
        }
        super.onResume();
    }

    @Override
    protected void onRestart() {
        if (m_isOn) {
            setupThread();
        }
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        m_isOn = false;
        stopThreads();
        super.onDestroy();
    }

    private void stopThreads() {
        if(m_bluetoothAdapter!=null&&m_bluetoothAdapter.isDiscovering()){
            m_bluetoothAdapter.cancelDiscovery();
        }

        if (m_serverThread != null) {
            m_serverThread.cancel();
            m_serverThread = null;
        }

        int size = m_connectedThreadList.size();
        if (size != 0) {
            for (int i = 0; i<size; ++i) {
                ConnectedThread ct = m_connectedThreadList.get(i);
                if (ct != null) {
                    ct.cancel();
                    ct.m_isStoppedByServer = true;
                }
            }
        }
        m_connectedThreadList.clear();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setStatus("Not Connected");
            }
        });
        m_isOn = false;
    }

    @Override
    public void onClick(View v) {
        Button btn=(Button)this.findViewById(R.id.btn_switch);
        if (!m_isOn) {
            setupBluetooth();
            m_isOn = true;
            btn.setText("Stop Server");
        } else {
            stopThreads();
            m_isOn = false;
            btn.setText("Start Server");
        }
    }

    public void showToast(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setupBluetooth(){
        setStatus("Not Connected");

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(m_bluetoothAdapter != null){  //Device support Bluetooth
            if(!m_bluetoothAdapter.isEnabled()){
                Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
            }
            else {
                setupThread();
            }
        }
        else{   //Device does not support Bluetooth

            Toast.makeText(this,"Bluetooth not supported on device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                setupThread();
            }
            else {
                showToast("Bluetooth is not enable on your device");
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setupThread(){
        if (m_serverThread == null) {
            m_serverThread = new ServerThread();
            m_serverThread.start();
        }
    }

    private class ConnectedThread extends Thread {
        BluetoothSocket m_connectedSocket;
        InputStream m_inStream;
        OutputStream m_outStream;
        String m_deviceName;
        boolean m_isStoppedByServer;
        public ConnectedThread(BluetoothSocket socket) {
            try {
                m_connectedSocket = socket;
                m_inStream = m_connectedSocket.getInputStream();
                m_outStream = m_connectedSocket.getOutputStream();
                m_deviceName = "";
                m_isStoppedByServer = false;
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    // Read from the InputStream
                    if( m_inStream != null && (bytes = m_inStream.read(buffer)) > 0 )
                    {
                        byte[] buf_data = new byte[bytes];
                        for(int i=0; i<bytes; i++)
                        {
                            buf_data[i] = buffer[i];
                        }
                        String msg = new String(buf_data);
                        String name = receiveBTMessage(msg);
                        if (m_deviceName.equalsIgnoreCase("")) {
                            m_deviceName = name;
                        }
                    }
                } catch (IOException e) {
                    cancel();
                    if (!m_isStoppedByServer) {
                        m_connectedThreadList.remove(this);
                    }
                    m_mainView.removeDevice(m_deviceName);
                    if (m_connectedThreadList.isEmpty()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setStatus("Not Connected");
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setStatus("Connected");
                            }
                        });
                    }
                    break;
                }
            }
        }

        public void write(String msg) {
            try {
                if (m_outStream != null) {
                    m_outStream.write(msg.getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                if (m_inStream != null) {
                    m_inStream.close();
                    m_inStream = null;
                }
                if (m_outStream != null) {
                    m_outStream.flush();
                    m_outStream.close();
                    m_outStream = null;
                }
                if (m_connectedSocket != null) {
                    m_connectedSocket.close();
                    m_connectedSocket = null;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerThread extends Thread {
        public ServerThread () {
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setStatus("Connecting...");
                    }
                });
                //setStatus("Connecting...");
                m_serverSocket = m_bluetoothAdapter.listenUsingRfcommWithServiceRecord("BTServer", m_UUID);
            } catch (IOException e) {
                m_serverSocket = null;
            }
        }
        @Override
        public void run() {
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                BluetoothSocket socket;
                if (m_serverSocket != null) {
                    try {
                        socket = m_serverSocket.accept();
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                else {
                    try {
                        m_serverSocket = m_bluetoothAdapter.listenUsingRfcommWithServiceRecord("BTServer", m_UUID);
                    } catch (IOException e) {
                        m_serverSocket = null;
                    }
                    continue;
                }

                if (socket != null) {

                    m_socketList.add(socket);
                    ConnectedThread ct = new ConnectedThread(socket);
                    ct.start();
                    m_connectedThreadList.add(ct);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setStatus("Connected");
                        }
                    });
                }
            }
        }

        public void cancel() {
            try {
                if (m_serverSocket != null) {
                    m_serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendBTMessage(String msg) {
        if (!m_connectedThreadList.isEmpty()) {
            int size = m_connectedThreadList.size();

            for (int i = 0; i<size; ++i) {
                m_connectedThreadList.get(i).write(msg);
            }
        }
    }

    public void addMessage(String msg) {
        m_messageList.add(msg);
    }

    public void sendMessage(){
        if (m_messageList.size() != 0) {
            String msg = (String)m_messageList.get(0);
            m_messageList.remove(0);
            sendBTMessage(msg);
        }
    }

    private String receiveBTMessage(String msg){
        String rt = "";
        try {
            JSONObject jsonObject = new JSONObject(msg);

            String senderName = jsonObject.getString("name");
            float senderX = (float) jsonObject.getDouble("x");
            float senderY = (float) jsonObject.getDouble("y");
            float senderZ = (float) jsonObject.getDouble("z");
            int color = jsonObject.getInt("color");

            String receiverName = "";
            String ballId = "";
            int ballColor = 0;

            boolean isSendingBall = jsonObject.getBoolean("isSendingBall");
            if (isSendingBall) {
                receiverName = jsonObject.getString("receiverName");
                ballId = jsonObject.getString("ballId");
                ballColor = jsonObject.getInt("ballColor");
            }

            m_mainView.updateClientInfo(senderName, color, senderX, senderY, senderZ, isSendingBall, ballId, ballColor, receiverName);

            rt = senderName;
        }catch (JSONException e) {
            e.printStackTrace();
        }

        return rt;
    }
}
