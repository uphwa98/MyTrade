package com.shim.myapplication;

import android.util.Log;

import com.shim.myapplication.bit.Api_Client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.HashMap;

public class MyTrade {
    private final String TAG = "DEBUG_" + this.getClass().getSimpleName();

    private Api_Client mApi = new Api_Client("",
            "");

    private int mCount;

    private String mCurrency;
    private Float mSellRatio = 0.9F; // 10%
    private double mBuyingPrice;
    private Float mTotalBalance;
    private double mMaxPrice;

    public MyTrade(String currency) {
        mCurrency = currency;
    }

    public void setSellRatio(Float sellLimit) {
        mSellRatio = 1 - (sellLimit / 100F);
    }

    public Float getTotalBalance() {
        return mTotalBalance;
    }

    public void initialize() {
        mTotalBalance = getMyBalance(mCurrency);
        Log.i(TAG, "mTotalBalance : " + mTotalBalance);
        mBuyingPrice = getMyBuyingPrice(mCurrency);
    }

    public Price runLoop() {
        mCount++;
        if (mCount > 10000) {
            Log.i(TAG, "Stop here");
            return null;
        }

        if (mTotalBalance < 0.2) {
            Log.i(TAG, "Nothing to sell");
            return null;
        }

        Float currentPrice = getCurrentPrice(mCurrency);
        if (currentPrice < 0) {
            Log.i(TAG, "network error");
            return null;
        } else {
            if (currentPrice > mMaxPrice) {
                mMaxPrice = currentPrice;
            }

            if (currentPrice < mSellRatio * mBuyingPrice) {
                String result = sellNow(mCurrency, mTotalBalance);
                return new Price(-1f, 0, mBuyingPrice);
            }

            if (currentPrice < 0.98 * mMaxPrice) {
                if (currentPrice > 1.02 * mBuyingPrice) {
//                    String result = sellNow(mCurrency, mTotalBalance);
//                    return new Price(-1f, 0, mBuyingPrice);
                }
            }
        }

        return new Price(currentPrice, mMaxPrice, mBuyingPrice);
    }

    public String sellNow() {
        String result = sellNow(mCurrency, mTotalBalance);

        mTotalBalance = getMyBalance(mCurrency);

        return result;
    }

    public int buyNow(int count) {
        final HashMap<String, String> rgParams = new HashMap<>();

        rgParams.put("units", "1");
        rgParams.put("currency", mCurrency);

        try {
            String result = mApi.callApi("/trade/market_buy", rgParams);
            JSONObject json = new JSONObject(result);
            Log.v(TAG, "sell result : " + json);
        } catch (Exception e) {
            Log.d(TAG, "failed : " + e.getMessage());
        }

        return 0;
    }

    private Float getMyBalance(String reqCurrency) {
        HashMap<String, String> rgParams = new HashMap<>();
        rgParams.put("currency", reqCurrency);

        try {
            String result = mApi.callApi("/info/balance", rgParams);

            JSONObject json = new JSONObject(result);
            Log.v(TAG, "getMyBalance result : " + json);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                JSONObject data = json.getJSONObject("data");
                String total_currency = data.getString("total_" + reqCurrency.toLowerCase());
                String available_currency = data.getString("available_" + reqCurrency.toLowerCase());

                return Float.valueOf(available_currency);
            } else {
                return 0F;
            }
        } catch (Exception e) {
            return 0F;
        }
    }

    private int getMyBuyingPrice(String reqCurrency) {
        int count = 1;
        HashMap<String, String> rgParams = new HashMap<>();
        rgParams.put("offset", "0");
        rgParams.put("count", String.valueOf(count));
        rgParams.put("searchGb", "1");
        rgParams.put("currency", reqCurrency);

        try {
            String result = mApi.callApi("/info/user_transactions", rgParams);

            JSONObject json = new JSONObject(result);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                Log.v(TAG, "getMyBuyingPrice result : " + json);
                JSONArray data = json.getJSONArray("data");
                JSONObject first = data.getJSONObject(0);
                String buyingPrice = first.getString(reqCurrency.toLowerCase() + "1krw");

                return Integer.valueOf(buyingPrice);
            } else {
                return 0;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    private String sellNow(final String reqCurrency, final float units) {
        final HashMap<String, String> rgParams = new HashMap<>();
        DecimalFormat format = new DecimalFormat(".#####");
        String str = format.format(units);

        rgParams.put("units", str.substring(0, 5));
        rgParams.put("currency", reqCurrency);

        try {
            String result = mApi.callApi("/trade/market_sell", rgParams);
            JSONObject json = new JSONObject(result);
            Log.v(TAG, "sell result : " + json);

            return result;
        } catch (Exception e) {
            Log.d(TAG, "failed : " + e.getMessage());
            return "failed : " + e.getMessage();
        }
    }

    private Float getCurrentPrice(String reqCurrency) {
        HashMap<String, String> rgParams = new HashMap<>();

        try {
            String result = mApi.callApi("/public/ticker/" + reqCurrency, rgParams);

            JSONObject json = new JSONObject(result);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                Log.v(TAG, "getCurrentPrice result : " + json);
                JSONObject data = json.getJSONObject("data");
                String buy_price = data.getString("buy_price");

                return Float.valueOf(buy_price);
            } else {
                return 0f;
            }
        } catch (Exception e) {
            Log.d(TAG, "failed : " + e.getMessage());
            return -1f;
        }
    }

    public String getOrderbook() {
        HashMap<String, String> rgParams = new HashMap<>();

        rgParams.put("group_orders", "1");
        rgParams.put("count", "15");

        try {
            String result = mApi.callApi("/public/orderbook/" + mCurrency, rgParams);

            JSONObject json = new JSONObject(result);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                JSONObject data = json.getJSONObject("data");

                JSONArray bids = data.getJSONArray("bids");
                int length = bids.length();
                for (int i = 0; i < length; i++) {
                    Log.v(TAG, "bids " + i + " " + bids.getString(i));
                }

                JSONArray asks = data.getJSONArray("asks");
                length = asks.length();
                for (int i = 0; i < length; i++) {
                    Log.v(TAG, "bids " + i + " " + asks.getString(i));
                }

                return result;
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.d(TAG, "failed : " + e.getMessage());
            return null;
        }
    }
}
