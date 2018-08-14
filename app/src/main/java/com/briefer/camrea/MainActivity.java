package com.briefer.camrea;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.briefer.camrea.camrea.CameraHelper;
import com.briefer.camrea.camrea.OnCaptureCallback;
import com.briefer.camrea.view.MaskSurfaceView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends Activity implements OnCaptureCallback {
    private static final int STORAGE_REQUEST_CODE = 000;
    private static final int CAMREA = 111;

    private MaskSurfaceView surfaceview;
    private ImageView imageView;
    //	拍照
    private Button btn_capture;
    //	重拍
    private Button btn_recapture;
    //	取消
    private Button btn_cancel;
    //	确认
    private Button btn_ok;

    //	拍照后得到的保存的文件路径
    private String filepath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        requestPermission();

        this.setContentView(R.layout.activity_main);

        this.surfaceview = (MaskSurfaceView) findViewById(R.id.surface_view);
        this.imageView = (ImageView) findViewById(R.id.image_view);
        btn_capture = (Button) findViewById(R.id.btn_capture);
        btn_recapture = (Button) findViewById(R.id.btn_recapture);
        btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_cancel = (Button) findViewById(R.id.btn_cancel);

//		设置矩形区域大小
        this.surfaceview.setMaskSize(900, 600);

//		拍照
        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_capture.setEnabled(false);
                btn_ok.setEnabled(true);
                btn_recapture.setEnabled(true);
                CameraHelper.getInstance().tackPicture(MainActivity.this);
            }
        });

//		重拍
        btn_recapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_capture.setEnabled(true);
                btn_ok.setEnabled(false);
                btn_recapture.setEnabled(false);
                imageView.setVisibility(View.GONE);
                surfaceview.setVisibility(View.VISIBLE);
                deleteFile();
                CameraHelper.getInstance().startPreview();
            }
        });

//		确认
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Bitmap bitmap = BitmapFactory.decodeFile(filepath);
                Toast.makeText(MainActivity.this, "图片高度：" + bitmap.getHeight() + "..." + "图片宽度：" + bitmap.getWidth() + "...图片大小：" + bitmap.getByteCount()/10240, Toast.LENGTH_LONG).show();

                //上传服务器到后台
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] datas = baos.toByteArray();
                String imageDatasString = Base64.encodeToString(datas, Base64.DEFAULT);
                xinYanShiBie(Base64.encodeToString(datas, Base64.NO_WRAP));
            }
        });

//		取消
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                deleteFile();
                finish();
            }
        });
    }

    /**
     * 删除图片文件呢
     */
    private void deleteFile(){
        if(this.filepath==null || this.filepath.equals("")){
            return;
        }
        File f = new File(this.filepath);
        if(f.exists()){
            f.delete();
        }
    }

    @Override
    public void onCapture(boolean success, String filepath) {
        this.filepath = filepath;
        String message = "拍照成功";
        if(!success){
            message = "拍照失败";
            CameraHelper.getInstance().startPreview();
            this.imageView.setVisibility(View.GONE);
            this.surfaceview.setVisibility(View.VISIBLE);
        }else{
            this.imageView.setVisibility(View.VISIBLE);
            this.surfaceview.setVisibility(View.GONE);
            this.imageView.setImageBitmap(BitmapFactory.decodeFile(filepath));
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    //请求视频和存储权限
    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMREA);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST_CODE || requestCode == CAMREA) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
                Toast.makeText(this, "相机和存储权限必须打开", Toast.LENGTH_SHORT).show();
            }else {
                //....
            }
        }
    }


    //发送图片到后台
    private void xinYanShiBie(final String imageString) {
        //请求
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Log.e("上传", "开始");

                    // 0.相信证书

                    // 1. 获取访问地址URL
                    URL url = new URL("http://10.6.20.13:8080/s/idcard/uploadBack");
                    // 2. 创建HttpURLConnection对象
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    // 3. 设置请求参数等
                    // 请求方式
                    connection.setRequestMethod("POST");
                    // 超时时间
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(30000);
                    // 设置是否输出
                    connection.setDoOutput(true);
                    // 设置是否读入
                    connection.setDoInput(true);
                    // 设置是否使用缓存
                    connection.setUseCaches(false);
                    // 设置此 HttpURLConnection 实例是否应该自动执行 HTTP 重定向
                    connection.setInstanceFollowRedirects(true);
                    // 设置使用标准编码格式编码参数的名-值对
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");//使用的是表单请求类型
                    // 连接
                    connection.connect();
                    // 4. 处理输入输出
                    // 写入参数到请求中

                    String params = "member_id" + "=" + "8000013189" + "&" +
                            "data_type" + "=" + "json" + "&" +
                            //"image" + "=" + URLEncoder.encode(imageString, "utf-8");//如果图片使用Base64 的 DEFAULT 那么传输的时候使用URLEncoder编码一下被Base64编码过的图片，否则会出现\n \r
                            "image" + "=" + imageString;//如果图片使用Base64 的 NO_WRAP 那么不需求编码，直接传输

                    Log.e("上传", "请求参数：" + params);

                    OutputStream out = connection.getOutputStream();
                    out.write(params.getBytes());
                    out.flush();
                    out.close();
                    // 从连接中读取响应信息
                    String msg = "";
                    int code = connection.getResponseCode();
                    if (code == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            msg += line + "\n";
                        }
                        reader.close();
                    }
                    // 5. 断开连接
                    connection.disconnect();

                    //loading

                    // 处理结果
                    //sysData(msg);
                    Log.e("上传", "请求结果：" + msg);
                } catch (Exception e) {
                    //loading
                    Log.e("上传", "异常：" + e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "请求出错，请检查网络设置！", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();


    }
}
