#include <jni.h>
#include <jni-private.h>
#include "java_lang_Object.h"

/*
 * Class:     java_lang_Object
 * Method:    getClass
 * Signature: ()Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_java_lang_Object_getClass
  (JNIEnv *env, jobject obj) {
    return (*env)->GetObjectClass(env, obj);
}

/*
 * Class:     java_lang_Object
 * Method:    hashCode
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_java_lang_Object_hashCode
  (JNIEnv *env, jobject obj) {
    return Java_java_lang_System_identityHashCode(env, NULL, obj);
}

#if 0
/*
 * Class:     java_lang_Object
 * Method:    clone
 * Signature: ()Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_java_lang_Object_clone
  (JNIEnv *, jobject);

/*
 * Class:     java_lang_Object
 * Method:    notify
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_lang_Object_notify
  (JNIEnv *, jobject);

/*
 * Class:     java_lang_Object
 * Method:    notifyAll
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_lang_Object_notifyAll
  (JNIEnv *, jobject);

/*
 * Class:     java_lang_Object
 * Method:    wait
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_java_lang_Object_wait
  (JNIEnv *, jobject, jlong);

#endif
