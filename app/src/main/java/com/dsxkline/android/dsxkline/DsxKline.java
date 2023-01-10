package com.dsxkline.android.dsxkline;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.List;

public class DsxKline  extends WebView {

    // 图表类型
    public enum ChartType{
        timeSharing,    // 分时图
        timeSharing5,   // 五日分时图
        candle,         // K线图
    } ;

    // 蜡烛图实心空心
    public enum CandleType{
        hollow, // 空心
        solid   // 实心
    } ;
    // 缩放K线锁定类型
    public enum ZoomLockType{
        none,       // 无
        left,       // 锁定左边进行缩放
        middle,     // 锁定中间进行缩放
        right,      // 锁定右边进行缩放
        follow,     // 跟随鼠标位置进行缩放，web版效果比较好
    } ;

    public interface ICallback{
        public void call(Object ... args);
    }

    // 首页地址
    private static final String HOMEHTML = "file:///android_asset/dsxkline/index.html";

    // K线数据
    public List<String> datas;
    // 主题 white dark 等
    public String theme = "white";
    // 图表类型 1=分时图 2=k线图
    public ChartType chartType = ChartType.timeSharing;
    // 蜡烛图k线样式 1=空心 2=实心
    public CandleType candleType = CandleType.hollow;
    // 缩放类型 1=左 2=中 3=右 4=跟随
    public ZoomLockType zoomLockType = ZoomLockType.right;
    // 每次缩放大小
    public double zoomStep = 1;
    // k线默认宽度
    public double klineWidth = 5;
    // 是否显示默认k线提示
    public boolean isShowKlineTipPannel;
    // 副图高度
    public double sideHeight = 60;
    // 高度
    public double height;
    // 宽度
    public double width;
    // 默认主图指标 ["MA"]
    public String[] main = new String[]{"MA"};
    // 默认副图指标 副图数组代表副图数量 ["VOL","MACD"]
    public String[] sides = new String[]{"VOL","MACD","RSI"};
    // 昨日收盘价
    public double lastClose;
    // 首次加载回调
    public ICallback onLoading;
    // 完成加载回调
    public ICallback updateComplate;
    // 滚动到左边尽头回调 通常用来加载下一页数据
    public ICallback nextPage;
    // 提示数据返回
    public ICallback onCrossing;
    // 右边空出k线数量
    public int rightEmptyKlineAmount = 2;
    // 当前页码
    public int page = 1;
    // 开启调试
    public boolean debug = false;


    public DsxKline(Context context) {
        super(context);
        createViews();
    }

    public DsxKline(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        createViews();
    }

    public DsxKline(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        createViews();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DsxKline(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        createViews();
    }

    private void createViews(){
        setSettings();
        loadUrl(HOMEHTML);
        pageEvent();
    }
    private void setSettings() {
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);; //设置WebView属性，能够执行Javascript脚本
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        settings.setAllowFileAccess(true); //设置可以访问文件
        settings.setBuiltInZoomControls(false); //设置不支持缩放
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAppCacheEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    @SuppressLint("NewApi")
    private void executeJs(String js){
        System.out.println(js);
        try {
            evaluateJavascript(js, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    System.out.println(value);
                }
            });
        }catch (Exception e){

        }

    }

    /**
     * 初始化并创建K线图组件
     * @throws JSONException
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void createKline() throws JSONException {
        String m = new JSONArray(main).toString();
        String s = new JSONArray(sides).toString();
        if(width<=0) width = getWidth();
        if(height<=0) width = getHeight();
        String js = "var page = 1; "+
                "var c=document.getElementById(\"kline\");"+
                "var kline = new dsxKline({"+
                    "element:c,"+
                    "chartType:"+chartType.ordinal()+","+
                    "theme:\""+theme+"\", "+
                    "candleType:"+candleType.ordinal()+","+
                    "zoomLockType:"+zoomLockType.ordinal()+","+
                    "isShowKlineTipPannel:"+(isShowKlineTipPannel?"true":"false")+","+
                    "lastClose:"+lastClose+","+
                    "sideHeight:"+sideHeight+","+
                    "paddingBottom:0,"+
                    "rightEmptyKlineAmount:"+rightEmptyKlineAmount+","+
                    "autoSize:true,"+
                    "debug:"+(debug?"true":"false")+","+
                    "main:"+ m +","+
                    "sides:"+s+","+
                    "onLoading:function(o){"+
                            "window.injectedDsxKline.onLoading();"+
                    "},"+
                    "nextPage:function(data,index){"+
                        "window.injectedDsxKline.nextPage();"+
                    "},"+
                    "onCrossing:function(data,index){"+
                        "window.injectedDsxKline.onCrossing(JSON.stringify(data),index);"+
                    "},"+
                    "updateComplate:function(){"+
                        "window.injectedDsxKline.updateComplate();"+
                    "}"+
                "});";
        executeJs(js);
    }

    /**
     * 更新K线图
     * @param data
     * @throws JSONException
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void updateKline(JSONArray data) throws JSONException {
        String m = new JSONArray(main).toString();
        String s = new JSONArray(sides).toString();
        String js = "var data = "+(data.toString())+";"+
                "if(data!=null){"+
                    "if(kline!=null){"+
                        "kline.update({"+
                            "chartType:"+chartType.ordinal()+",  "+
                            "theme:\""+theme+"\","+
                            "candleType:"+candleType.ordinal()+","+
                            "zoomLockType:"+zoomLockType.ordinal()+","+
                            "isShowKlineTipPannel:"+(isShowKlineTipPannel?"true":"false")+","+
                            "lastClose:"+lastClose+","+
                            "sides:"+s+","+
                            //"main:"+m+","+
                            "page:"+page+","+
                            "sideHeight:"+sideHeight+","+
                            "datas:data"+
                        "});"+
                    "}"+
                "};" +
                "kline.finishLoading();";

       executeJs(js);
    }

    /**
     * 加载数据前调用
     * @throws JSONException
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void startLoading() throws JSONException {
        String js = "kline.chartType="+chartType.ordinal()+";kline.startLoading();";
        executeJs(js);
    }

    /**
     * 更新完K线图后调用
     */
    public void finishLoading(){
        String js = "kline.finishLoading();";
        executeJs(js);
    }

    /**
     * JS
     */
    private void pageEvent(){
        // js 通信
        addJavascriptInterface(new DsxKlineJavascriptInterface(getContext()), "injectedDsxKline");
        // 网页加载事件
        setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                System.out.println("创建K线图");
                try {
                    createKline();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
            }
        });
    }

    public class DsxKlineJavascriptInterface {
        private Context context;

        public DsxKlineJavascriptInterface(Context context) {
            this.context = context;
        }

        /**
         * 开始加载数据
         */
        @JavascriptInterface
        public void onLoading() {
           if(onLoading!=null){
               onLoading.call();
           }
        }

        /**
         * 滚动到最左边，继续加载下一页数据
         */
        @JavascriptInterface
        public void nextPage() {
            if(nextPage!=null){
                nextPage.call();
            }
        }
        @JavascriptInterface
        public void onCrossing(String data,int index){
            if(onCrossing!=null){
                onCrossing.call(data,index);
            }
        }

        @JavascriptInterface
        public void updateComplate(){
            if(updateComplate!=null){
                updateComplate.call();
            }
        }


    }
}