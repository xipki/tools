Demos the access to cryptographic libraries like libsodium (libsodium.so / libcrypto.dll) and openssl (libcrypto.so / crypto.dll).

## Generation of own TLS certificates
### libsodium
The `libsodium.so` / `libsodium.dll` can be used as it is.

### openssl

OpenSSL does not export the internal functions like `X25519` and `X25519_public_from_private`.
You need to add these functions to the file `util/libcrypto.num` for OpenSSL version 3.x and 1.1.x.
For the version 1.1.x, you need to add the definition of the functions to one of the header files
in the folder `include/openssl`.
Finally build openssl to get the `libcrypto.so` file.
