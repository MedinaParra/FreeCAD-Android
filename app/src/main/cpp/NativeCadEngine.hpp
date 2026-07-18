#pragma once
#include <string>
#include <unordered_map>
#include <memory>
#include <mutex>
#include "MeshData.hpp"

enum class ObjectType {
    BOX,
    CYLINDER,
    SPHERE,
    CONE
};

struct CadObject {
    uint64_t id;
    std::string name;
    ObjectType type;
    double dimensions[3]; // box: L, W, H. cylinder: R, H
    double translation[3] = {0.0, 0.0, 0.0};
    float color[4] = {0.2f, 0.6f, 0.8f, 1.0f};
    bool visible = true;
};

class CadDocument {
public:
    uint64_t id;
    std::string name;
    std::unordered_map<uint64_t, std::shared_ptr<CadObject>> objects;
    uint64_t nextObjectId = 1;

    CadDocument(uint64_t docId, const std::string& docName);
    uint64_t createBox(const std::string& name, double l, double w, double h);
    uint64_t createCylinder(const std::string& name, double r, double h);
    uint64_t createSphere(const std::string& name, double r);
    uint64_t createCone(const std::string& name, double r1, double r2, double h);
    bool translateObject(uint64_t objId, double x, double y, double z);
    bool updateObjectDimensions(uint64_t objId, double d1, double d2, double d3);
    bool deleteObject(uint64_t objId);
    bool setObjectVisibility(uint64_t objId, bool visible);
    bool recompute();
    MeshData getSceneMesh();
};

class NativeCadEngine {
private:
    std::unordered_map<uint64_t, std::shared_ptr<CadDocument>> documents;
    uint64_t nextDocId = 1;
    std::mutex engineMutex;

public:
    NativeCadEngine();
    ~NativeCadEngine();

    uint64_t createDocument(const std::string& name);
    bool closeDocument(uint64_t docId);
    std::shared_ptr<CadDocument> getDocument(uint64_t docId);
};
