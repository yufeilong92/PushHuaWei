package com.lawyee.myhuaweitext;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.android.pushagent.api.PushManager;
import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiApiAvailability;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.support.api.client.PendingResult;
import com.huawei.hms.support.api.client.ResultCallback;
import com.huawei.hms.support.api.client.Status;
import com.huawei.hms.support.api.hwid.HuaweiId;
import com.huawei.hms.support.api.hwid.HuaweiIdSignInOptions;
import com.huawei.hms.support.api.hwid.HuaweiIdStatusCodes;
import com.huawei.hms.support.api.hwid.SignInResult;
import com.huawei.hms.support.api.push.HuaweiPush;
import com.huawei.hms.support.api.push.TokenResult;

public class MainActivity extends AppCompatActivity implements HuaweiApiAvailability.OnUpdateListener, View.OnClickListener {

    public static final String TAG = "测试结果";
    private HuaweiIdSignInOptions signInOptions = new
            HuaweiIdSignInOptions.Builder(HuaweiIdSignInOptions.DEFAULT_SIGN_IN).build();

    // 接收Push消息
    public static final int RECEIVE_PUSH_MSG = 0x100;
    // 接收Push Token消息
    public static final int RECEIVE_TOKEN_MSG = 0x101;
    // 接收Push 自定义通知消息内容
    public static final int RECEIVE_NOTIFY_CLICK_MSG = 0x102;
    // 接收Push LBS 标签上报响应
    public static final int RECEIVE_TAG_LBS_MSG = 0x103;

    private ProgressDialog mProgressDialog;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    public static final int REQUEST_SIGN_IN_UNLOGIN = 1002;
    public static final int REQUEST_SIGN_IN_AUTH = 1003;
    private HuaweiApiClient mClient;
    private boolean mResolvingError = false;
    private Button mBtnToken;
    private Button mBtnSignIN;
    private TextView mTvShowToken;
    private TextView mTvImi;
    private Button mState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        MyApplication.instance().setMainActivity(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        //申请token
        PushManager.requestToken(this);

        mClient = new HuaweiApiClient.Builder(this)
                .addApi(HuaweiId.SIGN_IN_API, signInOptions)
                .addConnectionCallbacks(callbacks)
                .addOnConnectionFailedListener(listener)
                .build();
        mClient.connect();
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        mTvImi.setText(imei);

    }

    @Override
    protected void onStop() {
        super.onStop();
        mClient.disconnect();
    }

    /**
     * 华为申请token失败回调
     */

    private HuaweiApiClient.OnConnectionFailedListener listener = new HuaweiApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            if (mResolvingError) {
                return;
            }
            int errorCode = connectionResult.getErrorCode();
            HuaweiApiAvailability availability = HuaweiApiAvailability.getInstance();
            if (availability.isUserResolvableError(errorCode)) {
                mResolvingError = true;
                availability.resolveError(MainActivity.this, errorCode, REQUEST_RESOLVE_ERROR, MainActivity.this);
            }
        }
    };
    /**
     * 华为申请token
     */
    private HuaweiApiClient.ConnectionCallbacks callbacks = new HuaweiApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected() {
            Log.d(TAG, "api 连接成功");
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "onConnectionSuspended: 连接中失败错误代码" + i);
        }
    };

    @Override
    public void onUpdateFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "错误代码： " + connectionResult.getErrorCode());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            int errorcode = HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(this);
            if (errorcode == ConnectionResult.SUCCESS) {
                if (!mClient.isConnecting() && !mClient.isConnected()) {
                    mClient.connect();
                }
            } else {
                Log.d(TAG, "onActivityResult:回调错误 " + errorcode);
            }

        } else if (requestCode == REQUEST_SIGN_IN_UNLOGIN) {
            SignIn();//调用华为授权接口
        } else if (requestCode == REQUEST_SIGN_IN_AUTH) {
            mProgressDialog.dismiss();
            SignInResult signInResult = HuaweiId.HuaweiIdApi.getSignInResultFromIntent(data);
            if (signInResult.isSuccess()) {
                // TODO: 授权成功，result.getSignInHuaweiId()获取华为帐号信息
            } else {
                // TODO: 授权失败，result.getStatus()获取错误原因
                Status status = signInResult.getStatus();
                Log.d(TAG, "onActivityResult:获取错误原因 " + status);
            }
        }

    }

    private void SignIn() {
        if (!mClient.isConnected()) {
            return;
        }
        /**
         * 登陆华为授权
         */
        final PendingResult<SignInResult> result = HuaweiId.HuaweiIdApi.signIn(mClient);
        result.setResultCallback(new ResultCallback<SignInResult>() {
            @Override
            public void onResult(SignInResult signInResult) {
                Status status = signInResult.getStatus();
                Log.d(TAG, "登陆成功返回结果" + status.getStatusMessage());
                if (signInResult.isSuccess()) {
                    // TODO: 2017/4/1  授权成功   // TODO: 授权成功，result.getSignInHuaweiId()获取华为帐号信息
                    Toast.makeText(MainActivity.this, "授权成功", Toast.LENGTH_SHORT).show();
                } else {
                    // TODO: 华为帐号未登录
                    if (signInResult.getStatus().getStatusCode() == HuaweiIdStatusCodes.SIGN_IN_UNLOGIN) {
                        Intent intent = signInResult.getData();
                        startActivityForResult(intent, REQUEST_SIGN_IN_UNLOGIN);
                    } else if (signInResult.getStatus().getStatusCode() == HuaweiIdStatusCodes.SIGN_IN_UNLOGIN) {
                        // TODO: 2017/4/1 未授权
                        mProgressDialog = new ProgressDialog(getApplicationContext());
                        mProgressDialog.setMessage("loading...");
                        mProgressDialog.show();
                        Intent intent = signInResult.getData();
                        startActivityForResult(intent, REQUEST_SIGN_IN_AUTH);
                    }
                }
            }
        });
    }


    /*
     * 处理提示消息，更新界面
     */
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECEIVE_PUSH_MSG:
                    showMsg((String) msg.obj);
                    break;
                case RECEIVE_TOKEN_MSG:
                    showMsg((String) msg.obj);
                    break;
                case RECEIVE_NOTIFY_CLICK_MSG:
                    showMsg((String) msg.obj);
                    break;
                case RECEIVE_TAG_LBS_MSG:
                    showToast((String) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    private void showMsg(String obj) {
        Toast.makeText(MainActivity.this, "" + obj, Toast.LENGTH_SHORT).show();
    }

    private void showToast(String obj) {
        Toast.makeText(MainActivity.this, obj, Toast.LENGTH_SHORT).show();
    }

    private void initView() {
        mBtnToken = (Button) findViewById(R.id.btn_Token);
        mBtnSignIN = (Button) findViewById(R.id.btn_SignIN);
        mTvShowToken = (TextView) findViewById(R.id.tv_showToken);
        mState = (Button) findViewById(R.id.btn_State);
        mState.setOnClickListener(this);
        mBtnToken.setOnClickListener(this);
        mBtnSignIN.setOnClickListener(this);
        mTvImi = (TextView) findViewById(R.id.tv_imi);
        mTvImi.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_Token:
                getToken();
                break;
            case R.id.btn_SignIN:
                SignIn();
                break;
            case R.id.btn_State:
                getState();
                break;
        }
    }

    private void getToken() {
        Log.d(TAG, "getToken: ==");
        if (!mClient.isConnected()) {
            showMsg("get token faild,hms is disconnect");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                PendingResult<TokenResult> token = HuaweiPush.HuaweiPushApi.getToken(mClient);
                token.await();
            }
        }).start();


//        PendingResult<TokenResult> token = HuaweiPush.HuaweiPushApi.getToken(mClient);
//        token.setResultCallback(callback);
    }

    private ResultCallback<TokenResult> callback = new ResultCallback<TokenResult>() {
        @Override
        public void onResult(TokenResult tokenResult) {
            String token = tokenResult.getTokenRes().getToken();
            int retCode = tokenResult.getTokenRes().getRetCode();
            Log.d(TAG, "onResult: ===" + token + retCode);
            int statusCode = tokenResult.getStatus().getStatusCode();
            String message = tokenResult.getStatus().getStatusMessage();
            Log.d(TAG, "onResult: ===" + statusCode + message);
        }
    };

    private void getState() {
        if (!mClient.isConnected()) {
            showMsg("get Push State faild ,HMS is disconnect");
            return;
        }
        Log.d(TAG, "getState: ============");
        new Thread(new Runnable() {
            @Override
            public void run() {
                HuaweiPush.HuaweiPushApi.getPushState(mClient);
            }
        }).start();
    }
}
