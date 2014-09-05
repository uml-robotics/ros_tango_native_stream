//THIS IS A WORK IN PROGRESS
assert(IS_BROKEN || PROBABLE(WILL EAT YOUR CAT));
LOGW(DISCLAIMER); //told you so!

[out of date]
This is our implementation of using the C API to expose the Tango Depth and Visual Odometry information to ROS.
Since the Java services are being deprecated, we sought to create a good ROS alternative. We found the depth
information to publish at 25hz and the oodometry at 5hz, which is the speed at which the information becomes
available. The depth image is published by default to /tango/depth/image_raw with a frame id of /tango. The
odometry is published as a tf transform from /map to /tango, along with Odometry to /odom. The frame_id's and
depth topic name are all changable by textboxes while running the app, or you can change these in peanutstreams
string resources.

requires:
-ROS Hydro
-Android Studio
-NDK r10
-A compatible device

additional packages:
-ros-hydro-rosjava-core
-maven2

Initial setup:
-Same as on the master branch, with less success. HAVE FUN!
