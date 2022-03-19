package com.example.mdp_android_grp_31;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.example.mdp_android_grp_31.ui.main.BluetoothConnectionService;
import com.example.mdp_android_grp_31.ui.main.BluetoothPopUp;
import com.example.mdp_android_grp_31.ui.main.BluetoothChatFragment;
import com.example.mdp_android_grp_31.ui.main.ControlFragment;
import com.example.mdp_android_grp_31.ui.main.GridMap;
import com.example.mdp_android_grp_31.ui.main.SectionsPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    final Handler handler = new Handler();
    // Declaration Variables
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;

    private static GridMap gridMap;
    private ControlFragment controlFragment;
    static TextView xAxisTextView, yAxisTextView, directionAxisTextView;
    static TextView robotStatusTextView, bluetoothStatus, bluetoothDevice;
    static ImageButton upBtn, downBtn, leftBtn, rightBtn;

    BluetoothDevice mBTDevice;
    private static UUID myUUID;
    ProgressDialog myDialog;

    String obstacleID = "4";
    String imageID = "";

    private static final String TAG = "Main Activity";
    public static boolean stopTimerFlag = false;
    public static boolean stopWk9TimerFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Initialization
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this,
                getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(9999);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        // Set up sharedPreferences
        MainActivity.context = getApplicationContext();
        this.sharedPreferences();
        editor.putString("message", "");
        editor.putString("direction","None");
        editor.putString("connStatus", "Disconnected");
        editor.commit();

        // Toolbar
        ImageButton bluetoothButton = findViewById(R.id.bluetoothButton);
        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent popup = new Intent(MainActivity.this, BluetoothPopUp.class);
                startActivity(popup);
            }
        });

        // Bluetooth Status
        bluetoothStatus = findViewById(R.id.bluetoothStatus);
        bluetoothDevice = findViewById(R.id.bluetoothConnectedDevice);

        // Map
        gridMap = new GridMap(this);
        gridMap = findViewById(R.id.mapView);
        xAxisTextView = findViewById(R.id.xAxisTextView);
        yAxisTextView = findViewById(R.id.yAxisTextView);
        directionAxisTextView = findViewById(R.id.directionAxisTextView);

        // ControlFragment for Timer
        controlFragment = new ControlFragment();

        // initialize ITEM_LIST and imageBearings strings
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                gridMap.ITEM_LIST.get(i)[j] = "";
                gridMap.imageBearings.get(i)[j] = "";
            }
        }

        // Controller
        upBtn = findViewById(R.id.upBtn);
        downBtn = findViewById(R.id.downBtn);
        leftBtn = findViewById(R.id.leftBtn);
        rightBtn = findViewById(R.id.rightBtn);

        // Robot Status
        robotStatusTextView = findViewById(R.id.robotStatus);

        myDialog = new ProgressDialog(MainActivity.this);
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                "Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );
    }

    public static GridMap getGridMap() {
        return gridMap;
    }

    public static TextView getRobotStatusTextView() {  return robotStatusTextView; }

    public static ImageButton getUpBtn() { return upBtn; }
    public static ImageButton getDownBtn() { return downBtn; }
    public static ImageButton getLeftBtn() { return leftBtn; }
    public static ImageButton getRightBtn() { return rightBtn; }

    public static TextView getBluetoothStatus() { return bluetoothStatus; }
    public static TextView getConnectedDevice() { return bluetoothDevice; }

    public static void sharedPreferences() {
        sharedPreferences = MainActivity.getSharedPreferences(MainActivity.context);
        editor = sharedPreferences.edit();
    }

    // Send Coordinates to ALG
    public static void printCoords(String message){
        showLog("Displaying Coords untranslated and translated");
        String[] strArr = message.split("-",2);

        if (BluetoothConnectionService.BluetoothConnectionStatus == true){
            byte[] bytes = strArr[1].getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }

        refreshMessageReceivedNS("Untranslated Coordinates: " + strArr[0] + "\n");
        refreshMessageReceivedNS("Translated Coordinates: "+strArr[1]);
        showLog("Exiting printCoords");


    }

    // Send message to bluetooth
    public static void printMessage(String message) {
        showLog("Entering printMessage");
        editor = sharedPreferences.edit();

        if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }
        showLog(message);
//        editor.putString("message",
//            BluetoothChatFragment.getMessageReceivedTextView().getText() + "\n" + message);
//        editor.commit();
        refreshMessageReceivedNS(message);
        showLog("Exiting printMessage");
    }

    // Store received message into string
    public static void printMessage(String name, int x, int y) throws JSONException {
        showLog("Entering printMessage");
        sharedPreferences();

        JSONObject jsonObject = new JSONObject();
        String message;

        switch(name) {
            case "waypoint":
                jsonObject.put(name, name);
                jsonObject.put("x", x);
                jsonObject.put("y", y);
                message = name + " (" + x + "," + y + ")";
                break;
            default:
                message = "Unexpected default for printMessage: " + name;
                break;
        }
        editor.putString("message",
                BluetoothChatFragment.getMessageReceivedTextView().getText() + "\n" + message);
        editor.commit();
        if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }
        showLog("Exiting printMessage");
    }

    public static void refreshMessageReceivedNS(String message){
        BluetoothChatFragment.getMessageReceivedTextView().append(message+ "\n");
    }

    public static void refreshMessageReceived() {
        BluetoothChatFragment
                .getMessageReceivedTextView()
                .setText(sharedPreferences.getString("message", ""));
    }

    public void refreshDirection(String direction) {
        gridMap.setRobotDirection(direction);
        directionAxisTextView.setText(sharedPreferences.getString("direction",""));
        printMessage("Direction is set to " + direction);
    }

    public static void refreshLabel() {
        xAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[0]-1));
        yAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[1]-1));
        directionAxisTextView.setText(sharedPreferences.getString("direction",""));
    }

    public static void receiveMessage(String message) {
        showLog("Entering receiveMessage");
        sharedPreferences();
        editor.putString("message",
                sharedPreferences.getString("message", "") + "\n" + message);
        editor.commit();
        showLog("Exiting receiveMessage");
    }

    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences();

            if(status.equals("connected")){
                try {
                    myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                Toast.makeText(MainActivity.this, "Device now connected to "
                        + mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected from "+mDevice.getName());
                Toast.makeText(MainActivity.this, "Disconnected from "
                        + mDevice.getName(), Toast.LENGTH_LONG).show();

                editor.putString("connStatus", "Disconnected");

                myDialog.show();
            }
            editor.commit();
        }
    };

    // message handler
    // alg sends x,y,robotDirection,movementAction
    // alg sends ALG,<obstacle id>
    // rpi sends RPI,<image id>
    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            showLog("receivedMessage: message --- " + message);
            int previous_x, previous_y;
            ArrayList<String> mapCoord = new ArrayList<>();
            if(message.contains("|")) {
                String [] cmd = message.split("\\|");
                for(int i =1; i<cmd.length; i++){
                    int[] coords = gridMap.getCurCoord();
                    previous_x = coords[0];
                    previous_y = coords[1];
                    String[] sentCoords = cmd[i].split(",");
                    String direction = "";
                    switch(sentCoords[2]){
                        case "0":
                            direction = "E";
                            break;
                        case "90":
                            direction = "N";
                            break;
                        case "180":
                            direction= "W";
                            break;
                        case "270":
                            direction = "S";
                            break;
                        default:
                            direction = "";
                    }
                    int current_x = 0;
                    int current_y = 0;
                    //This code runs in a situation in which either one x or y  is the same
                    //or both are the same coordinates they'll still move
                    if (direction.equals("N") || direction.equals("E")){
                        current_x = Integer.parseInt(sentCoords[0])+1;
                        current_y = 19 - Integer.parseInt(sentCoords[1]);
                    }
                    // When Direction is Heading South, Current X is different, it does need the X+1 attribute
                    else if (direction.equals("S") || direction.equals("W")){
                        current_x = Integer.parseInt(sentCoords[0]);
                        current_y = 19 - Integer.parseInt(sentCoords[1]);
                    }

                    if(current_x + 1 == previous_x){
                        if(!checkIfYWithinGrid(current_y+1)) {
                            gridMap.performAlgoCommand(current_x+1, current_y, direction);
                        }
                        else{
                            gridMap.performAlgoCommand(current_x+1, current_y + 1, direction);
                        }
                    }
                    else if (current_y+1 == previous_y){
                        if(!checkIfXWithinGrid(current_x+1)){
                            gridMap.performAlgoCommand(current_x, current_y+1, direction);
                        }
                        else{
                            gridMap.performAlgoCommand(current_x+1, current_y+1, direction);
                        }
                    }

                    else{
                        int result_x = current_x - (previous_x - 1);
                        int result_y = current_y - (previous_y - 1);

                        switch (direction) {
                            case "E":
                                if (result_y > 0) {
                                    for (int j = 0; j < result_y; j++) {
                                        int[] yMoveCoords = gridMap.getCurCoord();
                                        direction = gridMap.getRobotDirection();
                                        if(!checkIfYWithinGrid(yMoveCoords[1]+1)) {
                                            gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1], direction);
                                        }
                                        else{
                                            gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1] + 1, direction);
                                        }
                                    }
                                } else if (result_y < 0) {
                                    for (int j = result_y; j < 0; j++) {
                                        int[] yMoveCoords = gridMap.getCurCoord();
                                        direction = gridMap.getRobotDirection();
                                        if(!checkIfYWithinGrid(yMoveCoords[1]-1)){
                                            gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1], direction);
                                        }
                                        else{
                                            gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1] - 1, direction);
                                        }
                                    }
                                }
                                gridMap.setRobotDirection("right");
                                int [] getCurCoords = gridMap.getCurCoord();
                                ArrayList<int[]> obstacleList = gridMap.getObstaclesList();
                                int [] getSingleObstacle = getClosestObstacle(obstacleList, getCurCoords);
                                int xCoordCheck = getCurCoords[0];
                                int yCoordCheck = getCurCoords[1];
                                int compensation = 0;
                                for (int j= xCoordCheck; j<19; j++){
                                    if (j == getSingleObstacle[0] && (yCoordCheck) == getSingleObstacle[1]){
                                        compensation+=1;
                                    }
                                }
                                int counterCompensation = compensation;
                                if (compensation > 0){
                                    for (int k=0; k<compensation; k++){
                                        int[] yMoveCoords = gridMap.getCurCoord();
                                        direction = gridMap.getRobotDirection();
                                        gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1]-1, direction);
                                    }
                                }
                                for (int j = 0; j < result_x; j++) {
                                    int[] xMoveCoords = gridMap.getCurCoord();
                                    direction = gridMap.getRobotDirection();
                                    if(!checkIfXWithinGrid(xMoveCoords[0]+1)) {
                                        gridMap.performAlgoCommand(xMoveCoords[0], xMoveCoords[1], direction);
                                    }
                                    else{
                                        gridMap.performAlgoCommand(xMoveCoords[0]+1, xMoveCoords[1], direction);
                                    }
                                }
                                if (counterCompensation>0){
                                    for (int k = 0; k< counterCompensation; k++){
                                        int []yMoveCoords = gridMap.getCurCoord();
                                        gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1]+1, "up");
                                    }
                                }
                                break;
                            case "W":
                                if (result_y > 0) {
                                    for (int j = 0; j < result_y; j++) {
                                        int[] yMoveCoords = gridMap.getCurCoord();
                                        direction = gridMap.getRobotDirection();
                                        if(!checkIfYWithinGrid(yMoveCoords[1]+1)) {
                                            gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1], direction);
                                        }
                                        else{
                                            gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1] + 1, direction);
                                        }
                                    }
                                } else if (result_y < 0) {
                                    for (int j = result_y; j < 0; j++) {
                                        int[] yMoveCoords = gridMap.getCurCoord();
                                        direction = gridMap.getRobotDirection();
                                        if(!checkIfYWithinGrid(yMoveCoords[1]-1)){
                                            gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1], direction);
                                        }
                                        else{
                                            gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1] - 1, direction);
                                        }
                                    }
                                }
                                gridMap.setRobotDirection("left");
                                for (int j = result_x; j < 0; j++) {
                                    int[] xMoveCoords = gridMap.getCurCoord();
                                    direction = gridMap.getRobotDirection();
                                    if(!checkIfXWithinGrid(xMoveCoords[0]-1))
                                    {
                                        gridMap.performAlgoCommand(xMoveCoords[0], xMoveCoords[1], direction);
                                    }
                                    else{
                                        gridMap.performAlgoCommand(xMoveCoords[0]-1, xMoveCoords[1], direction);
                                    }
                                }
                                break;
                            case "S":
                                if (result_x > 0) {
                                    for (int j = 0; j < result_x; j++) {
                                        int[] xMoveCoords = gridMap.getCurCoord();
                                        direction = gridMap.getRobotDirection();
                                        if(!checkIfXWithinGrid(xMoveCoords[0]+1)) {
                                            gridMap.performAlgoCommand(xMoveCoords[0], xMoveCoords[1], direction);
                                        }
                                        else{
                                            gridMap.performAlgoCommand(xMoveCoords[0]+1, xMoveCoords[1], direction);
                                        }
                                    }
                                } else if (result_x < 0) {
                                    for (int j = result_x; j < 0; j++) {
                                        int[] xMoveCoords = gridMap.getCurCoord();
                                        direction = gridMap.getRobotDirection();
                                        if(!checkIfXWithinGrid(xMoveCoords[0]-1))
                                        {
                                            gridMap.performAlgoCommand(xMoveCoords[0], xMoveCoords[1], direction);
                                        }
                                        else{
                                            gridMap.performAlgoCommand(xMoveCoords[0]-1, xMoveCoords[1], direction);
                                        }
                                    }
                                }
                                gridMap.setRobotDirection("down");
                                for (int j = result_y; j < 0; j++) {
                                    int[] yMoveCoords = gridMap.getCurCoord();
                                    direction = gridMap.getRobotDirection();
                                    if(!checkIfYWithinGrid(yMoveCoords[1]-1)) {
                                        gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1], direction);
                                    }
                                    else{
                                        gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1]-1, direction);
                                    }
                                }
                                break;
                            case "N":
                                if (result_x > 0) {
                                    for (int j = 0; j < result_x; j++) {
                                        int[] xMoveCoords = gridMap.getCurCoord();
                                        direction = gridMap.getRobotDirection();
                                        if(!checkIfXWithinGrid(xMoveCoords[0]+1)) {
                                            gridMap.performAlgoCommand(xMoveCoords[0], xMoveCoords[1], direction);
                                        }
                                        else{
                                            gridMap.performAlgoCommand(xMoveCoords[0]+1, xMoveCoords[1], direction);
                                        }
                                    }
                                } else if (result_x < 0) {
                                    for (int j = result_x; j < 0; j++) {
                                        int[] xMoveCoords = gridMap.getCurCoord();
                                        direction = gridMap.getRobotDirection();
                                        if(!checkIfXWithinGrid(xMoveCoords[0]-1))
                                        {
                                            gridMap.performAlgoCommand(xMoveCoords[0], xMoveCoords[1], direction);
                                        }
                                        else{
                                            gridMap.performAlgoCommand(xMoveCoords[0]-1, xMoveCoords[1], direction);
                                        }
                                    }
                                }
                                gridMap.setRobotDirection("up");
                                for (int j = 0; j < result_y; j++) {
                                    int[] yMoveCoords = gridMap.getCurCoord();
                                    direction = gridMap.getRobotDirection();
                                    if(!checkIfYWithinGrid(yMoveCoords[1]+1)) {
                                        gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1], direction);
                                    }
                                    else{
                                        gridMap.performAlgoCommand(yMoveCoords[0], yMoveCoords[1]+1, direction);
                                    }
                                }
                                break;
                        }
                    }
                }
            }
            //image format from RPI is "IMG-Obstacle ID-ImageID" eg IMG-3-7
            else if(message.contains("IMG")) {
                String[] cmd = message.split("-");
                gridMap.updateIDFromRpi(cmd[1], cmd[2]);
                obstacleID = cmd[1];
            }
            else if (message.equals("ENDED")) {
                // if wk 8 btn is checked, means running wk 8 challenge and likewise for wk 9
                // end the corresponding timer
                ToggleButton exploreButton = findViewById(R.id.exploreToggleBtn2);
                ToggleButton fastestButton = findViewById(R.id.fastestToggleBtn2);

                if (exploreButton.isChecked()) {
                    showLog("explorebutton is checked");
                    stopTimerFlag = true;
                    exploreButton.setChecked(false);
                    robotStatusTextView.setText("Auto Movement/ImageRecog Stopped");
                } else if (fastestButton.isChecked()) {
                    showLog("fastestbutton is checked");
                    stopTimerFlag = true;
                    fastestButton.setChecked(false);
                    robotStatusTextView.setText("Week 9 Stopped");
                    ControlFragment.timerHandler.removeCallbacks(ControlFragment.timerRunnableFastest);
                }
            }
            for (int i=0; i<mapCoord.size();i++){
                System.out.println(mapCoord.get(i));
            }
        }
    };

    public int[] getClosestObstacle(ArrayList<int[]> obstacleList, int[] getCurCoords) {
        int coords_X = getCurCoords[0];
        int coords_Y = getCurCoords[1];
        int smallest_index = 0;
        int trackSmallestDistance_X = 10000000;
        int trackSmallestDistance_Y = 10000000;
        for (int i = 0; i < obstacleList.size(); i++) {
            if (trackSmallestDistance_X > Math.abs(obstacleList.get(i)[0] - coords_X)) {
                if (trackSmallestDistance_Y > Math.abs(obstacleList.get(i)[1] - coords_Y)) {
                    smallest_index = i;
                    trackSmallestDistance_X = Math.abs(obstacleList.get(i)[0] - coords_X);
                    trackSmallestDistance_Y = Math.abs(obstacleList.get(i)[1] - coords_Y);
                }
            }
        }
        return obstacleList.get(smallest_index);
    }

    public boolean checkIfXWithinGrid(int coord){
        return coord > 1 && coord < 21;
    }

    public boolean checkIfYWithinGrid(int coord){
        return coord > -1 && coord <20;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 1:
                if(resultCode == Activity.RESULT_OK){
                    mBTDevice = (BluetoothDevice) data.getExtras().getParcelable("mBTDevice");
                    myUUID = (UUID) data.getSerializableExtra("myUUID");
                }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        try{
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putString(TAG, "onSaveInstanceState");
        showLog("Exiting onSaveInstanceState");
    }
}