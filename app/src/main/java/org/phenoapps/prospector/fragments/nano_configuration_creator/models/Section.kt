package org.phenoapps.prospector.fragments.nano_configuration_creator.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Section(var method: String,
              var methodIndex: Int,
              var start: Float,
              var end: Float,
              var width: Double,
              var widthIndex: Int,
              var exposure: Double,
              var exposureIndex: Int,
              var resolution: Int): Parcelable