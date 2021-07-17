#include "ImgAlgorithm.h"
#include "LogUtil.h"

#define TAG "ImgAlgorithm"

bool ImgAlgorithm::histEqual(cv::Mat &src, cv::Mat &dst) {
    if (src.empty() || !src.data) {
        LogUtil::logI(TAG, {"histEqual: src is invalid"});
        return false;
    }
    std::vector<cv::Mat> grbMat;
    cv::split(src, grbMat);

    cv::equalizeHist(grbMat[0], grbMat[0]);
    cv::equalizeHist(grbMat[1], grbMat[1]);
    cv::equalizeHist(grbMat[2], grbMat[2]);

    cv::merge(grbMat, dst);
    return true;
}

