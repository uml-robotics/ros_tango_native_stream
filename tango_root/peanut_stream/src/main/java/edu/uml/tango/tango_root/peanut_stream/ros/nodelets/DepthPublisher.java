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

package edu.uml.tango.tango_root.peanut_stream.ros.nodelets;

import android.util.Log;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import edu.uml.DepthReceiver;
import edu.uml.tango.tango_root.peanut_stream.RateWatcher;
import sensor_msgs.PointCloud2;
import sensor_msgs.PointField;

public class DepthPublisher extends DepthReceiver implements NodeMain {
    private static final int SIZEOF_FLOAT = 4;

    private ConnectedNode connectedNode;

    private Publisher<PointCloud2> depthPublisher;
    private ArrayList<PointField>  depthCloudFields;
    private String topicName,frameId,tangoPrefix;
    private boolean okPublish;

    ChannelBuffer cb = null;
    public DepthPublisher(String topicName, String frameId, String tangoPrefix)
    {
        super();
        this.topicName=topicName;
        this.frameId=frameId;
        this.tangoPrefix = tangoPrefix;
    }

    private RateWatcher.RateProvider mRateProvider;
    public void setRateWatcher(RateWatcher.RateProvider rw)
    {
        mRateProvider = rw;
    }

    PointCloud2 mPointCloud;
    std_msgs.Header mHeader;

    @Override
    public void DepthCallback(ByteBuffer buffer, ByteBuffer bufferLength)
    {
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        bufferLength = bufferLength.order(ByteOrder.LITTLE_ENDIAN);

        if (!okPublish || connectedNode == null) {
            return;
        }
        Time currentTime = connectedNode.getCurrentTime();
        if(depthPublisher != null && mPointCloud != null && buffer !=null) {
            mHeader.setStamp(currentTime);
            mHeader.setSeq(mHeader.getSeq()+1);
            if (cb == null) {
                cb = ChannelBuffers.wrappedBuffer(buffer);
                mPointCloud.setHeader(mHeader);
            }

            mPointCloud.setWidth(bufferLength.getInt(0));
            final int length = mPointCloud.getWidth() * SIZEOF_FLOAT * 3;
            mPointCloud.setRowStep(length);
            // The next line will cause a gc eventually but there are only three fields
            // on the cb itself: a capacity, a ByteBuffer pointer and ByteOrder so it shouldn't
            // be too bad
            mPointCloud.setData(cb.slice(0, length));
            depthPublisher.publish(mPointCloud);
            if (mRateProvider != null)
                mRateProvider.addStamp(currentTime.secs,currentTime.nsecs);
        }                 // assumes Tegra is running in little Endian mode
    }


    public void setTopicName(String topicName) {
        this.topicName = topicName;
        if (depthPublisher != null)
            depthPublisher.shutdown();
        depthPublisher = connectedNode.newPublisher(tangoPrefix + topicName, PointCloud2._TYPE);
    }

    public void setFrameId(String frameId) {
        this.frameId = frameId;
        if (mPointCloud !=null)
            mHeader.setFrameId(tangoPrefix.replace("/", "") + frameId);
    }

    public void setTangoPrefix(String tangoPrefix){
        this.tangoPrefix = tangoPrefix;
        setTopicName(topicName);
        setFrameId(frameId);
    }

    public void setOkPublish(boolean okPublish) {
        this.okPublish = okPublish;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("yellowstone/depth_publisher");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;

        depthCloudFields = new ArrayList<PointField>();

        for(int i = 0; i < 3; ++i)
            depthCloudFields.add((PointField) connectedNode
                                                .getTopicMessageFactory()
                                                .newFromType(PointField._TYPE));

        // x
        depthCloudFields.get(0).setName("x");

        depthCloudFields.get(0).setCount(1);
        depthCloudFields.get(0).setDatatype((byte)7);       // float type
        depthCloudFields.get(0).setOffset(0);               // first float

        // y
        depthCloudFields.get(1).setName("y");
        depthCloudFields.get(1).setCount(1);
        depthCloudFields.get(1).setDatatype((byte)7);       // float type
        depthCloudFields.get(1).setOffset(1*SIZEOF_FLOAT);  // second float

        // z
        depthCloudFields.get(2).setName("z");
        depthCloudFields.get(2).setCount(1);
        depthCloudFields.get(2).setDatatype((byte)7);       // float type
        depthCloudFields.get(2).setOffset(2*SIZEOF_FLOAT);  // third float

        mPointCloud = connectedNode.getTopicMessageFactory().newFromType(PointCloud2._TYPE);
        mHeader = mPointCloud.getHeader();
        mPointCloud.setHeader(mPointCloud.getHeader());
        mPointCloud.setHeight(1);                           // use unordered 1D point cloud form
        mPointCloud.setIsDense(true);
        mPointCloud.setIsBigendian(false);
        mPointCloud.setPointStep(3 * SIZEOF_FLOAT);
        mPointCloud.setFields(depthCloudFields);

        setTangoPrefix(tangoPrefix);
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
