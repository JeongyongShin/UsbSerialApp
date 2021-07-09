package sec.unitile.usb_serial_kai_morich;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;

import sec.unitile.usb_serial_kai_morich.util.HexDump;

public class UsbSerialService  implements SerialInputOutputManager.Listener{
    private static final String TAG = "Unitile-Serial";
    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    static class ListItem {
        UsbDevice device;
        int port;
        UsbSerialDriver driver;

        ListItem(UsbDevice device, int port, UsbSerialDriver driver) {
            this.device = device;
            this.port = port;
            this.driver = driver;
        }
    }

    private Context mContext;
    private final ArrayList<ListItem> listItems = new ArrayList<>();
    private int baudRate = 115200;
    private int venderId, deviceId, portNum;
    private boolean withIoManager = true;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private Handler mainLooper;
    public BroadcastReceiver broadcastReceiver;
    private UsbSerialPort usbSerialPort;
    private SerialInputOutputManager usbIoManager;
    public boolean connected = false;
    private static final int WRITE_WAIT_MILLIS = 2000;
    private final int MOTOR_DELAY = 10;

    public  static int MIN_X = -150;
    public  static int MAX_X = 150;
    public  static int MIN_Y = -60;
    public  static int MAX_Y = 120;

    public UsbSerialService(Context context){
        this.mContext = context;

    }


    void init() {
        Log.i(TAG, "init( )  ");
        mainLooper = new Handler(Looper.getMainLooper());
        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        UsbSerialProber usbDefaultProber = UsbSerialProber.getDefaultProber();
        UsbSerialProber usbCustomProber = CustomProber.getCustomProber();
        listItems.clear();
        for(UsbDevice device : usbManager.getDeviceList().values()) {
            UsbSerialDriver driver = usbDefaultProber.probeDevice(device);
            Log.i(TAG, "init( ) --> for loop...  --> driver  : " + driver);
            if(driver == null) {
                driver = usbCustomProber.probeDevice(device);
            }
            if(driver != null) {
                for(int port = 0; port < driver.getPorts().size(); port++)
                    listItems.add(new ListItem(device, port, driver));
            } else {
                listItems.add(new ListItem(device, 0, null));
            }
        }

        ListItem item = listItems.get(0);
        portNum = item.port;
        deviceId = item.device.getDeviceId();
        venderId = item.device.getVendorId();
        Log.i(TAG, "init( ) --> final  --> Vendor id : "  +item.device.getVendorId()+ ", Product ID: " +item.device.getProductId()+ ", deviceId :" +deviceId);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };

        mContext.registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
        if(usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted)
            mainLooper.post(this::connect);

    }

    private void connect() {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

        Log.i(TAG, "connect( )  start...   @UsbSerialService.java  , usbManager : " +usbManager );
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId) {
                device = v;
                Log.i(TAG, "connect( )  if(v.getDeviceId()...   @UsbSerialService.java  , device : " + device);
            }
        if(device == null) {
            Log.i(TAG, "connect( )  ..   connection failed: device not found" );
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            Log.i(TAG, "connect( )  ..   connection failed: no driver for device" );
            return;
        }
        if(driver.getPorts().size() < portNum) {
            Log.i(TAG, "connect( )  ..   connection failed: not enough ports at device" );
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        Log.i(TAG, "connect( )  getPorts().get(portNum); ...   @UsbSerialService.java  , usbSerialPort : " + usbSerialPort);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                Log.i(TAG, "connect( )  ..   connection failed: permission denied" );
            else
                Log.i(TAG, "connect( )  ..   connection failed: open failed" );
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);

            Log.i(TAG, "connect( )  usbSerialPort.setParameters() ...   @UsbSerialService.java  , baudRate : " + baudRate+ ", UsbSerialPort : " + UsbSerialPort.PARITY_NONE );
            if(withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            Log.i(TAG, "connect( )  ..   connected!!!" );
            connected = true;

        } catch (Exception e) {
            Log.i(TAG, "connect( )  ..   connection failed!!!" );
            disconnect();
        }
    }


    @Override
    public void onNewData(byte[] data) {
        mainLooper.post(() -> {
   //         receive(data);
        });
    }

    @Override
    public void onRunError(Exception e) {
        Log.i(TAG, "onRunError( )   @UsbSerialService.java  " );
        mainLooper.post(() -> {
            disconnect();
        });
    }

    private void receive(byte[] data) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        spn.append("receive " + data.length + " bytes\n");
        if(data.length > 0)  spn.append(HexDump.dumpHexString(data)).append("\n");
        Log.i(TAG, "receive( ) ...   @UsbSerialService.java , spn : " +spn );
    }

    public void disconnect() {
        Log.i(TAG, "disconnect( ) ...   @UsbSerialService.java  "  );
        connected = false;
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;
    }


    public void moveX(double pos) {
        if (checkUsbSerial()) {
            moveMotor(true, pos);
        }
    }
    public void moveY(double pos) {
        if (checkUsbSerial()) {
            moveMotor(false, pos);
        }
    }

    public boolean checkUsbSerial() {
        if(venderId == 4292)  return  true;
        else return false;
    }

    public void moveMotor(boolean x, double pos) {
        if (checkUsbSerial()) {
            String cmd = "";
            if(x) {
                cmd = "x," + pos +"," + MOTOR_DELAY + "\n";
                if(pos < MIN_X || pos > MAX_X) {
                    Log.e(TAG, "X ,Range over");
                    return;
                }
            }
            else {
                cmd = "y," + pos +"," + MOTOR_DELAY + "\n";
                if(pos < MIN_Y || pos > MAX_Y) {
                    Log.e(TAG, "Y ,Range over");
                    return;
                }
            }
            Log.e(TAG, "MoveMotor " + (x?"X":"Y") + pos);
            send(cmd);
        }
    }

    public void send(String str) {
        if(!connected) {
            Toast.makeText(mContext, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = (str + '\n').getBytes();
            Log.i(TAG, "send( ) ...   @UsbSerialService.java , data : " +data );
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            onRunError(e);
        }
    }

}
