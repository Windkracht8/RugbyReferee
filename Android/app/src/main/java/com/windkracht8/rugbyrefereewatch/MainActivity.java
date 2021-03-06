package com.windkracht8.rugbyrefereewatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.samsung.android.sdk.accessory.SAAgentV2;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private GestureDetector gestureDetector;
    private static MainActivity ma;
    private static communication comms = null;

    private long backpresstime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gestureDetector = new GestureDetector(getApplicationContext(), new GestureListener());
        ma = this;
        setContentView(R.layout.activity_main);

        handleOrientation();
        try{
            getPackageManager().getPackageInfo("com.samsung.accessory", PackageManager.GET_ACTIVITIES);
            SAAgentV2.requestAgent(getApplicationContext(), communication.class.getName(), mAgentCallback1);
            findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
        }catch(PackageManager.NameNotFoundException e){
            findViewById(R.id.tvStatus).setVisibility(View.GONE);
            TextView tvError = findViewById(R.id.tvError);
            tvError.setText(R.string.tvFatal);
        }
    }

    @Override
    protected void onDestroy(){
        if (comms != null) {
            comms.closeConnection();
            comms.releaseAgent();
            comms = null;
        }
        ma = null;
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleOrientation();
    }

    private void handleOrientation(){
        ImageView ivIcon = findViewById(R.id.ivIcon);
        Resources r = getResources();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ivIcon.getLayoutParams().width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, r.getDisplayMetrics()));
            ivIcon.getLayoutParams().height = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, r.getDisplayMetrics()));
        }else {
            ivIcon.getLayoutParams().width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, r.getDisplayMetrics()));
            ivIcon.getLayoutParams().height = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, r.getDisplayMetrics()));
        }
    }
    @Override
    public void onBackPressed() {
        Date date = new Date();
        if(date.getTime() - backpresstime < 1000){
            bExitClick(null);
        }
        backpresstime = date.getTime();
        if(findViewById(R.id.hHistory).getVisibility() == View.VISIBLE){
            history hHistory = findViewById(R.id.hHistory);
            hHistory.unselect();
        }else if(findViewById(R.id.rReport).getVisibility() == View.VISIBLE){
            tabHistoryClick(null);
        }else if(findViewById(R.id.pPrepare).getVisibility() == View.VISIBLE){
            tabReportClick(null);
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        result = true;
                    }
                }
            }catch(Exception e){
                Log.e("MainActivity", "onFling: " + e.getMessage());
            }
            return result;
        }
    }

    public void onSwipeRight() {
        if(findViewById(R.id.rReport).getVisibility() == View.VISIBLE){
            tabHistoryClick(null);
        }else if(findViewById(R.id.pPrepare).getVisibility() == View.VISIBLE){
            tabReportClick(null);
        }
    }

    public void onSwipeLeft() {
        if(findViewById(R.id.hHistory).getVisibility() == View.VISIBLE){
            tabReportClick(null);
        }else if(findViewById(R.id.rReport).getVisibility() == View.VISIBLE){
            tabPrepareClick(null);
        }
    }

    private final SAAgentV2.RequestAgentCallback mAgentCallback1 = new SAAgentV2.RequestAgentCallback() {
        @Override
        public void onAgentAvailable(SAAgentV2 agent) {
            comms = (communication) agent;
            findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
        }

        @Override
        public void onError(int errorCode, String errorMessage) {
            Log.e("MainActivity", "Agent initialization error: " + errorCode + ". ErrorMsg: " + errorMessage);
            gotError(errorMessage);
        }
    };

    public void bExitClick(View view) {
        finish();
        System.exit(0);
    }
    public void bConnectClick(View view) {
        gotError("");
        findViewById(R.id.bConnect).setVisibility(View.GONE);
        if(comms == null) {
            gotError("Watch not found");
            return;
        }
        if(comms.status == communication.Status.DISCONNECTED ||
            comms.status == communication.Status.CONNECTION_LOST ||
            comms.status == communication.Status.ERROR
        ) {
            comms.findPeers();
        }
    }
    public void bGetMatchesClick(View view) {
        if(comms == null || comms.status != communication.Status.CONNECTED){gotError(getString(R.string.first_connect));return;}
        gotError("");
        history hHistory = findViewById(R.id.hHistory);
        comms.sendRequest("getMatches", hHistory.getDeletedMatches());
    }
    public void bGetMatchClick(View view) {
        if(comms == null || comms.status != communication.Status.CONNECTED){gotError(getString(R.string.first_connect));return;}
        gotError("");
        comms.sendRequest("getMatch", null);
    }
    public void bPrepareClick(View view) {
        if(comms == null || comms.status != communication.Status.CONNECTED){gotError(getString(R.string.first_connect));return;}
        gotError("");
        prepare pPrepare = findViewById(R.id.pPrepare);
        JSONObject requestData = pPrepare.getSettings();
        if(requestData == null){
            gotError("Error with settings");
            return;
        }
        comms.sendRequest("prepare", requestData);
    }
    public static void historyMatchClick(JSONObject match) {
        report rReport = ma.findViewById(R.id.rReport);
        rReport.gotMatch(match);
        ma.tabReportClick(null);
    }

    private static communication.Status updateStatus_status;
    public static void updateStatus_runOnUiThread(final communication.Status newstatus){
        updateStatus_status = newstatus;
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ma.updateStatus(updateStatus_status);
            }
        });
    }
    private void updateStatus(final communication.Status newstatus) {
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvError = findViewById(R.id.tvError);
        tvError.setText("");

        String status;
        switch(newstatus){
            case FATAL:
                findViewById(R.id.bConnect).setVisibility(View.GONE);
                tvStatus.setVisibility(View.GONE);
                return;
            case DISCONNECTED:
                status = ma.getString(R.string.status_DISCONNECTED);
                break;
            case FINDING_PEERS:
                status = ma.getString(R.string.status_CONNECTING);
                break;
            case CONNECTION_LOST:
                status = ma.getString(R.string.status_CONNECTION_LOST);
                findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
                findViewById(R.id.bExit).setVisibility(View.GONE);
                findViewById(R.id.bGetMatches).setVisibility(View.GONE);
                findViewById(R.id.bGetMatch).setVisibility(View.GONE);
                findViewById(R.id.bPrepare).setVisibility(View.GONE);
                break;
            case CONNECTED:
                status = ma.getString(R.string.status_CONNECTED);
                findViewById(R.id.bExit).setVisibility(View.VISIBLE);
                findViewById(R.id.bGetMatches).setVisibility(View.VISIBLE);
                findViewById(R.id.bGetMatch).setVisibility(View.VISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.VISIBLE);
                break;
            case GETTING_MATCHES:
                status = ma.getString(R.string.status_GETTING_MATCHES);
                findViewById(R.id.bGetMatches).setVisibility(View.INVISIBLE);
                findViewById(R.id.bGetMatch).setVisibility(View.INVISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.INVISIBLE);
                break;
            case GETTING_MATCH:
                status = ma.getString(R.string.status_GETTING_MATCH);
                findViewById(R.id.bGetMatches).setVisibility(View.INVISIBLE);
                findViewById(R.id.bGetMatch).setVisibility(View.INVISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.INVISIBLE);
                break;
            case PREPARE:
                status = ma.getString(R.string.status_PREPARE);
                findViewById(R.id.bGetMatches).setVisibility(View.INVISIBLE);
                findViewById(R.id.bGetMatch).setVisibility(View.INVISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.INVISIBLE);
                break;
            case ERROR:
            default:
                status = ma.getString(R.string.status_ERROR);
                findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
                findViewById(R.id.bExit).setVisibility(View.GONE);
                findViewById(R.id.bGetMatches).setVisibility(View.GONE);
                findViewById(R.id.bGetMatch).setVisibility(View.GONE);
                findViewById(R.id.bPrepare).setVisibility(View.GONE);
        }
        tvStatus.setText(status);
    }

    private static JSONObject gotResponse_msg;
    public static void gotResponse_runOnUiThread(final JSONObject msg){
        gotResponse_msg = msg;
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ma.gotResponse(gotResponse_msg);
            }
        });
    }
    private void gotResponse(final JSONObject responseMessage){
        Log.i("MainActivity.gotResponse", responseMessage.toString());
        try {
            String requestType = responseMessage.getString("requestType");
            switch (requestType) {
                case "getMatches":
                    Log.i("MainActivity.gotResult", "getMatches");
                    JSONArray getMatchesResponse = responseMessage.getJSONArray("responseData");
                    history hHistory = findViewById(R.id.hHistory);
                    hHistory.gotMatches(getMatchesResponse);
                    break;
                case "getMatch":
                    Log.i("MainActivity.gotResult", "getMatch");
                    JSONObject getMatchResponse = responseMessage.getJSONObject("responseData");
                    report rReport = findViewById(R.id.rReport);
                    rReport.gotMatch(getMatchResponse);
                    break;
                case "prepare":
                    Log.i("MainActivity.gotResult", "prepare");
                    String responseString = responseMessage.getString("responseData");
                    if (!responseString.equals("okilly dokilly")) {
                        gotError(responseString);
                    }
                    break;
            }
        }catch(Exception e){
            gotError("gotResponse exception: " + e.getMessage());
        }
    }
    private static String gotError_error;
    public static void gotError_runOnUiThread(String error){
        gotError_error = error;
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ma.gotError(gotError_error);
            }
        });
    }
    private void gotError(String error) {
        Log.i("MainActivity.gotError", error);
        if(error.equals(ma.getString(R.string.did_not_understand_message))) {
            error = ma.getString(R.string.update_watch_app);
        }
        TextView tvError = findViewById(R.id.tvError);
        tvError.setText(error);
    }

    public void tabHistoryClick(View view) {
        findViewById(R.id.tabHistory).setBackgroundResource(R.drawable.tab_active);
        findViewById(R.id.tabReport).setBackgroundResource(0);
        findViewById(R.id.tabPrepare).setBackgroundResource(0);
        findViewById(R.id.hHistory).setVisibility(View.VISIBLE);
        findViewById(R.id.rReport).setVisibility(View.GONE);
        findViewById(R.id.pPrepare).setVisibility(View.GONE);
    }
    public void tabReportClick(View view) {
        findViewById(R.id.tabHistory).setBackgroundResource(0);
        findViewById(R.id.tabReport).setBackgroundResource(R.drawable.tab_active);
        findViewById(R.id.tabPrepare).setBackgroundResource(0);
        findViewById(R.id.hHistory).setVisibility(View.GONE);
        findViewById(R.id.rReport).setVisibility(View.VISIBLE);
        findViewById(R.id.pPrepare).setVisibility(View.GONE);
    }
    public void tabPrepareClick(View view) {
        findViewById(R.id.tabHistory).setBackgroundResource(0);
        findViewById(R.id.tabReport).setBackgroundResource(0);
        findViewById(R.id.tabPrepare).setBackgroundResource(R.drawable.tab_active);
        findViewById(R.id.hHistory).setVisibility(View.GONE);
        findViewById(R.id.rReport).setVisibility(View.GONE);
        findViewById(R.id.pPrepare).setVisibility(View.VISIBLE);
    }

    public static void updateCardReason(String reason, long matchid, long eventid){
        history hHistory = ma.findViewById(R.id.hHistory);
        hHistory.updateCardReason(reason, matchid, eventid);
    }
    public static void updateTeamName(String name, String teamid, long matchid){
        history hHistory = ma.findViewById(R.id.hHistory);
        hHistory.updateTeamName(name, teamid, matchid);
    }

    public static String getTeamName(JSONObject team) {
        String name = "";
        try {
            name = team.getString("team");
            if (team.has("id") && !team.getString("id").equals(name)) {
                return name;
            }
            String color = team.getString("color");
            color = color.equals("lightgray") ? "white" : color;
            return name + " (" + color + ")";
        } catch (Exception e) {
            Log.e("MainActivity", "getTeamName: " + e.getMessage());
        }
        return name;
    }
}
