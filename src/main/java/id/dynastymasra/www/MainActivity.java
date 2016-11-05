package id.dynastymasra.www;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

import android.os.Handler;

/**
 * Author   : Dynastymasra
 * Name     : Dimas Ragil T
 * Email    : dynastymasra@gmail.com
 * LinkedIn : http://www.linkedin.com/in/dynastymasra
 * Blogspot : dynastymasra.wordpress.com | dynastymasra.blogspot.com
 */

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private EditText textPrint;
    private Button print, bluetoothOpen, blueToothClose;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Thread thread;
    private byte[] readBuffer;
    private int readBufferPos;
    private boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textPrint = (EditText) findViewById(R.id.input_print);
        bluetoothOpen = (Button) findViewById(R.id.open);
        blueToothClose = (Button) findViewById(R.id.close);
        print = (Button) findViewById(R.id.print);

        actionBarStatus(0);

        bluetoothOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findBluetooth();
                try {
                    openBluetooth();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "" + e, Toast.LENGTH_SHORT).show();
                }
            }
        });
        blueToothClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    closeBluetooth();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "" + e, Toast.LENGTH_SHORT).show();
                }
            }
        });
        print.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendData();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "" + e, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void actionBarStatus(Integer status) {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setTitle("Bluetooth Printer");
        if (status == 0) {
            actionBar.setSubtitle("No Bluetooth Connection");
        } else if (status == 1) {
            actionBar.setSubtitle("Bluetooth Device Found!");
        } else if (status == 2) {
            actionBar.setSubtitle("Bluetooth Opened!");
        } else if (status == 3) {
            actionBar.setSubtitle("Data Sent!");
        } else if (status == 4) {
            actionBar.setSubtitle("Bluetooth Closed!");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void findBluetooth() {
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Toast.makeText(getApplicationContext(), "No Bluetooth Adapter Available!", Toast.LENGTH_SHORT).show();
            }
            if (!bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, 0);
            }

            Set<BluetoothDevice> deviceSet = bluetoothAdapter.getBondedDevices();
            if (deviceSet.size() > 0) {
                for (BluetoothDevice device : deviceSet) {
                    if (device.getName().equals("P25_061146_01")) {
                        bluetoothDevice = device;
                        break;
                    }
                }
            }

            actionBarStatus(1);
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Bluetooth Connection Failed!" + ex, Toast.LENGTH_SHORT).show();
        }
    }

    public void openBluetooth() throws IOException {
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            listenFromData();
            actionBarStatus(2);
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Open Bluetooth" + ex, Toast.LENGTH_SHORT).show();
        }
    }

    private void listenFromData() {
        try {
            final Handler handler = new Handler();
            final byte delimiter = 10;
            stopWorker = false;
            readBufferPos = 0;
            readBuffer = new byte[1024];

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        try {
                            int bytesAvailable = inputStream.available();
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                inputStream.read(packetBytes);
                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == delimiter) {
                                        byte[] encodedBytes = new byte[readBufferPos];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPos = 0;

                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(getApplicationContext(), "Data" + data, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPos++] = b;
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            stopWorker = true;
                        }
                    }
                }
            });
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendData() throws IOException {
        PrinterCommandTranslator translator = new PrinterCommandTranslator();
        String msg = textPrint.getText().toString();
        print(translator.toNormalRepeatTillEnd('-'));
        print(translator.toNormalCenterAll("Example"));
        print(translator.toNormalRepeatTillEnd('-'));
        print(translator.toNormalLeft("Test : " + msg));
        print(translator.toNormalTwoColumn2(12345, "C2"));
        print(translator.toMiniLeft("TEST"));
        print(translator.toNormalTwoColumn("C1", 12345));
//            convertImagetoByte();

        actionBarStatus(3);
    }

    void closeBluetooth() throws IOException {
        try {
            stopWorker = true;
            outputStream.close();
            inputStream.close();
            bluetoothSocket.close();
            actionBarStatus(4);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void print(byte[] cmd) {
        if(outputStream != null) {
            try {
                outputStream.write(cmd);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void convertImagetoByte() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_test);
        byte[] bmp_example = {(byte) 0x1B, (byte) 0x58, (byte) 0x31, (byte) 0x24, (byte) 0x2D, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1B, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x39, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7C, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0F, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x3F, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x37, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x9F, (byte) 0x88, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x25, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0C, (byte) 0x4F, (byte) 0xF0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x27, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1E, (byte) 0x27, (byte) 0xE6, (byte) 0x00, (byte) 0x03, (byte) 0xFF, (byte) 0xFC, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0xFF, (byte) 0xF0, (byte) 0x07, (byte) 0xFF, (byte) 0xF8, (byte) 0x7F, (byte) 0xFF, (byte) 0x1E, (byte) 0x00, (byte) 0x7D, (byte) 0xFF, (byte) 0xFE, (byte) 0x0F, (byte) 0xFF, (byte) 0xC1, (byte) 0xFF, (byte) 0xF8, (byte) 0x25, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3F, (byte) 0x93, (byte) 0xCD, (byte) 0x00, (byte) 0x03, (byte) 0xFF, (byte) 0xFE, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0xFF, (byte) 0xF0, (byte) 0x07, (byte) 0xFF, (byte) 0xFC, (byte) 0x7F, (byte) 0xFF, (byte) 0x9F, (byte) 0x00, (byte) 0x7D, (byte) 0xFF, (byte) 0xFF, (byte) 0x1F, (byte) 0xFF, (byte) 0xE3, (byte) 0xFF, (byte) 0xFC, (byte) 0x10, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1F, (byte) 0xC9, (byte) 0x98, (byte) 0x80, (byte) 0x03, (byte) 0xFF, (byte) 0xFF, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0xFF, (byte) 0xF0, (byte) 0x07, (byte) 0xFF, (byte) 0xFC, (byte) 0xFF, (byte) 0xFF, (byte) 0x9F, (byte) 0x00, (byte) 0xFD, (byte) 0xFF, (byte) 0xFF, (byte) 0x3F, (byte) 0xFF, (byte) 0xE3, (byte) 0xFF, (byte) 0xFE, (byte) 0x0F, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xCF, (byte) 0xE4, (byte) 0x3C, (byte) 0x60, (byte) 0x03, (byte) 0xC0, (byte) 0x0F, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0xFF, (byte) 0xFC, (byte) 0xFF, (byte) 0xFF, (byte) 0x9F, (byte) 0x80, (byte) 0xFD, (byte) 0xFF, (byte) 0xFF, (byte) 0xBF, (byte) 0xFF, (byte) 0xF7, (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xA7, (byte) 0xF2, (byte) 0x3F, (byte) 0x30, (byte) 0x03, (byte) 0x80, (byte) 0x07, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x1C, (byte) 0xE0, (byte) 0x03, (byte) 0x9F, (byte) 0x81, (byte) 0xFD, (byte) 0xC0, (byte) 0x07, (byte) 0xB8, (byte) 0x00, (byte) 0xF7, (byte) 0x80, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x93, (byte) 0xFC, (byte) 0x3F, (byte) 0x98, (byte) 0x03, (byte) 0x80, (byte) 0x07, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x1C, (byte) 0xE0, (byte) 0x03, (byte) 0x9F, (byte) 0xC3, (byte) 0xFD, (byte) 0xC0, (byte) 0x07, (byte) 0xB8, (byte) 0x00, (byte) 0x77, (byte) 0x80, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0xC9, (byte) 0xF9, (byte) 0x9F, (byte) 0xCC, (byte) 0x03, (byte) 0xFF, (byte) 0xFE, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0xFF, (byte) 0xE0, (byte) 0x07, (byte) 0xFF, (byte) 0xFC, (byte) 0xE0, (byte) 0x03, (byte) 0x9F, (byte) 0xC3, (byte) 0xFD, (byte) 0xFF, (byte) 0xFF, (byte) 0x38, (byte) 0x00, (byte) 0x77, (byte) 0x80, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0xE4, (byte) 0x73, (byte) 0x4F, (byte) 0xE4, (byte) 0x03, (byte) 0xFF, (byte) 0xFE, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0xFF, (byte) 0xE0, (byte) 0x07, (byte) 0xFF, (byte) 0xF8, (byte) 0xE7, (byte) 0xFF, (byte) 0x9D, (byte) 0xE7, (byte) 0xBD, (byte) 0xFF, (byte) 0xFF, (byte) 0x38, (byte) 0x00, (byte) 0x77, (byte) 0x80, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0xE2, (byte) 0x72, (byte) 0x27, (byte) 0xFC, (byte) 0x03, (byte) 0xFF, (byte) 0xFE, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0xFF, (byte) 0xE0, (byte) 0x07, (byte) 0xFF, (byte) 0xF8, (byte) 0xE7, (byte) 0xFF, (byte) 0x9D, (byte) 0xE7, (byte) 0xBD, (byte) 0xFF, (byte) 0xFF, (byte) 0x38, (byte) 0x00, (byte) 0x77, (byte) 0x80, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xF1, (byte) 0x07, (byte) 0x13, (byte) 0xF8, (byte) 0x03, (byte) 0xFF, (byte) 0xFF, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0xFF, (byte) 0xE0, (byte) 0x07, (byte) 0xFF, (byte) 0xFC, (byte) 0xE7, (byte) 0xFF, (byte) 0x9C, (byte) 0xFF, (byte) 0x3D, (byte) 0xFF, (byte) 0xFF, (byte) 0x38, (byte) 0x00, (byte) 0x77, (byte) 0x80, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xF9, (byte) 0x8F, (byte) 0x89, (byte) 0xF0, (byte) 0x03, (byte) 0xC0, (byte) 0x07, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x1E, (byte) 0xE7, (byte) 0xFF, (byte) 0x9C, (byte) 0xFF, (byte) 0x3D, (byte) 0xC0, (byte) 0x07, (byte) 0xB8, (byte) 0x00, (byte) 0x77, (byte) 0x80, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x8F, (byte) 0xC4, (byte) 0xE0, (byte) 0x03, (byte) 0x80, (byte) 0x07, (byte) 0x78, (byte) 0x00, (byte) 0x70, (byte) 0x00, (byte) 0xEF, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x1E, (byte) 0xE0, (byte) 0x03, (byte) 0x9C, (byte) 0x7E, (byte) 0x3D, (byte) 0xC0, (byte) 0x03, (byte) 0xB8, (byte) 0x00, (byte) 0x77, (byte) 0x80, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x27, (byte) 0xE2, (byte) 0x00, (byte) 0x03, (byte) 0xC0, (byte) 0x07, (byte) 0x78, (byte) 0x00, (byte) 0x78, (byte) 0x01, (byte) 0xEF, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x1E, (byte) 0xE0, (byte) 0x03, (byte) 0x9C, (byte) 0x3E, (byte) 0x3D, (byte) 0xE0, (byte) 0x07, (byte) 0xBC, (byte) 0x00, (byte) 0xF7, (byte) 0xC0, (byte) 0x1E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3C, (byte) 0xD3, (byte) 0xF1, (byte) 0x00, (byte) 0x03, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F, (byte) 0xFF, (byte) 0x3F, (byte) 0xFF, (byte) 0xEF, (byte) 0xFF, (byte) 0xF0, (byte) 0x07, (byte) 0xFF, (byte) 0xFC, (byte) 0xE0, (byte) 0x03, (byte) 0x9C, (byte) 0x3C, (byte) 0x3D, (byte) 0xFF, (byte) 0xFF, (byte) 0xBF, (byte) 0xFF, (byte) 0xF3, (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x19, (byte) 0xC9, (byte) 0xFA, (byte) 0x00, (byte) 0x03, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F, (byte) 0xFF, (byte) 0x3F, (byte) 0xFF, (byte) 0xCF, (byte) 0xFF, (byte) 0xF0, (byte) 0x07, (byte) 0xFF, (byte) 0xFC, (byte) 0xE0, (byte) 0x03, (byte) 0x9C, (byte) 0x18, (byte) 0x3D, (byte) 0xFF, (byte) 0xFF, (byte) 0x1F, (byte) 0xFF, (byte) 0xE3, (byte) 0xFF, (byte) 0xFC, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xE4, (byte) 0xFC, (byte) 0x00, (byte) 0x03, (byte) 0xFF, (byte) 0xFE, (byte) 0x7F, (byte) 0xFF, (byte) 0x1F, (byte) 0xFF, (byte) 0x8F, (byte) 0xFF, (byte) 0xF0, (byte) 0x07, (byte) 0xFF, (byte) 0xF8, (byte) 0xE0, (byte) 0x03, (byte) 0x9C, (byte) 0x18, (byte) 0x3D, (byte) 0xFF, (byte) 0xFF, (byte) 0x0F, (byte) 0xFF, (byte) 0xC1, (byte) 0xFF, (byte) 0xF8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0xF2, (byte) 0x78, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0xFF, (byte) 0xC0, (byte) 0xC0, (byte) 0x01, (byte) 0x9C, (byte) 0x00, (byte) 0x19, (byte) 0xFF, (byte) 0xF8, (byte) 0x03, (byte) 0xFF, (byte) 0x00, (byte) 0x3F, (byte) 0xE0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xF9, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xFC, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3F, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3F, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1F, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        };

        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, blob);
        byte[] bitmapdata = blob.toByteArray();
        try {
            String temp = byteArrayToHexString(bitmapdata);
            byte[] tempByte = hexStringToByteArray(temp);
            Toast.makeText(getApplicationContext(), "" + tempByte[0], Toast.LENGTH_SHORT).show();
            outputStream.write(tempByte);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String byteArrayToHexString(byte[] b) throws Exception {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
