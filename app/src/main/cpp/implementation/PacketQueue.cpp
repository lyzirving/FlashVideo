#include "PacketQueue.h"

void PacketQueue::clear() {
    pthread_mutex_lock(&mutex_lock);
    while(!packet_queue->empty())
        packet_queue->pop();
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

