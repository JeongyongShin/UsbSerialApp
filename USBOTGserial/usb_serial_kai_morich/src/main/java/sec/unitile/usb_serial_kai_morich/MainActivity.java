package sec.unitile.usb_serial_kai_morich;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private UsbSerialService mUsbSerial;
    private Button btnLeft, btnRight, btnUp, btnDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);

        btnLeft.setOnClickListener(this);
        btnRight.setOnClickListener(this);
        btnUp.setOnClickListener(this);
        btnDown.setOnClickListener(this);

        mUsbSerial = new UsbSerialService(this);
        Log.i("App-Serial", "onCreate( ) @MainAcvity.java");

    }

    @Override
    public void onResume() {
        Log.i("App-Serial", "onResume( ) ...   @MainAcvity.java " );
        super.onResume();
        mUsbSerial.init();
    }

    @Override
    public void onPause() {
        if(mUsbSerial.connected) {
            Log.i("App-Serial", "onPause( ) ... --> disconnecte()  @MainAcvity.java    " );
            mUsbSerial.disconnect();
        }
        this.unregisterReceiver(mUsbSerial.broadcastReceiver);
        super.onPause();
    }


    @Override
    public void onClick(View v) {
        if(v == btnDown){
            mUsbSerial.send( "y,30,100");
        }
        else if(v == btnUp){
            mUsbSerial.send( "y,0,100");
        }
        else if(v == btnLeft){
            mUsbSerial.send( "x,45,100");
        }
        else if(v == btnRight){
            mUsbSerial.send( "x,-45,100");
        }
    }
}
