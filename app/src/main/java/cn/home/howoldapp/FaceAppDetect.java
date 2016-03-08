package cn.home.howoldapp;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by Zhichao on 2016/3/6.
 */
public class FaceAppDetect {
    public interface CallBack{
        void success(JSONObject result);
        void error(FaceppParseException e);
    }

    public static void detect(final Bitmap bitmap , final CallBack callBack){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpRequests requests = new HttpRequests(Constants.KEY,Constants.SECRET);
                    //这个方法可以用来截图
                    Bitmap bmSmall = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    byte[] array = stream.toByteArray();

                    PostParameters parameters = new PostParameters();
                    parameters.setImg(array);

                    JSONObject jsonObject = requests.detectionDetect(parameters);
                    Log.e("TAG",jsonObject.toString());
                    if (callBack != null){
                        callBack.success(jsonObject);
                    }
                } catch (FaceppParseException e) {
                    e.printStackTrace();
                    if (callBack != null){
                        callBack.error(e);
                    }
                }
            }
        }).start();
    }
}
