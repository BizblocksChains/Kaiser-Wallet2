package io.kaiser.kaiserwallet2.Threads;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import io.kaiser.kaiserwallet2.BuildConfig;

public class ExchangeThread extends Thread implements Runnable {


    private final long TIME_GAP = 1000 * 3;

    private final long remainTime = TIME_GAP;

    private final ArrayList<String> queueCoinTypes = new ArrayList<>();

    private boolean isRun = true;


    private String strURL = null;
    public HashMap<String, String> coinRateStorage = new HashMap<>();
    public HashMap<String, String> moneyRateStorage = new HashMap<>();

    public ExchangeThread(){
        Log.d("TTTT","ExchangeThread run over.");

        if (BuildConfig.BUILD_DEBUG){
            strURL = URL_BB_CURRENCY_DEBUG;
        }else{
            strURL = URL_BB_CURRENCY;
        }
    }

    @Override
    public void run() {
        super.run();
        Log.d("TTTT","ExchangeThread run over.");

        while(isRun){
            exchangeRate();

            try {
                Thread.sleep(TIME_GAP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void start()
    {
        isRun = true;
        super.start();
    }



    private void exchangeRate()
    {
        // get money exchage rate
        String strJson = "";
        try{
            URL obj = new URL(strURL);
            final String USER_AGENT = "Mozilla/5.0";
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Charset", "UTF-8");


            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer();

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            strJson = response.toString();

        }catch (Exception e) {
            Log.d("TTTT", "Exception");
        }

        String strData;
        try {
            JSONObject reJson = new JSONObject(strJson);
            String rr = reJson.getString("result");

            if (rr.equals("success")){
                strData = reJson.getString("data");
                parsingExchangeMoneyRate(strData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    public String getStringCoinName(String cointype){
        String result = "";
        switch (cointype){
            case "80000000":
            case "80000001":
                result = "BTC";
                break;
            case "80000002":
                result = "LTC";
                break;
            case "80000005":
                result = "DASH";
                break;
            case "80000085":
                result = "ZEC";
                break;
            case "8000003C":
                result = "ETH";
                break;
            case "8000003D":
                result = "ETC";
                break;
            case "80000091":
                result = "BCH";
                break;
            case "80000079":
                result = "ZEC";
                break;
            case "8000009C":
                result = "BTG";
                break;
            case "8000001C":
                result = "VTC";
                break;
            case "800008FD":
                result = "QTUM";
                break;
            case "80005E00":
                result = "KIS";
                break;
        }

        return result;

    }


    private void parsingExchangeMoneyRate(String strData)
    {
        if(strData == null)
        {
            return;
        }

        try {
            JSONObject json = new JSONObject(strData);
            JSONObject jsonMoneyRate = new JSONObject(json.getString("currency"));
            JSONObject jsonCoinRate  = new JSONObject(json.getString("rates"));

            Iterator<String> MoneyKeys = jsonMoneyRate.keys();
            while(MoneyKeys.hasNext())
            {
                String key = MoneyKeys.next();

                moneyRateStorage.put(key, jsonMoneyRate.getString(key));
            }

            Iterator<String> CoinKeys = jsonCoinRate.keys();
            while(CoinKeys.hasNext())
            {
                String key = CoinKeys.next();

                coinRateStorage.put(key, jsonCoinRate.getString(key));
            }

            Log.i("","");

        } catch (JSONException e) {
            Log.d("ExchangeRate", "ExchangeRate JSONException");
            e.printStackTrace();
        }
    }

    private void parsingExchangeCoinRate(String strJson/*, int coinMode*/){
        Double coinRate = 0.0;
        try {
            JSONArray jsonArr = new JSONArray(strJson);
            JSONObject json = jsonArr.getJSONObject(0);
            coinRate = Double.parseDouble(json.getString("tradePrice"));

        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }

        //dCoinRate = coinRate;
    }
}
