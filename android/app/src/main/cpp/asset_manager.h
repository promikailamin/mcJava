#pragma once

#include <android/asset_manager.h>
#include <string>
#include <vector>

namespace mcjava {

class AssetManager {
public:
    AssetManager();
    ~AssetManager();

    void setManager(AAssetManager* mgr);

    bool exists(const char* path) const;
    std::vector<uint8_t> readFile(const char* path) const;
    std::string readTextFile(const char* path) const;

    AAsset* open(const char* path, int mode) const;
    void close(AAsset* asset) const;
    off_t getLength(AAsset* asset) const;
    int read(AAsset* asset, void* buf, size_t count) const;
    off_t seek(AAsset* asset, off_t offset, int whence) const;

    std::vector<std::string> listDir(const char* path) const;

private:
    AAssetManager* manager;
};

} // namespace mcjava
