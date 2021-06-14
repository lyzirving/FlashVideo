#include "PacketQueue.h"

AVPacket * PacketQueue::back() {
    AVPacket* res = nullptr;
    if (packet_queue != nullptr && !packet_queue->empty()) {
        res = &packet_queue->back();
    }
    return res;
}

void PacketQueue::clear() {
    AVPacket* tmp = nullptr;
    pthread_mutex_lock(&mutex_lock);
    while(!packet_queue->empty()) {
        tmp = &packet_queue->front();
        packet_queue->pop();
        //must call unref to release native memory
        if (tmp != nullptr) av_packet_unref(tmp);
    }
    pthread_mutex_unlock(&mutex_lock);
}

AVPacket* PacketQueue::dequeue() {
    AVPacket* res = nullptr;
    pthread_mutex_lock(&mutex_lock);
    if (!packet_queue->empty()) {
        res = &packet_queue->front();
        packet_queue->pop();
    }
    pthread_mutex_unlock(&mutex_lock);
    return res;
}

void PacketQueue::enqueue(AVPacket& packet) {
    pthread_mutex_lock(&mutex_lock);
    packet_queue->push(packet);
    pthread_cond_signal(&cond_lock);
    pthread_mutex_unlock(&mutex_lock);
}

int PacketQueue::size() {
    return packet_queue->size();
}

