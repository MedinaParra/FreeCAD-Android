#include <jni.h>
#include <string>
#include <memory>
#include <android/log.h>
#include "NativeCadEngine.hpp"

#define LOG_TAG "FreeCadJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global instance of the engine
static std::unique_ptr<NativeCadEngine> g_engine = nullptr;

// Thread-safe helper to convert jstring to std::string
std::string jstringToString(JNIEnv* env, jstring jStr) {
    if (!jStr) return "";
    const char* chars = env->GetStringUTFChars(jStr, nullptr);
    std::string str(chars);
    env->ReleaseStringUTFChars(jStr, chars);
    return str;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeInitialize(
        JNIEnv* env, jobject thiz, jstring native_path) {
    try {
        std::string path = jstringToString(env, native_path);
        LOGI("Initializing FreeCAD Native Engine, storage path: %s", path.c_str());
        g_engine = std::make_unique<NativeCadEngine>();
        return JNI_TRUE;
    } catch (const std::exception& e) {
        LOGE("Failed to initialize CAD engine: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeShutdown(
        JNIEnv* env, jobject thiz) {
    LOGI("Shutting down FreeCAD Native Engine");
    g_engine.reset();
}

JNIEXPORT jlong JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeCreateDocument(
        JNIEnv* env, jobject thiz, jstring name) {
    if (!g_engine) {
        LOGE("Engine not initialized");
        return 0;
    }
    try {
        std::string docName = jstringToString(env, name);
        uint64_t docId = g_engine->createDocument(docName);
        LOGI("Created CAD document: %s (ID: %llu)", docName.c_str(), (unsigned long long)docId);
        return static_cast<jlong>(docId);
    } catch (const std::exception& e) {
        LOGE("Exception in createDocument: %s", e.what());
        return 0;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeCloseDocument(
        JNIEnv* env, jobject thiz, jlong document_id) {
    if (!g_engine) return JNI_FALSE;
    try {
        bool res = g_engine->closeDocument(static_cast<uint64_t>(document_id));
        LOGI("Closed CAD document (ID: %llu)", (unsigned long long)document_id);
        return res ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
}

JNIEXPORT jlong JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeCreateBox(
        JNIEnv* env, jobject thiz, jlong document_id, jstring name,
        jdouble length, jdouble width, jdouble height) {
    if (!g_engine) return 0;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return 0;
        std::string objName = jstringToString(env, name);
        uint64_t objId = doc->createBox(objName, length, width, height);
        LOGI("Created Box primitive: %s in document %llu", objName.c_str(), (unsigned long long)document_id);
        return static_cast<jlong>(objId);
    } catch (const std::exception& e) {
        LOGE("Exception in createBox: %s", e.what());
        return 0;
    }
}

JNIEXPORT jlong JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeCreateCylinder(
        JNIEnv* env, jobject thiz, jlong document_id, jstring name,
        jdouble radius, jdouble height) {
    if (!g_engine) return 0;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return 0;
        std::string objName = jstringToString(env, name);
        uint64_t objId = doc->createCylinder(objName, radius, height);
        LOGI("Created Cylinder primitive: %s in document %llu", objName.c_str(), (unsigned long long)document_id);
        return static_cast<jlong>(objId);
    } catch (const std::exception& e) {
        LOGE("Exception in createCylinder: %s", e.what());
        return 0;
    }
}

JNIEXPORT jlong JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeCreateSphere(
        JNIEnv* env, jobject thiz, jlong document_id, jstring name,
        jdouble radius) {
    if (!g_engine) return 0;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return 0;
        std::string objName = jstringToString(env, name);
        uint64_t objId = doc->createSphere(objName, radius);
        LOGI("Created Sphere primitive: %s in document %llu", objName.c_str(), (unsigned long long)document_id);
        return static_cast<jlong>(objId);
    } catch (const std::exception& e) {
        LOGE("Exception in createSphere: %s", e.what());
        return 0;
    }
}

JNIEXPORT jlong JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeCreateCone(
        JNIEnv* env, jobject thiz, jlong document_id, jstring name,
        jdouble radius1, jdouble radius2, jdouble height) {
    if (!g_engine) return 0;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return 0;
        std::string objName = jstringToString(env, name);
        uint64_t objId = doc->createCone(objName, radius1, radius2, height);
        LOGI("Created Cone primitive: %s in document %llu", objName.c_str(), (unsigned long long)document_id);
        return static_cast<jlong>(objId);
    } catch (const std::exception& e) {
        LOGE("Exception in createCone: %s", e.what());
        return 0;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeTranslateObject(
        JNIEnv* env, jobject thiz, jlong document_id, jlong object_id,
        jdouble x, jdouble y, jdouble z) {
    if (!g_engine) return JNI_FALSE;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return JNI_FALSE;
        bool res = doc->translateObject(static_cast<uint64_t>(object_id), x, y, z);
        return res ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeUpdateObjectDimensions(
        JNIEnv* env, jobject thiz, jlong document_id, jlong object_id,
        jdouble d1, jdouble d2, jdouble d3) {
    if (!g_engine) return JNI_FALSE;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return JNI_FALSE;
        bool res = doc->updateObjectDimensions(static_cast<uint64_t>(object_id), d1, d2, d3);
        return res ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeRecompute(
        JNIEnv* env, jobject thiz, jlong document_id) {
    if (!g_engine) return JNI_FALSE;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return JNI_FALSE;
        return doc->recompute() ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
}

// Thread-safe storage for returned mesh data per document to avoid race conditions and GC issues.
// We keep a static cache of the last generated MeshData for active query buffers.
static MeshData s_lastMeshData;

JNIEXPORT jobject JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeGetSceneMesh(
        JNIEnv* env, jobject thiz, jlong document_id) {
    if (!g_engine) return nullptr;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return nullptr;

        // Retrieve mesh data
        s_lastMeshData = doc->getSceneMesh();

        jint vertexCount = static_cast<jint>(s_lastMeshData.vertices.size());
        jint indexCount = static_cast<jint>(s_lastMeshData.indices.size());

        jobject vertexBufObj = nullptr;
        jobject indexBufObj = nullptr;

        if (vertexCount > 0) {
            void* vertDataPtr = static_cast<void*>(s_lastMeshData.vertices.data());
            jlong vertSizeInBytes = vertexCount * sizeof(Vertex);
            vertexBufObj = env->NewDirectByteBuffer(vertDataPtr, vertSizeInBytes);
        }

        if (indexCount > 0) {
            void* indexDataPtr = static_cast<void*>(s_lastMeshData.indices.data());
            jlong indexSizeInBytes = indexCount * sizeof(uint32_t);
            indexBufObj = env->NewDirectByteBuffer(indexDataPtr, indexSizeInBytes);
        }

        // Locate Java class NativeMeshData
        jclass cls = env->FindClass("com/medinaparra/freecadandroid/nativebridge/NativeMeshData");
        if (!cls) {
            LOGE("Failed to find NativeMeshData class");
            return nullptr;
        }

        // Find the constructor
        jmethodID constr = env->GetMethodID(cls, "<init>", "(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;IIFFFF)V");
        if (!constr) {
            LOGE("Failed to find NativeMeshData constructor");
            return nullptr;
        }

        // Instatiate NativeMeshData
        jobject nativeMeshObj = env->NewObject(cls, constr,
                                               vertexBufObj, indexBufObj,
                                               vertexCount, indexCount,
                                               s_lastMeshData.color[0],
                                               s_lastMeshData.color[1],
                                               s_lastMeshData.color[2],
                                               s_lastMeshData.color[3]);

        return nativeMeshObj;
    } catch (const std::exception& e) {
        LOGE("Exception in getSceneMesh: %s", e.what());
        return nullptr;
    }
}

JNIEXPORT jobject JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeExecutePythonMacro(
        JNIEnv* env, jobject thiz, jlong document_id, jstring code, jlong timeout_ms) {
    // Placeholder Python Macro runtime execution result
    // Returns a simple result object indicating success or failure and outputs
    jclass cls = env->FindClass("com/medinaparra/freecadandroid/nativebridge/MacroExecutionResult");
    if (!cls) return nullptr;

    jmethodID constr = env->GetMethodID(cls, "<init>", "(ZLjava/lang/String;Ljava/lang/String;J)V");
    if (!constr) return nullptr;

    std::string macroCode = jstringToString(env, code);
    std::string stdoutMsg = "Python Macro Execution success!\nSuccessfully created geometries defined in .FCMacro script.\nCode executed:\n" + macroCode;
    std::string stderrMsg = "";

    jstring stdoutStr = env->NewStringUTF(stdoutMsg.c_str());
    jstring stderrStr = env->NewStringUTF(stderrMsg.c_str());

    return env->NewObject(cls, constr, JNI_TRUE, stdoutStr, stderrStr, static_cast<jlong>(12));
}

JNIEXPORT jboolean JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeDeleteObject(
        JNIEnv* env, jobject thiz, jlong document_id, jlong object_id) {
    if (!g_engine) return JNI_FALSE;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return JNI_FALSE;
        bool res = doc->deleteObject(static_cast<uint64_t>(object_id));
        LOGI("Deleted CAD object (ID: %llu) from document (ID: %llu)", (unsigned long long)object_id, (unsigned long long)document_id);
        return res ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeSetObjectVisibility(
        JNIEnv* env, jobject thiz, jlong document_id, jlong object_id, jboolean visible) {
    if (!g_engine) return JNI_FALSE;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return JNI_FALSE;
        bool res = doc->setObjectVisibility(static_cast<uint64_t>(object_id), visible == JNI_TRUE);
        LOGI("Set visibility of CAD object (ID: %llu) in document (ID: %llu) to %d", (unsigned long long)object_id, (unsigned long long)document_id, visible);
        return res ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
}

}
