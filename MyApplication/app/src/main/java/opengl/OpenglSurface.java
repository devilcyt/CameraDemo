package opengl;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class OpenglSurface extends SurfaceView implements SurfaceHolder.Callback {

    private NativeOpengl nativeOpengl;
    private OnSurfaceListener onSurfaceListener;

    public void setOnSurfaceListener(OnSurfaceListener onSurfaceListener){
        this.onSurfaceListener = onSurfaceListener;
    }


    public OpenglSurface(Context context) {
        this(context, null);
    }

    public OpenglSurface(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OpenglSurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        nativeOpengl = new NativeOpengl();
        nativeOpengl.openGlNative(holder.getSurface());
        if(onSurfaceListener != null){
            Log.i("swq","java : surfaceCreated()");
            onSurfaceListener.init();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(nativeOpengl != null){
            Log.i("swq", "java : surfaceChanged()");
            nativeOpengl.surfaceChange(width, height);
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(nativeOpengl != null){
            Log.i("swq", "java : surfaceDestroyed()");
            nativeOpengl.surfaceDestroy();
        }
    }

    public interface OnSurfaceListener{
        void init();
    }
}
