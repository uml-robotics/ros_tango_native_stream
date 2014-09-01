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

    private String frameId, parentId;
    private boolean okPublish;
    private Boolean sendGoal = false;

    private RateWatcher.RateProvider mRateProvider;
    
    public void setRateWatcher(RateWatcher.RateProvider rw)
    {
        mRateProvider = rw;
    }

    public void VIOCallback(float x, float y, float z, float ox, float oy, float oz, float ow) {
        if(tfMessagePublisher != null && odometryPublisher != null && okPublish) {
            final Time t = connectedNode.getCurrentTime();
            final Odometry mOdom;
            try{
                mOdom = connectedNode.getTopicMessageFactory().newFromType(Odometry._TYPE);
            } catch (Exception ex) {
                return;
            }
            mOdom.getHeader().setFrameId(parentId);
            mOdom.getHeader().setStamp(t);
            mOdom.getHeader().setSeq(mOdom.getHeader().getSeq() + 1);
            mOdom.getPose().getPose().getOrientation().setX(-oz / ow); //transpositions gleaned from OLogic. see NOTICE
            mOdom.getPose().getPose().getOrientation().setY(ox / ow);  //normalization = not
            mOdom.getPose().getPose().getOrientation().setZ(-oy / ow);
            mOdom.getPose().getPose().getOrientation().setW(ow / ow);
            mOdom.getPose().getPose().getPosition().setX(z);
            mOdom.getPose().getPose().getPosition().setY(-x);
            mOdom.getPose().getPose().getPosition().setZ(y);
            odometryPublisher.publish(mOdom);

            final TFMessage mTFMessage;
            try {
                mTFMessage = connectedNode.getTopicMessageFactory().newFromType(TFMessage._TYPE);
                mTFMessage.getTransforms().add((TransformStamped) connectedNode.getTopicMessageFactory().newFromType(TransformStamped._TYPE));
            } catch (Exception ex) {
                return;
            }
            mTFMessage.getTransforms().get(0).getHeader().setFrameId(parentId);
            mTFMessage.getTransforms().get(0).getHeader().setStamp(t);
            mTFMessage.getTransforms().get(0).setChildFrameId(frameId);
            mTFMessage.getTransforms().get(0).getTransform().getRotation().setX(mOdom.getPose().getPose().getOrientation().getX());
            mTFMessage.getTransforms().get(0).getTransform().getRotation().setY(mOdom.getPose().getPose().getOrientation().getY());
            mTFMessage.getTransforms().get(0).getTransform().getRotation().setZ(mOdom.getPose().getPose().getOrientation().getZ());
            mTFMessage.getTransforms().get(0).getTransform().getRotation().setW(mOdom.getPose().getPose().getOrientation().getW());
            mTFMessage.getTransforms().get(0).getTransform().getTranslation().setX(z);
            mTFMessage.getTransforms().get(0).getTransform().getTranslation().setY(-x);
            mTFMessage.getTransforms().get(0).getTransform().getTranslation().setZ(y);

            tfMessagePublisher.publish(mTFMessage);
            if (mRateProvider != null)
                mRateProvider.addStamp(t);
            if(sendGoal) {
                sendGoal = false;
                final PoseStamped mPoseStamped;
                try {
                    mPoseStamped = connectedNode.getTopicMessageFactory().newFromType(PoseStamped._TYPE);
                } catch (Exception ex) {
                    return;
                }
                mPoseStamped.getPose().setPosition(mOdom.getPose().getPose().getPosition());
                mPoseStamped.getPose().getPosition().setZ(0);
                mPoseStamped.getPose().setOrientation(mOdom.getPose().getPose().getOrientation());
                poseStampedPublisher.publish(mPoseStamped);
            }
        }
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
    public void setFrameId(String frameId) {
        this.frameId = frameId;
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
