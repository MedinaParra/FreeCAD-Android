cmake_minimum_required(VERSION 3.10.2)
project("freecad-android-native" CXX)

# Support modern C++17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Configure compiler flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -fexceptions -frtti")

set(OCCT_ROOT "${CMAKE_SOURCE_DIR}/../../../../third-party/opencascade/${ANDROID_ABI}")
set(OCCT_FOUND OFF)

if(EXISTS "${OCCT_ROOT}/include/opencascade/Standard.hxx")
    set(OCCT_FOUND ON)
    message(STATUS "OpenCASCADE Technology (OCCT) found at: ${OCCT_ROOT}")
else()
    message(WARNING "OpenCASCADE Technology (OCCT) NOT found. Real STEP/FCStd BRep loading will be mock-disabled. Expected path: ${OCCT_ROOT}")
endif()

if(OCCT_FOUND)
    add_definitions(-DUSE_OCCT)
    include_directories("${OCCT_ROOT}/include/opencascade")
    
    # List of OCCT libraries to link
    set(OCCT_LIBS
        "${OCCT_ROOT}/lib/libTKSTEP.so"
        "${OCCT_ROOT}/lib/libTKSTEPBase.so"
        "${OCCT_ROOT}/lib/libTKSTEPAttr.so"
        "${OCCT_ROOT}/lib/libTKSTEP209.so"
        "${OCCT_ROOT}/lib/libTKXSBase.so"
        "${OCCT_ROOT}/lib/libTKBRep.so"
        "${OCCT_ROOT}/lib/libTKMesh.so"
        "${OCCT_ROOT}/lib/libTKMath.so"
        "${OCCT_ROOT}/lib/libTKernel.so"
        "${OCCT_ROOT}/lib/libTKG3d.so"
        "${OCCT_ROOT}/lib/libTKG2d.so"
        "${OCCT_ROOT}/lib/libTKGeomBase.so"
        "${OCCT_ROOT}/lib/libTKGeomAlgo.so"
        "${OCCT_ROOT}/lib/libTKTopAlgo.so"
    )
endif()

# Define our native library
add_library(freecad_native SHARED
        freecad_jni.cpp
        NativeCadEngine.cpp
        MeshData.cpp
)

# Locate Android logging support
find_library(log-lib log)

# Link libs to our native target
if(OCCT_FOUND)
    target_link_libraries(freecad_native
            ${log-lib}
            ${OCCT_LIBS}
    )
else()
    target_link_libraries(freecad_native
            ${log-lib}
    )
endif()
