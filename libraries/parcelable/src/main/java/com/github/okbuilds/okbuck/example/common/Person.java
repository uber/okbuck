package com.github.okbuilds.okbuck.example.common;

import android.os.Parcel;
import android.os.Parcelable;

public class Person implements Parcelable {
    private final String mName;
    private final int mAge;

    public Person(String name, int age) {
        mName = name;
        mAge = age;
    }

    public int getAge() {
        return mAge;
    }

    public String getName() {
        return mName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mName);
        dest.writeInt(this.mAge);
    }

    protected Person(Parcel in) {
        this.mName = in.readString();
        this.mAge = in.readInt();
    }

    public static final Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<Person>() {
        public Person createFromParcel(Parcel source) {
            return new Person(source);
        }

        public Person[] newArray(int size) {
            return new Person[size];
        }
    };
}
