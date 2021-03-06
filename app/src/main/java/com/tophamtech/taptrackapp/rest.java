package com.tophamtech.taptrackapp;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by jamestopham on 09/08/17.
 */
public class rest {

    public static final String auth = "http://tophamtech.noip.me:3000/api/auth/authenticate";
    public static final String setup = "http://tophamtech.noip.me:3000/api/auth/setup";
    public static final String data = "http://tophamtech.noip.me:3000/api/data";
    public static final String increment = "http://tophamtech.noip.me:3000/api/data/increment";
    public static final String init = "http://tophamtech.noip.me:3000/api/data/init";

    public static class restParams {
        String[][] data;
        String url;

        restParams(String[][] data, String url) {
            this.data = data;
            this.url = url;
        }
    }

    public class httpPost extends AsyncTask<restParams, Void, String> {

        private Context mContext;
        private String mTarget;
        String customError = "none";


        public httpPost(Context context) {
            mContext = context;
        }

        private String streamToString(InputStream in) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
        @Override
        protected String doInBackground(restParams... params) {
            URL url = null;
            try {
                switch (params[0].url){
                    case "auth" :
                        url = new URL(rest.auth);
                        break;
                    case "setup" :
                        url = new URL(rest.setup);
                        break;
                    case "data" :
                        url = new URL(rest.data);
                        break;
                    case "increment" :
                        url = new URL(rest.increment);
                        break;
                }

            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }

            // TODO: Fix create data button call
            HttpURLConnection serverConnection = null;
            try {
                serverConnection = (HttpURLConnection) url.openConnection();
                if (params[0].url.equals("data") || params[0].url.equals("increment")) {
                    String jwt = session.getJWT(mContext);
                    serverConnection.setRequestProperty("x-access-token", jwt);
                }
                serverConnection.setDoOutput(true);
                serverConnection.setConnectTimeout(1000);
                serverConnection.setReadTimeout(1000);
                serverConnection.setRequestMethod("POST");
                serverConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                serverConnection.connect();
                //Once the connection is open, write the body
                OutputStreamWriter writer = new OutputStreamWriter(serverConnection.getOutputStream(), "UTF-8");
                StringBuilder sbData = new StringBuilder();
                mTarget = params[0].data[0][1];
                for (int i=0;i<params[0].data.length;i++)
                {
                    if (i!=0){
                        sbData.append("&");
                    }
                    sbData.append(params[0].data[i][0] + "=" + params[0].data[i][1]);
                }
                //writer.write(params[0][0][0]+"="+params[0][0][1]+"&"+params[0][1][0]+"="+params[0][1][1]);
                writer.write(sbData.toString());
                writer.close();

                //read back the server response
                InputStream in = new BufferedInputStream(serverConnection.getInputStream());
                return streamToString(in);

            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                if (e.getMessage().contains("host")){
                    customError = "connectionIssue";
                }
                e.printStackTrace();
            } finally {
                serverConnection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            signInActivity signIn = new signInActivity();
            JSONObject jObject = null;
            if (customError.equals("connectionIssue")) {
                signIn.invalidCreds(mContext, "Error","It appears we're having some other technical difficulties.");
            }
            if (result != null) {
                try {
                    jObject = new JSONObject(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    switch (jObject.getString("id")){
                        case "100":
                            session.setJWT(jObject.getString("token"),mContext);
                            signIn.validCreds();
                            break;
                        case "107":
                            helper.toastMaker(mContext, "Successfully signed up!");
                            mContext.startActivity(new Intent(mContext, signInActivity.class));
                            break;
                        case "115":
                        case "116":
                            // if app already open, just update the fragment other wise start the full activity
                            // TODO: On increment call, find which target, id target fragment then update data
                                Intent updateFragIntent = new Intent(mContext, homeActivity.class);
                                updateFragIntent.putExtra("updateTarget", mTarget);
                                mContext.startActivity(updateFragIntent);
                            break;
                        default:
                            signIn.invalidCreds(mContext, "Error","Incorrect username or password");
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    public class httpGet extends AsyncTask<Void, Void, String> {

        private Context mContext;

        public httpGet(Context context) {
            mContext = context;
        }

        private String streamToString(InputStream in) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                result.append(line);
            }
            Log.d("fans", result.toString());
            return result.toString();
        }
        @Override
        protected String doInBackground(Void... params) {
            String jwt = session.getJWT(mContext);

            URL url = null;
            try {
                url = new URL(rest.init);
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection serverConnection = null;
            try {
                serverConnection = (HttpURLConnection) url.openConnection();
                serverConnection.setRequestMethod("GET");
                serverConnection.setRequestProperty("x-access-token", jwt);
                serverConnection.connect();

                //read back the server response
                InputStream in = new BufferedInputStream(serverConnection.getInputStream());
                return streamToString(in);

            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                serverConnection.disconnect();
            }
            return "Error";
        }
    }
}
