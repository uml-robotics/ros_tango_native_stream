package edu.uml;

/**
 * Created by emarcoux on 10/30/14.
 */
public class TangoAPIException {
    public static final int SUCCESS = 0;
    public static final int ERR_ALREADY_RUNNING = 1;
    public static final int ERR_ALREADY_STOPPED = 2;
    public static final int ERR_COULD_NOT_CREATE_WORKER_THREAD = 3;
    public static final int ERR_COULD_NOT_JOIN_WORKER_THREAD = 4;
    public static final int ERR_COULD_NOT_CREATE_CONTEXT_MUTEX = 5;
    public static final int ERR_COULD_NOT_DESTROY_CONTEXT_MUTEX = 6;
}
