package com.ofilm.cameraview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.ofilm.camera.R;
import com.ofilm.camera1.Camera1;
import com.ofilm.camera2.Camera2;
import com.ofilm.camera2Api23.Camera2Api23;
import com.ofilm.utils.AspectRatio;
import com.ofilm.utils.Constants;
import com.ofilm.utils.LogUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Set;


/**
*    负责运转 activity 与 camera之间的操作, 对外暴露了几个封装的方法, 外部只要调用即可完成相机操作, 无需知道实现
*
* */

public class CameraView extends FrameLayout {


    public static final int FACING_BACK = 0;
    public static final int FACING_FRONT = 1;


    @IntDef({FACING_BACK, FACING_FRONT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Facing {
    }

    /** Flash will not be fired. */
    public static final int FLASH_OFF = Constants.FLASH_OFF;

    /** Flash will always be fired during snapshot. */
    public static final int FLASH_ON = Constants.FLASH_ON;

    /** Constant emission of light during preview, auto-focus and snapshot. */
    public static final int FLASH_TORCH = Constants.FLASH_TORCH;

    /** Flash will be fired automatically when required. */
    public static final int FLASH_AUTO = Constants.FLASH_AUTO;

    /** Flash will be fired in red-eye reduction mode. */
    public static final int FLASH_RED_EYE = Constants.FLASH_RED_EYE;
    @IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
    public @interface Flash {
    }


    CameraViewImpl mImpl; // view 具体实现类

    private final CallbackBridge mCallbacks;

    private boolean mAdjustViewBounds;

    private final DisplayOrientationDetector mDisplayOrientationDetector;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        LogUtil.d("CameraView(): begin construct .");
        if(isInEditMode()){
            mCallbacks = null;
            mDisplayOrientationDetector = null;
            return;
        }

        // 获取渲染视图 { surfaceview, textureview }
        final PreviewImpl preview = createPreviewImpl(context);
        mCallbacks = new CallbackBridge();

        // 根据sdk 使用不同的camera api
        if(/*Build.VERSION.SDK_INT < 21*/true){
            mImpl = new Camera1(mCallbacks, preview);
            mImpl.setUsingCamName("CAM1");
        }else if(Build.VERSION.SDK_INT < 23){ // android 6.0之后使用新的api2
            mImpl = new Camera2(mCallbacks, preview, context);
            mImpl.setUsingCamName("CAM2");
        }else{
            mImpl = new Camera2Api23(mCallbacks, preview, context);
            mImpl.setUsingCamName("CAM23");
        }

        // 配置相机默认参数
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr,
                R.style.Widget_CameraView);
        mAdjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false);
        setFacing(a.getInt(R.styleable.CameraView_facing, FACING_BACK));
        String aspectRatio = a.getString(R.styleable.CameraView_aspectRatio);
        if(aspectRatio != null){
            setAspectRatio(AspectRatio.parse(aspectRatio));
        }else{
            setAspectRatio(Constants.DEFAULT_ASPECT_RATIO); // default 4:3
        }
        setAutoFocus(a.getBoolean(R.styleable.CameraView_autoFocus, true)); // default open autofocus
        setFlash(a.getInt(R.styleable.CameraView_flash, Constants.FLASH_AUTO)); // flash default auto

        a.recycle();

        mDisplayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                mImpl.setDisplayOrientation(displayOrientation);
            }
        };

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(!isInEditMode()){
            // 开启 orientation sensor 监听
            LogUtil.d(" begin orientation detector .");
            mDisplayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if(!isInEditMode()){
            // 关闭 orientation sensor 监听
            mDisplayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }


    /**
     *  对尺寸进行测量，并响应屏幕方向的改变
     *
     * */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(isInEditMode()){
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // adjustViewBounds 是为了使拍出来的图片比例尽量与预览比例一致 例如预览4:3 拍出来的图片可能是16:9
        if(mAdjustViewBounds){
            if(!isCameraOpened()){
                mCallbacks.reserveRequestLayoutOnOpen();
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }

            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if(widthMode == MeasureSpec.EXACTLY && heightMode  != MeasureSpec.EXACTLY){
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int height = (int) (MeasureSpec.getSize(widthMeasureSpec) * ratio.toFloat());
                if(heightMode == MeasureSpec.AT_MOST){
                    height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
                }
                super.onMeasure(widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int width = (int)(MeasureSpec.getSize(heightMeasureSpec) * ratio.toFloat());
                if(widthMode == MeasureSpec.AT_MOST){
                    width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                }
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        heightMeasureSpec);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }else{
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        // 测量 TextureView
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        AspectRatio ratio = getAspectRatio();
        if(mDisplayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0){
            ratio = ratio.inverse();
        }
        assert ratio != null;
        if(height < width * ratio.getY() / ratio.getX()){
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(),
                            MeasureSpec.EXACTLY));
        }else{
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    /**
     * 因为CameraView 是一个View,所以需要处理恢复视图的情况, 需要保存相机的配置和视图状态
     *
     * 保存数据
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.facing = getFacing();
        state.ratio = getAspectRatio();
        state.autoFocus = getAutoFocus();
        state.flash = getFlash();
        return state;
    }


    /**
     *  恢复数据
     *
     * */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)){
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState s = (SavedState) state;
        super.onRestoreInstanceState(s.getSuperState());
        setFacing(s.facing);
        setAspectRatio(s.ratio);
        setFlash(s.flash);
        setAutoFocus(s.autoFocus);
    }

    /**
     *   打开相机,并开启预览, 暴露给外部调用的api
     *
     * */
    public void start(){
        if(!mImpl.start()){
            LogUtil.d("next start() ---- .");
            Parcelable state = onSaveInstanceState();

            mImpl = new Camera1(mCallbacks, createPreviewImpl(getContext()));
            onRestoreInstanceState(state);
            mImpl.start();
        }
    }
    /**
     * Stop camera preview and close the device. This is typically called from
     * {@link Activity#onPause()}.
     */
    public void stop() {
        mImpl.stop();
    }


    public void removeCallback(@NonNull Callback callback) {
        mCallbacks.remove(callback);
    }

    public void setAdjustViewBounds(boolean adjustViewBounds){
        if(mAdjustViewBounds != adjustViewBounds){
            mAdjustViewBounds = adjustViewBounds;
            requestLayout();
        }
    }

    public boolean getAdjustViewBounds() {
        return mAdjustViewBounds;
    }

    public void addCallback(Callback callback){
        mCallbacks.add(callback);
    }

    private boolean isCameraOpened() {
        return mImpl.isCameraOpened();
    }

    /**
     *   相机的一些参数 set 和 get api
     * */
    public void setFlash(int flash) {
        mImpl.setFlash(flash);
    }
    public void setAutoFocus(boolean autofocus) {
        mImpl.setAutoFocus(autofocus);
    }
    public void setAspectRatio(AspectRatio ratio) {
        if(mImpl.setAspectRatio(ratio)){
            requestLayout();
        }
    }
    public Set<AspectRatio> getSupportedAspectRatios() {
        return mImpl.getSupportedAspectRatios();
    }
    public void setFacing(int facing){
        mImpl.setFacing(facing);
    }
    public AspectRatio getAspectRatio() {
        return mImpl.getAspectRatio();
    }
    public int getFacing() {
        return mImpl.getFacing();
    }
    public int getFlash() {
        return mImpl.getFlash();
    }
    public boolean getAutoFocus() {
        return mImpl.getAutoFocus();
    }
    /**
     *   end
     * */

    private PreviewImpl createPreviewImpl(Context context){

        PreviewImpl preview;
        if(true){
            preview = new SurfaceViewPreview(context, this);
        }else{
            preview = new TextureViewPreview(context, this);
        }
        LogUtil.d("createPreviewImpl(): preview = " + preview);
        return preview;
    }

    public void takePicture(){
        mImpl.takePicture();
    }


    /**
     *  回调方法具体实现类
    * */
    private class CallbackBridge implements  CameraViewImpl.Callback{

        private final ArrayList<Callback> mCallbacks = new ArrayList<>();

        private boolean mRequestLayoutOnOpen;

        CallbackBridge(){}

        public void add(Callback callback){
            mCallbacks.add(callback);
        }

        public void remove(Callback callback) {
            mCallbacks.remove(callback);
        }

        @Override
        public void onCameraOpened() {
            if(mRequestLayoutOnOpen){
                mRequestLayoutOnOpen = false;
                requestLayout();
            }

            for(Callback callback : mCallbacks){
                callback.onCameraOpened(CameraView.this);
            }
        }

        @Override
        public void onCameraClosed() {
            for(Callback callback : mCallbacks){
                callback.onCameraClosed(CameraView.this);
            }
        }

        @Override
        public void onPictureTaken(byte[] data) {
            for(Callback callback : mCallbacks){
                callback.onPictureTaken(CameraView.this, data);
            }
        }

        public void reserveRequestLayoutOnOpen(){ mRequestLayoutOnOpen = true; }
    }


    // 自定义一个inner class,用于保存数据
    protected static class SavedState extends BaseSavedState{

        @Facing
        int facing;

        AspectRatio ratio;

        boolean autoFocus;

        @Flash
        int flash;

        public SavedState(Parcel source, ClassLoader loader) {
            super(source);
            facing = source.readInt();
            ratio = source.readParcelable(loader);
            autoFocus = source.readByte() != 0;
            flash = source.readInt();
        }

        public SavedState(Parcelable superState){super(superState);}

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(facing);
            out.writeParcelable(ratio, 0);
            out.writeByte((byte) (autoFocus ? 1 : 0));
            out.writeInt(flash);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }

        });
    }



    public abstract static class Callback {

        /**
         * Called when camera is opened.
         *
         * @param cameraView The associated {@link CameraView}.
         */
        public void onCameraOpened(CameraView cameraView) {
        }

        /**
         * Called when camera is closed.
         *
         * @param cameraView The associated {@link CameraView}.
         */
        public void onCameraClosed(CameraView cameraView) {
        }

        /**
         * Called when a picture is taken.
         *
         * @param cameraView The associated {@link CameraView}.
         * @param data       JPEG data.
         */
        public void onPictureTaken(CameraView cameraView, byte[] data) {
        }
    }
}
