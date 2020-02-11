package example;


import jnr.ffi.LibraryLoader;
import jnr.ffi.NativeLong;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.In;

public class OSSL {

    public static OpenSSL ossl() {
        OpenSSL ossl = SingletonHolder.OPENSSL_INSTANCE;
//        checkVersion(ossl);
        return ossl;
    }

    private static final String LIBRARY_NAME = libraryName();

    private static String libraryName() {
        switch (Platform.getNativePlatform().getOS()) {
            case WINDOWS:
                return "crypto";
            default:
                return "crypto";
        }
    }

    private static final class SingletonHolder {
        public static final OpenSSL OPENSSL_INSTANCE =
                LibraryLoader.create(OpenSSL.class)
                        .search("/usr/local/lib")
                        .search("/opt/local/lib")
                        .search("lib")
                        .load(LIBRARY_NAME);
        

    }

//    public static final Integer[] MIN_SUPPORTED_VERSION =
//            new Integer[] { 1, 0, 3 };
//
//    private static boolean versionSupported = false;
//
//    private static final void checkVersion(OpenSSL lib) {
//        if (!versionSupported) {
//            System.out.println("'" + lib.sodium_version_string() + "'");
//            String[] version = lib.sodium_version_string().split("\\.");
//            versionSupported = version.length >= 3 &&
//                MIN_SUPPORTED_VERSION[0] <= new Integer(version[0]) &&
//                MIN_SUPPORTED_VERSION[1] <= new Integer(version[1]) &&
//                MIN_SUPPORTED_VERSION[2] <= new Integer(version[2]);
//        }
//        if (!versionSupported) {
//            String message = String.format("Unsupported libsodium version: %s. Please update",
//                 
//                lib.sodium_version_string());
//            throw new UnsupportedOperationException(message);
//        }
//    }

    private OSSL() {
    }

    public interface OpenSSL {

      /**
       * @return the earliest error code from the thread's error queue without modifying it.
       */
      NativeLong ERR_peek_error();

      /**
       * Generates a human-readable string representing the error code e.
       *
       * @see <a>https://www.openssl.org/docs/manmaster/crypto/ERR_error_string.html</a>
       *
       * @param err
       *            the error code
       * @param null_
       *            buf is NULL, the error string is placed in a static buffer
       * @return the human-readable error messages.
       */
      String ERR_error_string(@In NativeLong err, @In char[] null_);

      Pointer CRYPTO_malloc(@In int num, @In String file, @In int line);
      
      /**
       * Creates a cipher context.
       *
       * @return a pointer to a newly created EVP_CIPHER_CTX for success and NULL for failure.
       */
      Pointer EVP_CIPHER_CTX_new();

      /**
       * Enables or disables padding
       *
       * @param c
       *            cipher context
       * @param pad
       *            If the pad parameter is zero then no padding is performed
       * @return always returns 1
       */
      int EVP_CIPHER_CTX_set_padding(@In Pointer c, @In int pad);

      /**
       * @return an openssl AES evp cipher instance with a 128-bit key CBC mode
       */
      Pointer EVP_aes_128_cbc();

      /**
       * @return an openssl AES evp cipher instance with a 128-bit key CTR mode
       */
      Pointer EVP_aes_128_ctr();

      /**
       * @return an openssl AES evp cipher instance with a 128-bit key GCM mode
       */
      Pointer EVP_aes_128_gcm();
      
      /**
       * @return an openssl AES evp cipher instance with a 192-bit key CBC mode
       */
      Pointer EVP_aes_192_cbc();

      /**
       * @return an openssl AES evp cipher instance with a 192-bit key CTR mode
       */
      Pointer EVP_aes_192_ctr();


      /**
       * @return an openssl AES evp cipher instance with a 192-bit key GCM mode
       */
      Pointer EVP_aes_192_gcm();
      
      /**
       * @return an openssl AES evp cipher instance with a 256-bit key CBC mode
       */
      Pointer EVP_aes_256_cbc();

      /**
       * @return an openssl AES evp cipher instance with a 256-bit key CTR mode
       */
      Pointer EVP_aes_256_ctr();

      /**
       * @return an openssl AES evp cipher instance with a 256-bit key GCM mode
       */
      Pointer EVP_aes_256_gcm();

      /**
       * Init a cipher.
       *
       * @param ctx
       *            cipher context
       * @param cipher
       *            evp cipher instance
       * @param impl
       *            engine
       * @param key
       *            key
       * @param iv
       *            iv
       * @param enc
       *            1 for encryption, 0 for decryption
       * @return 1 for success and 0 for failure.
       */
      int EVP_CipherInit_ex(@In Pointer ctx, @In Pointer cipher,
          @In Pointer impl, @In byte key[], @In byte iv[], @In int enc);

      int EVP_CIPHER_CTX_ctrl(@In Pointer ctx, @In int type, @In int arg, Pointer ptr);

      /**
       * Continues a multiple-part encryption/decryption operation.
       *
       * @param ctx
       *            cipher context
       * @param bout
       *            output byte buffer
       * @param outl
       *            output length
       * @param in
       *            input byte buffer
       * @param inl
       *            input length
       * @return 1 for success and 0 for failure.
       */
      int EVP_CipherUpdate(Pointer ctx, byte[] bout, int[] outl,
              byte[] in, int inl);

      /**
       * Finishes a multiple-part operation.
       *
       * @param ctx
       *            cipher context
       * @param bout
       *            output byte buffer
       * @param outl
       *            output length
       * @return 1 for success and 0 for failure.
       */
      int EVP_CipherFinal_ex(Pointer ctx, byte[] bout,
              int[] outl);

      /**
       * Clears all information from a cipher context and free up any allocated memory associate with
       * it, including ctx itself.
       *
       * @param c
       *            openssl evp cipher
       */
      void EVP_CIPHER_CTX_free(Pointer c);

      /**
       * Clears all information from a cipher context and free up any allocated * memory associate
       * with it.
       *
       * @param c
       *            openssl evp cipher
       */

      // Random generator
      /**
       * OpenSSL uses for random number generation
       *
       * @return pointers to the respective methods
       */
      Pointer RAND_get_rand_method();

      /**
       * Generates random data
       *
       * @param buf
       *            the bytes for generated random.
       * @param num
       *            buffer length
       * @return 1 on success, 0 otherwise.
       */
      int RAND_bytes(byte[] buf, int num);

      /**
       * Releases all functional references.
       *
       * @param e
       *            engine reference.
       * @return 0 on success, 1 otherwise.
       */
      int ENGINE_finish(Pointer e);

      /**
       * Frees the structural reference
       *
       * @param e
       *            engine reference.
       * @return 0 on success, 1 otherwise.
       */
      int ENGINE_free(Pointer e);

      /**
       * Obtains a functional reference from an existing structural reference.
       *
       * @param e
       *            engine reference
       * @return zero if the ENGINE was not already operational and couldn't be successfully
       *         initialized
       */
      int ENGINE_init(Pointer e);

      /**
       * Sets the engine as the default for random number generation.
       *
       * @param e
       *            engine reference
       * @param flags
       *            ENGINE_METHOD_RAND
       * @return zero if failed.
       */
      int ENGINE_set_default(Pointer e, int flags);

      /**
       * Gets engine by id
       *
       * @param id
       *            engine id
       * @return engine instance
       */
      Pointer ENGINE_by_id(String id);

      /**
       * Retrieves version/build information about OpenSSL library.
       *
       * @param type
       *            type can be OPENSSL_VERSION, OPENSSL_CFLAGS, OPENSSL_BUILT_ON...
       * @return A pointer to a constant string describing the version of the OpenSSL library or
       *         giving information about the library build.
       */
      String OpenSSL_version(int type);
    }
}
