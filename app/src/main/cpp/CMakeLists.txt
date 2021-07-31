cmake_minimum_required(VERSION 3.4.1)

set(FFMPEG_HEAD ${CMAKE_SOURCE_DIR}/ffmpeghead)
set(SOUND_TOUCH_HEAD ${CMAKE_SOURCE_DIR}/soundtouchhead)
set(OPENCV_HEAD ${CMAKE_SOURCE_DIR}/opencvhead)
set(HEAR_DIR ${CMAKE_SOURCE_DIR}/include)
set(DLIB_DIR_HEAD ${CMAKE_SOURCE_DIR}/dlibhead)
set(SHARED_DIR ${CMAKE_SOURCE_DIR}/shared)

include_directories(${FFMPEG_HEAD})
include_directories(${SOUND_TOUCH_HEAD})
include_directories(${OPENCV_HEAD})
include_directories(${DLIB_DIR_HEAD})
include_directories(${HEAR_DIR})

add_library(
        avcodec
        SHARED
        IMPORTED)

set_target_properties(
        # lib name
        avcodec
        # property
        PROPERTIES IMPORTED_LOCATION
        # value
        ${SHARED_DIR}/${ANDROID_ABI}/libavcodec-58.so)

add_library(
        avdevice
        SHARED
        IMPORTED)

set_target_properties(
        # lib name
        avdevice
        # property
        PROPERTIES IMPORTED_LOCATION
        # value
        ${SHARED_DIR}/${ANDROID_ABI}/libavdevice-58.so)

add_library(
        avfilter
        SHARED
        IMPORTED)

set_target_properties(
        # lib name
        avfilter
        # property
        PROPERTIES IMPORTED_LOCATION
        # value
        ${SHARED_DIR}/${ANDROID_ABI}/libavfilter-7.so)

add_library(
        avformat
        SHARED
        IMPORTED)

set_target_properties(
        # lib name
        avformat
        # property
        PROPERTIES IMPORTED_LOCATION
        # value
        ${SHARED_DIR}/${ANDROID_ABI}/libavformat-58.so)

add_library(
        avutil
        SHARED
        IMPORTED)

set_target_properties(
        # lib name
        avutil
        # property
        PROPERTIES IMPORTED_LOCATION
        # value
        ${SHARED_DIR}/${ANDROID_ABI}/libavutil-56.so)

add_library(
        swresample
        SHARED
        IMPORTED)

set_target_properties(
        # lib name
        swresample
        # property
        PROPERTIES IMPORTED_LOCATION
        # value
        ${SHARED_DIR}/${ANDROID_ABI}/libswresample-3.so)

add_library(
        swscale
        SHARED
        IMPORTED)

set_target_properties(
        # lib name
        swscale
        # property
        PROPERTIES IMPORTED_LOCATION
        # value
        ${SHARED_DIR}/${ANDROID_ABI}/libswscale-5.so)

add_library(
        opencv_java4
        SHARED
        IMPORTED)

set_target_properties(
        # lib name
        opencv_java4
        # property
        PROPERTIES IMPORTED_LOCATION
        # value
        ${SHARED_DIR}/${ANDROID_ABI}/libopencv_java4.so)

add_library(
        dlib
        SHARED
        IMPORTED)

set_target_properties(
        # lib name
        dlib
        # property
        PROPERTIES IMPORTED_LOCATION
        # value
        ${SHARED_DIR}/${ANDROID_ABI}/libandroid_dlib.so)

aux_source_directory(${CMAKE_SOURCE_DIR}/implementation CPP_DIR)
aux_source_directory(${CMAKE_SOURCE_DIR}/soundtouch SOUND_TOUCH_CPP_DIR)

add_library( native-lib
             SHARED
             native-lib.cpp ${CPP_DIR} ${SOUND_TOUCH_CPP_DIR})

find_library( # name of the lib
              lib-log
              # name of lib in NDK
              log )

find_library(# name of the lib
             lib-opensl
             # name of lib in NDK
             openSLES)

target_link_libraries( # Specifies the target library.
                       native-lib
                       # Links the target library to the libraries below
                       jnigraphics
                       avcodec
                       avdevice
                       avfilter
                       avformat
                       avutil
                       swresample
                       swscale
                       opencv_java4
                       dlib
                       ${lib-log}
                       ${lib-opensl})