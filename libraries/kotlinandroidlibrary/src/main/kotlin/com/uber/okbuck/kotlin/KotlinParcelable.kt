package com.uber.okbuck.kotlin

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator") // https://youtrack.jetbrains.com/issue/KT-19300
@Parcelize
class KotlinParcelable(val name: String) : Parcelable
