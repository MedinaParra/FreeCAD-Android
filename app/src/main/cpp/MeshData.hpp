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
};
