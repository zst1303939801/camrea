package com.briefer.camrea.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.briefer.camrea.R;
import com.briefer.camrea.camrea.CameraHelper;
import com.briefer.camrea.util.UiUtil;

/**
 * 自定义蒙版相机
 * 思路：
 * --1、自定义相机蒙版
 * --2、自定义周边阴影，中间透明蒙版
 */
public class MaskSurfaceView extends FrameLayout {

    private Context context;
    private MSurfaceView surfaceView;//加载相机的蒙版
    private MaskView maskView;//加载布局的蒙版
    private int width;//屏幕宽度
    private int height;//屏幕高度
    private int maskWidth;//中部透明区域宽度
    private int maskHeight;//中部透明区域高度
    private int screenWidth;//相机拍照截取宽度
    private int screenHeight;//相机拍照截取高度

    public MaskSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        surfaceView = new MSurfaceView(context);//第一层画布用来加载相机
        maskView = new MaskView(context);//第二层画布用来画所有布局
        this.addView(surfaceView, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        this.addView(maskView, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        screenHeight = display.getHeight();
        screenWidth = display.getWidth();
        CameraHelper.getInstance().setMaskSurfaceView(this);
    }

    public void setMaskSize(Integer width, Integer height) {
        maskHeight = height;
        maskWidth = width;
    }

    public int[] getMaskSize() {
        return new MaskSize().size;
    }

    private class MaskSize {
        private int[] size;

        private MaskSize() {
            this.size = new int[]{maskWidth, maskHeight, width, height};
        }
    }

    /**
     * 承载相机的布局
     */
    private class MSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder holder;

        public MSurfaceView(Context context) {
            super(context);
            this.holder = this.getHolder();
            //translucent半透明 transparent透明
            this.holder.setFormat(PixelFormat.TRANSPARENT);
            this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            this.holder.addCallback(this);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            width = w;
            height = h;
            CameraHelper.getInstance().openCamera(holder, format, width, height, screenWidth, screenHeight);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            CameraHelper.getInstance().releaseCamera();
        }
    }

    /**
     * 蒙层所有布局
     */
    private class MaskView extends View {
        private Paint linePaint;
        private Paint rectPaint;
        private Paint topTextPaint;
        private Paint bottomTextPaint;

        public MaskView(Context context) {
            super(context);

            //绘制中间透明区域矩形边界的Paint
            linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setColor(Color.TRANSPARENT);//设置中间区域颜色为透明
            linePaint.setStyle(Style.STROKE);
            linePaint.setStrokeWidth(3f);
            linePaint.setAlpha(0);//取值范围为0~255，数值越小越透明

            //绘制四周矩形阴影区域
            rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            rectPaint.setColor(Color.BLACK);
            rectPaint.setStyle(Style.FILL);
            rectPaint.setAlpha(170);//取值范围为0~255，数值越小越透明

            //绘制顶部中间提示字体
            topTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            topTextPaint.setColor(Color.WHITE);
            //topTextPaint.setStyle(Paint.Style.FILL);
            topTextPaint.setTextAlign(Paint.Align.CENTER);//把x,y坐标放到字体中间（默认x,y坐标是字体头部）
            topTextPaint.setTextSize(UiUtil.sp2px(context, 14));

            //绘制顶部中间提示字体
            bottomTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bottomTextPaint.setColor(Color.parseColor("#A0A0A0"));
            //bottomTextPaint.setStyle(Paint.Style.FILL);
            bottomTextPaint.setTextAlign(Paint.Align.CENTER);//把x,y坐标放到字体中间（默认x,y坐标是字体头部）
            bottomTextPaint.setTextSize(UiUtil.sp2px(context, 12));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (maskHeight == 0 && maskWidth == 0) {
                return;
            }
            if (maskHeight == height || maskWidth == width) {
                return;
            }

            if ((height > width && maskHeight < maskWidth) || (height < width && maskHeight > maskWidth)) {
                int temp = maskHeight;
                maskHeight = maskWidth;
                maskWidth = temp;
            }

            //height：屏幕高度
            //width：屏幕宽度
            //maskHeight：中间透明区域高度
            //maskWidth：中间透明区域宽度
            int h = Math.abs((height - maskHeight) / 2);//顶部阴影高度
            int w = Math.abs((width - maskWidth) / 2);//右侧阴影宽度

            //上阴影
            canvas.drawRect(0, 0, width, h, this.rectPaint);
            //右阴影
            canvas.drawRect(width - w, h, width, height - h, this.rectPaint);
            //下阴影
            canvas.drawRect(0, height - h, width, height, this.rectPaint);
            //左阴影
            canvas.drawRect(0, h, w, h + maskHeight, this.rectPaint);
            //中透明
            canvas.drawRect(w, h, w + maskWidth, h + maskHeight, this.linePaint);
            canvas.save();//保存上面的上下左右中
            //中-顶部-字体
            canvas.rotate(90, width - w / 2, height / 2);//把画布旋转旋转90度
            canvas.drawText("请扫描本人身份证人像面", width - w / 2, height / 2, topTextPaint);
            canvas.restore();//把画布恢复到上次保存的位置，防止本次旋转影响下面的操作
            canvas.save();//保存中顶部字体
            //中-底部-字体
            canvas.rotate(90, w / 2, height / 2);//旋转90度
            canvas.drawText("请保持光线充足，背景干净，手机与卡片持平", w / 2, height / 2, bottomTextPaint);
            canvas.restore();//把画布恢复到上次保存的位置，防止本次旋转影响下面的操作
            canvas.save();//保存中底部字体
            //身份证头像框 - 画图片，就是贴图
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_face_border);
            canvas.rotate(90, width - w  - 80, height - h - bitmap.getHeight() - 30);//把画布旋转旋转90度
            canvas.drawBitmap(bitmap, width - w  - 80, height - h - bitmap.getHeight() - 30, new Paint());

            //打印logo
            Log.e("高度宽度", "height:" + height + ",width:" + width + ",h:" + h + ",w:" + w + ",mskHeight:" + maskHeight + ",maskWidth:" + maskWidth);

            super.onDraw(canvas);
        }
    }
}
