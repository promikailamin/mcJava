cmake_minimum_required(VERSION 3.22.1)
project("mcjava-native" LANGUAGES C CXX)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Android specific settings
if(ANDROID)
    set(CMAKE_ANDROID_NDK_TOOLCHAIN_VERSION "clang")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions -frtti")
endif()

# Include directories
include_directories(${CMAKE_SOURCE_DIR}/include)

# Minecraft native library
add_library(mcjava-native SHARED
    src/main/cpp/mcjava_native.cpp
    src/main/cpp/egl_context.cpp
    src/main/cpp/input_handler.cpp
    src/main/cpp/asset_manager.cpp
)

# Link libraries
target_link_libraries(mcjava-native
    android
    log
    EGL
    GLESv3
    native_app_glue
)

# Copy native library to jniLibs
add_custom_command(TARGET mcjava-native POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
    $<TARGET_FILE:mcjava-native>
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libmcjava-native.so
)
