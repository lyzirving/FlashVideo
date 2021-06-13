#ifndef FLASHVIDEO_PACKETQUEUE_H
#define FLASHVIDEO_PACKETQUEUE_H

#include <queue>
#include <pthread.h>

#ifdef __cplusplus
extern "C" {
#include "libavcodec/packet.h"
}
#endif

class PacketQueue {
public:
    PacketQueue() {
        packet_queue = new std::queue<AVPacket>();
        pthread_mutex_init(&mutex_lock, nullptr);
        pthread_cond_init(&cond_lock, nullptr);
    }
    ~PacketQueue() {
        if (packet_queue != nullptr)
            delete packet_queue;
        packet_queue = nullptr;
        pthread_mutex_destroy(&mutex_lock);
        pthread_cond_destroy(&cond_lock);
    }
    AVPacket* dequeue();
    void enqueue(AVPacket& packet);
    void clear();
    int size();
private:
    std::queue<AVPacket>* packet_queue;
    pthread_mutex_t mutex_lock;
    pthread_cond_t cond_lock;
};

#endif //FLASHVIDEO_PACKETQUEUE_H
