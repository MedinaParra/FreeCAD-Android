cmake_minimum_required(VERSION 3.10.2)
project("freecad-android-native" CXX)

# Support modern C++17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Configure compiler flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -fexceptions -frtti")

# Define our native library
add_library(freecad_native SHARED
        freecad_jni.cpp
        NativeCadEngine.cpp
        MeshData.cpp
)

# Locate Android logging support
find_library(log-lib log)

# Link log-lib to our native target
target_link_libraries(freecad_native
        ${log-lib}
)
