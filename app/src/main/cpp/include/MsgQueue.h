#ifndef FLASHVIDEO_MSGQUEUE_H
#define FLASHVIDEO_MSGQUEUE_H

#include <queue>
#include <pthread.h>

#include "Common.h"

class MsgQueue{
public:
    MsgQueue() {
        queue = new std::queue<Msg>();
        pthread_mutex_init(&mutex_lock, nullptr);
        pthread_cond_init(&cond_lock, nullptr);
    }
    ~MsgQueue() {
        if (queue != nullptr)
            delete queue;
        queue = nullptr;
        pthread_mutex_destroy(&mutex_lock);
        pthread_cond_destroy(&cond_lock);
    }
    void dequeue(Msg* packet);
    void enqueue(Msg& packet);
    void clear();
private:
    std::queue<Msg>* queue;
    pthread_mutex_t mutex_lock;
    pthread_cond_t cond_lock;
};

#endif //FLASHVIDEO_MSGQUEUE_H
