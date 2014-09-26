package edu.uml;

import java.nio.ByteBuffer;

/**
 * Created by emarcoux on 9/16/14.
 */
public abstract class VideoReciever {
    /**
     * The underlying byte buffer representing the current frame
     * of the camera in argb
     */
    public ByteBuffer buffer;

    /**
     * Called by TangoCameraView whenever buffer is updated
     */
    public abstract void VideoCallback();
}
