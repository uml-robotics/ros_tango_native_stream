#ROS Tango Native Stream

#Deprecated:
This implementation is now deprecated on favor of https://github.com/uml-robotics/tango_ros_ndk, which uses a native port of ros for android, resulting in better performance. However, if someone has the need to use the old rosjava version, the yellowstone branch of this repository contains the old project before the switch to native ROS (master contains an even older project, for the first gen tango phones). 

This is our implementation of using the C API to expose the simultaneous streaming of Tango Depth and Visual Odometry information to ROS.
Since the Java services are being deprecated, we sought to create a good ROS alternative. We found the depth
information to publish at 25hz and the oodometry at 5hz, which is the speed at which the information becomes
available. The depth image is published by default to /tango/depth/image_raw with a frame id of /tango. The
odometry is published as a tf transform from /map to /tango, along with Odometry to /odom. The frame_id's and
depth topic name are all changable by textboxes while running the app, or you can change these in peanutstreams
string resources.

##requires:
* Ubuntu 12.04
* ROS Hydro
* Android Studio (and the Android SDK)
* Android NDK
* Java JDK
* rosjava (rosjava_core and android_core)

##Build Environment:
1. Make sure you have Java JDK installed. Android Studio can be picky about using OpenJDk, so for best results stick with oracle Java 7+. Make sure your JAVA_HOME env is set, and the bins are on your PATH.
2. Install android SDK (preferably, via the android studio bundle). Set ANDROID_HOME to the sdk directory, and add sdk/tools and sdk/platform-tools to your PATH.
3. run the sdk manager ("android"). Install android build tools 19.1, android support repository, android support library, and API 17 (this list may not be up to date, but is mostly correct as of 10/10/14)
4. Install Android NDK, and make sure to add to your path. We used ndk-r10.

###ROS Hydro:
- See http://wiki.ros.org/hydro/Installation/Ubuntu

###Rosjava:
- http://wiki.ros.org/rosjava (on ubuntu 12.04, the deb installation of rosjava works -- "apt-get install ros-hydro-rosjava")
- http://wiki.ros.org/android

###Initial setup:
1. initialize a catkin workspace, source the devel/setup.bash
2. inside its 'src' directory, git clone this repository.
3. Download Tango C API from the Tango SDK page (we've been using "Tango_C_API_20140605_Phone")
4. Extract its contents to the EXTRACT_C_API_HERE directory such that the repository-relative paths EXTRACT_C_API_HERE/tango_api.so and EXTRACT_C_API_HERE/include/ exist.
5. in the toplevel workspace, run 'catkin_make'
6. run android studio within your ROS environment, and open project on tango_root/build.gradle, press ok.
7. It should ask you to sync your gradle files. If not, right click on build.gradle and select "synchronize build.gradle"
8. Run the program (Shift+f10) with peanutstream as the default application. It will prompt you to select your device.

##Usage:
1. Start PC-side components
  1. roscore
  2. roslaunch tango_helper tango.launch
  3. (optional) rosrun rviz rviz
2. Tango-side
  1. Launch PeanutStream on your tango.
  2. Enter your pc's master URI
  3. Hit connect

###On-device configuration
* Publication of depth and odometry are independently toggleable
* Textboxes allow changing the topic names
* Textboxes allow specification of the frame_id and parent_frame_id of the transformation published based on the device odometry

###WIP
The button "Come to me" publishes the current displacement of the device to /move_base_simple/goal, which would cause a robot to drive to that location if the device was initialized over the robot.

###TroubleShooting:

####ndk-build not found:
- Did you install the android ndk? Is it on your PATH?

####other funk (whether runtime or compile time):
- Try restarting android studio (if you're using it)
- Try a clean build (either or both of "pushd $(rospack find tango_root) && pushd $(dirname $(find -name Android.mk)) && ndk-build clean; popd; popd" and "pushd $(rospack find tango_root) && ./gradlew clean; popd"), followed by your usual compilation workflow
