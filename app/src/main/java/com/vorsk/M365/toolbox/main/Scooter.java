package com.vorsk.M365.toolbox.main;


import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.vorsk.M365.toolbox.util.NbCommands;
import com.vorsk.M365.toolbox.util.NbMessage;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class Scooter {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String CHAR_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private final static String TAG = Scooter.class.getSimpleName();
    public static final String CHAR_WRITE = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"; //WRITE
    public static final String CHAR_READ = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"; //READ
    public static final String GET_NAME_SERVICE = "00001800-0000-1000-8000-00805f9b34fb";
    public static final String GET_NAME_CHAR = "00002a00-0000-1000-8000-00805f9b34fb";

    private RxBleClient rxBleClient;
    private String mDeviceName;
    private String mDeviceAddress;
    private RxBleDevice bleDevice;
    private Disposable connectionDisposable;
    private RxBleConnection connection;
    private DeviceActivity activity;

    public Scooter(DeviceActivity activity, String name, String macAddr) {
        this.activity = activity;
        this.mDeviceName = name;
        this.mDeviceAddress = macAddr;

        rxBleClient = RxBleClient.create(activity);
        bleDevice = rxBleClient.getBleDevice(mDeviceAddress);

        prepareConnectionObservable();
    }

    //private Handler handler = new Handler();
    //private Handler handler1 = new Handler();
    private Handler handlerPW = new Handler();
    private Handler handlerSN = new Handler();


//    private Runnable runnableMeta = new Runnable() {
//        @Override
//        public void run() {
//            if(isConnected()) {
//                handler.removeCallbacksAndMessages(null);
//                handler.postDelayed(updateAmpsRunnable, 100);
//                handler.postDelayed(updateVoltageBatterylife, 100);
//                handler.postDelayed(updateVoltageRunnable, 1000);
//                handler1.postDelayed(this, 10000);
//            }
//        }
//    };

//    private Runnable updateAmpsRunnable = new Runnable() {
//        @Override
//        public void run() {
//            if(isConnected()) {
//                setupNotificationAndSend();
//                updateAmps();
//                handler.postDelayed(updateVoltageRunnable, 100);
//                handler.postDelayed(this, 200);
//            }
//        }
//    };

//    private Runnable updateVoltageRunnable = new Runnable() {
//        @Override
//        public void run() {
//            if(isConnected()) {
//                setupNotificationAndSend();
//                updateVoltage();
//                //handler.postDelayed(this, 200);
//            }
//        }
//    };

//    private Runnable updateVoltageBatterylife = new Runnable() {
//        @Override
//        public void run() {
//            setupNotificationAndSend();
//            updateBatteryLife();
//            handler.postDelayed(this, 5150);
//        }
//    };

    private Runnable updatePWRunnable = new Runnable() {
        @Override
        public void run() {
            if(isConnected()) {
                setupNotificationAndSend();
                requestPWCommand();
                //handler.postDelayed(this, 200);
            } else {
                activity.toast("Need to connect first..");
            }
        }
    };

    private Runnable updateSNRunnable = new Runnable() {
        @Override
        public void run() {
            if(isConnected()) {
                setupNotificationAndSend();
                requestSNCommand();
                //handler.postDelayed(this, 200);
            } else {
                activity.toast("Need to connect first..");
            }
        }
    };

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice
                .establishConnection(false);
    }

    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }


    public void requestPW() {
        if (!isConnected()) {
            Log.i(TAG, "reconnecting");
            connect();
        }
        //Start
        handlerPW.post(updatePWRunnable);
    }

    public void requestSN() {
        if (!isConnected()) {
            Log.i(TAG, "reconnecting");
            connect();
        }
        //Start
        handlerSN.post(updateSNRunnable);
        //requestSNCommand();
    }


    public void setPW(String pw) {
        // TODO basic sanitation
        if (!isConnected()) {
            Log.i(TAG, "reconnecting");
            connect();
        }
        // TODO move reconnect to generic handler location

        setPWCommand(pw.getBytes());
    }

    public void lock() {
        if (!isConnected()) {
            Log.i(TAG, "reconnecting");
            connect();
        }
        // TODO move reconnect to generic handler location

        sendLockCommand();
    }

    public void unlock() {
        if (!isConnected()) {
            Log.i(TAG, "reconnecting");
            connect();
        }
        // TODO move reconnect to generic handler location

        sendUnlockCommand();
    }



    @SuppressLint("CheckResult")
    private void setupNotificationAndSend() {

        //Log.d(TAG, "response: " + HexString.bytesToHex(bytes));
        connection.setupNotification(UUID.fromString(CHAR_READ))
                .doOnNext(notificationObservable -> Log.d(TAG, "notification has been setup"))
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .timeout(200, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(Observable.empty())
                .subscribe(
                        this::parseResponse

                );
    }

    private void writeField(byte[] command) {
        Log.d(TAG,"sending: "+ bytesToHex(command));
        if(isConnected()) {
            connection.writeCharacteristic(UUID.fromString(CHAR_WRITE), command).subscribe();
        }

    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    private void parseResponse(byte[] bytes) {
        Log.i(TAG, "parseResponse " + bytesToHex(bytes));

        if (bytes.length >= 6) {
            if (bytes[5] == NbCommands.CMD_PW.getCommand()) {
                // pw response
                byte[] pwarray = Arrays.copyOfRange(bytes, 6, bytes.length - 2);
                String pw = new String(pwarray);

                activity.setPWView(pw);
            } else if (bytes[5] == NbCommands.CMD_SN.getCommand()) {
                // SN response
                byte[] snarray = Arrays.copyOfRange(bytes, 6, bytes.length - 2);
                String sn = new String(snarray);

                activity.setSNView(sn);
            }
        }

    }

    private void requestPWCommand() {
        Log.i(TAG, "requestPWCommand request sent");
        byte[] ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_M365)
                .setRW(NbCommands.READ)
                .setPosition(NbCommands.CMD_PW.getCommand())
                .setPayload(0x06)
                .build();
        setupNotificationAndSend();
        writeField(ctrlVersion);
    }

    private void requestSNCommand() {
        Log.i(TAG, "requestSNCommand request sent");
        byte[] ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_M365)
                .setRW(NbCommands.READ)
                .setPosition(NbCommands.CMD_SN.getCommand())
                .setPayload(0x0E)
                .build();
        setupNotificationAndSend();
        writeField(ctrlVersion);
    }

    private void sendUnlockCommand() {
        Log.i(TAG, "sendUnlockCommand request sent");
        byte[] ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_M365)
                .setRW(NbCommands.CMD)
                .setPosition(NbCommands.CMD_UNLOCK.getCommand())
                .setPayload(0x01)
                .build();
        setupNotificationAndSend();
        writeField(ctrlVersion);
    }

    private void sendLockCommand() {
        Log.i(TAG, "sendLockCommand request sent");
        byte[] ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_M365)
                .setRW(NbCommands.CMD)
                .setPosition(NbCommands.CMD_LOCK.getCommand())
                .setPayload(0x01)
                .build();
        setupNotificationAndSend();
        writeField(ctrlVersion);
    }

    private void setPWCommand(byte[] pw) {
        Log.i(TAG, "requestPWCommand request sent");
        byte[] ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_M365)
                .setRW(NbCommands.WRITE)
                .setPosition(NbCommands.CMD_PW.getCommand())
                .setPayload(pw)
                .build();
        setupNotificationAndSend();
        writeField(ctrlVersion);
    }

//    private void updateVoltage() {
//        String ctrlVersion = new NbMessage()
//                .setDirection(NbCommands.MASTER_TO_BATTERY)
//                .setRW(NbCommands.READ)
//                .setPosition(0x34)
//                .setPayload(0x02)
//                .build();
//        //setupNotificationAndSend(ctrlVersion);
//        writeField(ctrlVersion);
//    }

//    private void updateAmps() {
//        String ctrlVersion = new NbMessage()
//                .setDirection(NbCommands.MASTER_TO_BATTERY)
//                .setRW(NbCommands.READ)
//                .setPosition(0x33)
//                .setPayload(0x02)
//                .build();
//        //setupNotificationAndSend(ctrlVersion);
//        writeField(ctrlVersion);
//    }

/*    private void updateBatteryLife() {
        String ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_BATTERY)
                .setRW(NbCommands.READ)
                .setPosition(0x32)
                .setPayload(0x02)
                .build();
        //setupNotificationAndSend(ctrlVersion);
        writeField(ctrlVersion);
    }*/

    public void connect() {
        if (isConnected()) {
            Log.d(TAG, "triggering disconnect");
            triggerDisconnect();
        } else {
            Log.d(TAG, "attempting to connect");
            connectionDisposable = bleDevice.establishConnection(false)
                    //.compose(bindUntilEvent(PAUSE))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(this::dispose)
                    .doOnError(throwable -> {
                        //System.out.println("ERROR,disconnect");
                        activity.toast("Scooter disconnected");
                        //handler.removeCallbacksAndMessages(null);
                        //handler1.removeCallbacksAndMessages(null);
                        handlerPW.removeCallbacksAndMessages(null);
                        handlerSN.removeCallbacksAndMessages(null);
                        dispose();
                        activity.setConnStatus("connected");
                        //requestSNCommand();
                    })
                    .subscribe(this::onConnectionReceived, this::onConnectionFailure);
        }
    }

    private void triggerDisconnect() {
        if (connectionDisposable != null) {
            connectionDisposable.dispose();
        }
    }

    public void disconnect() {
        this.triggerDisconnect();
    }

    private void dispose() {
        connectionDisposable = null;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Log.d(TAG,"connection fail: "+throwable.getMessage());
        // TODO toast
    }

    private void onConnectionReceived(RxBleConnection connection) {
        this.connection = connection;
        activity.setConnStatus("connected");
    }

    public String getName() {
        return this.mDeviceName;
    }
}
