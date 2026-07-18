#include "NativeCadEngine.hpp"
#include <cmath>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// Forward declarations of helper mesh generators
void generateBoxMesh(double l, double w, double h, double tx, double ty, double tz, MeshData& outMesh, float color[4]);
void generateCylinderMesh(double r, double h, double tx, double ty, double tz, MeshData& outMesh, float color[4]);
void generateSphereMesh(double r, double tx, double ty, double tz, MeshData& outMesh, float color[4]);
void generateConeMesh(double r1, double r2, double h, double tx, double ty, double tz, MeshData& outMesh, float color[4]);

CadDocument::CadDocument(uint64_t docId, const std::string& docName)
    : id(docId), name(docName) {}

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
    // Phase 0: Recompute acts as a no-op / local flag sync
    return true;
}

MeshData CadDocument::getSceneMesh() {
    // Find bounding box to scale and center
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
        minX = -10.0; maxX = 10.0;
        minY = -10.0; maxY = 10.0;
        minZ = -10.0; maxZ = 10.0;
    }

    double length = maxX - minX;
    double width = maxY - minY;
    double height = maxZ - minZ;
    double maxVal = std::max(length, std::max(width, height));
    double visualScale = (maxVal > 200.0) ? (100.0 / maxVal) : 1.0;

    double centerX = (minX + maxX) / 2.0;
    double centerY = (minY + maxY) / 2.0;
    double centerZ = (minZ + maxZ) / 2.0;

    MeshData combinedMesh;
    for (const auto& pair : objects) {
        const auto& obj = pair.second;
        if (!obj->visible) continue;

        double tx = (obj->translation[0] - centerX) * visualScale;
        double ty = (obj->translation[1] - centerY) * visualScale;
        double tz = (obj->translation[2] - centerZ) * visualScale;

        if (obj->type == ObjectType::BOX) {
            generateBoxMesh(obj->dimensions[0] * visualScale, obj->dimensions[1] * visualScale, obj->dimensions[2] * visualScale,
                            tx, ty, tz,
                            combinedMesh, obj->color);
        } else if (obj->type == ObjectType::CYLINDER) {
            generateCylinderMesh(obj->dimensions[0] * visualScale, obj->dimensions[1] * visualScale,
                                 tx, ty, tz,
                                 combinedMesh, obj->color);
        } else if (obj->type == ObjectType::SPHERE) {
            generateSphereMesh(obj->dimensions[0] * visualScale,
                               tx, ty, tz,
                               combinedMesh, obj->color);
        } else if (obj->type == ObjectType::CONE) {
            generateConeMesh(obj->dimensions[0] * visualScale, obj->dimensions[1] * visualScale, obj->dimensions[2] * visualScale,
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
// Mesh Generation Helpers
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
        {{1.0f, 0.0f, 0.0f}, {{lf, 0.0f, 0.0f}, {lf, wf, 0.0f}, {lf, wf, hf}, {lf, 0.0f, hf}}},
        // Back face (-X)
        {{-1.0f, 0.0f, 0.0f}, {{0.0f, 0.0f, 0.0f}, {0.0f, 0.0f, hf}, {0.0f, wf, hf}, {0.0f, wf, 0.0f}}},
        // Right face (+Y)
        {{0.0f, 1.0f, 0.0f}, {{0.0f, wf, 0.0f}, {0.0f, wf, hf}, {lf, wf, hf}, {lf, wf, 0.0f}}},
        // Left face (-Y)
        {{0.0f, -1.0f, 0.0f}, {{0.0f, 0.0f, 0.0f}, {lf, 0.0f, 0.0f}, {lf, 0.0f, hf}, {0.0f, 0.0f, hf}}},
        // Top face (+Z)
        {{0.0f, 0.0f, 1.0f}, {{0.0f, 0.0f, hf}, {lf, 0.0f, hf}, {lf, wf, hf}, {0.0f, wf, hf}}},
        // Bottom face (-Z)
        {{0.0f, 0.0f, -1.0f}, {{0.0f, 0.0f, 0.0f}, {0.0f, wf, 0.0f}, {lf, wf, 0.0f}, {lf, 0.0f, 0.0f}}}
    };

    for (const auto& face : faces) {
        uint32_t startIdx = outMesh.vertices.size();
        for (int i = 0; i < 4; ++i) {
            Vertex v;
            v.x = face.pos[i][0] + dxf;
            v.y = face.pos[i][1] + dyf;
            v.z = face.pos[i][2] + dzf;
            v.nx = face.normal[0];
            v.ny = face.normal[1];
            v.nz = face.normal[2];
            outMesh.vertices.push_back(v);
        }
        // CCW Indices
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
