#include "SodiumJni.h"
#include "crypto_scalarmult_curve25519.h"

JNIEXPORT jint JNICALL Java_example_SodiumJni_jni_1crypto_1scalarmult_1curve25519
  (JNIEnv *env, jobject obj, jbyteArray q, jbyteArray n, jbyteArray p) {
  jbyte* jq = (jbyte*)(*env)->GetByteArrayElements(env, q, 0);
  jbyte* jn = (jbyte*)(*env)->GetByteArrayElements(env, n, 0);
  jbyte* jp = (jbyte*)(*env)->GetByteArrayElements(env, p, 0);

  jint rc = crypto_scalarmult_curve25519(jq, jn, jp);

  (*env)->ReleaseByteArrayElements(env, q, jq, 0);
  (*env)->ReleaseByteArrayElements(env, n, jn, 0);
  (*env)->ReleaseByteArrayElements(env, p, jp, 0);

  return rc;
}

JNIEXPORT jint JNICALL Java_example_SodiumJni_jni_1crypto_1scalarmult_1curve25519_1base
  (JNIEnv *env, jobject obj, jbyteArray q, jbyteArray n) {
  jbyte* jq = (jbyte*)(*env)->GetByteArrayElements(env, q, 0);
  jbyte* jn = (jbyte*)(*env)->GetByteArrayElements(env, n, 0);

  jint rc = crypto_scalarmult_curve25519_base(jq, jn);

  (*env)->ReleaseByteArrayElements(env, q, jq, 0);
  (*env)->ReleaseByteArrayElements(env, n, jn, 0);

  return rc;
}

