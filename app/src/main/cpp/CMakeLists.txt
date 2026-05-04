cmake_minimum_required(VERSION 3.22.1)
project("usagi-native")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_BUILD_TYPE Release)

# Check if libjpeg-turbo source files exist
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/libjpeg-turbo/jpeglib.h")
    message(STATUS "libjpeg-turbo sources found, enabling native JPEG decoder")

    # libjpeg-turbo - statically linked for JPEG decoding
    add_library(libjpeg-turbo STATIC
        libjpeg-turbo/jaricom.c
        libjpeg-turbo/jcapimin.c
        libjpeg-turbo/jcapistd.c
        libjpeg-turbo/jcarith.c
        libjpeg-turbo/jccoefct.c
        libjpeg-turbo/jccolor.c
        libjpeg-turbo/jcdctmgr.c
        libjpeg-turbo/jchuff.c
        libjpeg-turbo/jcinit.c
        libjpeg-turbo/jcmainct.c
        libjpeg-turbo/jcmarker.c
        libjpeg-turbo/jcmaster.c
        libjpeg-turbo/jcomapi.c
        libjpeg-turbo/jcparam.c
        libjpeg-turbo/jcprepct.c
        libjpeg-turbo/jcsample.c
        libjpeg-turbo/jctrans.c
        libjpeg-turbo/jdapimin.c
        libjpeg-turbo/jdapistd.c
        libjpeg-turbo/jdarith.c
        libjpeg-turbo/jdatadst.c
        libjpeg-turbo/jdatasrc.c
        libjpeg-turbo/jdcoefct.c
        libjpeg-turbo/jdcolor.c
        libjpeg-turbo/jddctmgr.c
        libjpeg-turbo/jdhuff.c
        libjpeg-turbo/jdinput.c
        libjpeg-turbo/jdmainct.c
        libjpeg-turbo/jdmarker.c
        libjpeg-turbo/jdmaster.c
        libjpeg-turbo/jdmerge.c
        libjpeg-turbo/jdpostct.c
        libjpeg-turbo/jdsample.c
        libjpeg-turbo/jdtrans.c
        libjpeg-turbo/jerror.c
        libjpeg-turbo/jfdctflt.c
        libjpeg-turbo/jfdctfst.c
        libjpeg-turbo/jfdctint.c
        libjpeg-turbo/jidctflt.c
        libjpeg-turbo/jidctfst.c
        libjpeg-turbo/jidctint.c
        libjpeg-turbo/jmemmgr.c
        libjpeg-turbo/jmemnobs.c
        libjpeg-turbo/jquant1.c
        libjpeg-turbo/jquant2.c
        libjpeg-turbo/jsimd_none.c
        libjpeg-turbo/jutils.c
    )

    target_include_directories(libjpeg-turbo PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/libjpeg-turbo
    )

    add_definitions(-DNATIVE_JPEG_ENABLED=1)
else()
    message(STATUS "libjpeg-turbo sources not found, JPEG decoder will use fallback")
    add_definitions(-DNATIVE_JPEG_ENABLED=0)
endif()

# Our JNI bridge library
add_library(usagi-native SHARED
    native-init.cpp
    native_image_probe.cpp
    native_zip_writer.cpp
    yuv-converter.cpp
)

# Conditionally add JPEG bridge
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/libjpeg-turbo/jpeglib.h")
    target_sources(usagi-native PRIVATE jpeg-bridge.cpp)
else()
    # Provide stubs for JNI method registration
    target_sources(usagi-native PRIVATE jpeg-stubs.cpp)
endif()

if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/libjpeg-turbo/jpeglib.h")
    target_link_libraries(usagi-native libjpeg-turbo)
endif()

target_link_libraries(usagi-native
    jnigraphics
    log
    z
)

target_include_directories(usagi-native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Android-specific flags
target_compile_options(usagi-native PRIVATE
    -O2
    -fvisibility=hidden
    -Wall -Wextra
)

# Strip symbols in release
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    set_target_properties(usagi-native PROPERTIES
        LINK_FLAGS "-Wl,--gc-sections -Wl,--strip-all"
    )
endif()
