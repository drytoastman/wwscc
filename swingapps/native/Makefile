ifndef TARGET
$(error "No TARGET supplied")
endif

BDIR := ${TARGET}

ifeq "${TARGET}" "Linux32"
JAVAHOME=/usr/lib/jvm/java-1.5.0-gcj-1.5.0.0
JVMINC=-I${JAVAHOME}/include/ -I${JAVAHOME}/include/linux
CC = gcc
all:  ${BDIR}/libsqliteintf.so
endif

ifeq "${TARGET}" "Mac32"
JAVAHOME=$(shell /usr/libexec/java_home)
JVMINC=-I${JAVAHOME}/include/ 
CC = gcc
all:  ${BDIR}/libsqliteintf.jnilib
endif

ifeq "${TARGET}" "Windows32"
JAVAHOME=/cygdrive/c/Program\ Files/Java/jdk1.6.0_16
JVMINC=-I${JAVAHOME}/include -I${JAVAHOME}/include/win32
CC = gcc -mno-cygwin
all:  ${BDIR}/sqliteintf.dll
endif

# Windows x64
ifeq "${TARGET}" "Windows64"
JAVAHOME=/cygdrive/c/Program\ Files/Java/jdk1.6.0_16
JVMINC=-I${JAVAHOME}/include -I${JAVAHOME}/include/win32
CC = /home/bwilson/x64compiler/bin/x86_64-w64-mingw32-gcc.exe
all:  ${BDIR}/sqliteintf.dll
endif


${BDIR}/sqliteintf.o: sqliteintf.c SqliteDatabase.h
	${CC} -Wall -O2 -c ${JVMINC} -o $@ sqliteintf.c

${BDIR}/sqlite3.o: sqlite3.c sqlite3.h 
	${CC} -O2 -c -o $@ sqlite3.c

${BDIR}/libsqliteintf.so: ${BDIR}/sqlite3.o ${BDIR}/sqliteintf.o
	${CC} -Wl,-s -shared -o $@ $^

${BDIR}/sqliteintf.dll: ${BDIR}/sqlite3.o ${BDIR}/sqliteintf.o
	${CC} -Wl,--add-stdcall-alias -Wl,-s -shared -o $@ $^

${BDIR}/libsqliteintf.jnilib: ${BDIR}/sqlite3.o ${BDIR}/sqliteintf.o
	${CC} -dynamiclib -o $@ $^

clean:
	rm -f ${BDIR}/*


