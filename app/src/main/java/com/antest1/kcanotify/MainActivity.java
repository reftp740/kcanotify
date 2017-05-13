package com.antest1.kcanotify;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_S2_CACHE_FILENAME;
import static com.antest1.kcanotify.KcaConstants.PREFS_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_FILTERLIST;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STARLIST;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STAR_CHECKED;
import static com.antest1.kcanotify.KcaConstants.PREF_APK_DOWNLOAD_SITE;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_FULLMORALE_SETTING;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_BATTLEVIEW_USE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_EXP_VIEW;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_DOCK;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_EXP;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_NOTIFYATSVCOFF;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_RINGTONE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_SOUND_KIND;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_V_HD;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_V_NS;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_QUESTVIEW_USE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_OPENDB_API_USE;
import static com.antest1.kcanotify.KcaConstants.PREF_SHOWDROP_SETTING;
import static com.antest1.kcanotify.KcaConstants.PREF_SVC_ENABLED;
import static com.antest1.kcanotify.KcaConstants.PREF_VPN_ENABLED;
import static com.antest1.kcanotify.KcaConstants.SEEK_33CN1;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getKcIntent;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.readCacheData;
import static com.antest1.kcanotify.KcaUtils.setPreferences;
import static com.antest1.kcanotify.KcaUtils.writeCacheData;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "KCAV";
    public static boolean[] warnType = new boolean[5];
    private static final int REQUEST_VPN = 1;
    public static final int REQUEST_OVERLAY_PERMISSION = 2;
    public static final int REQUEST_READEXT_PERMISSION = 3;

    AssetManager assetManager;
    Toolbar toolbar;
    private boolean running = false;
    private AlertDialog dialogVpn = null;
    Context ctx;
    Intent kcIntent;
    ToggleButton vpnbtn, svcbtn;
    Button kcbtn;
    ImageButton kcakashibtn;
    public static ImageButton kcafairybtn;
    TextView textDescription = null;
    TextView textWarn, textUpdate;
    Gson gson = new Gson();

    SharedPreferences prefs;
    Boolean is_kca_installed = false;
    private WindowManager windowManager;

    public MainActivity() {
        LocaleUtils.updateConfig(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        assetManager = getAssets();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        prefs.edit().putBoolean(PREF_SVC_ENABLED, KcaService.getServiceStatus()).apply();
        prefs.edit().putBoolean(PREF_VPN_ENABLED, KcaVpnService.checkOn()).apply();

        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);
        setDefaultPreferences();

        kcIntent = getKcIntent(getApplicationContext());
        is_kca_installed = (kcIntent != null);

        vpnbtn = (ToggleButton) findViewById(R.id.vpnbtn);
        vpnbtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        final Intent prepare = VpnService.prepare(MainActivity.this);
                        if (prepare == null) {
                            //Log.i(TAG, "Prepare done");
                            onActivityResult(REQUEST_VPN, RESULT_OK, null);
                        } else {
                            startActivityForResult(prepare, REQUEST_VPN);
                        }
                    } catch (Throwable ex) {
                        // Prepare failed
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                    }
                } else {
                    KcaVpnService.stop("switch off", MainActivity.this);
                    prefs.edit().putBoolean(PREF_VPN_ENABLED, false).apply();
                }
            }
        });

        svcbtn = (ToggleButton) findViewById(R.id.svcbtn);
        svcbtn.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, KcaService.class);
                if (!prefs.getBoolean(PREF_SVC_ENABLED, false)) {
                    if (is_kca_installed) {
                        prefs.edit().putBoolean(PREF_SVC_ENABLED, true).apply();
                        setCheckBtn();
                        loadTranslationData(assetManager, getApplicationContext());
                        startService(intent);
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.ma_toast_kancolle_not_installed), Toast.LENGTH_LONG).show();
                    }
                } else {
                    stopService(intent);
                }
            }
        });

        kcbtn = (Button) findViewById(R.id.kcbtn);
        kcbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (is_kca_installed) {
                    startActivity(kcIntent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.ma_toast_kancolle_not_installed), Toast.LENGTH_LONG).show();
                }
            }
        });

        kcakashibtn = (ImageButton) findViewById(R.id.kcakashibtn);
        kcakashibtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AkashiActivity.class);
                startActivity(intent);
            }
        });

        kcafairybtn = (ImageButton) findViewById(R.id.kcafairybtn);
        String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
        String fairyPath = "noti_icon_".concat(fairyIdValue);
        int viewBitmapSmallId = getId(fairyPath.concat("_small"), R.mipmap.class);
        kcafairybtn.setImageResource(viewBitmapSmallId);
        kcafairybtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                        R.color.black), PorterDuff.Mode.MULTIPLY);
        kcafairybtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(getApplicationContext())) {
                    // Can not draw overlays: pass
                } else if(KcaService.getServiceStatus()) {
                    startService(new Intent(getApplicationContext(), KcaViewButtonService.class)
                            .setAction(KcaViewButtonService.RETURN_FAIRY_ACTION));
                }
            }
        });

        textWarn = (TextView) findViewById(R.id.textMainWarn);
        textUpdate = (TextView) findViewById(R.id.textMainUpdate);
        textWarn.setVisibility(View.INVISIBLE);
        textUpdate.setVisibility(View.INVISIBLE);
        textUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String downloadUrl = getStringPreferences(getApplicationContext(), PREF_APK_DOWNLOAD_SITE);
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        });

        textDescription = (TextView) findViewById(R.id.textDescription);
        textDescription.setText(R.string.description);
        Linkify.addLinks(textDescription, Linkify.WEB_URLS);

        ctx = getApplicationContext();
        int setDefaultGameDataResult = setDefaultGameData();
        if (setDefaultGameDataResult != 1) {
            Toast.makeText(this, "error loading game data", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        new getRecentVersion().execute();

        setVpnBtn();
        setCheckBtn();
        loadTranslationData(assetManager, getApplicationContext());

        Arrays.fill(warnType, false);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READEXT_PERMISSION);
        }

        if (getBooleanPreferences(getApplicationContext(), PREF_KCA_BATTLEVIEW_USE)
                || getBooleanPreferences(getApplicationContext(), PREF_KCA_QUESTVIEW_USE)) {
            warnType[REQUEST_OVERLAY_PERMISSION] = !checkOverlayPermission();
        }
        setWarning();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setVpnBtn();
        setCheckBtn();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    public void setWarning() {
        String warnText = "";
        if (warnType[REQUEST_OVERLAY_PERMISSION]) {
            warnText = warnText.concat("\n").concat(getString(R.string.ma_toast_overay_diabled));
        }
        if (warnType[REQUEST_READEXT_PERMISSION]) {
            warnText = warnText.concat("\n").concat(getString(R.string.ma_permission_readext_denied));
        }

        if (warnText.length() > 0) {
            textWarn.setVisibility(View.VISIBLE);
            textWarn.setText(warnText.trim());
        } else {
            textWarn.setVisibility(View.GONE);
            textWarn.setText("");
        }
    }

    public void setVpnBtn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        vpnbtn.setChecked(prefs.getBoolean(PREF_VPN_ENABLED, false));
    }

    public void setCheckBtn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        svcbtn.setChecked(prefs.getBoolean(PREF_SVC_ENABLED, false));
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
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setDefaultPreferences() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        for (String prefKey : PREFS_LIST) {
            if (!pref.contains(prefKey)) {
                Log.e("KCA", prefKey + " pref add");
                switch (prefKey) {
                    case PREF_KCA_SEEK_CN:
                        editor.putString(prefKey, String.valueOf(SEEK_33CN1));
                        break;
                    case PREF_OPENDB_API_USE:
                    case PREF_AKASHI_STAR_CHECKED:
                        editor.putBoolean(prefKey, false);
                        break;
                    case PREF_KCA_EXP_VIEW:
                    case PREF_KCA_NOTI_NOTIFYATSVCOFF:
                    case PREF_KCA_NOTI_DOCK:
                    case PREF_KCA_NOTI_EXP:
                    case PREF_KCA_BATTLEVIEW_USE:
                    case PREF_KCA_QUESTVIEW_USE:
                    case PREF_KCA_NOTI_V_HD:
                    case PREF_KCA_NOTI_V_NS:
                    case PREF_SHOWDROP_SETTING:
                        editor.putBoolean(prefKey, true);
                        break;
                    case PREF_FULLMORALE_SETTING:
                        editor.putBoolean(prefKey, false);
                        break;
                    case PREF_KCA_LANGUAGE:
                        String localecode = getString(R.string.default_locale);
                        editor.putString(prefKey, localecode);
                        break;
                    case PREF_KCA_NOTI_SOUND_KIND:
                        editor.putString(prefKey, getString(R.string.sound_kind_value_vibrate));
                        break;
                    case PREF_KCA_NOTI_RINGTONE:
                        editor.putString(prefKey, DEFAULT_NOTIFICATION_URI.toString());
                        break;
                    case PREF_APK_DOWNLOAD_SITE:
                        editor.putString(prefKey, getString(R.string.app_download_link_googledrive));
                        break;
                    case PREF_AKASHI_STARLIST:
                    case PREF_AKASHI_FILTERLIST:
                        editor.putString(prefKey, "|");
                        break;
                    case PREF_FAIRY_ICON:
                        editor.putString(prefKey, "0");
                        break;
                    default:
                        editor.putString(prefKey, "");
                        break;
                }
            }
        }
        editor.commit();
    }

    private boolean checkOverlayPermission() {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext()));
    }

    private int setDefaultGameData() {
        if (KcaApiData.isGameDataLoaded()) return 1;
        String current_version = getStringPreferences(getApplicationContext(), PREF_KCA_VERSION);
        String default_version = getString(R.string.default_gamedata_version);
        JsonObject cachedData = readCacheData(getApplicationContext(), KCANOTIFY_S2_CACHE_FILENAME);
        boolean isValidCachedData = cachedData != null && !cachedData.isJsonNull() && cachedData.has("api_data");
        if (current_version.length() > 0 && isValidCachedData && KcaUtils.compareVersion(current_version, default_version)) {
            KcaApiData.getKcGameData(cachedData.getAsJsonObject("api_data"));
            return 1;
        } else {
            try {
                AssetManager.AssetInputStream ais =
                        (AssetManager.AssetInputStream) assetManager.open("api_start2");
                byte[] bytes = KcaUtils.gzipdecompress(ByteStreams.toByteArray(ais));
                JsonElement data = new JsonParser().parse(new String(bytes));
                JsonObject api_data = gson.fromJson(data, JsonObject.class).getAsJsonObject("api_data");
                KcaApiData.getKcGameData(api_data);
                writeCacheData(getApplicationContext(), bytes, KCANOTIFY_S2_CACHE_FILENAME);
                setPreferences(getApplicationContext(), PREF_KCA_VERSION, default_version);
            } catch (IOException e) {
                return 0;
            }
            return 1;
        }
    }

    public class getRecentVersion extends AsyncTask<Context, String, String> {
        public String currentVersion = BuildConfig.VERSION_NAME;

        public String getStringWithLocale(int id) {
            return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Context... params) {
            String content = null;
            try {
                content = executeClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return content;
        }

        public String executeClient() {
            final MediaType FORM_DATA = MediaType.parse("application/x-www-form-urlencoded");
            OkHttpClient client = new OkHttpClient.Builder().build();

            String checkUrl = String.format(getString(R.string.kcanotify_checkversion_link));
            Request.Builder builder = new Request.Builder().url(checkUrl).get();
            builder.addHeader("Referer", "app:/KCA/");
            builder.addHeader("Content-Type", "application/x-www-form-urlencoded");
            Request request = builder.build();

            try {
                Response response = client.newCall(request).execute();
                return response.body().string().trim();
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            } catch (AssertionError e) {
                e.printStackTrace();
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null || result.length() == 0) {
                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.sa_checkupdate_nodataerror), Toast.LENGTH_LONG).show();
            } else if (result.length() > 0) {
                Log.e("KCA", "Received: " + result);
                JsonObject jsonDataObj = new JsonObject();
                try {
                    StringReader reader = new StringReader(result.trim());
                    JsonReader jsonReader = new JsonReader(reader);
                    jsonReader.setLenient(true);
                    jsonDataObj = new JsonParser().parse(jsonReader).getAsJsonObject();

                    if (jsonDataObj.has("version")) {
                        String recentVersion = jsonDataObj.get("version").getAsString();
                        if (!compareVersion(currentVersion, recentVersion)) { // True if latest
                            textUpdate.setVisibility(View.VISIBLE);
                            textUpdate.setText(String.format(getStringWithLocale(R.string.ma_hasupdate), recentVersion));
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            getStringWithLocale(R.string.sa_checkupdate_servererror),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Log.i(TAG, "onActivityResult request=" + requestCode + " result=" + resultCode + " ok=" + (resultCode == RESULT_OK));
        if (requestCode == REQUEST_VPN) {
            prefs.edit().putBoolean(PREF_VPN_ENABLED, resultCode == RESULT_OK).apply();
            if (resultCode == RESULT_OK) {
                KcaVpnService.start("prepared", this);
            } else if (resultCode == RESULT_CANCELED) {
                // Canceled
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READEXT_PERMISSION: {
                warnType[REQUEST_READEXT_PERMISSION] = !(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                setWarning();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage() + " " + newConfig.getLocales().get(0).getCountry());
            KcaApplication.defaultLocale = newConfig.getLocales().get(0);
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage() + " " + newConfig.locale.getCountry());
            KcaApplication.defaultLocale = newConfig.locale;
        }
        if(getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(KcaApplication.defaultLocale);
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        loadTranslationData(assetManager, getApplicationContext());
        super.onConfigurationChanged(newConfig);
    }
}

