#include <jni.h>
#include <string>
#include <memory>
#include <unordered_map>
#include <mutex>
#include <android/log.h>
#include "NativeCadEngine.hpp"

#define LOG_TAG "FreeCadJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global instance of the engine
static std::unique_ptr<NativeCadEngine> g_engine = nullptr;

// Thread-safe error storage
static std::mutex g_errorMutex;
static std::string g_last_error = "";

// Thread-safe mesh cache to persist Vertex/Index memory for Java direct ByteBuffers
static std::mutex g_meshCacheMutex;
static std::unordered_map<uint64_t, MeshData> g_meshCache;

void setLastError(const std::string& err) {
    std::lock_guard<std::mutex> lock(g_errorMutex);
    g_last_error = err;
}

std::string getLastError() {
    std::lock_guard<std::mutex> lock(g_errorMutex);
    return g_last_error;
}

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
        setLastError("");
        return JNI_TRUE;
    } catch (const std::exception& e) {
        LOGE("Failed to initialize CAD engine: %s", e.what());
        setLastError(e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeShutdown(
        JNIEnv* env, jobject thiz) {
    LOGI("Shutting down FreeCAD Native Engine");
    std::lock_guard<std::mutex> lock(g_meshCacheMutex);
    g_meshCache.clear();
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
        uint64_t docId = static_cast<uint64_t>(document_id);
        
        // Remove from cache
        {
            std::lock_guard<std::mutex> lock(g_meshCacheMutex);
            g_meshCache.erase(docId);
        }

        bool res = g_engine->closeDocument(docId);
        LOGI("Closed CAD document (ID: %llu)", (unsigned long long)docId);
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

JNIEXPORT jobject JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeImportStep(
        JNIEnv* env, jobject thiz, jstring name, jstring file_path) {
    
    jclass cls = env->FindClass("com/medinaparra/freecadandroid/nativebridge/CadImportResult");
    jmethodID constr = env->GetMethodID(cls, "<init>", "(ZJIIILjava/lang/String;Ljava/lang/String;)V");

    if (!g_engine) {
        jstring code = env->NewStringUTF("ENGINE_NOT_INIT");
        jstring msg = env->NewStringUTF("El motor nativo CAD no se inicializó correctamente.");
        return env->NewObject(cls, constr, JNI_FALSE, (jlong)0, (jint)0, (jint)0, (jint)0, code, msg);
    }

    std::string docName = jstringToString(env, name);
    std::string path = jstringToString(env, file_path);

    LOGI("nativeImportStep: Loading STEP file from %s...", path.c_str());

    uint64_t docId = g_engine->createDocument(docName);
    auto doc = g_engine->getDocument(docId);

    if (!doc) {
        jstring code = env->NewStringUTF("DOC_CREATION_FAILED");
        jstring msg = env->NewStringUTF("Error creando el espacio de trabajo.");
        return env->NewObject(cls, constr, JNI_FALSE, (jlong)0, (jint)0, (jint)0, (jint)0, code, msg);
    }

    bool success = doc->loadStep(path);
    if (!success) {
        g_engine->closeDocument(docId);
#ifndef USE_OCCT
        setLastError("OpenCASCADE Technology (OCCT) no se compiló en este artefacto. Use build_opencascade_android.sh.");
        jstring code = env->NewStringUTF("OCCT_MISSING");
        jstring msg = env->NewStringUTF("OpenCASCADE no está compilado en esta versión nativa.");
#else
        setLastError("La importación nativa del archivo STEP falló. Verifique el formato CAD original.");
        jstring code = env->NewStringUTF("STEP_LOAD_FAILED");
        jstring msg = env->NewStringUTF("El importador STEP de OpenCASCADE reportó un error de lectura.");
#endif
        return env->NewObject(cls, constr, JNI_FALSE, (jlong)0, (jint)0, (jint)0, (jint)0, code, msg);
    }

    // Cache mesh and calculate statistics
    MeshData mesh;
    {
        std::lock_guard<std::mutex> lock(g_meshCacheMutex);
        mesh = doc->getSceneMesh();
        g_meshCache[docId] = mesh;
    }

    jint vertexCount = static_cast<jint>(mesh.vertices.size());
    jint triangleCount = static_cast<jint>(mesh.indices.size() / 3);

    LOGI("nativeImportStep: Import succeeded. Vertices: %d, Triangles: %d", vertexCount, triangleCount);

    return env->NewObject(cls, constr, JNI_TRUE, static_cast<jlong>(docId), (jint)1, vertexCount, triangleCount, nullptr, nullptr);
}

JNIEXPORT jobject JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeImportBrep(
        JNIEnv* env, jobject thiz, jstring name, jstring file_path) {
    
    jclass cls = env->FindClass("com/medinaparra/freecadandroid/nativebridge/CadImportResult");
    jmethodID constr = env->GetMethodID(cls, "<init>", "(ZJIIILjava/lang/String;Ljava/lang/String;)V");

    if (!g_engine) {
        jstring code = env->NewStringUTF("ENGINE_NOT_INIT");
        jstring msg = env->NewStringUTF("El motor nativo CAD no se inicializó correctamente.");
        return env->NewObject(cls, constr, JNI_FALSE, (jlong)0, (jint)0, (jint)0, (jint)0, code, msg);
    }

    std::string docName = jstringToString(env, name);
    std::string path = jstringToString(env, file_path);

    LOGI("nativeImportBrep: Loading BRep/FCStd file from %s...", path.c_str());

    uint64_t docId = g_engine->createDocument(docName);
    auto doc = g_engine->getDocument(docId);

    if (!doc) {
        jstring code = env->NewStringUTF("DOC_CREATION_FAILED");
        jstring msg = env->NewStringUTF("Error creando el espacio de trabajo.");
        return env->NewObject(cls, constr, JNI_FALSE, (jlong)0, (jint)0, (jint)0, (jint)0, code, msg);
    }

    bool success = doc->loadBrep(path);
    if (!success) {
        g_engine->closeDocument(docId);
#ifndef USE_OCCT
        setLastError("OpenCASCADE Technology (OCCT) no se compiló en este artefacto. Use build_opencascade_android.sh.");
        jstring code = env->NewStringUTF("OCCT_MISSING");
        jstring msg = env->NewStringUTF("OpenCASCADE no está compilado en esta versión nativa.");
#else
        setLastError("La importación nativa del archivo BRep falló. Verifique el formato.");
        jstring code = env->NewStringUTF("BREP_LOAD_FAILED");
        jstring msg = env->NewStringUTF("El importador BRep de OpenCASCADE reportó un error de lectura.");
#endif
        return env->NewObject(cls, constr, JNI_FALSE, (jlong)0, (jint)0, (jint)0, (jint)0, code, msg);
    }

    // Cache mesh and calculate statistics
    MeshData mesh;
    {
        std::lock_guard<std::mutex> lock(g_meshCacheMutex);
        mesh = doc->getSceneMesh();
        g_meshCache[docId] = mesh;
    }

    jint vertexCount = static_cast<jint>(mesh.vertices.size());
    jint triangleCount = static_cast<jint>(mesh.indices.size() / 3);

    LOGI("nativeImportBrep: Import succeeded. Vertices: %d, Triangles: %d", vertexCount, triangleCount);

    return env->NewObject(cls, constr, JNI_TRUE, static_cast<jlong>(docId), (jint)1, vertexCount, triangleCount, nullptr, nullptr);
}

JNIEXPORT jobject JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeGetSceneMesh(
        JNIEnv* env, jobject thiz, jlong document_id) {
    if (!g_engine) return nullptr;
    try {
        uint64_t docId = static_cast<uint64_t>(document_id);
        auto doc = g_engine->getDocument(docId);
        if (!doc) return nullptr;

        // Retrieve and cache mesh data safely
        MeshData mesh;
        {
            std::lock_guard<std::mutex> lock(g_meshCacheMutex);
            mesh = doc->getSceneMesh();
            g_meshCache[docId] = mesh;
        }

        // We reference the mesh stored inside our thread-safe cache to avoid raw pointer invalidation
        const MeshData& cachedMesh = g_meshCache[docId];

        jint vertexCount = static_cast<jint>(cachedMesh.vertices.size());
        jint indexCount = static_cast<jint>(cachedMesh.indices.size());

        jobject vertexBufObj = nullptr;
        jobject indexBufObj = nullptr;

        if (vertexCount > 0) {
            void* vertDataPtr = const_cast<void*>(static_cast<const void*>(cachedMesh.vertices.data()));
            jlong vertSizeInBytes = vertexCount * sizeof(Vertex);
            vertexBufObj = env->NewDirectByteBuffer(vertDataPtr, vertSizeInBytes);
        }

        if (indexCount > 0) {
            void* indexDataPtr = const_cast<void*>(static_cast<const void*>(cachedMesh.indices.data()));
            jlong indexSizeInBytes = indexCount * sizeof(uint32_t);
            indexBufObj = env->NewDirectByteBuffer(indexDataPtr, indexSizeInBytes);
        }

        jclass cls = env->FindClass("com/medinaparra/freecadandroid/nativebridge/NativeSceneMesh");
        if (!cls) {
            LOGE("Failed to find NativeSceneMesh class");
            return nullptr;
        }

        jmethodID constr = env->GetMethodID(cls, "<init>", "(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;IIFFFFFF)V");
        if (!constr) {
            LOGE("Failed to find NativeSceneMesh constructor");
            return nullptr;
        }

        return env->NewObject(cls, constr,
                               vertexBufObj, indexBufObj,
                               vertexCount, indexCount,
                               cachedMesh.minX, cachedMesh.minY, cachedMesh.minZ,
                               cachedMesh.maxX, cachedMesh.maxY, cachedMesh.maxZ);
    } catch (const std::exception& e) {
        LOGE("Exception in getSceneMesh: %s", e.what());
        return nullptr;
    }
}

JNIEXPORT jobject JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeExecutePythonMacro(
        JNIEnv* env, jobject thiz, jlong document_id, jstring code, jlong timeout_ms) {
    jclass cls = env->FindClass("com/medinaparra/freecadandroid/nativebridge/MacroExecutionResult");
    if (!cls) return nullptr;

    jmethodID constr = env->GetMethodID(cls, "<init>", "(ZLjava/lang/String;Ljava/lang/String;J)V");
    if (!constr) return nullptr;

    // Report that execution is simulated or unsupported to prevent fake feedback
    jstring stdoutStr = env->NewStringUTF("");
    jstring stderrStr = env->NewStringUTF("Intérprete de Python embebido no está compilado en esta versión de Android.");

    return env->NewObject(cls, constr, JNI_FALSE, stdoutStr, stderrStr, static_cast<jlong>(0));
}

JNIEXPORT jboolean JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeDeleteObject(
        JNIEnv* env, jobject thiz, jlong document_id, jlong object_id) {
    if (!g_engine) return JNI_FALSE;
    try {
        auto doc = g_engine->getDocument(static_cast<uint64_t>(document_id));
        if (!doc) return JNI_FALSE;
        
        uint64_t docId = static_cast<uint64_t>(document_id);
        {
            std::lock_guard<std::mutex> lock(g_meshCacheMutex);
            g_meshCache.erase(docId);
        }

        bool res = doc->deleteObject(static_cast<uint64_t>(object_id));
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
        
        uint64_t docId = static_cast<uint64_t>(document_id);
        {
            std::lock_guard<std::mutex> lock(g_meshCacheMutex);
            g_meshCache.erase(docId);
        }

        bool res = doc->setObjectVisibility(static_cast<uint64_t>(object_id), visible == JNI_TRUE);
        return res ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
}

JNIEXPORT jstring JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeGetLastNativeError(
        JNIEnv* env, jobject thiz) {
    std::string err = getLastError();
    if (err.empty()) return nullptr;
    return env->NewStringUTF(err.c_str());
}

JNIEXPORT jobject JNICALL
Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_nativeGetNativeCapabilities(
        JNIEnv* env, jobject thiz) {
    jclass cls = env->FindClass("com/medinaparra/freecadandroid/nativebridge/NativeCapabilities");
    if (!cls) {
        LOGE("Failed to find NativeCapabilities class");
        return nullptr;
    }
    jmethodID constr = env->GetMethodID(cls, "<init>", "(ZZZZZZZZ)V");
    if (!constr) {
        LOGE("Failed to find NativeCapabilities constructor");
        return nullptr;
    }

    jboolean loaded = JNI_TRUE;
#ifdef USE_OCCT
    jboolean occt = JNI_TRUE;
    jboolean step = JNI_TRUE;
#else
    jboolean occt = JNI_FALSE;
    jboolean step = JNI_FALSE;
#endif
    jboolean freecadBase = JNI_FALSE;
    jboolean freecadApp = JNI_FALSE;
    jboolean partModule = JNI_FALSE;
    jboolean python = JNI_FALSE;
    jboolean fcstd = JNI_FALSE;

    return env->NewObject(cls, constr, loaded, occt, freecadBase, freecadApp, partModule, python, step, fcstd);
}

}
