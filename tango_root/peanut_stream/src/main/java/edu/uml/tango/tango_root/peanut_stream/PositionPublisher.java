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

import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import edu.uml.TangoAPI;
import edu.uml.VIOReceiver;
import geometry_msgs.PoseStamped;
import geometry_msgs.TransformStamped;
import nav_msgs.Odometry;
import tf2_msgs.TFMessage;
import android.util.Log;

public class PositionPublisher extends VIOReceiver implements NodeMain {
    ConnectedNode connectedNode;
    private Publisher<TFMessage> tfMessagePublisher;
    private Publisher<Odometry> odometryPublisher;
    private Publisher<PoseStamped> poseStampedPublisher;

    private TFMessage mTFMessage;
    private TFMessage mTFMessage2;
    private String frameId, parentId,cameraId;
    private boolean okPublish;

    private Odometry mOdom;
    private PoseStamped mPoseStamped;
    private Boolean sendGoal = false;

    private RateWatcher.RateProvider mRateProvider;
    
    public void setRateWatcher(RateWatcher.RateProvider rw)
    {
        mRateProvider = rw;
    }

    public void VIOCallback() {
        if(tfMessagePublisher != null && odometryPublisher != null && mOdom != null && okPublish) {
            Time t = connectedNode.getCurrentTime();

            mOdom.getHeader().setStamp(t);
            mOdom.getPose().getPose().getOrientation().setX(-buffer.getFloat(20) / buffer.getFloat(24)); //transpositions gleaned from OLogic. see NOTICE
            mOdom.getPose().getPose().getOrientation().setY(buffer.getFloat(12) / buffer.getFloat(24));  //normalization = not
            mOdom.getPose().getPose().getOrientation().setZ(-buffer.getFloat(16) / buffer.getFloat(24));
            mOdom.getPose().getPose().getOrientation().setW(buffer.getFloat(20) / buffer.getFloat(24));
            mOdom.getPose().getPose().getPosition().setX(buffer.getFloat(8));
            mOdom.getPose().getPose().getPosition().setY(-buffer.getFloat(0));
            mOdom.getPose().getPose().getPosition().setZ(buffer.getFloat(4));

            mTFMessage.getTransforms().get(0).getHeader().setStamp(t);
            mTFMessage.getTransforms().get(0).getTransform().getRotation().setX(mOdom.getPose().getPose().getOrientation().getX());
            mTFMessage.getTransforms().get(0).getTransform().getRotation().setY(mOdom.getPose().getPose().getOrientation().getY());
            mTFMessage.getTransforms().get(0).getTransform().getRotation().setZ(mOdom.getPose().getPose().getOrientation().getZ());
            mTFMessage.getTransforms().get(0).getTransform().getRotation().setW(mOdom.getPose().getPose().getOrientation().getW());
            mTFMessage.getTransforms().get(0).getTransform().getTranslation().setX(buffer.getFloat(8));
            mTFMessage.getTransforms().get(0).getTransform().getTranslation().setY(-buffer.getFloat(0));
            mTFMessage.getTransforms().get(0).getTransform().getTranslation().setZ(buffer.getFloat(4));

            mTFMessage2.getTransforms().get(0).getHeader().setFrameId(frameId);
            mTFMessage2.getTransforms().get(0).getHeader().setStamp(t);
            mTFMessage2.getTransforms().get(0).setChildFrameId(cameraId);
            mTFMessage2.getTransforms().get(0).getTransform().getRotation().setX(-0.499999841466);
            mTFMessage2.getTransforms().get(0).getTransform().getRotation().setY(0.499601836645);
            mTFMessage2.getTransforms().get(0).getTransform().getRotation().setZ(-0.499999841466);
            mTFMessage2.getTransforms().get(0).getTransform().getRotation().setW(0.500398163355);
            mTFMessage2.getTransforms().get(0).getTransform().getTranslation().setX(0);
            mTFMessage2.getTransforms().get(0).getTransform().getTranslation().setY(0);
            mTFMessage2.getTransforms().get(0).getTransform().getTranslation().setZ(0);

            tfMessagePublisher.publish(mTFMessage);
            tfMessagePublisher.publish(mTFMessage2);
            odometryPublisher.publish(mOdom);

            if (mRateProvider != null)
                mRateProvider.addStamp(t.secs,t.nsecs);
            if(sendGoal) {
                sendGoal = false;
                mPoseStamped.getPose().setPosition(mOdom.getPose().getPose().getPosition());
                mPoseStamped.getPose().getPosition().setZ(0);
                mPoseStamped.getPose().setOrientation(mOdom.getPose().getPose().getOrientation());
                poseStampedPublisher.publish(mPoseStamped);
            }
        }
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
        if (mTFMessage != null && mTFMessage.getTransforms().size() > 0)
            mTFMessage.getTransforms().get(0).getHeader().setFrameId(parentId);
        if (mOdom != null)
            mOdom.getHeader().setFrameId(parentId);
    }
    public void setFrameId(String frameId) {
        this.frameId = frameId;
        if (mTFMessage != null && mTFMessage.getTransforms().size() > 0)
            mTFMessage.getTransforms().get(0).setChildFrameId(frameId);
        if (mOdom != null)
            mOdom.getHeader().setFrameId(parentId);
    }
    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public void setOkPublish(boolean okPublish) {
        this.okPublish = okPublish;
    }

    public void publishCurrent() {
        sendGoal = true;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("peanut/position_publisher");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
        tfMessagePublisher = connectedNode.newPublisher("/tf", TFMessage._TYPE);
        odometryPublisher = connectedNode.newPublisher("/odom", Odometry._TYPE);
        poseStampedPublisher = connectedNode.newPublisher("/move_base_simple/goal",PoseStamped._TYPE);

        while (mTFMessage == null || mTFMessage.getTransforms().size() == 0 || mOdom == null || mPoseStamped == null)
            try {
                if (mTFMessage == null)
                    mTFMessage = connectedNode.getTopicMessageFactory().newFromType(TFMessage._TYPE);
                if (mTFMessage != null && mTFMessage.getTransforms().size() == 0)
                    mTFMessage.getTransforms().add((TransformStamped) connectedNode.getTopicMessageFactory().newFromType(TransformStamped._TYPE));
                mTFMessage.getTransforms().get(0).getHeader().setFrameId(parentId);
                mTFMessage.getTransforms().get(0).setChildFrameId(frameId);
                if (mOdom == null)
                    mOdom = connectedNode.getTopicMessageFactory().newFromType(Odometry._TYPE);
                if (mOdom != null)
                    mOdom.getHeader().setFrameId(parentId);
                if(mPoseStamped == null)
                    mPoseStamped = connectedNode.getTopicMessageFactory().newFromType(PoseStamped._TYPE);
                if (mPoseStamped != null)
                    mPoseStamped.getHeader().setFrameId(parentId);
                if(mTFMessage2 == null)
                    mTFMessage2 = connectedNode.getTopicMessageFactory().newFromType(TFMessage._TYPE);
                if (mTFMessage2 != null && mTFMessage2.getTransforms().size() == 0)
                    mTFMessage2.getTransforms().add((TransformStamped) connectedNode.getTopicMessageFactory().newFromType(TransformStamped._TYPE));
            } catch (Exception ex) {
                Log.e("PositionPublisher", "Exception while initializing", ex);
            }
    }

    @Override
    public void onShutdown(Node node) {
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }
}
