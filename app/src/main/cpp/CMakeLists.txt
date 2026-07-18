cmake_minimum_required(VERSION 3.10.2)
project("freecad-android-native" CXX)

# Support modern C++17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Configure compiler flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -fexceptions -frtti")

option(
    REQUIRE_REAL_CAD_BACKEND
    "Require a real OpenCASCADE Android backend"
    ON
)

set(OCCT_ROOT "${CMAKE_SOURCE_DIR}/../../../../third-party/opencascade/${ANDROID_ABI}")

if(EXISTS "${OCCT_ROOT}/include/opencascade/Standard.hxx")
    set(OCCT_FOUND ON)
    message(STATUS "OpenCASCADE Technology (OCCT) found at: ${OCCT_ROOT}")
else()
    set(OCCT_FOUND OFF)
    if(REQUIRE_REAL_CAD_BACKEND)
        message(FATAL_ERROR
            "OpenCASCADE no está compilado para ${ANDROID_ABI}. "
            "Ejecute scripts/build_opencascade_android.sh"
        )
    else()
        message(WARNING "OpenCASCADE Technology (OCCT) NOT found. Real STEP/FCStd BRep loading will be mock-disabled. Expected path: ${OCCT_ROOT}")
    endif()
endif()

if(OCCT_FOUND)
    add_definitions(-DUSE_OCCT)
    include_directories("${OCCT_ROOT}/include/opencascade")
    
    # List of required OCCT libraries
    set(OCCT_LIB_NAMES
        TKernel
        TKMath
        TKG2d
        TKG3d
        TKGeomBase
        TKGeomAlgo
        TKBRep
        TKTopAlgo
        TKPrim
        TKBO
        TKMesh
        TKXSBase
        TKSTEPBase
        TKSTEPAttr
        TKSTEP209
        TKSTEP
        TKIGES
    )

    set(OCCT_LIBS "")
    foreach(libname ${OCCT_LIB_NAMES})
        set(LIB_FILE "${OCCT_ROOT}/lib/lib${libname}.so")
        if(NOT EXISTS "${LIB_FILE}")
            message(FATAL_ERROR "Biblioteca de OpenCASCADE requerida no encontrada: ${LIB_FILE}")
        endif()
        
        # Create an IMPORTED target for each library to avoid path issues
        add_library(${libname} SHARED IMPORTED)
        set_target_properties(${libname} PROPERTIES
            IMPORTED_LOCATION "${LIB_FILE}"
        )
        list(APPEND OCCT_LIBS ${libname})
    endforeach()
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
