package opengl;

import android.view.Surface;

public class NativeOpengl {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    public native void openGlNative(Surface surface);

    public native void surfaceChange(int width, int height);

    /*
     *
     *  绘制多边形形
     */
    public native void createTriangle(int num);


    /*
    *
    *   向底层传入图片data数据
     */

    public native void imgData(int w, int h, int length, byte[] data);

}
