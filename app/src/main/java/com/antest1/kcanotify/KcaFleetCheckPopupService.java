package com.antest1.kcanotify;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Calendar;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.SEEK_PURE;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaFleetCheckPopupService extends BaseService {
    public static final String FCHK_SHOW_ACTION = "fchk_show_action";

    private static final int FCHK_FUNC_SEEKTP = 0;
    private static final int FCHK_FUNC_AIRBATTLE = 1;
    private static final int FCHK_FUNC_FUELBULL = 2;

    private static final int[] FCHK_FLEET_LIST = {
        R.id.fleet_1, R.id.fleet_2, R.id.fleet_3, R.id.fleet_4, R.id.fleet_5
    };

    private static final int[] FCHK_BTN_LIST = {
        R.id.fchk_btn_seektp, R.id.fchk_btn_airbattle, R.id.fchk_btn_fuelbull
    };

    private View popupView;
    private WindowManager windowManager;
    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    private KcaDeckInfo deckInfoCalc;
    private KcaDBHelper dbHelper;
    JsonArray portdeckdata;
    WindowManager.LayoutParams layoutParams;
    NotificationManagerCompat notificationManager;

    public static int type;
    public static boolean active = false;
    public static int recent_no = 0;
    public static int current_func = FCHK_FUNC_SEEKTP;
    public static int deck_cnt = 1;

    public static boolean isActive() {
        return active;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        active = true;
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            deckInfoCalc = new KcaDeckInfo(getBaseContext());
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
        }
    }

    private void setPopupLayout() {
        if (checkPopupExist()) return;

        Context context = new ContextThemeWrapper(this, R.style.AppTheme);
        LayoutInflater mInflater = LayoutInflater.from(context);

        popupView = mInflater.inflate(R.layout.view_fleet_check, null);
        popupView.setOnTouchListener(mViewTouchListener);
        popupView.findViewById(R.id.view_fchk_head).setOnTouchListener(mViewTouchListener);
        ((TextView) popupView.findViewById(R.id.view_fchk_title)).setText(getString(R.string.fleetcheckview_title));

        for (int fchk_id: FCHK_BTN_LIST) {
            popupView.findViewById(fchk_id).setOnTouchListener(mViewTouchListener);
        }
        for (int fleet_id: FCHK_FLEET_LIST) {
            popupView.findViewById(fleet_id).setOnTouchListener(mViewTouchListener);
        }

        setPopupContent();

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popupWidth = popupView.getMeasuredWidth();
        popupHeight = popupView.getMeasuredHeight();

        SizeInsets screenSize = KcaUtils.getDefaultDisplaySizeInsets(this);
        screenWidth = screenSize.size.x;
        screenHeight = screenSize.size.y;
        Log.e("KCA", "w/h: " + screenWidth + " " + screenHeight);

        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.TOP | Gravity.START;
        }

        layoutParams.x = (screenWidth - popupWidth) / 2;
        layoutParams.y = (screenHeight - popupHeight) / 2;
        windowManager.addView(popupView, layoutParams);
    }

    private void setPopupContent() {
        if (portdeckdata != null) {
            deck_cnt = portdeckdata.size();
            setFchkFleetBtnColor(recent_no, deck_cnt);
            setFchkFuncBtnColor(current_func);
            setText();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA-MPS", "onStartCommand");
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(FCHK_SHOW_ACTION)) {
                portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                setPopupLayout();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        active = false;
        if (windowManager != null && popupView.getParent() != null) {
            windowManager.removeViewImmediate(popupView);
        }
        super.onDestroy();
    }

    private void stopPopup() {
        if (popupView != null) popupView.setVisibility(View.GONE);
        stopSelf();
    }

    private void setText() {
        TextView fchk_info = popupView.findViewById(R.id.fchk_value);
        if (fchk_info != null) {
            int target = recent_no;
            String target_str;
            if (recent_no == 4) {
                target = 0;
                target_str = "0,1";
            } else {
                target_str = String.valueOf(target);
            }

            if (KcaApiData.isGameDataLoaded() && KcaApiData.checkUserShipDataLoaded() && portdeckdata != null) {
                switch (current_func) {
                    case FCHK_FUNC_SEEKTP:
                        int seekValue_0 = (int) deckInfoCalc.getSeekValue(portdeckdata, target_str, SEEK_PURE, KcaBattle.getEscapeFlag());
                        double seekValue_1 = deckInfoCalc.getSeekValue(portdeckdata, target_str, 1, KcaBattle.getEscapeFlag());
                        double seekValue_2 = deckInfoCalc.getSeekValue(portdeckdata, target_str, 2, KcaBattle.getEscapeFlag());
                        double seekValue_3 = deckInfoCalc.getSeekValue(portdeckdata, target_str, 3, KcaBattle.getEscapeFlag());
                        double seekValue_4 = deckInfoCalc.getSeekValue(portdeckdata, target_str, 4, KcaBattle.getEscapeFlag());

                        int[] tp = deckInfoCalc.getTPValue(portdeckdata, target_str, KcaBattle.getEscapeFlag());
                        fchk_info.setText(KcaUtils.format(getString(R.string.fleetcheckview_content_seeklos),
                                seekValue_0, seekValue_1, seekValue_2, seekValue_3, seekValue_4, tp[0], tp[1]));
                        break;
                    case FCHK_FUNC_AIRBATTLE:
                        int[] airPowerRange = deckInfoCalc.getAirPowerRange(portdeckdata, target, KcaBattle.getEscapeFlag());
                        JsonObject contact = deckInfoCalc.getContactProb(portdeckdata, target_str, KcaBattle.getEscapeFlag());
                        double start_rate_1 = contact.getAsJsonArray("stage1").get(0).getAsDouble() * 100;
                        double select_rate_1 = contact.getAsJsonArray("stage2").get(0).getAsDouble() * 100;
                        double start_rate_2 = contact.getAsJsonArray("stage1").get(1).getAsDouble() * 100;
                        double select_rate_2 = contact.getAsJsonArray("stage2").get(1).getAsDouble() * 100;
                        fchk_info.setText(KcaUtils.format(getString(R.string.fleetcheckview_content_airbattle),
                                airPowerRange[0], airPowerRange[1], start_rate_1, select_rate_1, start_rate_2, select_rate_2));
                        break;
                    case FCHK_FUNC_FUELBULL:
                        fchk_info.setText("fuel_bull");
                        break;
                    default:
                        fchk_info.setText("");
                        break;
                }
            } else {
                fchk_info.setText("data not loaded");
            }
        }
    }

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private final View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;

        private long startClickTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchX = event.getRawX();
                    mTouchY = event.getRawY();
                    mViewX = layoutParams.x;
                    mViewY = layoutParams.y;
                    Log.e("KCA", KcaUtils.format("mView: %d %d", mViewX, mViewY));
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    break;

                case MotionEvent.ACTION_UP:
                    long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        if (id == R.id.view_fchk_head) {
                            stopPopup();
                        }
                        for (int i = 0; i < FCHK_FLEET_LIST.length; i++) {
                            if ((id == FCHK_FLEET_LIST[i]) && (i < deck_cnt || i == 4)) {
                                recent_no = i;
                                setFchkFleetBtnColor(recent_no, deck_cnt);
                                setText();
                            }
                        }
                        for (int i = 0; i < FCHK_BTN_LIST.length; i++) {
                            if (id == FCHK_BTN_LIST[i]) {
                                current_func = i;
                                setFchkFuncBtnColor(current_func);
                                setText();
                            }
                        }
                    }

                    int[] locations = new int[2];
                    popupView.getLocationOnScreen(locations);
                    int xx = locations[0];
                    int yy = locations[1];
                    Log.e("KCA", KcaUtils.format("Coord: %d %d", xx, yy));
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = (int) (event.getRawX() - mTouchX);
                    int y = (int) (event.getRawY() - mTouchY);

                    layoutParams.x = mViewX + x;
                    layoutParams.y = mViewY + y;
                    if (layoutParams.x < 0) layoutParams.x = 0;
                    else if (layoutParams.x > screenWidth - popupWidth)
                        layoutParams.x = screenWidth - popupWidth;
                    if (layoutParams.y < 0) layoutParams.y = 0;
                    else if (layoutParams.y > screenHeight - popupHeight)
                        layoutParams.y = screenHeight - popupHeight;
                    windowManager.updateViewLayout(popupView, layoutParams);
                    break;
            }

            return true;
        }
    };

    private void setFchkFleetBtnColor(int selected, int size) {
        for (int i = 0; i < FCHK_FLEET_LIST.length; i++) {
            int fleet_id = FCHK_FLEET_LIST[i];
            Chip chip = popupView.findViewById(fleet_id);
            chip.setChecked(selected == i);
            if (size < 4 && i >= size) {
                chip.setChipStrokeColorResource(R.color.grey);
            } else if (i == selected) {
                chip.setChipStrokeColorResource(R.color.colorAccent);
            } else {
                chip.setChipStrokeColorResource(R.color.colorFleetInfoNormalBtn);
            }
        }
    }

    private void setFchkFuncBtnColor(int selected) {
        for (int i = 0; i < FCHK_BTN_LIST.length; i++) {
            Chip chip = popupView.findViewById(FCHK_BTN_LIST[i]);
            if (i == selected) {
                chip.setChipStrokeColorResource(R.color.colorAccent);
            } else {
                chip.setChipStrokeColorResource(R.color.colorFleetInfoBtn);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (windowManager != null && checkPopupExist()) {
            windowManager.removeViewImmediate(popupView);
        }
    }

    private boolean checkPopupExist() {
        return popupView != null && popupView.getParent() != null;
    }
}