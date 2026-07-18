#pragma once
#include <vector>
#include <cstdint>

struct Vertex {
    float x, y, z;
    float nx, ny, nz;
};

struct MeshData {
    std::vector<Vertex> vertices;
    std::vector<uint32_t> indices;
    float color[4] = {0.2f, 0.6f, 0.8f, 1.0f}; // default CAD teal/blue
    
    // Geometry bounds for dynamic near/far clipping plane and fitAll camera centering
    float minX = 0.0f;
    float minY = 0.0f;
    float minZ = 0.0f;
    float maxX = 0.0f;
    float maxY = 0.0f;
    float maxZ = 0.0f;
};
