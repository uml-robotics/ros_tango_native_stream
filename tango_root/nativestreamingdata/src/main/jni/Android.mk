LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := tango_client_api
LOCAL_SRC_FILES := lib/libtango_client_api.so
LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := tango_api_jni
LOCAL_SHARED_LIBRARIES += tango_client_api liblfds611
LOCAL_SRC_FILES := src/tango_api_jni.cpp src/reader_writer.c
LOCAL_C_INCLUDES += $(LOCAL_PATH)/src \
	$(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/src \
	$(LOCAL_PATH)/include
LOCAL_LDLIBS += -llog -landroid -lEGL -lGLESv2 -lm
include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/lib/Android.mk
