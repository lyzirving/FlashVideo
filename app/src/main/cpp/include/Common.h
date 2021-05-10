#ifndef FLASHVIDEO_COMMON_H
#define FLASHVIDEO_COMMON_H

enum MediaMsg{
    MSG_PLAY = 1,
    MSG_PAUSE = 2,
    MSG_STOP = 3,
    MSG_SEEK = 4,
    MSG_QUIT = 5,
};
struct Msg {
    MediaMsg what;
};
enum MediaState{
    STATE_IDLE = 1,
    STATE_INITIALIZED = 2,
    STATE_PLAY = 3,
    STATE_PAUSE = 4,
    STATE_SEEK = 5,
    STATE_STOP = 6,
    STATE_ERROR = 7,
};

#endif //FLASHVIDEO_COMMON_H
