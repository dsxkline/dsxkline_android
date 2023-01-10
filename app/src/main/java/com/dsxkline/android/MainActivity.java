package com.dsxkline.android;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.dsxkline.android.dsxkline.DsxKline;
import com.dsxkline.android.dsxkline.HqModel;
import com.dsxkline.android.dsxkline.QQhq;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DsxKline kline;
    private static String code = "sh000001";
    // cycle 0=分时图 1=五日 2=日k 3=周k 4=年K，5=1分钟
    private static int cycle = 0;
    private static int page = 1;
    private static JSONArray datas = new JSONArray();

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager wm1 = this.getWindowManager();
        int width = wm1.getCurrentWindowMetrics().getBounds().width();
        kline = new DsxKline(this);

        setContentView(R.layout.activity_main);
        ScrollView scrollView = findViewById(R.id.scrollView);
        LinearLayout linearLayout = findViewById(R.id.scrollViewContent);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width,width);
        layoutParams.topMargin = 50;
        linearLayout.addView(kline,layoutParams);
        createTabbar();
        //addContentView(kline,layoutParams);
        // 开始请求并加载数据
        kline.onLoading = new DsxKline.ICallback() {
            @Override
            public void call(Object... args) {
                try {
                    getStockDatas(code);
                } catch (JSONException e) {
                    // 千万记得手动执行finish，要不然会卡住
                    kline.finishLoading();
                    e.printStackTrace();
                }
            }
        };
        // 自动加载下一页数据
        kline.nextPage = new DsxKline.ICallback() {
            @Override
            public void call(Object... args) {
                try {
                    getStockDatas(code);
                } catch (JSONException e) {
                    // 千万记得手动执行finish，要不然会卡住
                    kline.finishLoading();
                    e.printStackTrace();
                }
            }
        };
    }

    private void createTabbar(){
        Button bt0 = findViewById(R.id.bt0);
        Button bt1 = findViewById(R.id.bt1);
        Button bt2 = findViewById(R.id.bt2);
        Button bt3 = findViewById(R.id.bt3);
        Button bt4 = findViewById(R.id.bt4);
        Button bt5 = findViewById(R.id.bt5);
        bt0.setTag(0);
        bt1.setTag(1);
        bt2.setTag(2);
        bt3.setTag(3);
        bt4.setTag(4);
        bt5.setTag(5);
        View.OnClickListener click = new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                cycle = Integer.parseInt(v.getTag().toString());
                page = 1;
                datas = new JSONArray();
                kline.chartType = cycle>=2? DsxKline.ChartType.candle:cycle==0? DsxKline.ChartType.timeSharing: DsxKline.ChartType.timeSharing5;
                try {
                    kline.startLoading();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        bt0.setOnClickListener(click);
        bt1.setOnClickListener(click);
        bt2.setOnClickListener(click);
        bt3.setOnClickListener(click);
        bt4.setOnClickListener(click);
        bt5.setOnClickListener(click);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void getStockDatas(String code) throws JSONException {
        if(kline.chartType == DsxKline.ChartType.candle) getDay(code);
        if(kline.chartType.ordinal() <= DsxKline.ChartType.timeSharing5.ordinal())getQuotes(code);
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void getQuotes(String code) throws JSONException {
        try {
            List<HqModel> result = QQhq.getQuote(code);
            if (result != null) {
                HqModel model = result.get(0);
                kline.lastClose = Double.parseDouble(model.lastClose);
                if (cycle == 0) getTimeline(code);
                if (cycle == 1) getTimeline5(code);
            }
        }catch (Exception e){
            e.printStackTrace();
            kline.finishLoading();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void getTimeline(String code){
        List<String> result = null;
        try {
            result = QQhq.getTimeLine(code);
            JSONArray datas = new JSONArray(result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        kline.updateKline(datas);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    kline.finishLoading();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            kline.finishLoading();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void getTimeline5(String code) throws JSONException {
        Map<String,Object> result = QQhq.getFdayLine(code);
        if(result!=null){
            double lastClose = Double.parseDouble(result.get("lastClose").toString());
            kline.lastClose = lastClose;
            List<String> data = (List<String>) result.get("data");
            JSONArray datas = new JSONArray(data);
            runOnUiThread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void run() {
                    try {
                        kline.updateKline(datas);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    kline.finishLoading();
                }
            });
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void getDay(String code) throws JSONException {
        String cycleName = "day";
        if(cycle==2) cycleName = "day";
        if(cycle==3) cycleName = "week";
        if(cycle==4) cycleName = "month";
        if(cycle==5) cycleName = "m1";
        List<String> result = QQhq.getkLine(code,cycleName,"","",320,"qfq");
        if(!result.isEmpty()){
            try {
                JSONArray data = new JSONArray(result);
                if(data.length()>0){
                    if(page<=1) datas = new JSONArray();
                    String a = data.toString()+(datas.length()>0?datas.toString():"");
                    a = a.replace("][",",");
                    datas = new JSONArray(a);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                kline.page = page;
                                kline.updateKline(datas);
                                page ++;

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            kline.finishLoading();
                        }
                    });
                }


            } catch (JSONException e) {
                e.printStackTrace();
                kline.finishLoading();
            }
        }

    }


}