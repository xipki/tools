CC = gcc

SRC_DIR = src
CFLAGS = -shared -fPIC -O3 -Iinclude -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -I/usr/include/sodium -lsodium

SRCS = $(wildcard ${SRC_DIR}/*.c)

all: libsodiumjni

libsodiumjni:
	${CC} -o $@.so ${SRCS} ${CFLAGS}
clean:
	rm -f *.o *.so
