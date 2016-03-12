package hochschuledarmstadt.photostream_tools;

import android.os.AsyncTask;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import hochschuledarmstadt.photostream_tools.model.Photo;

/**
 * Created by Andreas Schattney on 19.02.2016.
 */
class StorePhotoAsyncTask extends AsyncTask<JSONObject, Void, Photo> {

    private static final String TAG = StorePhotoAsyncTask.class.getName();
    public static final int CONNECT_TIMEOUT_IN_MILLIS = 6000;
    public static final String UTF_8 = "UTF-8";
    private final OnPhotoStoredCallback callback;
    private final String installationId;

    public StorePhotoAsyncTask(String installationId, OnPhotoStoredCallback callback){
        this.callback = callback;
        this.installationId = installationId;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Photo doInBackground(JSONObject... params) {
        try {
            return uploadPhoto(params[0]);
        } catch (IOException e) {
            Logger.log(TAG, LogLevel.ERROR, e.toString());
        }
        return null;
    }

    private Photo uploadPhoto(JSONObject jsonObject) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) new URL("http://5.45.97.155:8081/photostream/image").openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoOutput(true);
        urlConnection.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLIS);
        urlConnection.addRequestProperty("installation_id", installationId);
        urlConnection.addRequestProperty("Content-Type", "application/json");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), Charset.forName(UTF_8)));
        String s = jsonObject.toString();
        writer.write(s, 0, s.length());
        writer.flush();
        writer.close();

        int status = urlConnection.getResponseCode();

        Photo photo = null;

        if (status == HttpURLConnection.HTTP_OK){
            InputStreamReader reader = new InputStreamReader(urlConnection.getInputStream(), Charset.forName("UTF-8"));
            char[] buffer = new char[4096];
            StringBuilder stringBuilder = new StringBuilder();
            int read;
            while((read = reader.read(buffer, 0, buffer.length)) != -1){
                stringBuilder.append(buffer,0,read);
            }
            Gson gson = new Gson();
            photo = gson.fromJson(stringBuilder.toString(), Photo.class);
        }

        return photo;
    }

    @Override
    protected void onPostExecute(Photo photo) {
        super.onPostExecute(photo);
        if (photo != null)
            callback.onPhotoStoreSuccess(photo);
        else
            callback.onPhotoStoreError();
    }

    public interface OnPhotoStoredCallback {
        void onPhotoStoreSuccess(Photo photo);
        void onPhotoStoreError();
    }

}