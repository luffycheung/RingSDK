LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog
LOCAL_MODULE    := EventsJNI
LOCAL_SRC_FILES := EventInjector.c
include $(BUILD_SHARED_LIBRARY)
