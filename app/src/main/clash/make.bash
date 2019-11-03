#!/bin/bash

function try () {
"$@" || exit -1
}

[ -z "$ANDROID_NDK_HOME" ] && ANDROID_NDK_HOME=$ANDROID_HOME/ndk-bundle

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
MIN_API=$1
TARGET=$DIR/bin
DEPS=$DIR/.deps

ANDROID_ARM_TOOLCHAIN=$DEPS/android-toolchain-${MIN_API}-arm
ANDROID_ARM64_TOOLCHAIN=$DEPS/android-toolchain-${MIN_API}-arm64
ANDROID_X86_TOOLCHAIN=$DEPS/android-toolchain-${MIN_API}-x86
ANDROID_X86_64_TOOLCHAIN=$DEPS/android-toolchain-${MIN_API}-x86_64

ANDROID_ARM_CC=$ANDROID_ARM_TOOLCHAIN/bin/arm-linux-androideabi-clang
ANDROID_ARM_STRIP=$ANDROID_ARM_TOOLCHAIN/bin/arm-linux-androideabi-strip

ANDROID_ARM64_CC=$ANDROID_ARM64_TOOLCHAIN/bin/aarch64-linux-android-clang
ANDROID_ARM64_STRIP=$ANDROID_ARM64_TOOLCHAIN/bin/aarch64-linux-android-strip

ANDROID_X86_CC=$ANDROID_X86_TOOLCHAIN/bin/i686-linux-android-clang
ANDROID_X86_STRIP=$ANDROID_X86_TOOLCHAIN/bin/i686-linux-android-strip

ANDROID_X86_64_CC=$ANDROID_X86_64_TOOLCHAIN/bin/x86_64-linux-android-clang
ANDROID_X86_64_STRIP=$ANDROID_X86_64_TOOLCHAIN/bin/x86_64-linux-android-strip

try mkdir -p $TARGET/armeabi-v7a $TARGET/x86 $TARGET/arm64-v8a $TARGET/x86_64

if [ ! -f "$ANDROID_ARM_CC" ]; then
    echo "Make standalone toolchain for ARM arch"
    $ANDROID_NDK_HOME/build/tools/make_standalone_toolchain.py --arch arm \
        --api ${MIN_API} --install-dir $ANDROID_ARM_TOOLCHAIN
fi

if [ ! -f "$ANDROID_ARM64_CC" ]; then
    echo "Make standalone toolchain for ARM64 arch"
    $ANDROID_NDK_HOME/build/tools/make_standalone_toolchain.py --arch arm64 \
        --api ${MIN_API} --install-dir $ANDROID_ARM64_TOOLCHAIN
fi

if [ ! -f "$ANDROID_X86_CC" ]; then
    echo "Make standalone toolchain for X86 arch"
    $ANDROID_NDK_HOME/build/tools/make_standalone_toolchain.py --arch x86 \
        --api ${MIN_API} --install-dir $ANDROID_X86_TOOLCHAIN
fi

if [ ! -f "$ANDROID_X86_64_CC" ]; then
    echo "Make standalone toolchain for X86_64 arch"
    $ANDROID_NDK_HOME/build/tools/make_standalone_toolchain.py --arch x86_64 \
        --api ${MIN_API} --install-dir $ANDROID_X86_64_TOOLCHAIN
fi

if [ ! -f "$TARGET/armeabi-v7a/libclash.so" ] || [ ! -f "$TARGET/arm64-v8a/libclash.so" ] ||
   [ ! -f "$TARGET/x86/libclash.so" ] || [ ! -f "$TARGET/x86_64/libclash.so" ]; then

    pushd $DIR/src/clash

    echo "Get dependences for clash"

    echo "Cross compile overture for arm"
    if [ ! -f "$TARGET/armeabi-v7a/libclash.so" ]; then
        try env CGO_ENABLED=1 CC=$ANDROID_ARM_CC GOOS=android GOARCH=arm GOARM=7 go build -ldflags="-s -w"
        try $ANDROID_ARM_STRIP clash
        try mv clash $TARGET/armeabi-v7a/libclash.so
    fi

    echo "Cross compile overture for arm64"
    if [ ! -f "$TARGET/arm64-v8a/libclash.so" ]; then
        try env CGO_ENABLED=1 CC=$ANDROID_ARM64_CC GOOS=android GOARCH=arm64 go build -ldflags="-s -w"
        try $ANDROID_ARM64_STRIP clash
        try mv clash $TARGET/arm64-v8a/libclash.so
    fi

    echo "Cross compile overture for x86"
    if [ ! -f "$TARGET/x86/libclash.so" ]; then
        try env CGO_ENABLED=1 CC=$ANDROID_X86_CC GOOS=android GOARCH=386 go build -ldflags="-s -w"
        try $ANDROID_X86_STRIP clash
        try mv clash $TARGET/x86/libclash.so
    fi

    echo "Cross compile overture for x86_64"
        if [ ! -f "$TARGET/x86_64/libclash.so" ]; then
            try env CGO_ENABLED=1 CC=$ANDROID_X86_64_CC GOOS=android GOARCH=amd64 go build -ldflags="-s -w"
            try $ANDROID_X86_STRIP clash
            try mv clash $TARGET/x86_64/libclash.so
        fi
    popd

fi

echo "Successfully build clash"
