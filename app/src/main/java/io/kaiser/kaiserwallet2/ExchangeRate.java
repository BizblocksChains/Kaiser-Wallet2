package io.kaiser.kaiserwallet2;

import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

import io.kaiser.util.TestClass;


public class ExchangeRate{

    private static final String TAG = ExchangeRate.class.getName();

    private double  bRateKRW = 0.0;
    private double  bRateEUR= 0.0;
    private double  bRateCNY= 0.0;
    private double  bRateJPY= 0.0;
    private double  bRateHKD= 0.0;



    protected ExchangeRate(String strCoinType){

        String rateURL = "";
        if (BuildConfig.BUILD_DEBUG){
            rateURL = URL_BB_CURRENCY_DEBUG;
        }else{
            rateURL = URL_BB_CURRENCY;
        }


        // get money exchage rate
        String strJson = "";
        try{
            URL obj = new URL(rateURL);
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
            Log.d(TAG, "Exception");
        }

        parsingExchangeMoneyRate(strJson);

        /////////////////////////


        String strCoinName = getStringCoinName(strCoinType);


        try{
            URL obj_coin = new URL(URL_COIN_RATE+strCoinName);
            final String USER_AGENT = "Mozilla/5.0";
            HttpURLConnection con_coin = (HttpURLConnection) obj_coin.openConnection();
            con_coin.setRequestMethod("GET");
            con_coin.setRequestProperty("User-Agent", USER_AGENT);
            con_coin.setRequestProperty("Accept-Charset", "UTF-8");


            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con_coin.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer();

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            strJson = response.toString();

        }catch (Exception e){
            Log.d(TAG, "Exception");
        }
        parsingExchangeCoinRate(strJson);

//        getExchange();
    }

    private String getStringCoinName(String cointype){
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

    public String getExcheageValue(String strMoneyType, double dCoinValue, boolean isContract){
        double result = 0.0;
        strMoneyType = strMoneyType.toUpperCase();
        switch (strMoneyType) {
            case "KRW":
                result = dCoinRate * dCoinValue;
                break;
            case "USD":
                result = (dCoinRate * dCoinValue) / bRateKRW;
                break;
            case "EUR":
                result = ((dCoinRate * dCoinValue) / bRateKRW) * bRateEUR;
                break;
            case "CNY":
                result = ((dCoinRate * dCoinValue) / bRateKRW) * bRateCNY;
                break;
            case "JPY":
                result = ((dCoinRate * dCoinValue) / bRateKRW) * bRateJPY;
                break;
            case "HKD":
                result = ((dCoinRate * dCoinValue) / bRateKRW) * bRateHKD;
                break;

        }
        if (isContract){
            return "0.0";
        }else{

            return decimalFormatMoney(result);
        }
    }

    private void parsingExchangeCoinRate(String strJson/*, int coinMode*/){
        Double coinRate = 0.0;
        try {
            JSONArray jsonArr = null;

            Object json = new JSONTokener(strJson).nextValue();
            if(json instanceof JSONObject)
            {
                return;
            }
            else
            {
                jsonArr = (JSONArray) json;
            }

            JSONObject jsonObj = null;
            if(jsonArr.length() > 0)
            {
                jsonObj = jsonArr.getJSONObject(0);
                coinRate = TestClass.parseDouble(jsonObj.getString("tradePrice"),"1");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        dCoinRate = coinRate;
    }

    private void parsingExchangeMoneyRate(String strJson){
        try {
            JSONObject json = new JSONObject(strJson);
            JSONObject jsonNote = new JSONObject(json.getString("note"));
            JSONObject jsonRate = new JSONObject(jsonNote.getString("rates"));

            bRateKRW = Double.parseDouble(jsonRate.getString("KRW"));
            bRateEUR = Double.parseDouble(jsonRate.getString("EUR"));
            bRateCNY = Double.parseDouble(jsonRate.getString("CNY"));
            bRateJPY = Double.parseDouble(jsonRate.getString("JPY"));
            bRateHKD = Double.parseDouble(jsonRate.getString("HKD"));

            Log.i("","");

        } catch (JSONException e) {
            Log.d("ExchangeRate", "ExchangeRate JSONException");
            e.printStackTrace();
        }
    }

    private String decimalFormatMoney(double money){

        DecimalFormat dFormat = new DecimalFormat("###,###.###");//콤마
        String result_int = dFormat.format(money);

        return result_int;
    }
}
