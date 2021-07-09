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

    }

    @Override
    public void onResume() {
        Log.i("Unitile-Serial", "onResume( ) ...   @MainAcvity.java " );
        super.onResume();
        mUsbSerial.init();
    }

    @Override
    public void onPause() {
        if(mUsbSerial.connected) {
            Log.i("Unitile-Serial", "onPause( ) ... --> disconnecte()  @MainAcvity.java    " );
            mUsbSerial.disconnect();
        }
        this.unregisterReceiver(mUsbSerial.broadcastReceiver);
        super.onPause();
    }


    @Override
    public void onClick(View v) {
        if(v == btnDown){
            mUsbSerial.moveY(45);
        }
        else if(v == btnUp){
            mUsbSerial.moveY(0);
        }
        else if(v == btnLeft){
            mUsbSerial.moveX(30);
        }
        else if(v == btnRight){
            mUsbSerial.moveX(-30);
        }
    }
}
