package org.phenoapps.prospector.datastructures

import android.util.SparseLongArray

private typealias Indices = HashMap<Double, SparseLongArray>

class Posting {

    private val posting = emptyMap<Double, Indices>()
            .withDefault { Indices() }.toMutableMap()

    var keys = this.posting.keys

    var values = this.posting.values

    val size: Int
        get() {
            return this.posting.keys.size
        }

    operator fun get(key: Double): Indices? {

        return posting[key]

    }

    operator fun set(key: Double, wavelength: Double, id: Long) {

        posting.getOrPut(key) {

            Indices().also {
                it[wavelength] = SparseLongArray().also { array ->
                    array.append(array.size(), id)
                }
            }

        }.let { indices ->

            indices.getOrPut(wavelength) {
                SparseLongArray().also {
                    it.append(it.size(), id)
                }
            }.let {
                it.append(it.size(), id)
            }
        }
    }
}