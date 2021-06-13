#include "MsgQueue.h"

void MsgQueue::clear() {
    pthread_mutex_lock(&mutex_lock);
    while(!queue->empty())
        queue->pop();
    pthread_mutex_unlock(&mutex_lock);
}

void MsgQueue::dequeue(Msg* msg) {
    Msg* res = nullptr;
    pthread_mutex_lock(&mutex_lock);
    if (!queue->empty()) {
        res = &queue->front();
        queue->pop();
    }
    pthread_mutex_unlock(&mutex_lock);
    msg = res;
}

void MsgQueue::enqueue(Msg& msg) {
    pthread_mutex_lock(&mutex_lock);
    queue->push(msg);
    pthread_cond_signal(&cond_lock);
    pthread_mutex_unlock(&mutex_lock);
}

