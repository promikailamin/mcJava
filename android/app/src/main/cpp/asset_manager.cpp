#include "asset_manager.h"
#include <android/log.h>
#include <android/asset_manager.h>
#include <cstring>
#include <vector>

#define LOG_TAG "mcJava-Asset"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace mcjava {

AssetManager::AssetManager() : manager(nullptr) {}

AssetManager::~AssetManager() {}

void AssetManager::setManager(AAssetManager* mgr) {
    manager = mgr;
}

bool AssetManager::exists(const char* path) const {
    if (!manager) return false;
    AAsset* asset = AAssetManager_open(manager, path, AASSET_MODE_BUFFER);
    if (asset) {
        AAsset_close(asset);
        return true;
    }
    return false;
}

std::vector<uint8_t> AssetManager::readFile(const char* path) const {
    std::vector<uint8_t> data;
    if (!manager) return data;

    AAsset* asset = AAssetManager_open(manager, path, AASSET_MODE_BUFFER);
    if (!asset) {
        LOGE("Failed to open asset: %s", path);
        return data;
    }

    off_t length = AAsset_getLength(asset);
    if (length <= 0) {
        AAsset_close(asset);
        return data;
    }

    data.resize(length);
    int read = AAsset_read(asset, data.data(), length);
    AAsset_close(asset);

    if (read != length) {
        LOGE("Failed to read full asset: %s", path);
        data.clear();
    }

    return data;
}

std::string AssetManager::readTextFile(const char* path) const {
    auto data = readFile(path);
    return std::string(data.begin(), data.end());
}

AAsset* AssetManager::open(const char* path, int mode) const {
    if (!manager) return nullptr;
    return AAssetManager_open(manager, path, mode);
}

void AssetManager::close(AAsset* asset) const {
    if (asset) {
        AAsset_close(asset);
    }
}

off_t AssetManager::getLength(AAsset* asset) const {
    return asset ? AAsset_getLength(asset) : 0;
}

int AssetManager::read(AAsset* asset, void* buf, size_t count) const {
    return asset ? AAsset_read(asset, buf, count) : -1;
}

off_t AssetManager::seek(AAsset* asset, off_t offset, int whence) const {
    return asset ? AAsset_seek(asset, offset, whence) : -1;
}

std::vector<std::string> AssetManager::listDir(const char* path) const {
    std::vector<std::string> files;
    if (!manager) return files;

    AAssetDir* dir = AAssetManager_openDir(manager, path);
    if (!dir) return files;

    const char* filename;
    while ((filename = AAssetDir_getNextFileName(dir)) != nullptr) {
        files.push_back(filename);
    }
    AAssetDir_close(dir);
    return files;
}

} // namespace mcjava
