package edu.uml.tango.tango_root.peanut_stream;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import org.ros.node.NodeMainExecutor;

import edu.uml.TangoCameraView;

/**
 * Created by emarcoux on 9/12/14.
 */
public class PeanutStreamVideo extends RosFragmentActivity {
        private static final String TAG = "TangoJNIActivity";
        public PeanutStreamVideo() {
            super("peanut_stream", "peanut_stream", "http://robot-lab0:11311");
        }

        private TangoCameraView tangoCameraView;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.main_video);

            tangoCameraView = (TangoCameraView) findViewById(R.id.tango_camera_view);
        }

        @Override
        protected void onDestroy()
        {
            super.onDestroy();
            Log.e("peanut", "WHOA WHOA, WE'RE GOING DOWN!");
        }

        @Override
        protected void init(NodeMainExecutor nodeMainExecutor) {
//            NodeConfiguration nodeConfiguration =
//                    NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),getMasterUri());
//            nodeMainExecutor.execute(posePub, nodeConfiguration);
//            nodeMainExecutor.execute(depthPub, nodeConfiguration);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return true;
        }
}
