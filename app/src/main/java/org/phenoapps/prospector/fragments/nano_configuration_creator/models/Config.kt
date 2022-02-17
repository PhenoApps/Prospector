package org.phenoapps.prospector.fragments.nano_configuration_creator.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Config(
    val name: String,
    val repeats: Int,
    var currentIndex: Int = 0,
    var sections: Array<Section>? = null): Parcelable
