/*
 * Copyright (c) 2014, University Of Massachusetts Lowell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Massachusetts Lowell nor the names
 * from of its contributors may be used to endorse or promote products
 * derived this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * Author: Jordan Allspaw <jallspaw@cs.uml.edu>
*/

package edu.uml.tango.tango_root.peanut_stream;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import org.ros.address.InetAddressFactory;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import java.nio.ByteBuffer;
import android.util.Log;

import edu.uml.TangoAPI;

public class PeanutStream extends RosFragmentActivity implements RateWatcher.RateUpdater{
    private PositionPublisher posePub;
    private DepthPublisher depthPub;
    private static final String TAG = "TangoJNIActivity";
    public PeanutStream() {
        super("peanut_stream", "peanut_stream");
    }

    RateWatcher mRateWatcher = new RateWatcher(this);
    private Handler mHandler = new Handler();
    TangoAPI mTangoAPI;

    // generic specific textbox event handling
    private void smartSet(View v, String str)
    {
        switch (v.getId()) {
            case R.id.positionEditText:
                posePub.setParentId(str);
                break;
            case R.id.positionFrameEditText:
                posePub.setFrameId(str);
                break;
            case R.id.depthEditText:
                depthPub.setTopicName(str);
                break;
            case R.id.depthFrameEditText:
                depthPub.setFrameId(str);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);

        if(posePub == null) {
            posePub = new PositionPublisher();
            posePub.setParentId(getResources().getString(R.string.parent_id));
            posePub.setFrameId(getResources().getString(R.string.odom_frame_id));
            posePub.setRateWatcher(mRateWatcher.add(R.id.odom_rate));
        }
        if(depthPub == null) {
            depthPub = new DepthPublisher(getResources().getString(R.string.depth_topic),getResources().getString(R.string.depth_frame_id));
            depthPub.setRateWatcher(mRateWatcher.add(R.id.depth_rate));
        }
        mTangoAPI = new TangoAPI(posePub, depthPub);
        mTangoAPI.start();

        EditText e1 = (EditText) findViewById(R.id.positionEditText);
        EditText e2 = (EditText) findViewById(R.id.positionFrameEditText);
        EditText e3 = (EditText) findViewById(R.id.depthEditText);
        EditText e4 = (EditText) findViewById(R.id.depthFrameEditText);
        final EditText[] ets = new EditText[]{e1,e2,e3,e4};

        for(EditText et : ets) {
            et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (!b)
                        smartSet(view,((EditText) view).getText().toString());
                }
            });
        }

        ToggleButton tb1 = (ToggleButton) findViewById(R.id.positionToggleButton);
        ToggleButton tb2 = (ToggleButton) findViewById(R.id.depthToggleButton);

        tb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                posePub.setOkPublish(isChecked);
            }
        });

        tb2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                depthPub.setOkPublish(isChecked);
            }
        });
        tb1.setChecked(true);
        tb2.setChecked(true);
    }

    @Override
    protected void onDestroy()
    {
        mTangoAPI.die();
        super.onDestroy();
        Log.e("peanut","WHOA WHOA, WE'RE GOING DOWN!");
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),getMasterUri());
        nodeMainExecutor.execute(posePub, nodeConfiguration);
        nodeMainExecutor.execute(depthPub, nodeConfiguration);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public void update(final int id) {
        final TextView tv = (TextView)findViewById(id);
        final Resources res = getResources();
        mHandler.post(new Runnable() {
            @Override
            public void run()
            {
                tv.setText(String.format("%.3f %s",mRateWatcher.getRate(id),res.getString(R.string.frequency_suffix)));
            }
        });
    }
}
