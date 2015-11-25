package edu.uml;

import java.nio.ByteBuffer;

/**
 * Abstract class that acts as a receiver for image updates.
 *
 * @author Eric Marcoux <emarcoux@cs.uml.edu>
 */
public abstract class ImageReciever {
    protected static final int SIZEOF_INT = Integer.SIZE / Byte.SIZE;

    protected TangoAPI tangoAPI;  // reference to the TangoAPI that called this object is bound to

    /**
     * Called by the TangoAPI every time a new image has been stored in the
     * buffer.  For information on the buffer's size please use the provided
     * helper functions.
     */
    abstract protected void ImageCallback(ByteBuffer image);

    /**
     * Called once by the TangoAPI on the first received image frame to
     * give information on the image size.
     */
    abstract protected void ImageInfo(int Height, int Width, int Stride);
}
