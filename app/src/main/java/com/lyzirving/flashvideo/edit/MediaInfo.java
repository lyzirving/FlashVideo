package com.lyzirving.flashvideo.edit;

import android.os.Parcel;
import android.os.Parcelable;

import com.lyzirving.flashvideo.util.AssetsManager;

import androidx.annotation.NonNull;

/**
 * @author lyzirving
 */
public class MediaInfo implements Parcelable {
    public AssetsManager.AssetsType type;
    /**
     * media duration in second
     */
    public long duration;
    /**
     * absolute path of media data
     */
    public String path;
    public String name;

    public MediaInfo(AssetsManager.AssetsType type) {
        this.type = type;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString()
                + "\ntype = " + type
                + "\nname = " + name
                + "\nduration = " + duration
                + "\npath = " + path;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeLong(this.duration);
        dest.writeString(this.path);
        dest.writeString(this.name);
    }

    protected MediaInfo(Parcel in) {
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : AssetsManager.AssetsType.values()[tmpType];
        this.duration = in.readLong();
        this.path = in.readString();
        this.name = in.readString();
    }

    public static final Creator<MediaInfo> CREATOR = new Creator<MediaInfo>() {
        @Override
        public MediaInfo createFromParcel(Parcel source) {
            return new MediaInfo(source);
        }

        @Override
        public MediaInfo[] newArray(int size) {
            return new MediaInfo[size];
        }
    };
}
