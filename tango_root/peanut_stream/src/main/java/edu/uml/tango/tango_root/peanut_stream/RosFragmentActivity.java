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
 * Author: Eric Marcoux <emarcoux@cs.uml.edu>
*/
package edu.uml.tango.tango_root.peanut_stream;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import com.google.common.base.Preconditions;
import org.ros.android.MasterChooser;
import org.ros.android.NodeMainExecutorService;
import org.ros.android.NodeMainExecutorServiceListener;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeMainExecutor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Modified version of RosActivity that extends Fragment Activity
 * rather than Activity allowing the use of the Support Library for
 * Fragments in android.  Also allows the pre-setting of the master
 * by calling super with the added value of the master URI in the
 * constructor (this will make it never call MasterChooser implicitly).
 *
 * @author emarcoux
 *
 */
public abstract class RosFragmentActivity extends FragmentActivity {
    private static final int MASTER_CHOOSER_REQUEST_CODE = 0;
    private final String notificationTicker;
    private final String notificationTitle;

    private final ServiceConnection nodeMainExecutorServiceConnection;
    private NodeMainExecutorService nodeMainExecutorService;


    protected RosFragmentActivity(String notificationTicker, String notificationTitle) {
        super();
        this.notificationTicker = notificationTicker;
        this.notificationTitle = notificationTitle;
        nodeMainExecutorServiceConnection = new NodeMainExecutorServiceConnection();
    }


    protected RosFragmentActivity(String notificationTicker, String notificationTitle, String masterURI) {
        super();
        this.notificationTicker = notificationTicker;
        this.notificationTitle = notificationTitle;
        URI uri = null;
        try {
            uri = new URI(masterURI);
        } catch (URISyntaxException e) {
            uri = null;
            e.printStackTrace();
        } finally {
            nodeMainExecutorServiceConnection = new NodeMainExecutorServiceConnection(uri);
        }
    }

    protected RosFragmentActivity(String notificationTicker, String notificationTitle, URI masterURI) {
        super();
        this.notificationTicker = notificationTicker;
        this.notificationTitle = notificationTitle;
        nodeMainExecutorServiceConnection = new NodeMainExecutorServiceConnection(masterURI);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startNodeMainExecutorService();
    }

    private void startNodeMainExecutorService() {
        Intent intent = new Intent(this, NodeMainExecutorService.class);
        intent.setAction("org.ros.android.ACTION_START_NODE_RUNNER_SERVICE");
        intent.putExtra("org.ros.android.EXTRA_NOTIFICATION_TITLE", notificationTicker);
        intent.putExtra("org.ros.android.EXTRA_NOTIFICATION_TICKER", notificationTitle);
        startService(intent);
        Preconditions.checkState(
                bindService(intent, nodeMainExecutorServiceConnection, BIND_AUTO_CREATE),
                "Failed to bind NodeMainExecutorService.");
    }

    @Override
    protected void onDestroy() {
        if (nodeMainExecutorService != null) {
            nodeMainExecutorService.shutdown();
            unbindService(nodeMainExecutorServiceConnection);
            // NOTE(damonkohler): The activity could still be restarted. In that case,
            // nodeMainExectuorService needs to be null for everything to be started
            // up again.
            nodeMainExecutorService = null;
        }
        Toast.makeText(this, notificationTitle + " shut down.", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }


    /**
     * This method is called in a background thread once this {@link android.app.Activity} has
     * been initialized with a master {@link java.net.URI} via the {@link org.ros.android.MasterChooser}
     * and a {@link org.ros.android.NodeMainExecutorService} has started. Your {@link org.ros.node.NodeMain}s
     * should be started here using the provided {@link org.ros.node.NodeMainExecutor}.
     *
     * @param nodeMainExecutor
     *          the {@link org.ros.node.NodeMainExecutor} created for this {@link android.app.Activity}
     */
    protected abstract void init(NodeMainExecutor nodeMainExecutor);


    private void startMasterChooser() {
        Preconditions.checkState(getMasterUri() == null);
        // Call this method on super to avoid triggering our precondition in the
        // overridden startActivityForResult().
        super.startActivityForResult(new Intent(this, MasterChooser.class), 0);
    }


    public URI getMasterUri() {
        Preconditions.checkNotNull(nodeMainExecutorService);
        return nodeMainExecutorService.getMasterUri();
    }


    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        Preconditions.checkArgument(requestCode != MASTER_CHOOSER_REQUEST_CODE);
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MASTER_CHOOSER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data == null) {
                    nodeMainExecutorService.startMaster();
                } else {
                    URI uri;
                    try {
                        uri = new URI(data.getStringExtra("ROS_MASTER_URI"));
                    } catch (URISyntaxException e) {
                        throw new RosRuntimeException(e);
                    }
                    nodeMainExecutorService.setMasterUri(uri);
                }
                // Run init() in a new thread as a convenience since it often requires
                // network access.
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        RosFragmentActivity.this.init(nodeMainExecutorService);
                        return null;
                    }
                }.execute();
            } else {
                // Without a master URI configured, we are in an unusable state.
                nodeMainExecutorService.shutdown();
                finish();
            }
        }
    }


    private final class NodeMainExecutorServiceConnection implements ServiceConnection {
        URI masterURI;
        public NodeMainExecutorServiceConnection(URI masterURI) {
            this.masterURI = masterURI;
        }

        public NodeMainExecutorServiceConnection() {
            this.masterURI = null;
        }


        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            // use reflection to get NodeMainExecutorService.LocalBinder access
            // if a less ugly way comes up, this should really be changed
            try {
                Class<?> localBinderClass = Class.forName("org.ros.android.NodeMainExecutorService$LocalBinder");
                Method localBinderGetServiceMethod = localBinderClass.getDeclaredMethod("getService", null);
                localBinderGetServiceMethod.setAccessible(true);

                nodeMainExecutorService = (NodeMainExecutorService) localBinderGetServiceMethod.invoke(binder, null);
                nodeMainExecutorService.addListener(new NodeMainExecutorServiceListener() {
                    @Override
                    public void onShutdown(NodeMainExecutorService nodeMainExecutorService) {
                        RosFragmentActivity.this.finish();
                    }
                });
                if(masterURI == null) {
                    startMasterChooser();
                    Log.v("MASTER", "calling master chooser");
                } else {
                    Log.v("MASTER", "directly setting it");
                    nodeMainExecutorService.setMasterUri(masterURI);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            RosFragmentActivity.this.init(nodeMainExecutorService);
                            return null;
                        }
                    }.execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    }
}
