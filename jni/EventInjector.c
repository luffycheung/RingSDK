/*
 * Android Event Injector 
 *
 * Copyright (c) 2013 by Radu Motisan , radu.motisan@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For more information on the GPL, please go to:
 * http://www.gnu.org/copyleft/gpl.html
 *
 */

#include <string.h>
#include <stdint.h>
#include <jni.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <dirent.h>
#include <time.h>
#include <errno.h>

#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/inotify.h>
#include <sys/limits.h>
#include <sys/poll.h>

#include <linux/fb.h>
#include <linux/kd.h>
#include <linux/input.h>

#include <android/log.h>
#define TAG "EventInjector::JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__) 
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#include "EventInjector.h"

/* Debug tools
 */
int g_debug = 0;

void debug(char *szFormat, ...) {
	if (g_debug == 0)
		return;
	//if (strlen(szDbgfile) == 0) return;

	char szBuffer[4096]; //in this buffer we form the message
	const size_t NUMCHARS = sizeof(szBuffer) / sizeof(szBuffer[0]);
	const int LASTCHAR = NUMCHARS - 1;
	//format the input string
	va_list pArgs;
	va_start(pArgs, szFormat);
	// use a bounded buffer size to prevent buffer overruns.  Limit count to
	// character size minus one to allow for a NULL terminating character.
	vsnprintf(szBuffer, NUMCHARS - 1, szFormat, pArgs);
	va_end(pArgs);
	//ensure that the formatted string is NULL-terminated
	szBuffer[LASTCHAR] = '\0';
	LOGI(szBuffer);

	//TextCallback(szBuffer);
}

jint Java_com_xyworm_ringsdk_EventsJNI_intEnableDebug(JNIEnv* env,
		jobject thiz, jint enable) {

	g_debug = enable;
	return g_debug;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	debug("eventinterceptor native lib loaded.");
	return JNI_VERSION_1_2; //1_2 1_4
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
	debug("eventinterceptor native lib unloaded.");
}

static struct typedev {
	struct pollfd ufds;
	char *device_path;
	char *device_name;
}*pDevs = NULL;
struct pollfd *ufds;
static int nDevsCount;

const char *device_path = "/dev/input";

int g_Polling = 0;
struct input_event event;
int c;
int i;
int pollres;
int get_time = 0;
char *newline = "\n";
uint16_t get_switch = 0;
struct input_event event;
int version;

int dont_block = -1;
int event_count = 0;
int sync_rate = 0;
int64_t last_sync_time = 0;
const char *device = NULL;

static int open_device(int index) {
	if (index >= nDevsCount || pDevs == NULL)
		return -1;
	debug("open_device prep to open");
	char *device = pDevs[index].device_path;

	debug("open_device call %s", device);
	int version;
	int fd;

	char name[80];
	char location[80];
	char idstr[80];
	struct input_id id;

	fd = open(device, O_RDWR);
	if (fd < 0) {
		pDevs[index].ufds.fd = -1;

		pDevs[index].device_name = NULL;
		debug("could not open %s, %s", device, strerror(errno));
		return -1;
	}

	pDevs[index].ufds.fd = fd;
	ufds[index].fd = fd;

	name[sizeof(name) - 1] = '\0';
	if (ioctl(fd, EVIOCGNAME(sizeof(name) - 1), &name) < 1) {
		debug("could not get device name for %s, %s", device, strerror(errno));
		name[0] = '\0';
	}
	debug("Device %d: %s: %s", nDevsCount, device, name);

	pDevs[index].device_name = strdup(name);

	return 0;
}

int remove_device(int index) {
	if (index >= nDevsCount || pDevs == NULL)
		return -1;

	int count = nDevsCount - index - 1;
	debug("remove device %d", index);
	free(pDevs[index].device_path);
	free(pDevs[index].device_name);

	memmove(&pDevs[index], &pDevs[index + 1], sizeof(pDevs[0]) * count);
	nDevsCount--;
	return 0;
}

static int scan_dir(const char *dirname) {
	nDevsCount = 0;
	char devname[PATH_MAX];
	char *filename;
	DIR *dir;
	struct dirent *de;
	dir = opendir(dirname);
	if (dir == NULL)
		return -1;
	strcpy(devname, dirname);
	filename = devname + strlen(devname);
	*filename++ = '/';
	while ((de = readdir(dir))) {
		if (de->d_name[0] == '.'
				&& (de->d_name[1] == '\0'
						|| (de->d_name[1] == '.' && de->d_name[2] == '\0')))
			continue;
		strcpy(filename, de->d_name);
		debug("scan_dir:prepare to open:%s", devname);
		// add new filename to our structure: devname
		struct typedev *new_pDevs = realloc(pDevs,
				sizeof(pDevs[0]) * (nDevsCount + 1));
		if (new_pDevs == NULL) {
			debug("out of memory");
			return -1;
		}
		pDevs = new_pDevs;

		struct pollfd *new_ufds = realloc(ufds,
				sizeof(ufds[0]) * (nDevsCount + 1));
		if (new_ufds == NULL) {
			debug("out of memory");
			return -1;
		}
		ufds = new_ufds;
		ufds[nDevsCount].events = POLLIN;

		pDevs[nDevsCount].ufds.events = POLLIN;
		pDevs[nDevsCount].device_path = strdup(devname);

		nDevsCount++;
	}
	closedir(dir);
	return 0;
}

char* jstringTostring(JNIEnv* env, jstring jstr) {
	char* rtn = NULL;
	jclass clsstring = (*env)->FindClass(env, "java/lang/String");
	jstring strencode = (*env)->NewStringUTF(env, "utf-8");
	jmethodID mid = (*env)->GetMethodID(env, clsstring, "getBytes",
			"(Ljava/lang/String;)[B");
	jbyteArray barr = (jbyteArray) (*env)->CallObjectMethod(jstr, mid,
			strencode);
	jsize alen = (*env)->GetArrayLength(env, barr);
	jbyte* ba = (*env)->GetByteArrayElements(env, barr, JNI_FALSE);
	if (alen > 0) {
		rtn = (char*) malloc(alen + 1);

		memcpy(rtn, ba, alen);
		rtn[alen] = 0;
	}
	(*env)->ReleaseByteArrayElements(env, barr, ba, 0);

	return rtn;
}

jstring CharTojstring(JNIEnv* env, char* str) {
	jsize len = strlen(str);

	jclass clsstring = (*env)->FindClass(env, "java/lang/String");
	jstring strencode = (*env)->NewStringUTF(env, "GB2312");

	jmethodID mid = (*env)->GetMethodID(env, clsstring, "<init>",
			"([BLjava/lang/String;)V");
	jbyteArray barr = (*env)->NewByteArray(env, len);

	(*env)->SetByteArrayRegion(env, barr, 0, len, (jbyte*) str);
	return (jstring) (*env)->NewObject(env, clsstring, mid, barr, strencode);
}

jint Java_com_xyworm_ringsdk_EventsJNI_intSendEvent(JNIEnv* env,
		jobject thiz, jint index, uint16_t type, uint16_t code, int32_t value) {
	if (index >= nDevsCount || pDevs[index].ufds.fd == -1)
		return -1;
	int fd = pDevs[index].ufds.fd;
	debug("SendEvent call (%d,%d,%d,%d)", fd, type, code, value);
	struct uinput_event event;
	int len;

	if (fd <= fileno(stderr))
		return 0;

	memset(&event, 0, sizeof(event));
	event.type = type;
	event.code = code;
	event.value = value;

	len = write(fd, &event, sizeof(event));
	debug("SendEvent done:%d", len);
	return 1;
}

jint Java_com_xyworm_ringsdk_EventsJNI_ScanFiles(JNIEnv* env, jobject thiz) {
	int res = scan_dir(device_path);
	if (res < 0) {
		debug("scan dir failed for %s:", device_path);
		return -1;
	}

	return nDevsCount;
}

jstring Java_com_xyworm_ringsdk_EventsJNI_getDevPath(JNIEnv* env,
		jobject thiz, jint index) {
	return (*env)->NewStringUTF(env, pDevs[index].device_path);
}
jstring Java_com_xyworm_ringsdk_EventsJNI_getDevName(JNIEnv* env,
		jobject thiz, jint index) {
	if (pDevs[index].device_name == NULL)
		return NULL;
	else
		return (*env)->NewStringUTF(env, pDevs[index].device_name);
}

jint Java_com_xyworm_ringsdk_EventsJNI_OpenDev(JNIEnv* env, jobject thiz,
		jint index) {
	return open_device(index);
}

jint Java_com_xyworm_ringsdk_EventsJNI_RemoveDev(JNIEnv* env, jobject thiz,
		jint index) {
	return remove_device(index);
}

jint Java_com_xyworm_ringsdk_EventsJNI_PollDev(JNIEnv* env, jobject thiz,
		jint index) {
	if (index >= nDevsCount || pDevs[index].ufds.fd == -1)
		return -1;
	int pollres = poll(ufds, nDevsCount, -1);
	if (ufds[index].revents) {
		if (ufds[index].revents & POLLIN) {
			int res = read(ufds[index].fd, &event, sizeof(event));
			if (res < (int) sizeof(event)) {
				return 1;
			} else
				return 0;
		}
	}
	return -1;
}

jint Java_com_xyworm_ringsdk_EventsJNI_getType(JNIEnv* env, jobject thiz) {
	return event.type;
}

jint Java_com_xyworm_ringsdk_EventsJNI_getCode(JNIEnv* env, jobject thiz) {
	return event.code;
}

jint Java_com_xyworm_ringsdk_EventsJNI_getValue(JNIEnv* env, jobject thiz) {
	return event.value;
}

jdouble Java_com_xyworm_ringsdk_EventsJNI_getTime(JNIEnv* env, jobject thiz) {
	char str_time[100];
	sprintf(str_time, "%8ld.%06ld", event.time.tv_sec, event.time.tv_usec);
	return atof(str_time);
}

//
#include<pthread.h>

int *systemrun(char *command) {
	system(command);
	return -1;
}

jint Java_com_xyworm_ringsdk_EventsJNI_system(JNIEnv* env, jobject thiz,
		jstring s) {
	debug("begin trans");
	const char *get = (*env)->GetStringUTFChars(env, s, 0);
	debug(get);

	/* Use new thread to run command, otherwise the current thread will blocked*/
	pthread_t th;
	pthread_create(&th, NULL, &systemrun, get);
	return 1;
}

