//
// Created by lyzirving on 2021/7/24.
//
#include "TimeUtil.h"
#include <stdio.h>
#include <sys/time.h>

long long TimeUtil::getCurrentTimeMs() {
    timeval tv{};
    gettimeofday(&tv, nullptr);
    return ((long long)tv.tv_sec) * 1000 + tv.tv_usec / 1000;
}
