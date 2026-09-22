cmake_minimum_required(VERSION 3.22)
project("whisper-jni" C CXX)

# 强制 native 优化：AGP 的 debug 变体默认给 native 传 CMAKE_BUILD_TYPE=Debug（无 -O，即 -O0），
# 让 whisper 慢两个数量级（1.3s 音频要 77s）。覆盖成 Release 编译（-O3 -DNDEBUG）。
# 兜底：即便 build type 仍是 Debug，也把 Debug 标志改成 -O3。
set(CMAKE_BUILD_TYPE Release CACHE STRING "" FORCE)
set(CMAKE_C_FLAGS_DEBUG "-O3 -g" CACHE STRING "" FORCE)
set(CMAKE_CXX_FLAGS_DEBUG "-O3 -g" CACHE STRING "" FORCE)

# 只编 whisper 库，不编它的示例/测试/服务端；全部静态链接进一个 .so，避免分发包里拖一堆库
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(WHISPER_CURL OFF CACHE BOOL "" FORCE)
set(WHISPER_SDL2 OFF CACHE BOOL "" FORCE)
set(WHISPER_ALL_WARNINGS OFF CACHE BOOL "" FORCE)
set(WHISPER_ALL_WARNINGS_3RD_PARTY OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_CCACHE OFF CACHE BOOL "" FORCE)
# 关掉 OpenMP：ggml 的 OpenMP 后端在 ARM Android 上会让 GELU 等算子算出 NaN，
# 触发 assert(!isnan) 崩溃；改用 ggml 自带的 std::thread 线程池更稳。
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

add_subdirectory(whisper.cpp)

add_library(whisper-jni SHARED whisper-jni.cpp)

target_compile_features(whisper-jni PRIVATE cxx_std_11)
target_link_libraries(whisper-jni whisper log)

# 16KB 页对齐预演（Android 15+ / Play target 35）；NDK r27+ 支持该 flag
# 完整 E4c 可再升 NDK r28 并 zipalign -P 16 校验
if (ANDROID)
    target_link_options(whisper-jni PRIVATE "-Wl,-z,max-page-size=16384")
endif()
