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

additional packages:
-ros-hydro-rosjava-core
-maven2

Initial setup:
1. initialize a catkin workspace, source the devel/setup.bash
2. inside its 'src' directory, git clone this repository.
3. also inside the 'src' directory git clone https://github.com/rosjava/android_core
4. Download Tango C API from the Tango SDK page (we've been using "Tango_C_API_20140605_Phone")
5. Extract its contents to the EXTRACT_C_API_HERE directory such that the repository-relative paths EXTRACT_C_API_HERE/tango_api.so and EXTRACT_C_API_HERE/include/ exist.
6. in the toplevel workspace, run 'catkin_make'
7. run android studio within your ROS environment, and open project on tango_root/build.gradle, press ok.
8. It should ask you to sync your gradle files. If not, right click on build.gradle and select "synchronize build.gradle"
9. Run the program (Shift+f10) with peanutstream as the default application. It will prompt you to select your device.
10. Your tango will ask you to point to your rosmaster.
