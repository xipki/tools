package example;

// /usr/include/sodium/crypto_scalarmult_curve25519.h
public class SodiumJni {
  static {
    System.loadLibrary("sodiumjni");
  }

  public native int jni_crypto_scalarmult_curve25519(byte[] q, byte[] n, byte[] p);

  public native int jni_crypto_scalarmult_curve25519_base(byte[] q, byte[] n);

}
