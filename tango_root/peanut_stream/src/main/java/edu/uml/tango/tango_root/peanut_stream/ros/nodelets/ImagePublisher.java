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
 * Author Eric Marcoux <emarcoux@cs.uml.edu>
*/
package edu.uml.tango.tango_root.peanut_stream.ros.nodelets;

import android.util.Log;

import com.google.common.base.Preconditions;

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
import java.util.PropertyResourceBundle;

import edu.uml.ImageReciever;
import edu.uml.tango.tango_root.peanut_stream.RateWatcher;
import sensor_msgs.CameraInfo;
import sensor_msgs.Image;
import std_msgs.Header;


public class ImagePublisher extends ImageReciever implements NodeMain {
    private static final String TAG = "ImagePublisher";
    private Publisher<Image> mImagePublisher;

    private RateWatcher.RateProvider mRP;

    private ConnectedNode mConnectedNode;

    private String mTopicName, mFrameId;
    private boolean mOkPublish;

    private boolean isImageInfoSet;
    private int mHeight, mWidth, mStride;
    private int mLastKnownNumberOfSubscribers;

    private Time mCurTime;
    private Image mImage;
    private Header mImageHeader;
    private ChannelBuffer mChannelBuffer; // the channel buffer that wraps the image byte buffer

    public ImagePublisher(String topicName, String frameId) {
        Log.i(TAG, "imagePub Constructor");
        Preconditions.checkNotNull(topicName);
        Preconditions.checkNotNull(frameId);
        mTopicName = topicName;
        mFrameId = frameId;
        isImageInfoSet = false;
    }


    //TODO: Actually publish
    @Override
    protected void ImageInfo(int Height, int Width, int Stride) {
        mHeight = Height;
        mWidth  = Width;
        mStride = Stride;

        Log.w(TAG, "Image height = " + mHeight);
        Log.w(TAG, "Image width  = " + mWidth);
        Log.w(TAG, "Image stride = " + mStride);

        if(mImage != null) {
            isImageInfoSet = true;
            mImage.setWidth(mWidth);
            mImage.setHeight(mHeight);
            mImage.setStep(mStride);
        }
    }

    @Override
    protected void ImageCallback(ByteBuffer buffer) { // will only be sent after ImageInfo is called
        Log.i(TAG, "Got image in javaland");
        Log.i(TAG, buffer.toString());
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        // called once for every update in the stored image buffer
        if(!mOkPublish || mConnectedNode == null) return;

        mCurTime = mConnectedNode.getCurrentTime();
        if(mImagePublisher != null && mImage != null && buffer != null) {
            if(!isImageInfoSet) { // guaranteed to have been set before this function is called
                isImageInfoSet = true;
                mImage.setWidth(mWidth);
                mImage.setHeight(mHeight);
                mImage.setStep(mStride);
            }
            mImageHeader.setStamp(mCurTime);
            mImageHeader.setSeq(mImageHeader.getSeq()+1);

            if(mChannelBuffer == null) { // only ever done the first time called
                Log.i(TAG, "Setting up the channelbuffer");
                mChannelBuffer = ChannelBuffers.wrappedBuffer(buffer);
                mImage.setData(mChannelBuffer);
            }
            if(mChannelBuffer != null) {
                mImagePublisher.publish(mImage); // only publish after first image received
                //Log.i(TAG, "Publishing an image");
            }

            if(mRP != null) mRP.addStamp(mCurTime.secs, mCurTime.nsecs);
            mLastKnownNumberOfSubscribers = mImagePublisher.getNumberOfSubscribers();
        }
        else {
            if (mImagePublisher == null) ;
            Log.i(TAG, "imagePublisher is null");
            if (mImage == null);
            Log.i(TAG, "image is null");
            if (buffer == null) ;
            Log.i(TAG, "buffer is null");
        }
    }


    @Override
    public void onStart(ConnectedNode connectedNode) {
        Log.i(TAG, "starting imagepub node");
        mConnectedNode = connectedNode;
        mImage = connectedNode.getTopicMessageFactory().newFromType(Image._TYPE);
        mImageHeader = mImage.getHeader();
        mImage.setHeader(mImageHeader); //TODO is this really necessary
        mImage.setEncoding("RGB8"); //
        //TODO: set topic name in setTopicName function
        mImagePublisher = mConnectedNode.newPublisher(mTopicName, Image._TYPE);
        setTopicName(mTopicName);
        setFrameId(mFrameId);
    }

    public void setTopicName(String topic) {
        Preconditions.checkNotNull(topic);
        this.mTopicName = topic;

        if(mTopicName.equals(topic)) return;
        Log.i(TAG, "Hello from setTopicName");
    }

    public void setFrameId(String frameId) {
        Preconditions.checkNotNull(frameId);
        mImageHeader.setFrameId(frameId);
    }

    public void setOkPublish(boolean ok) { mOkPublish = ok; }
    public void setRP(RateWatcher.RateProvider rp) { this.mRP = rp; }

    @Override
    public GraphName getDefaultNodeName() { return GraphName.of("peanut/image_publisher"); }

    @Override
    public void onShutdown(Node node) { }

    @Override
    public void onShutdownComplete(Node node) { }

    @Override
    public void onError(Node node, Throwable throwable) { }


}
