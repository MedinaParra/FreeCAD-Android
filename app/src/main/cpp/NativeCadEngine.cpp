#include "NativeCadEngine.hpp"
#include <cmath>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "NativeCadEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#ifdef USE_OCCT
#include <BRep_Builder.hxx>
#include <BRepTools.hxx>
#include <Bnd_Box.hxx>
#include <BRepBndLib.hxx>
#include <BRepMesh_IncrementalMesh.hxx>
#include <TopExp_Explorer.hxx>
#include <TopoDS.hxx>
#include <TopoDS_Face.hxx>
#include <TopoDS_Compound.hxx>
#include <Poly_Triangulation.hxx>
#include <BRep_Tool.hxx>
#include <TColgp_Array1OfPnt.hxx>
#include <Poly_Array1OfTriangle.hxx>
#include <gp_Pnt.hxx>
#include <gp_Trsf.hxx>
#include <TopLoc_Location.hxx>
#include <STEPControl_Reader.hxx>
#include <IFSelect_ReturnStatus.hxx>
#endif

// Helper mesh generators for primitives
void generateBoxMesh(double l, double w, double h, double tx, double ty, double tz, MeshData& outMesh, float color[4]);
void generateCylinderMesh(double r, double h, double tx, double ty, double tz, MeshData& outMesh, float color[4]);
void generateSphereMesh(double r, double tx, double ty, double tz, MeshData& outMesh, float color[4]);
void generateConeMesh(double r1, double r2, double h, double tx, double ty, double tz, MeshData& outMesh, float color[4]);

#ifdef USE_OCCT
MeshData triangulateShape(const TopoDS_Shape& shape, double linearDeflection = 0.5, double angularDeflection = 0.5) {
    MeshData mesh;
    if (shape.IsNull()) {
        LOGE("triangulateShape: Shape is null!");
        return mesh;
    }

    LOGI("triangulateShape: Starting triangulation using BRepMesh_IncrementalMesh...");
    // 1. Compute triangulation on the solid shape
    BRepMesh_IncrementalMesh mesher(shape, linearDeflection, Standard_True, angularDeflection);

    double minX = 1e9, maxX = -1e9;
    double minY = 1e9, maxY = -1e9;
    double minZ = 1e9, maxZ = -1e9;

    // 2. Explore all faces
    TopExp_Explorer explorer(shape, TopAbs_FACE);
    int faceCount = 0;
    for (; explorer.More(); explorer.Next()) {
        faceCount++;
        const TopoDS_Face& face = TopoDS::Face(explorer.Current());
        TopLoc_Location loc;
        Handle(Poly_Triangulation) triangulation = BRep_Tool::Triangulation(face, loc);
        if (triangulation.IsNull()) {
            continue;
        }

        const TColgp_Array1OfPnt& nodes = triangulation->Nodes();
        const Poly_Array1OfTriangle& triangles = triangulation->Triangles();
        gp_Trsf trans = loc.Transformation();

        uint32_t startVertexIdx = mesh.vertices.size();

        // Copy vertices in absolute coordinates
        for (Standard_Integer i = nodes.Lower(); i <= nodes.Upper(); ++i) {
            gp_Pnt p = nodes.Value(i).Transformed(trans);
            Vertex v;
            v.x = static_cast<float>(p.X());
            v.y = static_cast<float>(p.Y());
            v.z = static_cast<float>(p.Z());

            // Initialize default normal
            v.nx = 0.0f; v.ny = 0.0f; v.nz = 0.0f;
            mesh.vertices.push_back(v);

            // Bounding box tracking
            if (v.x < minX) minX = v.x; if (v.x > maxX) maxX = v.x;
            if (v.y < minY) minY = v.y; if (v.y > maxY) maxY = v.y;
            if (v.z < minZ) minZ = v.z; if (v.z > maxZ) maxZ = v.z;
        }

        // Copy indices with CCW orientation winding verification
        bool reversed = (face.Orientation() == TopAbs_REVERSED);
        for (Standard_Integer i = triangles.Lower(); i <= triangles.Upper(); ++i) {
            Standard_Integer n1, n2, n3;
            triangles.Value(i).Get(n1, n2, n3);

            uint32_t idx1 = startVertexIdx + (n1 - nodes.Lower());
            uint32_t idx2 = startVertexIdx + (n2 - nodes.Lower());
            uint32_t idx3 = startVertexIdx + (n3 - nodes.Lower());

            if (reversed) {
                mesh.indices.push_back(idx1);
                mesh.indices.push_back(idx3);
                mesh.indices.push_back(idx2);
            } else {
                mesh.indices.push_back(idx1);
                mesh.indices.push_back(idx2);
                mesh.indices.push_back(idx3);
            }
        }
    }

    LOGI("triangulateShape: Traversed %d faces. Vertices: %zu, Indices: %zu", faceCount, mesh.vertices.size(), mesh.indices.size());

    // Fallback bounds if no vertices exist
    if (mesh.vertices.empty()) {
        minX = minY = minZ = -10.0;
        maxX = maxY = maxZ = 10.0;
    }

    mesh.minX = static_cast<float>(minX);
    mesh.maxX = static_cast<float>(maxX);
    mesh.minY = static_cast<float>(minY);
    mesh.maxY = static_cast<float>(maxY);
    mesh.minZ = static_cast<float>(minZ);
    mesh.maxZ = static_cast<float>(maxZ);

    // Compute robust, normalized smooth vertex normals for Phong rendering
    std::vector<int> normalCount(mesh.vertices.size(), 0);
    for (size_t i = 0; i < mesh.indices.size(); i += 3) {
        uint32_t i1 = mesh.indices[i];
        uint32_t i2 = mesh.indices[i+1];
        uint32_t i3 = mesh.indices[i+2];

        Vertex& v1 = mesh.vertices[i1];
        Vertex& v2 = mesh.vertices[i2];
        Vertex& v3 = mesh.vertices[i3];

        float ux = v2.x - v1.x;
        float uy = v2.y - v1.y;
        float uz = v2.z - v1.z;

        float vx = v3.x - v1.x;
        float vy = v3.y - v1.y;
        float vz = v3.z - v1.z;

        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;

        float len = std::sqrt(nx*nx + ny*ny + nz*nz);
        if (len > 1e-6f) {
            nx /= len; ny /= len; nz /= len;
            v1.nx += nx; v1.ny += ny; v1.nz += nz;
            v2.nx += nx; v2.ny += ny; v2.nz += nz;
            v3.nx += nx; v3.ny += ny; v3.nz += nz;
            normalCount[i1]++;
            normalCount[i2]++;
            normalCount[i3]++;
        }
    }

    for (size_t i = 0; i < mesh.vertices.size(); ++i) {
        if (normalCount[i] > 0) {
            float nx = mesh.vertices[i].nx;
            float ny = mesh.vertices[i].ny;
            float nz = mesh.vertices[i].nz;
            float len = std::sqrt(nx*nx + ny*ny + nz*nz);
            if (len > 1e-6f) {
                mesh.vertices[i].nx = nx / len;
                mesh.vertices[i].ny = ny / len;
                mesh.vertices[i].nz = nz / len;
            } else {
                mesh.vertices[i].nx = 0.0f;
                mesh.vertices[i].ny = 0.0f;
                mesh.vertices[i].nz = 1.0f;
            }
        } else {
            mesh.vertices[i].nx = 0.0f;
            mesh.vertices[i].ny = 0.0f;
            mesh.vertices[i].nz = 1.0f;
        }
    }

    return mesh;
}
#endif

CadDocument::CadDocument(uint64_t docId, const std::string& docName)
    : id(docId), name(docName) {
#ifdef USE_OCCT
    hasImportedShape = false;
#endif
}

uint64_t CadDocument::createBox(const std::string& name, double l, double w, double h) {
    auto obj = std::make_shared<CadObject>();
    obj->id = nextObjectId++;
    obj->name = name;
    obj->type = ObjectType::BOX;
    obj->dimensions[0] = l;
    obj->dimensions[1] = w;
    obj->dimensions[2] = h;
    obj->color[0] = 0.2f; obj->color[1] = 0.6f; obj->color[2] = 0.8f; obj->color[3] = 1.0f;
    objects[obj->id] = obj;
    return obj->id;
}

uint64_t CadDocument::createCylinder(const std::string& name, double r, double h) {
    auto obj = std::make_shared<CadObject>();
    obj->id = nextObjectId++;
    obj->name = name;
    obj->type = ObjectType::CYLINDER;
    obj->dimensions[0] = r;
    obj->dimensions[1] = h;
    obj->dimensions[2] = 0.0;
    obj->color[0] = 0.8f; obj->color[1] = 0.4f; obj->color[2] = 0.2f; obj->color[3] = 1.0f;
    objects[obj->id] = obj;
    return obj->id;
}

uint64_t CadDocument::createSphere(const std::string& name, double r) {
    auto obj = std::make_shared<CadObject>();
    obj->id = nextObjectId++;
    obj->name = name;
    obj->type = ObjectType::SPHERE;
    obj->dimensions[0] = r;
    obj->dimensions[1] = 0.0;
    obj->dimensions[2] = 0.0;
    obj->color[0] = 0.6f; obj->color[1] = 0.2f; obj->color[2] = 0.8f; obj->color[3] = 1.0f;
    objects[obj->id] = obj;
    return obj->id;
}

uint64_t CadDocument::createCone(const std::string& name, double r1, double r2, double h) {
    auto obj = std::make_shared<CadObject>();
    obj->id = nextObjectId++;
    obj->name = name;
    obj->type = ObjectType::CONE;
    obj->dimensions[0] = r1;
    obj->dimensions[1] = r2;
    obj->dimensions[2] = h;
    obj->color[0] = 0.9f; obj->color[1] = 0.7f; obj->color[2] = 0.1f; obj->color[3] = 1.0f;
    objects[obj->id] = obj;
    return obj->id;
}

bool CadDocument::translateObject(uint64_t objId, double x, double y, double z) {
    auto it = objects.find(objId);
    if (it != objects.end()) {
        it->second->translation[0] = x;
        it->second->translation[1] = y;
        it->second->translation[2] = z;
        return true;
    }
    return false;
}

bool CadDocument::updateObjectDimensions(uint64_t objId, double d1, double d2, double d3) {
    auto it = objects.find(objId);
    if (it != objects.end()) {
        it->second->dimensions[0] = d1;
        it->second->dimensions[1] = d2;
        it->second->dimensions[2] = d3;
        return true;
    }
    return false;
}

bool CadDocument::deleteObject(uint64_t objId) {
    return objects.erase(objId) > 0;
}

bool CadDocument::setObjectVisibility(uint64_t objId, bool visible) {
    auto it = objects.find(objId);
    if (it != objects.end()) {
        it->second->visible = visible;
        return true;
    }
    return false;
}

bool CadDocument::recompute() {
    return true;
}

bool CadDocument::loadStep(const std::string& filePath) {
#ifdef USE_OCCT
    LOGI("loadStep: Reading STEP file: %s", filePath.c_str());
    STEPControl_Reader reader;
    IFSelect_ReturnStatus status = reader.ReadFile(filePath.c_str());
    if (status != IFSelect_RetDone) {
        LOGE("loadStep: ReadFile failed with status: %d", status);
        return false;
    }
    
    reader.TransferRoots();
    importedShape = reader.OneShape();
    hasImportedShape = !importedShape.IsNull();
    
    if (hasImportedShape) {
        LOGI("loadStep: Successfully loaded solid shape from STEP.");
    } else {
        LOGE("loadStep: Result shape is null.");
    }
    return hasImportedShape;
#else
    LOGE("loadStep: OpenCASCADE is NOT compiled in this build! Cannot import STEP file natively.");
    (void)filePath;
    return false;
#endif
}

bool CadDocument::loadBrep(const std::string& filePath) {
#ifdef USE_OCCT
    LOGI("loadBrep: Reading BRep/FCStd files: %s", filePath.c_str());
    
    std::vector<std::string> paths;
    size_t start = 0;
    size_t end = filePath.find(';');
    while (end != std::string::npos) {
        paths.push_back(filePath.substr(start, end - start));
        start = end + 1;
        end = filePath.find(';', start);
    }
    paths.push_back(filePath.substr(start));

    BRep_Builder builder;
    TopoDS_Compound compound;
    builder.MakeCompound(compound);
    bool anyLoaded = false;

    for (const auto& path : paths) {
        if (path.empty()) continue;
        TopoDS_Shape shape;
        Standard_Boolean result = BRepTools::Read(shape, path.c_str(), builder);
        if (result && !shape.IsNull()) {
            builder.Add(compound, shape);
            anyLoaded = true;
            LOGI("loadBrep: Successfully loaded solid shape from: %s", path.c_str());
        } else {
            LOGE("loadBrep: Failed to read shape from: %s", path.c_str());
        }
    }

    if (!anyLoaded) {
        LOGE("loadBrep: No shapes could be loaded.");
        return false;
    }

    importedShape = compound;
    hasImportedShape = true;
    LOGI("loadBrep: Successfully loaded compound solid shapes from BRep.");
    return true;
#else
    LOGE("loadBrep: OpenCASCADE is NOT compiled in this build! Cannot import BRep file natively.");
    (void)filePath;
    return false;
#endif
}

MeshData CadDocument::getSceneMesh() {
#ifdef USE_OCCT
    if (hasImportedShape) {
        return triangulateShape(importedShape, 0.5, 0.5);
    }
#endif

    // Fallback: build combined mesh from primitives
    double minX = 1e9, maxX = -1e9;
    double minY = 1e9, maxY = -1e9;
    double minZ = 1e9, maxZ = -1e9;

    for (const auto& pair : objects) {
        const auto& obj = pair.second;
        if (!obj->visible) continue;
        
        double tx = obj->translation[0];
        double ty = obj->translation[1];
        double tz = obj->translation[2];
        
        double d1 = obj->dimensions[0]; // Box: length, Cyl: radius
        double d2 = obj->dimensions[1]; // Box: width, Cyl: height
        double d3 = obj->dimensions[2]; // Box: height, Cyl: 0
        
        double oxMin, oxMax, oyMin, oyMax, ozMin, ozMax;
        if (obj->type == ObjectType::BOX) {
            oxMin = tx; oxMax = tx + d1;
            oyMin = ty; oyMax = ty + d2;
            ozMin = tz; ozMax = tz + d3;
        } else if (obj->type == ObjectType::SPHERE) {
            oxMin = tx - d1; oxMax = tx + d1;
            oyMin = ty - d1; oyMax = ty + d1;
            ozMin = tz - d1; ozMax = tz + d1;
        } else if (obj->type == ObjectType::CONE) {
            double maxR = std::max(d1, d2);
            oxMin = tx - maxR; oxMax = tx + maxR;
            oyMin = ty - maxR; oyMax = ty + maxR;
            ozMin = tz; ozMax = tz + d3;
        } else { // CYLINDER
            oxMin = tx - d1; oxMax = tx + d1;
            oyMin = ty - d1; oyMax = ty + d1;
            ozMin = tz; ozMax = tz + d2;
        }
        
        if (oxMin < minX) minX = oxMin;
        if (oxMax > maxX) maxX = oxMax;
        if (oyMin < minY) minY = oyMin;
        if (oyMax > maxY) maxY = oyMax;
        if (ozMin < minZ) minZ = ozMin;
        if (ozMax > maxZ) maxZ = ozMax;
    }

    if (minX > maxX) {
        minX = -40.0; maxX = 40.0;
        minY = -40.0; maxY = 40.0;
        minZ = -40.0; maxZ = 40.0;
    }

    MeshData combinedMesh;
    combinedMesh.minX = static_cast<float>(minX);
    combinedMesh.maxX = static_cast<float>(maxX);
    combinedMesh.minY = static_cast<float>(minY);
    combinedMesh.maxY = static_cast<float>(maxY);
    combinedMesh.minZ = static_cast<float>(minZ);
    combinedMesh.maxZ = static_cast<float>(maxZ);

    for (const auto& pair : objects) {
        const auto& obj = pair.second;
        if (!obj->visible) continue;

        double tx = obj->translation[0];
        double ty = obj->translation[1];
        double tz = obj->translation[2];

        if (obj->type == ObjectType::BOX) {
            generateBoxMesh(obj->dimensions[0], obj->dimensions[1], obj->dimensions[2],
                            tx, ty, tz,
                            combinedMesh, obj->color);
        } else if (obj->type == ObjectType::CYLINDER) {
            generateCylinderMesh(obj->dimensions[0], obj->dimensions[1],
                                 tx, ty, tz,
                                 combinedMesh, obj->color);
        } else if (obj->type == ObjectType::SPHERE) {
            generateSphereMesh(obj->dimensions[0],
                               tx, ty, tz,
                               combinedMesh, obj->color);
        } else if (obj->type == ObjectType::CONE) {
            generateConeMesh(obj->dimensions[0], obj->dimensions[1], obj->dimensions[2],
                             tx, ty, tz,
                             combinedMesh, obj->color);
        }
    }
    return combinedMesh;
}

// Global Engine implementation
NativeCadEngine::NativeCadEngine() {}
NativeCadEngine::~NativeCadEngine() {}

uint64_t NativeCadEngine::createDocument(const std::string& name) {
    std::lock_guard<std::mutex> lock(engineMutex);
    uint64_t docId = nextDocId++;
    documents[docId] = std::make_shared<CadDocument>(docId, name);
    return docId;
}

bool NativeCadEngine::closeDocument(uint64_t docId) {
    std::lock_guard<std::mutex> lock(engineMutex);
    return documents.erase(docId) > 0;
}

std::shared_ptr<CadDocument> NativeCadEngine::getDocument(uint64_t docId) {
    std::lock_guard<std::mutex> lock(engineMutex);
    auto it = documents.find(docId);
    if (it != documents.end()) {
        return it->second;
    }
    return nullptr;
}

// ------------------------------------------------------------------------
// Primitive Mesh Generation Helpers (No scaling applied, keeps absolute coordinates)
// ------------------------------------------------------------------------

void generateBoxMesh(double l, double w, double h, double tx, double ty, double tz, MeshData& outMesh, float color[4]) {
    float lf = static_cast<float>(l);
    float wf = static_cast<float>(w);
    float hf = static_cast<float>(h);
    float dxf = static_cast<float>(tx);
    float dyf = static_cast<float>(ty);
    float dzf = static_cast<float>(tz);

    struct FaceDef {
        float normal[3];
        float pos[4][3];
    };

    FaceDef faces[] = {
        // Front face (+X)
        {{1.0f, 0.0f, 0.0f}, {{lf + dxf, dyf, dzf}, {lf + dxf, wf + dyf, dzf}, {lf + dxf, wf + dyf, hf + dzf}, {lf + dxf, dyf, hf + dzf}}},
        // Back face (-X)
        {{-1.0f, 0.0f, 0.0f}, {{dxf, dyf, dzf}, {dxf, dyf, hf + dzf}, {dxf, wf + dyf, hf + dzf}, {dxf, wf + dyf, dzf}}},
        // Right face (+Y)
        {{0.0f, 1.0f, 0.0f}, {{dxf, wf + dyf, dzf}, {dxf, wf + dyf, hf + dzf}, {lf + dxf, wf + dyf, hf + dzf}, {lf + dxf, wf + dyf, dzf}}},
        // Left face (-Y)
        {{0.0f, -1.0f, 0.0f}, {{dxf, dyf, dzf}, {lf + dxf, dyf, dzf}, {lf + dxf, dyf, hf + dzf}, {dxf, dyf, hf + dzf}}},
        // Top face (+Z)
        {{0.0f, 0.0f, 1.0f}, {{dxf, dyf, hf + dzf}, {lf + dxf, dyf, hf + dzf}, {lf + dxf, wf + dyf, hf + dzf}, {dxf, wf + dyf, hf + dzf}}},
        // Bottom face (-Z)
        {{0.0f, 0.0f, -1.0f}, {{dxf, dyf, dzf}, {dxf, wf + dyf, dzf}, {lf + dxf, wf + dyf, dzf}, {lf + dxf, dyf, dzf}}}
    };

    for (const auto& face : faces) {
        uint32_t startIdx = outMesh.vertices.size();
        for (int i = 0; i < 4; ++i) {
            Vertex v;
            v.x = face.pos[i][0];
            v.y = face.pos[i][1];
            v.z = face.pos[i][2];
            v.nx = face.normal[0];
            v.ny = face.normal[1];
            v.nz = face.normal[2];
            outMesh.vertices.push_back(v);
        }
        outMesh.indices.push_back(startIdx);
        outMesh.indices.push_back(startIdx + 1);
        outMesh.indices.push_back(startIdx + 2);
        outMesh.indices.push_back(startIdx);
        outMesh.indices.push_back(startIdx + 2);
        outMesh.indices.push_back(startIdx + 3);
    }
}

void generateCylinderMesh(double r, double h, double tx, double ty, double tz, MeshData& outMesh, float color[4]) {
    float rf = static_cast<float>(r);
    float hf = static_cast<float>(h);
    float dxf = static_cast<float>(tx);
    float dyf = static_cast<float>(ty);
    float dzf = static_cast<float>(tz);

    const int segments = 24;
    
    // Bottom Cap (at z = 0)
    uint32_t bottomCenterIdx = outMesh.vertices.size();
    Vertex bottomCenter;
    bottomCenter.x = dxf; bottomCenter.y = dyf; bottomCenter.z = dzf;
    bottomCenter.nx = 0.0f; bottomCenter.ny = 0.0f; bottomCenter.nz = -1.0f;
    outMesh.vertices.push_back(bottomCenter);

    uint32_t bottomRingStart = outMesh.vertices.size();
    for (int i = 0; i < segments; ++i) {
        float angle = static_cast<float>(2.0 * M_PI * i / segments);
        Vertex v;
        v.x = rf * cosf(angle) + dxf;
        v.y = rf * sinf(angle) + dyf;
        v.z = dzf;
        v.nx = 0.0f; v.ny = 0.0f; v.nz = -1.0f;
        outMesh.vertices.push_back(v);
    }
    for (int i = 0; i < segments; ++i) {
        outMesh.indices.push_back(bottomCenterIdx);
        outMesh.indices.push_back(bottomRingStart + (i + 1) % segments);
        outMesh.indices.push_back(bottomRingStart + i);
    }

    // Top Cap (at z = h)
    uint32_t topCenterIdx = outMesh.vertices.size();
    Vertex topCenter;
    topCenter.x = dxf; topCenter.y = dyf; topCenter.z = hf + dzf;
    topCenter.nx = 0.0f; topCenter.ny = 0.0f; topCenter.nz = 1.0f;
    outMesh.vertices.push_back(topCenter);

    uint32_t topRingStart = outMesh.vertices.size();
    for (int i = 0; i < segments; ++i) {
        float angle = static_cast<float>(2.0 * M_PI * i / segments);
        Vertex v;
        v.x = rf * cosf(angle) + dxf;
        v.y = rf * sinf(angle) + dyf;
        v.z = hf + dzf;
        v.nx = 0.0f; v.ny = 0.0f; v.nz = 1.0f;
        outMesh.vertices.push_back(v);
    }
    for (int i = 0; i < segments; ++i) {
        outMesh.indices.push_back(topCenterIdx);
        outMesh.indices.push_back(topRingStart + i);
        outMesh.indices.push_back(topRingStart + (i + 1) % segments);
    }

    // Side walls
    uint32_t wallStartIdx = outMesh.vertices.size();
    for (int i = 0; i <= segments; ++i) {
        float angle = static_cast<float>(2.0 * M_PI * (i % segments) / segments);
        float nx = cosf(angle);
        float ny = sinf(angle);
        
        Vertex vBottom;
        vBottom.x = rf * nx + dxf;
        vBottom.y = rf * ny + dyf;
        vBottom.z = dzf;
        vBottom.nx = nx; vBottom.ny = ny; vBottom.nz = 0.0f;
        outMesh.vertices.push_back(vBottom);

        Vertex vTop;
        vTop.x = rf * nx + dxf;
        vTop.y = rf * ny + dyf;
        vTop.z = hf + dzf;
        vTop.nx = nx; vTop.ny = ny; vTop.nz = 0.0f;
        outMesh.vertices.push_back(vTop);
    }

    for (int i = 0; i < segments; ++i) {
        uint32_t b0 = wallStartIdx + 2 * i;
        uint32_t t0 = b0 + 1;
        uint32_t b1 = wallStartIdx + 2 * (i + 1);
        uint32_t t1 = b1 + 1;

        outMesh.indices.push_back(b0);
        outMesh.indices.push_back(b1);
        outMesh.indices.push_back(t0);

        outMesh.indices.push_back(t0);
        outMesh.indices.push_back(b1);
        outMesh.indices.push_back(t1);
    }
}

void generateSphereMesh(double r, double tx, double ty, double tz, MeshData& outMesh, float color[4]) {
    float rf = static_cast<float>(r);
    float dxf = static_cast<float>(tx);
    float dyf = static_cast<float>(ty);
    float dzf = static_cast<float>(tz);

    const int rings = 12;
    const int sectors = 12;
    
    uint32_t startIdx = outMesh.vertices.size();

    for (int ring = 0; ring <= rings; ++ring) {
        float theta = static_cast<float>(ring * M_PI / rings);
        float sinTheta = sinf(theta);
        float cosTheta = cosf(theta);

        for (int sector = 0; sector <= sectors; ++sector) {
            float phi = static_cast<float>(sector * 2.0 * M_PI / sectors);
            float sinPhi = sinf(phi);
            float cosPhi = cosf(phi);

            Vertex v;
            v.nx = sinTheta * cosPhi;
            v.ny = sinTheta * sinPhi;
            v.nz = cosTheta;
            v.x = rf * v.nx + dxf;
            v.y = rf * v.ny + dyf;
            v.z = rf * v.nz + dzf;
            outMesh.vertices.push_back(v);
        }
    }

    for (int ring = 0; ring < rings; ++ring) {
        for (int sector = 0; sector < sectors; ++sector) {
            uint32_t r0 = startIdx + ring * (sectors + 1) + sector;
            uint32_t r1 = r0 + (sectors + 1);
            
            outMesh.indices.push_back(r0);
            outMesh.indices.push_back(r1);
            outMesh.indices.push_back(r0 + 1);

            outMesh.indices.push_back(r0 + 1);
            outMesh.indices.push_back(r1);
            outMesh.indices.push_back(r1 + 1);
        }
    }
}

void generateConeMesh(double r1, double r2, double h, double tx, double ty, double tz, MeshData& outMesh, float color[4]) {
    float r1f = static_cast<float>(r1);
    float r2f = static_cast<float>(r2);
    float hf = static_cast<float>(h);
    float dxf = static_cast<float>(tx);
    float dyf = static_cast<float>(ty);
    float dzf = static_cast<float>(tz);

    const int segments = 24;

    // Bottom Cap (at z = 0, radius = r1)
    uint32_t bottomCenterIdx = outMesh.vertices.size();
    Vertex bottomCenter;
    bottomCenter.x = dxf; bottomCenter.y = dyf; bottomCenter.z = dzf;
    bottomCenter.nx = 0.0f; bottomCenter.ny = 0.0f; bottomCenter.nz = -1.0f;
    outMesh.vertices.push_back(bottomCenter);

    uint32_t bottomRingStart = outMesh.vertices.size();
    for (int i = 0; i < segments; ++i) {
        float angle = static_cast<float>(2.0 * M_PI * i / segments);
        Vertex v;
        v.x = r1f * cosf(angle) + dxf;
        v.y = r1f * sinf(angle) + dyf;
        v.z = dzf;
        v.nx = 0.0f; v.ny = 0.0f; v.nz = -1.0f;
        outMesh.vertices.push_back(v);
    }
    for (int i = 0; i < segments; ++i) {
        outMesh.indices.push_back(bottomCenterIdx);
        outMesh.indices.push_back(bottomRingStart + (i + 1) % segments);
        outMesh.indices.push_back(bottomRingStart + i);
    }

    // Top Cap (at z = h, radius = r2)
    uint32_t topCenterIdx = outMesh.vertices.size();
    Vertex topCenter;
    topCenter.x = dxf; topCenter.y = dyf; topCenter.z = hf + dzf;
    topCenter.nx = 0.0f; topCenter.ny = 0.0f; topCenter.nz = 1.0f;
    outMesh.vertices.push_back(topCenter);

    uint32_t topRingStart = outMesh.vertices.size();
    for (int i = 0; i < segments; ++i) {
        float angle = static_cast<float>(2.0 * M_PI * i / segments);
        Vertex v;
        v.x = r2f * cosf(angle) + dxf;
        v.y = r2f * sinf(angle) + dyf;
        v.z = hf + dzf;
        v.nx = 0.0f; v.ny = 0.0f; v.nz = 1.0f;
        outMesh.vertices.push_back(v);
    }
    for (int i = 0; i < segments; ++i) {
        outMesh.indices.push_back(topCenterIdx);
        outMesh.indices.push_back(topRingStart + i);
        outMesh.indices.push_back(topRingStart + (i + 1) % segments);
    }

    // Side walls
    uint32_t wallStartIdx = outMesh.vertices.size();
    float slant = atan2f(r1f - r2f, hf);
    float cosSlant = cosf(slant);
    float sinSlant = sinf(slant);

    for (int i = 0; i <= segments; ++i) {
        float angle = static_cast<float>(2.0 * M_PI * (i % segments) / segments);
        float cosA = cosf(angle);
        float sinA = sinf(angle);
        
        float nx = cosA * cosSlant;
        float ny = sinA * cosSlant;
        float nz = sinSlant;
        
        Vertex vBottom;
        vBottom.x = r1f * cosA + dxf;
        vBottom.y = r1f * sinA + dyf;
        vBottom.z = dzf;
        vBottom.nx = nx; vBottom.ny = ny; vBottom.nz = nz;
        outMesh.vertices.push_back(vBottom);

        Vertex vTop;
        vTop.x = r2f * cosA + dxf;
        vTop.y = r2f * sinA + dyf;
        vTop.z = hf + dzf;
        vTop.nx = nx; vTop.ny = ny; vTop.nz = nz;
        outMesh.vertices.push_back(vTop);
    }

    for (int i = 0; i < segments; ++i) {
        uint32_t b0 = wallStartIdx + 2 * i;
        uint32_t t0 = b0 + 1;
        uint32_t b1 = wallStartIdx + 2 * (i + 1);
        uint32_t t1 = b1 + 1;

        outMesh.indices.push_back(b0);
        outMesh.indices.push_back(b1);
        outMesh.indices.push_back(t0);

        outMesh.indices.push_back(t0);
        outMesh.indices.push_back(b1);
        outMesh.indices.push_back(t1);
    }
}
