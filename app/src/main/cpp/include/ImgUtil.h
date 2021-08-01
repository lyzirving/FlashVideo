#ifndef FLASHVIDEO_IMGUTIL_H
#define FLASHVIDEO_IMGUTIL_H

#include "Common.h"
#include <android/bitmap.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc_c.h>

#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_processing/render_face_detections.h>
#include <dlib/image_processing.h>
#include <dlib/opencv/cv_image.h>

class ImgUtil {
public:
    static bool bitmap2Mat(JNIEnv* env, jobject& bitmap, cv::Mat& mat);
    static bool mat2Bitmap(JNIEnv* env, cv::Mat& mat, jobject& bitmap);
    static void mat2Gray(cv::Mat &src, cv::Mat &dst);
private:
};

#endif //FLASHVIDEO_IMGUTIL_H
