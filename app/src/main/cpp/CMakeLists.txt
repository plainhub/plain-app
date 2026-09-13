cmake_minimum_required(VERSION 3.22)
project(plain_whisper_jni C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Vendored whisper.cpp v1.9.4 (see whisper/VENDOR.md), CPU-only: GPU backend
# source dirs are pruned at vendor time and their GGML_* options stay OFF.
set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper)

set(BUILD_SHARED_LIBS OFF)
add_subdirectory(${WHISPER_DIR}/ggml ggml-build)

add_library(whisper_jni SHARED
    whisper_jni.cpp
    ${WHISPER_DIR}/src/whisper.cpp
)

target_compile_definitions(whisper_jni PUBLIC GGML_USE_CPU)
target_compile_definitions(whisper_jni PRIVATE WHISPER_VERSION="1.9.4")

target_include_directories(whisper_jni PRIVATE
    ${WHISPER_DIR}/include
    ${WHISPER_DIR}/src
    ${WHISPER_DIR}/ggml/include
    ${WHISPER_DIR}/ggml/src
    ${WHISPER_DIR}/ggml/src/ggml-cpu
)

find_library(LOG_LIB log)
target_link_libraries(whisper_jni PRIVATE ${LOG_LIB} android ggml)

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(whisper_jni PRIVATE
        -O3
        -fvisibility=hidden
        -fvisibility-inlines-hidden
        -ffunction-sections
        -fdata-sections)
    target_link_options(whisper_jni PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
endif()
