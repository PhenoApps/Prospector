package org.phenoapps.prospector.views

import ALPHA_ASC
import ALPHA_DESC
import DATE_ASC
import DATE_DESC
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import org.phenoapps.prospector.R
import org.phenoapps.prospector.databinding.SortbarBinding

class SortBarView(context: Context, attr: AttributeSet): ConstraintLayout(context, attr) {

    private var sortTypeToggleState = true
    private var sortOrderToggleState = false

    private var binding: SortbarBinding? = null

    private var isSorting = false

    var onClickSortType: (Boolean) -> Unit = {}
    var onClickSortOrder: (Boolean) -> Unit = {}

    //layout id: layout/sortbar.xml

    fun initializeSortState(state: Int) {

        binding?.let { ui ->

            ui.sortbarType.setImageDrawable(AppCompatResources.getDrawable(context, when (state) {
                ALPHA_ASC, ALPHA_DESC -> {
                    sortTypeToggleState = true
                    R.drawable.sort_alphabetical_variant
                }
                else -> {
                    sortTypeToggleState = false
                    R.drawable.ic_clock_outline
                }
            }))

            if (state in arrayOf(DATE_DESC, ALPHA_DESC)) {
                sortOrderToggleState = true
                ui.sortbarOrder.scaleY = 1f
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        binding = SortbarBinding.inflate(LayoutInflater.from(context))

        addView(binding?.root)

        with (binding?.sortbarType) {
           this?.setOnClickListener {
               if (!isSorting) {
                   isSorting = true
                   sortTypeToggleState = !sortTypeToggleState
                   this.setImageDrawable(
                       AppCompatResources.getDrawable(context,
                           if (sortTypeToggleState) R.drawable.sort_alphabetical_variant
                           else R.drawable.ic_clock_outline)
                   )
                   onClickSortType(sortTypeToggleState)
                   isSorting = false
               }
            }
        }

        with (binding?.sortbarOrder) {
            this?.setOnClickListener {
                if (!isSorting) {
                    isSorting = true
                    sortOrderToggleState = !sortOrderToggleState
                    this.scaleY.let { y ->
                        this.scaleY = y * -1f
                    }
                    onClickSortOrder(sortOrderToggleState)
                    isSorting = false
                }
            }
        }
    }
}