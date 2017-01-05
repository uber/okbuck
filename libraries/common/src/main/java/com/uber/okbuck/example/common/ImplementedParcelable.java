package com.uber.okbuck.example.common;

import android.os.Parcel;
import android.os.Parcelable;

public class ImplementedParcelable implements Parcelable {

    public ImplementedParcelable() {
    }

    public static final Creator<ImplementedParcelable> CREATOR
            = new Creator<ImplementedParcelable>() {
        public ImplementedParcelable createFromParcel(Parcel in) {
            return new ImplementedParcelable(in);
        }

        public ImplementedParcelable[] newArray(int size) {
            return new ImplementedParcelable[size];
        }
    };

    private ImplementedParcelable(Parcel in) {
    }

    @Override public void writeToParcel(Parcel parcel, int i) {
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        return true;
    }

    @Override public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
