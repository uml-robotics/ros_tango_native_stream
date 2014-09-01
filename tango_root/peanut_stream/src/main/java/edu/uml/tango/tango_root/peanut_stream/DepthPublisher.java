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

import android.util.Log;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.uml.DepthReceiver;
import sensor_msgs.CameraInfo;
import sensor_msgs.Image;

public class DepthPublisher extends DepthReceiver implements NodeMain {
    private ConnectedNode connectedNode;

    private Publisher<Image> depthPublisher;
    private Publisher<CameraInfo> cameraInfoPublisher;
    private String topicName,initialTopicName,frameId,initialFrameId;
    private boolean okPublish;
    private sensor_msgs.CameraInfo mCameraInfo;

    //Camera intrinsic parameters borrowed from OLogic's rostango, see NOTICE in project root.
    private final double[] D = {0.2104473, -0.5854902, 0.4575633, 0.0, 0.0};
    private final double[] K = {237.0, 0.0, 160.0, 0.0, 237.0, 90.0, 0.0, 0.0, 1.0};
    private final double[] R = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    private final double[] P = {237.0, 0.0, 160.0, 0.0, 0.0, 237.0, 90.0, 0.0, 0.0, 0.0, 1.0, 0.0};
    private final String distortionModel = "plumb_bob";
    final int width = 320, height=180;

    public DepthPublisher(String topicName, String frameId)
    {
        super();
        this.initialTopicName=topicName;
        this.initialFrameId=frameId;
    }

    private RateWatcher.RateProvider mRateProvider;
    public void setRateWatcher(RateWatcher.RateProvider rw)
    {
        mRateProvider = rw;
    }

    Image mImage;
    public void DepthCallback()
    {
        if (!okPublish || connectedNode == null)
            return;
        Time currentTime = connectedNode.getCurrentTime();
        if(depthPublisher != null && mImage != null && buffer !=null) {
            final ChannelBufferOutputStream stream = new ChannelBufferOutputStream(ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN,width*height*2));
            mImage.getHeader().setStamp(currentTime);
            try{
                stream.write(buffer.array());
                buffer.clear();
                stream.flush();
                mImage.setData(stream.buffer());
            } catch (IOException ie)
            {
            }
            finally{
                try {
                    stream.close();
                }
                catch(IOException ie) {

                }
            }

            depthPublisher.publish(mImage);
            if (mRateProvider != null)
                mRateProvider.addStamp(currentTime);
        }
        if (cameraInfoPublisher != null && mCameraInfo != null)
        {
            mCameraInfo.getHeader().setStamp(currentTime);
            cameraInfoPublisher.publish(mCameraInfo);
        }
    }


    public void setTopicName(String topicName) {
        if (depthPublisher != null && depthPublisher.getTopicName() == null)
            return;
        if (this.topicName != null && this.topicName.equals(topicName))
            return;
        this.topicName = topicName;
        if (depthPublisher == null || depthPublisher.getTopicName().toString() != topicName + "/image_raw") {
            topicName = topicName.replace("\n", "");
            depthPublisher = connectedNode.newPublisher(topicName + "/image_raw", Image._TYPE);
            cameraInfoPublisher = connectedNode.newPublisher(topicName + "/camera_info", CameraInfo._TYPE);
            cameraInfoPublisher.setLatchMode(true);

            mCameraInfo.setDistortionModel(distortionModel);
            mCameraInfo.setD(D);
            mCameraInfo.setK(K);
            mCameraInfo.setR(R);
            mCameraInfo.setP(P);

            mCameraInfo.setWidth(width);
            mCameraInfo.setHeight(height);

            if (frameId != null) {
                mCameraInfo.getHeader().setFrameId(frameId);
                cameraInfoPublisher.publish(mCameraInfo);
            }

            mImage.setStep(width * 2);
            mImage.setWidth(width);
            mImage.setHeight(height);
            mImage.setEncoding("16UC1");
        }
    }

    public void setFrameId(String frameId) {
        this.frameId = frameId;
        if (mCameraInfo != null && cameraInfoPublisher != null) {
            mCameraInfo.getHeader().setFrameId(frameId);
            cameraInfoPublisher.publish(mCameraInfo);
        }
        if (mImage !=null)
            mImage.getHeader().setFrameId(frameId);
    }

    public void setOkPublish(boolean okPublish) {
        this.okPublish = okPublish;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("peanut/depth_publisher");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
        mImage = connectedNode.getTopicMessageFactory().newFromType(Image._TYPE);
        mCameraInfo = connectedNode.getTopicMessageFactory().newFromType(CameraInfo._TYPE);
        setTopicName(initialTopicName);
        setFrameId(initialFrameId);
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
