package org.tensorflow.lite.examples.classification.misc;

import android.os.Parcel;
import android.os.Parcelable;

public class DataDetail implements Parcelable {
    private String key;
    private String description;
    private String value;

    public DataDetail(String key, String description, String value){
        this.key = key;
        this.description = description;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int describeContents() {
        // TODO implementierung fehlt noch?
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeString(description);
        dest.writeString(value);
    }

    public static final Parcelable.Creator<DataDetail> CREATOR = new Parcelable.Creator<DataDetail>() {
        public DataDetail createFromParcel(Parcel pc) {
            return new DataDetail(pc);
        }
        public DataDetail[] newArray(int size) {
            return new DataDetail[size];
        }
    };

    public DataDetail(Parcel pc){
        this.key = pc.readString();
        this.description = pc.readString();
        this.value = pc.readString();
    }

    public DataDetail(){};
}
