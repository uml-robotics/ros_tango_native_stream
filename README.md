#ROS Tango Native Stream
This is our implementation of using the C API to expose the simultaneous streaming of Tango Depth and Visual Odometry information to ROS.
The depth image is published to \<tango_prefix\>/depth with a  frame id of \<tango_prefix\>/tango_camera_depth. The odometry is published as a tf transform from \<tango_prefix\>/odom to \<tango_prefix\>/base_link, along with Odometry to \<tango_prefix\>/odom. The \<tango_prefix\> value is changeable in a textbox on the interface. There is also a setting (activated with the "publish map->odom tf" switch) to publish a transform from map to odom, this will initially cause a transform of value (0,0,0),(0,0,0,1) to be published, this can be changed by publishing a pose to \<tango_prefix\>/initialpose. This is useful to set the starting position, using RViz for example. When this setting is enabled, the current estimated pose will also be published to \<tango_prefix\>/amcl_pose (While useful to integrate with software that expects amcl to be running, it is NOT running amcl, just a copy of the map->base_link transform). 

##requires:
* Ubuntu 14.04
* ROS Indigo
* Android Studio (and the Android SDK)
* Android NDK
* Java JDK
* rosjava (rosjava_core and android_core)

##Build Environment:
1. Make sure you have Java JDK installed. Android Studio can be picky about using OpenJDk, so for best results stick with oracle Java 7+. Make sure your JAVA_HOME env is set, and the bins are on your PATH.
2. Install android SDK (preferably, via the android studio bundle). Set ANDROID_HOME to the sdk directory, and add sdk/tools and sdk/platform-tools to your PATH.
3. Install Android NDK(possible via the android studio bundle with android studio >= 1.3), and make sure to add to your path.

###ROS Indigo:
- See http://wiki.ros.org/indigo/Installation/Ubuntu

###Rosjava:
- On ubuntu 14.04, the deb installation of rosjava works -- "apt-get install ros-indigo-rosjava")

###Initial setup:
1. initialize a catkin workspace, source the devel/setup.bash
2. inside its 'src' directory, git clone this repository.
3. Download Tango C API from the Tango SDK page
4. Extract its contents to the EXTRACT_TANGO_CLIENT_API_HERE directory such that the repository-relative paths EXTRACT_C_API_HERE/tango_api.so and EXTRACT_C_API_HERE/include/ exist.
5. in the toplevel workspace, run 'catkin_make'
6. run android studio within your ROS environment, and open project on tango_root/build.gradle, press ok.
7. It should ask you to sync your gradle files. If not, right click on build.gradle and select "synchronize build.gradle"
8. Run the program (Shift+f10) with peanutstream as the default application. It will prompt you to select your device.

##Usage:
1. Start PC-side components
  1. roscore
  2. (optional) rosrun rviz rviz
2. Tango-side
  1. Launch PeanutStream2 on your tango.
  2. Enter your pc's master URI
  3. Hit connect

###WIP
The button "Come to me" publishes the current displacement of the device to robot/move_base_simple/goal, which would cause a robot to drive to that location if the device was initialized over the robot.
Color image publishing is also a WIP, there are some remnants of it inside the code.

###TroubleShooting:

####ndk-build not found:
- Did you install the android ndk? Is it on your PATH?

####other funk (whether runtime or compile time):
- Try restarting the app if it is already running.
- Try restarting android studio (if you're using it)
- Try a clean build (either or both of "pushd $(rospack find tango_root) && pushd $(dirname $(find -name Android.mk)) && ndk-build clean; popd; popd" and "pushd $(rospack find tango_root) && ./gradlew clean; popd"), followed by your usual compilation workflow
