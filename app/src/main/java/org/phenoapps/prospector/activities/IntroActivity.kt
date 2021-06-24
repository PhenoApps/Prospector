package org.phenoapps.prospector.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import org.phenoapps.prospector.R

class IntroActivity : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        isWizardMode = true

        askForPermissions(arrayOf(android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE), slideNumber = 1, required = false)

        addSlide(AppIntroFragment.newInstance(
            title = getString(R.string.accept_permissions),
            description = getString(R.string.frag_instructions_step_zero),
        ))
        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(AppIntroFragment.newInstance(
            title = getString(R.string.welcome),
            description = getString(R.string.frag_instructions_step_one),
            imageDrawable = R.drawable.icon_device_ap

        ))
        addSlide(AppIntroFragment.newInstance(
            title = getString(R.string.choose_network),
            description = getString(R.string.frag_instructions_step_two),
            imageDrawable = R.drawable.choose_ap_wifi_network
        ))
        addSlide(AppIntroFragment.newInstance(
            title = getString(R.string.check_connection),
            description = getString(R.string.frag_instructions_step_three),
            imageDrawable = R.drawable.icon_ap
        ))
        addSlide(AppIntroFragment.newInstance(
            title = getString(R.string.prospector_workflow),
            description = getString(R.string.frag_instructions_step_four),
            imageDrawable = R.drawable.instructions_create_button
        ))
        addSlide(AppIntroFragment.newInstance(
            title = getString(R.string.iot_compatible),
            description = getString(R.string.frag_instructions_step_five),
            imageDrawable = R.drawable.iot_instructions
        ))
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        finish()
    }
}