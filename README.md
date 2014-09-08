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
-Android SDK
-Android NDK
-Java JDK

additional packages:
-ros-hydro-rosjava-core

Build Environment
1. Make sure you have Java JDK installed. Android Studio can be picky about using OpenJDk, so for best results stick with oracle Java 7+. Make sure your JAVA_HOME env is set, and the bins are on your PATH.
2. Install android SDK using the sdk manager. At time of writing this we were building for SDK 17. Set ANDROID_HOME to the sdk directory, and add sdk/tools and sdk/platform-tools to your PATH.
3. Install Android NDK, and make sure to add to your path. We used ndk-r10.

ROS Hydro:
See http://wiki.ros.org/hydro/Installation/Ubuntu

Rosjava:
http://wiki.ros.org/rosjava
http://wiki.ros.org/android


Initial setup:
1. initialize a catkin workspace, source the devel/setup.bash
2. inside its 'src' directory, git clone this repository.
3. Download Tango C API from the Tango SDK page (we've been using "Tango_C_API_20140605_Phone")
4. Extract its contents to the EXTRACT_C_API_HERE directory such that the repository-relative paths EXTRACT_C_API_HERE/tango_api.so and EXTRACT_C_API_HERE/include/ exist.
5. in the toplevel workspace, run 'catkin_make'
6. run android studio within your ROS environment, and open project on tango_root/build.gradle, press ok.
7. It should ask you to sync your gradle files. If not, right click on build.gradle and select "synchronize build.gradle"
8. Run the program (Shift+f10) with peanutstream as the default application. It will prompt you to select your device.

Usage:
Your tango will ask you to point to your rosmaster. By default both depth and VIO are enabled. You can disable/enable them with the toggle buttons, as well as change the topic names with the textboxes. The button "Come to me" publishes the current position to /move_base_simple/goal. There is a helper package called tango_helper, that contains a launch file for running depth_image_proc, which takes in a depth image and outputs a point cloud. 

TroubleShooting:
ndk-build not found:
Did you install the android ndk? Is it on your PATH?
