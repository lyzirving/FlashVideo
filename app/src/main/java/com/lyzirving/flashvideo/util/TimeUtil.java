package com.lyzirving.flashvideo.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author lyzirving
 */
public class TimeUtil {
    private static final String TAG = "TimeUtil";
    public static final long ONE_SECOND = 1;
    public static final long ONE_MINUTE = 60 * ONE_SECOND;
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    public static final long ONE_DAY = 24 * ONE_HOUR;

    private static final int VALUE_TEN = 10;

    public static String transferDoubleTimeToHourMinuteSecond(double timeInSeconds) {
        if (timeInSeconds <= 0) {
            return "00:00:00";
        }
        double tmp = timeInSeconds;
        StringBuilder result = new StringBuilder();
        if (timeInSeconds > ONE_DAY) {
            tmp = timeInSeconds % ONE_DAY;
        }
        int completePart;
        if ((completePart = (int) (tmp / ONE_HOUR)) > 0) {
            //hour part
            if (completePart < VALUE_TEN) {
                result.append("0").append(completePart).append(":");
            } else {
                result.append(completePart).append(":");
            }
            tmp = tmp % ONE_HOUR;
        } else {
            result.append("00:");
        }
        if ((completePart = (int)(tmp / ONE_MINUTE)) > 0) {
            //minute part
            if (completePart < VALUE_TEN) {
                result.append("0").append(completePart).append(":");
            } else {
                result.append(completePart).append(":");
            }
            tmp = tmp % ONE_MINUTE;
        } else {
            result.append("00:");
        }
        //second part
        if (tmp < VALUE_TEN) {
            result.append("0").append((int) tmp);
        } else {
            result.append((int) tmp);
        }
        return result.toString();
    }

    public static String getCurrentTimeStr() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        return format.format(new Date());
    }

}
