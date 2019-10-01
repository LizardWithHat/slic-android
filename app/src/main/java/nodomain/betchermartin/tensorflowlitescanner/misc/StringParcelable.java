package nodomain.betchermartin.tensorflowlitescanner.misc;

import android.os.Parcel;
import android.os.Parcelable;

public class StringParcelable implements Parcelable {
    private String value;

    public StringParcelable(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(value);
    }

    public static final Parcelable.Creator<StringParcelable> CREATOR = new Parcelable.Creator<StringParcelable>() {
        public StringParcelable createFromParcel(Parcel pc) {
            return new StringParcelable(pc);
        }
        public StringParcelable[] newArray(int size) {
            return new StringParcelable[size];
        }
    };

    public StringParcelable(Parcel pc){
        this.value = pc.readString();
    }

    public StringParcelable(){}

    @Override
    public String toString(){
        return value;
    }
}
