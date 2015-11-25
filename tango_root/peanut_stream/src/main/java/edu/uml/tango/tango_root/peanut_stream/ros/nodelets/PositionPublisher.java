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
 * Authors: Jordan Allspaw <jallspaw@cs.uml.edu>, , Carlos Ibarra <clopez@cs.uml.edu>, Eric Marcoux<emarcoux@cs.uml.edu>
*/
package edu.uml.tango.tango_root.peanut_stream.ros.nodelets;

import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.Vector3;

import edu.uml.VIOReceiver;
import edu.uml.tango.tango_root.peanut_stream.RateWatcher;
import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.PoseStamped;
import geometry_msgs.PoseWithCovarianceStamped;
import geometry_msgs.Quaternion;
import geometry_msgs.Transform;
import geometry_msgs.TransformStamped;
import nav_msgs.Odometry;
import tf2_msgs.TFMessage;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PositionPublisher extends VIOReceiver implements NodeMain {
    private ConnectedNode connectedNode;

    //TF Fields
    private Publisher<TFMessage> tfMessagePublisher;
    private TFMessage mapToOdomTFMessage;
    private TFMessage baseToDepthCameraTFMessage2;
    private TFMessage odomToBaseTF;
    private String odomFrameId, parentId, cameraId, tangoPrefix, baseId;
    //amcl_pose publisher fields
    private Publisher<geometry_msgs.PoseWithCovarianceStamped> realPosePublisher;
    private geometry_msgs.PoseWithCovarianceStamped realPoseMsg;
    private geometry_msgs.Pose realPosePose;
    private Publisher<Odometry> odometryPublisher;
    private static final String realPoseId = "/amcl_pose";
    //initialpose subscriber
    private Subscriber<geometry_msgs.PoseWithCovarianceStamped> realPositionSubscriber;
    //move_base commands fields
    private PoseStamped poseToSendMoveBase;
    private Publisher<PoseStamped> moveBaseCommandsPublisher;
    private Boolean sendGoal = false;

    private Transform mapToOdomTransform;
    private Transform odomToBaseTransform;

    private Odometry mOdom;

    private RateWatcher.RateProvider mRateProvider;
    private static final int SIZEOF_ELEMENT = 8;
    private static final String TAG = "VIO_CALLBACK";
    private boolean okPublish;
    private boolean publishMapToOdom = true;

    public void setPublishMapToOdom(boolean value) {
        publishMapToOdom = value;
    }

    public boolean getPublishMapToOdom() {
        return publishMapToOdom;
    }

    public PositionPublisher(String tangoPrefix)
    {
        this(tangoPrefix, "/map", "/odom", "/tango_camera_depth", "/base_link");
    }

    public PositionPublisher(String tangoPrefix, String parentId, String odomId, String camId, String baseId) {
        this.tangoPrefix = tangoPrefix;
        this.parentId = parentId;
        this.cameraId = camId;
        this.odomFrameId = odomId;
        this.baseId = baseId;
    }

    public void setRateWatcher(RateWatcher.RateProvider rw) {
        mRateProvider = rw;
    }

    @Override
    public void VIOCallback(ByteBuffer buffer) {
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (tfMessagePublisher != null && mOdom != null && okPublish) {
            Time t = connectedNode.getCurrentTime();
            mOdom.getHeader().setStamp(t);
            final Pose odomPose = mOdom.getPose().getPose();
            //Odometry position has right values, considering the camera side of tango, the front face (this would coincide with the user pose), fnid out correct values of rotation to coincide with this
            odomPose.getPosition().setX((float) buffer.getDouble(0));
            odomPose.getPosition().setY((float) buffer.getDouble(1 * SIZEOF_ELEMENT));
            odomPose.getPosition().setZ((float) buffer.getDouble(2 * SIZEOF_ELEMENT));
            //lets use rosjava geometry quaternion to make this much easier for future tests/changes
            org.ros.rosjava_geometry.Quaternion rotationQ = new org.ros.rosjava_geometry.Quaternion(buffer.getDouble(3 * SIZEOF_ELEMENT), buffer.getDouble(4 * SIZEOF_ELEMENT), buffer.getDouble(5 * SIZEOF_ELEMENT), buffer.getDouble(6 * SIZEOF_ELEMENT));
            //this fixes the axes, (i.e.: rotates 90 over z axis then 90 over y axis, making tango rotation coincide with what ros expects (z up, y left, x out of rear camera))
            rotationQ = rotationQ.multiply(new org.ros.rosjava_geometry.Quaternion(-.5, .5, .5, .5));
            //no need to normalize, since the quat we multiplied by is a unit quat
            rotationQ.toQuaternionMessage(odomPose.getOrientation());

            realPoseMsg.getHeader().setStamp(t);
            if (mapToOdomTransform != null && odomToBaseTransform != null && realPosePose != null) {
                (org.ros.rosjava_geometry.Transform.fromTransformMessage(mapToOdomTransform).multiply(org.ros.rosjava_geometry.Transform.fromTransformMessage(odomToBaseTransform))).toPoseMessage(realPosePose);
                realPoseMsg.getPose().setPose(realPosePose);
            }

            //untouched, shouldnt need any changes (This should publish the odomcorrection vector and quat
            mapToOdomTFMessage.getTransforms().get(0).getHeader().setStamp(t);
            mapToOdomTFMessage.getTransforms().get(0).setTransform(mapToOdomTransform);

            //These are now fixed (Transform from base to camera, these are the right values for the depth to show in the right place in rviz)
            baseToDepthCameraTFMessage2.getTransforms().get(0).getHeader().setStamp(t);
            final Transform baseToDepthCameraTransform = baseToDepthCameraTFMessage2.getTransforms().get(0).getTransform();
            baseToDepthCameraTransform.getRotation().setX(-0.5);
            baseToDepthCameraTransform.getRotation().setY(0.5);
            baseToDepthCameraTransform.getRotation().setZ(-0.5);
            baseToDepthCameraTransform.getRotation().setW(0.5);
            baseToDepthCameraTransform.getTranslation().setX(0);
            baseToDepthCameraTransform.getTranslation().setY(0);
            baseToDepthCameraTransform.getTranslation().setZ(0);

            //Transform from odom to base, this should be equal to odometry
            odomToBaseTF.getTransforms().get(0).getHeader().setStamp(t);
            odomToBaseTransform = odomToBaseTF.getTransforms().get(0).getTransform();
            odomToBaseTransform.getRotation().setX(mOdom.getPose().getPose().getOrientation().getX());
            odomToBaseTransform.getRotation().setY(mOdom.getPose().getPose().getOrientation().getY());
            odomToBaseTransform.getRotation().setZ(mOdom.getPose().getPose().getOrientation().getZ());
            odomToBaseTransform.getRotation().setW(mOdom.getPose().getPose().getOrientation().getW());
            odomToBaseTransform.getTranslation().setX(mOdom.getPose().getPose().getPosition().getX());
            odomToBaseTransform.getTranslation().setY(mOdom.getPose().getPose().getPosition().getY());
            odomToBaseTransform.getTranslation().setZ(mOdom.getPose().getPose().getPosition().getZ());

            //Publish all transforms
            if (publishMapToOdom) {
                tfMessagePublisher.publish(mapToOdomTFMessage);
                realPosePublisher.publish(realPoseMsg);
            }
            tfMessagePublisher.publish(baseToDepthCameraTFMessage2);
            tfMessagePublisher.publish(odomToBaseTF);
            odometryPublisher.publish(mOdom);

            if (mRateProvider != null)
                mRateProvider.addStamp(t.secs, t.nsecs);          

            //Publishes tango's position to a move_base/goal topic, to make a robot come to the tango user, called with come to me button
            if (sendGoal) {
                sendGoal = false;
                poseToSendMoveBase.getPose().setPosition(realPoseMsg.getPose().getPose().getPosition());
                poseToSendMoveBase.getPose().getPosition().setZ(0);
                poseToSendMoveBase.getPose().setOrientation(realPoseMsg.getPose().getPose().getOrientation());
                moveBaseCommandsPublisher.publish(poseToSendMoveBase);
            }
        }
    }

    /**
     * Manually sets tango position on the map (i.e., adjusts the odom->real_pose transform translation accordingly)
     * is called either from the user interface, or by the subscriber to the /&lt;prefix&gt;/odom/set_real_pose
     *
     * @param pos Geometry_Msg/Point containing known pose
     * @param rot Geometry_Msg/Quaternion containing known rotation
     */
    public void setRealPosition(Point pos, Quaternion rot) {
        org.ros.rosjava_geometry.Transform knownPoseTf, odomTransform;
        knownPoseTf = new org.ros.rosjava_geometry.Transform(Vector3.fromPointMessage(pos), org.ros.rosjava_geometry.Quaternion.fromQuaternionMessage(rot));
        odomTransform = org.ros.rosjava_geometry.Transform.fromTransformMessage(odomToBaseTransform);
        (knownPoseTf.multiply(odomTransform.invert())).toTransformMessage(mapToOdomTransform);
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
        if (mapToOdomTFMessage != null && mapToOdomTFMessage.getTransforms().size() > 0)
            mapToOdomTFMessage.getTransforms().get(0).getHeader().setFrameId(parentId);
    }

    public void setOdomFrameId(String newOdomFrameId) {
        this.odomFrameId = newOdomFrameId;
        if (mapToOdomTFMessage != null && mapToOdomTFMessage.getTransforms().size() > 0)
            mapToOdomTFMessage.getTransforms().get(0).setChildFrameId(tangoPrefix + odomFrameId);

        if (mOdom != null)
            mOdom.getHeader().setFrameId(tangoPrefix + odomFrameId);

        if (odomToBaseTF != null && odomToBaseTF.getTransforms().size() > 0)
            odomToBaseTF.getTransforms().get(0).getHeader().setFrameId(tangoPrefix + odomFrameId);
    }

    public void setBaseFrameId(String baseFrameId)
    {
        this.baseId = baseFrameId;

        if (odomToBaseTF != null && odomToBaseTF.getTransforms().size() > 0)
            odomToBaseTF.getTransforms().get(0).setChildFrameId(tangoPrefix + baseFrameId);

        if (baseToDepthCameraTFMessage2 != null && baseToDepthCameraTFMessage2.getTransforms().size() > 0)
            baseToDepthCameraTFMessage2.getTransforms().get(0).getHeader().setFrameId(tangoPrefix + baseFrameId);
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
        if (baseToDepthCameraTFMessage2 != null && baseToDepthCameraTFMessage2.getTransforms().size() > 0)
            baseToDepthCameraTFMessage2.getTransforms().get(0).setChildFrameId(tangoPrefix.replace("/", "") + cameraId);
    }

    public void setTangoPrefix(String newTangoPrefix)
    {
        tangoPrefix = newTangoPrefix;
        setParentId(parentId);
        setOdomFrameId(odomFrameId);
        setCameraId(cameraId);
        setBaseFrameId(baseId);
        setCameraId(cameraId);
        if (realPosePublisher != null)
            realPosePublisher.shutdown();
        if (realPositionSubscriber != null)
            realPositionSubscriber.shutdown();
        if (odometryPublisher != null)
            odometryPublisher.shutdown();
        realPosePublisher = connectedNode.newPublisher(tangoPrefix + realPoseId, geometry_msgs.PoseWithCovarianceStamped._TYPE);
        odometryPublisher = connectedNode.newPublisher(tangoPrefix + "/odom", Odometry._TYPE);
        realPositionSubscriber=connectedNode.newSubscriber(tangoPrefix + "/initialpose", PoseWithCovarianceStamped._TYPE);
        realPositionSubscriber.addMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(geometry_msgs.PoseWithCovarianceStamped pose) {
                setRealPosition(pose.getPose().getPose().getPosition(), pose.getPose().getPose().getOrientation());
            }
        });
    }

    public void setOkPublish(boolean okPublish) {
        this.okPublish = okPublish;
    }

    public void publishCurrent() {
        sendGoal = true;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("yellowstone/position_publisher");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        this.connectedNode   = connectedNode;
        tfMessagePublisher   = connectedNode.newPublisher("/tf", TFMessage._TYPE);
        moveBaseCommandsPublisher = connectedNode.newPublisher("/robot/move_base_simple/goal", PoseStamped._TYPE);
        while (realPosePose == null || mapToOdomTransform == null || mapToOdomTFMessage == null || mapToOdomTFMessage.getTransforms().size() == 0 || mOdom == null || poseToSendMoveBase == null || odomToBaseTF == null || odomToBaseTF.getTransforms().size() == 0)
            try {
                if (mapToOdomTFMessage == null)
                    mapToOdomTFMessage = connectedNode.getTopicMessageFactory().newFromType(TFMessage._TYPE);
                if (mapToOdomTFMessage != null && mapToOdomTFMessage.getTransforms().size() == 0)
                    mapToOdomTFMessage.getTransforms().add((TransformStamped) connectedNode.getTopicMessageFactory().newFromType(TransformStamped._TYPE));
                if (mOdom == null)
                    mOdom = connectedNode.getTopicMessageFactory().newFromType(Odometry._TYPE);
                mOdom.getHeader().setFrameId(parentId);
                if(poseToSendMoveBase == null)
                    poseToSendMoveBase = connectedNode.getTopicMessageFactory().newFromType(PoseStamped._TYPE);
                if (poseToSendMoveBase != null)
                    poseToSendMoveBase.getHeader().setFrameId(parentId);
                if (realPoseMsg == null)
                    realPoseMsg = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PoseWithCovarianceStamped._TYPE);
                if (realPoseMsg != null)
                    realPoseMsg.getHeader().setFrameId(parentId);
                if (realPosePose == null)
                    realPosePose = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.Pose._TYPE);
                if(mapToOdomTransform == null)
                    mapToOdomTransform = connectedNode.getTopicMessageFactory().newFromType(Transform._TYPE);
                if(mapToOdomTransform != null)
                {
                    Quaternion tmpQ = connectedNode.getTopicMessageFactory().newFromType(Quaternion._TYPE);
                    (new org.ros.rosjava_geometry.Quaternion(0,0,0,1)).toQuaternionMessage(tmpQ);
                    geometry_msgs.Vector3 tmpV = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.Vector3._TYPE);
                    (new Vector3(0,0,0)).toVector3Message(tmpV);
                    mapToOdomTransform.setRotation(tmpQ);
                    mapToOdomTransform.setTranslation(tmpV);
                }

                if(baseToDepthCameraTFMessage2 == null)
                    baseToDepthCameraTFMessage2 = connectedNode.getTopicMessageFactory().newFromType(TFMessage._TYPE);

                if (baseToDepthCameraTFMessage2 != null && baseToDepthCameraTFMessage2.getTransforms().size() == 0)
                    baseToDepthCameraTFMessage2.getTransforms().add((TransformStamped) connectedNode.getTopicMessageFactory().newFromType(TransformStamped._TYPE));

                if(odomToBaseTF == null)
                    odomToBaseTF = connectedNode.getTopicMessageFactory().newFromType(TFMessage._TYPE);
                if(odomToBaseTF != null && odomToBaseTF.getTransforms().size() == 0)
                    odomToBaseTF.getTransforms().add((TransformStamped) connectedNode.getTopicMessageFactory().newFromType(TransformStamped._TYPE));

                setTangoPrefix(tangoPrefix);
            } catch (Exception ex) {
                Log.e("PositionPublisher", "Exception while initializing", ex);
            }
    }



    @Override
    public void onShutdown(Node node) { }

    @Override
    public void onShutdownComplete(Node node) { }

    @Override
    public void onError(Node node, Throwable throwable) { }
}
