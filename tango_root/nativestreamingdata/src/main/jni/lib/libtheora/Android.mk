LOCAL_PATH := $(call my-dir)

# #libtheora
# include $(CLEAR_VARS)
# LOCAL_MODULE     := libtheora
# LOCAL_SRC_FILES  := $(TARGET_ARCH_ABI)/libtheora.a
# LOCAL_C_INCLUDES := $(NDK_APP_PROJECT_PATH)/jni/include
# LOCAL_STATIC_LIBRARIES += libogg
# include $(PREBUILT_STATIC_LIBRARY)
# 
# #libtheoradec
# include $(CLEAR_VARS)
# LOCAL_MODULE     := libtheoradec
# LOCAL_SRC_FILES  := $(TARGET_ARCH_ABI)/libtheoradec.a
# LOCAL_C_INCLUDES := $(NDK_APP_PROJECT_PATH)/jni/include
# LOCAL_STATIC_LIBRARIES += libogg libtheora
# include $(PREBUILT_STATIC_LIBRARY)
# 
# #libtheoraenc
# include $(CLEAR_VARS)
# LOCAL_MODULE     := libtheoraenc
# LOCAL_SRC_FILES  := $(TARGET_ARCH_ABI)/libtheoraenc.a
# LOCAL_C_INCLUDES := $(NDK_APP_PROJECT_PATH)/jni/include
# LOCAL_STATIC_LIBRARIES += libogg libtheora
# include $(PREBUILT_STATIC_LIBRARY)
