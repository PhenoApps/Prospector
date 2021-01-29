package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.phenoapps.prospector.R
import org.phenoapps.prospector.databinding.FragmentConnectInstructionsBinding

class ConnectInstructionsFragment : Fragment() {

    private var mBinding: FragmentConnectInstructionsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_connect_instructions, null, false)

        return mBinding?.root

    }

}