package cn.home.howoldapp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int PICK_CODE = 0x001;
    private ImageView mPhotoIV;
    private Button mDetectBT;
    private Button mGetImageBT;
    private TextView mTipTV;
    private View mWaittingFL;

    private String mCurrentPhotoStr;
    private Bitmap mPhotoImg;
    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initEvents();

        mPaint = new Paint();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK && requestCode == PICK_CODE){
            if (intent != null){
                Uri uri = intent.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();

                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mCurrentPhotoStr = cursor.getString(idx);
                cursor.close();

                resizePhoto();

                mPhotoIV.setImageBitmap(mPhotoImg);
                mTipTV.setText("Click Detect ==>");
            }
        }
    }

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mCurrentPhotoStr,options);
        double ratio = Math.max(options.outWidth/1024d,options.outHeight/1024d);
        options.inSampleSize = (int) Math.ceil(ratio);

        options.inJustDecodeBounds = false;
        mPhotoImg = BitmapFactory.decodeFile(mCurrentPhotoStr);
    }

    private void initEvents() {
        mGetImageBT.setOnClickListener(this);
        mDetectBT.setOnClickListener(this);
    }

    private void initViews() {
        mPhotoIV = (ImageView) findViewById(R.id.id_photo);
        mDetectBT = (Button) findViewById(R.id.id_detect);
        mGetImageBT = (Button) findViewById(R.id.id_getImage);
        mTipTV = (TextView) findViewById(R.id.id_tip);
        mWaittingFL = findViewById(R.id.id_waitting);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private static final int MSG_SUCCESS = 0x002;
    private static final int MSG_ERROR = 0x003;


    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_SUCCESS:
                    mWaittingFL.setVisibility(View.GONE);
                    JSONObject rs = (JSONObject) msg.obj;

                    prepareRsBitmap(rs);

                    mPhotoIV.setImageBitmap(mPhotoImg);

                    break;
                case MSG_ERROR:
                    mWaittingFL.setVisibility(View.GONE);
                    String errorMsg = (String) msg.obj;
                    if (TextUtils.isEmpty(errorMsg)){
                        mTipTV.setText("ERROR");
                    }else {
                        mTipTV.setText(errorMsg);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void prepareRsBitmap(JSONObject rs) {
        Bitmap bitmap = Bitmap.createBitmap(mPhotoImg.getWidth(),mPhotoImg.getHeight(),mPhotoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImg,0,0,null);
        try {
            JSONArray faces = rs.getJSONArray("face");
            int faceCount = faces.length();
            mTipTV.setText("get "+ faceCount);

            for (int i = 0 ; i < faceCount ; i++){
                JSONObject face = faces.getJSONObject(i);
                JSONObject pos = face.getJSONObject("position");

                float x = (float) pos.getJSONObject("center").getDouble("x");
                float y = (float) pos.getJSONObject("center").getDouble("y");

                float w = (float) pos.getDouble("width");
                float h = (float) pos.getDouble("height");

                x = x/100 * mPhotoImg.getWidth();
                y = y/100 * mPhotoImg.getHeight();
                w = w/100 * mPhotoImg.getWidth();
                h = h/100 * mPhotoImg.getHeight();

                mPaint.setColor(Color.WHITE);
                mPaint.setStrokeWidth(3);
                canvas.drawLine(x - w/2, y - h/2, x + w/2, y - h/2, mPaint);
                canvas.drawLine(x - w/2, y - h/2, x - w/2, y + h/2, mPaint);
                canvas.drawLine(x + w/2, y - h/2, x + w/2, y + h/2, mPaint);
                canvas.drawLine(x-w/2,y+h/2,x+w/2,y+h/2,mPaint);

                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                Bitmap ageBitmap = buildAgeBitmap(age, "Male".equals(gender));

                int ageWidth = ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();

                if ( bitmap.getWidth()< ageWidth && bitmap.getHeight()< ageHeight) {
                    float ratio = Math.max(bitmap.getWidth() * 1.0f/ageWidth,bitmap.getHeight()*1.0f/ageHeight);
                    ageBitmap = Bitmap.createScaledBitmap(ageBitmap,(int)(ageWidth*ratio),(int)(ageHeight*ratio),false);

                }

                canvas.drawBitmap(ageBitmap,x-ageBitmap.getWidth()/2,y-h/2-ageBitmap.getHeight(),null);

                mPhotoImg = bitmap;




            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView tv = (TextView) mWaittingFL.findViewById(R.id.id_age_and_gender);
        tv.setText(age + "");
        if (isMale) {
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.mipmap.male), null, null, null);
        }else {
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.mipmap.female), null, null, null);
        }
        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();
        return bitmap;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.id_getImage:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,PICK_CODE);
                break;

            case R.id.id_detect:
                mWaittingFL.setVisibility(View.VISIBLE);

                if (!TextUtils.isEmpty(mCurrentPhotoStr)){
                    resizePhoto();
                }else {
                    mPhotoImg = BitmapFactory.decodeResource(getResources(),R.drawable.t4);
                }
                FaceAppDetect.detect(mPhotoImg, new FaceAppDetect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        Message message = Message.obtain();
                        message.what = MSG_SUCCESS;
                        message.obj = result;
                        mHandler.sendMessage(message);
                    }

                    @Override
                    public void error(FaceppParseException e) {
                        Message message = Message.obtain();
                        message.what = MSG_ERROR;
                        message.obj = e.getErrorMessage();
                        mHandler.sendMessage(message);

                    }
                });
                break;
        }
    }
}
