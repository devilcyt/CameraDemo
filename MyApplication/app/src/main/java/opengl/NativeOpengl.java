package opengl;

import android.view.Surface;

public class NativeOpengl {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    public native void openGlNative(Surface surface);
}
