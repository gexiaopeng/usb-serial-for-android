package com.hoho.android.usbserial.examples;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MyTestActivity extends Activity implements View.OnClickListener  {
    private static final int DEFAULT_TIMEOUT =5000 ;
    private static final int USB_TIMEOUT_MILLIS = 5000;
    private TextView textView;
    private UsbManager mUsbManager;
    private static final int MESSAGE_REFRESH = 101;
    private static final long REFRESH_TIMEOUT_MILLIS = 5000;
    private static int seq=0;
    private static String msg="";
    private static UsbDevice usbDevice;
    private EditText myEditText;
    private Button submitBtn;
    private TextView mtextView;
    private TextView mtextView3;
    private UsbDeviceConnection usbDeviceConnection=null;
    private UsbInterface usbInterface=null;
    private UsbEndpoint usbEndpointOut;
    private  UsbEndpoint usbEndpointIn = null;
    private final int DEFAULT_BAUD_RATE = 9600;
    private Handler handler = new Handler();
    private  UsbSerialPort sPort = null;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REFRESH:
                    refreshDeviceList();
                   // mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_test);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        textView= (TextView) findViewById(R.id.textView);
        myEditText= (EditText) findViewById(R.id.myEditText);
        submitBtn= (Button) findViewById(R.id.myButton);
        mtextView= (TextView) findViewById(R.id.textView2);
        mtextView3= (TextView) findViewById(R.id.textView3);
        submitBtn.setOnClickListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mHandler.sendEmptyMessage(MESSAGE_REFRESH);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeMessages(MESSAGE_REFRESH);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(sPort!=null){
            try {
                sPort.close();
                sPort=null;
            }catch (Exception e){

            }
        }
    }

    private void refreshDeviceList() {
        msg="";
        usbDevice=null;
        textView.setText("查询dlssdfsdfsdfdsfeq:"+(seq++));

        new AsyncTask<Void, Void, List<UsbDevice>>() {
            @Override
            protected List<UsbDevice> doInBackground(Void... params) {
                final List<UsbDevice> result = new ArrayList<UsbDevice>();
               try {

                    Thread.sleep(1000);
                    if(mUsbManager!=null) {
                       // UsbSerialProber  usbSerialProber=UsbSerialProber.getDefaultProber();
                        //final List<UsbSerialDriver> drivers = usbSerialProber.findAllDrivers(mUsbManager);
                        Map<String, UsbDevice> usbList = mUsbManager.getDeviceList();
                        if(usbList!=null && usbList.size()>0){
                           // final List<UsbSerialDriver> drivers = usbSerialProber.findAllDrivers(mUsbManager);
                            for (Map.Entry<String, UsbDevice> entry : usbList.entrySet()) {
                                msg+=","+entry.getKey();
                                result.add(entry.getValue());
                            }
                            // msg="usbList size:"+usbList.size();
                        }else{
                            msg="usbList is null";
                        }

                    }else{
                        msg="mUsbManager is null";
                    }
                }catch (Throwable e   ){
                   msg=e.getMessage();
                }
                 return result;
            }

            @Override
            protected void onPostExecute(List<UsbDevice> result) {
                msg="--onPostExecute--msg:"+msg+",size:"+(result==null?0:result.size()+"\r\n");
                if(result!=null && result.size()>0){
                    for(UsbDevice usb:result){
                        boolean isP=mUsbManager.hasPermission(usb);
                        msg+=","+usb.getDeviceName()+","+usb.getSerialNumber()+","+usb.getProductName()+","+isP+"\r\n";
                        if(isP && usb.getProductName().toLowerCase().indexOf("serial")!=-1){
                            usbDevice=usb;
                        }
                    }
                }
                textView.setText(msg);
            }

        }.execute((Void) null);
    }

    @Override
    public void onClick(View v) {
        String usbName = (usbDevice == null ? "" : usbDevice.getProductName());
        try {
           if (usbDevice != null && sPort==null) {
                Ch34xSerialDriver usbSerialDriver=new Ch34xSerialDriver(usbDevice);
               usbDeviceConnection = mUsbManager.openDevice(usbDevice);
                List<UsbSerialPort> list= usbSerialDriver.getPorts();
                mtextView.setText(" list:"+(list==null?-1:list.size())+"("+usbName+")\r\n");
                if(list!=null && list.size()>0){
                    sPort=list.get(0);
                }
               sPort.open(usbDeviceConnection);
                //115200
               sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
               showStatus(mtextView, "CD  - Carrier Detect", sPort.getCD());
               showStatus(mtextView, "CTS - Clear To Send", sPort.getCTS());
               showStatus(mtextView, "DSR - Data Set Ready", sPort.getDSR());
               showStatus(mtextView, "DTR - Data Terminal Ready", sPort.getDTR());
               showStatus(mtextView, "DSR - Data Set Ready", sPort.getDSR());
               showStatus(mtextView, "RI  - Ring Indicator", sPort.getRI());
               showStatus(mtextView, "RTS - Request To Send", sPort.getRTS());
            }
            if(sPort!=null){
               String msg=myEditText.getText().toString().trim();
               int res=-1;
               byte[] data=null;
               if("A".equals(msg)){
                   //开启
                   data=new byte[]{(byte)0xA0,(byte)0x01,(byte)0x01,(byte)0xA2};
               }else if("a".equals(msg)){
                   data=new byte[]{(byte)0xA0,(byte)0x01,(byte)0x00,(byte)0xA1};
               }else{
                   mtextView.append("\r\n输入["+msg+"]无效！("+(seq++)+")");
               }
               if(data!=null){
                  res= sPort.write(data,USB_TIMEOUT_MILLIS);
                  mtextView.append("\r\nres:"+res+"("+msg+"-"+(seq++)+")");
               }
            }
            //mtextView.setText("Serial device: " + sPort.getClass().getSimpleName());
        }catch (Exception e){
            if(sPort!=null){
                try {
                    sPort.close();
                    sPort=null;
                } catch (IOException e1) {
                }
            }
            mtextView3.setText(" error:"+e.getMessage());
        }
    }

    public void onClick2(View v) {
        String txt = (usbDevice == null ? "" : usbDevice.getProductName());
        mtextView.setText(txt);
        try {

            if (usbDevice != null) {
                if(usbEndpointOut==null) {
                    usbInterface = usbDevice.getInterface(0);
                    for (int index = 0; index < usbInterface.getEndpointCount(); index++) {
                        UsbEndpoint point = usbInterface.getEndpoint(index);
                        if (point.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (point.getDirection() == UsbConstants.USB_DIR_IN) {
                                usbEndpointIn = point;
                            } else if (point.getDirection() == UsbConstants.USB_DIR_OUT) {
                                usbEndpointOut = point;
                            }
                        }
                    }
                    txt += "\r\n" + "usbEndpointIn:" + usbEndpointIn + "\r\nusbEndpointOut:" + usbEndpointOut + "," + usbInterface.getEndpointCount();
                    usbDeviceConnection = mUsbManager.openDevice(usbDevice);
                    //setBaudRate(DEFAULT_BAUD_RATE);

                    configUsb340(9600);//115200
                    // usbDeviceConnection.
                    // usbDeviceConnection.setConfiguration()
                    // usb_receiveData();
                }
                String str = myEditText.getText().toString().trim();
                byte[] data = str.getBytes();
                int ret = usbDeviceConnection.bulkTransfer(usbEndpointOut, data, data.length, DEFAULT_TIMEOUT);

                mtextView.setText(txt + "\r\nret:" + ret + "[" + str + "]" + mUsbManager.hasPermission(usbDevice));
            }
        }catch (Exception e){
            mtextView.setText(" error:"+e.getMessage());
        }
    }

    private boolean configUsb340(int paramInt)
    {
        byte[] arrayOfByte = new byte[8];
        usbDeviceConnection.controlTransfer(192, 95, 0, 0, arrayOfByte, 8, 1000);
        usbDeviceConnection.controlTransfer(64, 161, 0, 0, null, 0, 1000);
        long l1 = 1532620800 / paramInt;
        for (int i = 3; ; i--)
        {
            if ((l1 <= 65520L) || (i <= 0))
            {
                long l2 = 65536L - l1;
                int j = (short)(int)(0xFF00 & l2 | i);
                int k = (short)(int)(0xFF & l2);
                usbDeviceConnection.controlTransfer(64, 154, 4882, j, null, 0, 1000);
                usbDeviceConnection.controlTransfer(64, 154, 3884, k, null, 0, 1000);
                usbDeviceConnection.controlTransfer(192, 149, 9496, 0, arrayOfByte, 8, 1000);
                usbDeviceConnection.controlTransfer(64, 154, 1304, 80, null, 0, 1000);
                usbDeviceConnection.controlTransfer(64, 161, 20511, 55562, null, 0, 1000);
                usbDeviceConnection.controlTransfer(64, 154, 4882, j, null, 0, 1000);
                usbDeviceConnection.controlTransfer(64, 154, 3884, k, null, 0, 1000);
                usbDeviceConnection.controlTransfer(64, 164, 0, 0, null, 0, 1000);
                return true;
            }
            l1 >>= 3;
        }
    }

    private void setBaudRate(int baudRate) throws IOException {
        int[] baud = new int[]{2400, 0xd901, 0x0038, 4800, 0x6402,
                0x001f, 9600, 0xb202, 0x0013, 19200, 0xd902, 0x000d, 38400,
                0x6403, 0x000a, 115200, 0xcc03, 0x0008};

        for (int i = 0; i < baud.length / 3; i++) {
            if (baud[i * 3] == baudRate) {
                int ret = controlOut(0x9a, 0x1312, baud[i * 3 + 1]);
                if (ret < 0) {
                    throw new IOException("Error setting baud rate. #1");
                }
                ret = controlOut(0x9a, 0x0f2c, baud[i * 3 + 2]);
                if (ret < 0) {
                    throw new IOException("Error setting baud rate. #2");
                }

                return;
            }
        }


        throw new IOException("Baud rate " + baudRate + " currently not supported");
    }
    private int controlOut(int request, int value, int index) {
        final int REQTYPE_HOST_TO_DEVICE = 0x41;
        return usbDeviceConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, request, value, index, null, 0, USB_TIMEOUT_MILLIS);
    }
    class ReceiveThread implements Runnable {
        @Override
        public void run() {
           // mtextView3.setText(" ===run==");
            while (true) {
                try {
                    byte[] receiveBytes = new  byte[32];
                    int r= receiveMessage(receiveBytes);
                   // mtextView.setText(" recive:"+r+"["+new String(receiveBytes)+"]");
                    Thread.sleep(3000);
                } catch (Throwable e) {
                    //mtextView3.setText(" recive error:"+e.getMessage());
                }
            }
        }
    }
    public  int receiveMessage(byte[] receiveBytes){
        int ret = -1;
        if(usbEndpointIn != null){
            ret = usbDeviceConnection.bulkTransfer(usbEndpointIn, receiveBytes, receiveBytes.length, USB_TIMEOUT_MILLIS);

        }
       // mtextView3.setText(" recive:"+ret+"["+new String(receiveBytes)+"]");
        return ret;
    }
    public void usb_receiveData(){
        // ReceiveThread receiveThread = new ReceiveThread();
        // Thread thread = new Thread(receiveThread);
         //thread.start();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                mtextView3.setText("usb_receiveData......");
                byte[] receiveBytes = new  byte[32];
                int r= receiveMessage(receiveBytes);
                mtextView3.setText(" recive:"+r+"["+new String(receiveBytes)+"]");
                //每隔1s循环执行run方法
               handler.postDelayed(this, 3000);
            }
        };
        handler.postDelayed(r, 100);//延时100毫秒

    }
    void showStatus(TextView theTextView, String theLabel, boolean theValue){
        String msg = theLabel + ": " + (theValue ? "enabled" : "disabled") + "\n";
        theTextView.append(msg);
    }
}
