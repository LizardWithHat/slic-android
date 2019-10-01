package nodomain.betchermartin.tensorflowlitescanner.misc;

import android.os.Parcel;
import android.os.Parcelable;

public class IntegerParcelable implements Parcelable {
    private int value;

    public IntegerParcelable(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(value);
    }

    public static final Creator<IntegerParcelable> CREATOR = new Creator<IntegerParcelable>() {
        public IntegerParcelable createFromParcel(Parcel pc) {
            return new IntegerParcelable(pc);
        }
        public IntegerParcelable[] newArray(int size) {
            return new IntegerParcelable[size];
        }
    };

    public IntegerParcelable(Parcel pc){
        this.value = pc.readInt();
    }

    public IntegerParcelable(){}

    @Override
    public String toString(){
        return Integer.toString(value);
    }
}
