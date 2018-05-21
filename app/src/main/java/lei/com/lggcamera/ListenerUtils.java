package lei.com.lggcamera;

/**
 * Created by xulei on 18-5-21.
 */

public class ListenerUtils {
    interface CameraOpendListener {
        public void isOpend();
    }

    interface SurfaceCreatedListener {
        public void onSurfaceAvailable();
    }

}
