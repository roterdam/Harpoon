/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class java_lang_VMSystem */

#ifndef _Included_java_lang_VMSystem
#define _Included_java_lang_VMSystem
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     java_lang_VMSystem
 * Method:    arraycopy
 * Signature: (Ljava/lang/Object;ILjava/lang/Object;II)V
 */
JNIEXPORT void JNICALL Java_java_lang_VMSystem_arraycopy
  (JNIEnv *, jclass, jobject, jint, jobject, jint, jint);

/*
 * Class:     java_lang_VMSystem
 * Method:    identityHashCode
 * Signature: (Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMSystem_identityHashCode
  (JNIEnv *, jclass, jobject);

/*
 * Class:     java_lang_VMSystem
 * Method:    isWordsBigEndian
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_VMSystem_isWordsBigEndian
  (JNIEnv *, jclass);

/*
 * Class:     java_lang_VMSystem
 * Method:    setIn
 * Signature: (Ljava/io/InputStream;)V
 */
JNIEXPORT void JNICALL Java_java_lang_VMSystem_setIn
  (JNIEnv *, jclass, jobject);

/*
 * Class:     java_lang_VMSystem
 * Method:    setOut
 * Signature: (Ljava/io/PrintStream;)V
 */
JNIEXPORT void JNICALL Java_java_lang_VMSystem_setOut
  (JNIEnv *, jclass, jobject);

/*
 * Class:     java_lang_VMSystem
 * Method:    setErr
 * Signature: (Ljava/io/PrintStream;)V
 */
JNIEXPORT void JNICALL Java_java_lang_VMSystem_setErr
  (JNIEnv *, jclass, jobject);

/*
 * Class:     java_lang_VMSystem
 * Method:    currentTimeMillis
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_java_lang_VMSystem_currentTimeMillis
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
