package com.github.cvzi.screenshottile.activities

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.assist.MyVoiceInteractionService
import com.github.cvzi.screenshottile.databinding.ActivityMainBinding
import com.github.cvzi.screenshottile.services.FloatingTileService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.*
import com.google.android.material.switchmaterial.SwitchMaterial


/**
 * Launcher activity. Explanations and selector for legacy/native method
 */
class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity.kt"

        /**
         * Start this activity from a service
         */
        fun startNewTask(ctx: Context, args: Bundle? = null) {
            ctx.startActivity(
                Intent(ctx, MainActivity::class.java).apply {
                    putExtra(TransparentContainerActivity.EXTRA_ARGS, args)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
        }

        var accessibilityConsent = false
    }

    private lateinit var binding: ActivityMainBinding
    private var hintAccessibilityServiceUnavailable: TextView? = null
    private var askedForStoragePermission = false
    private var restrictedSettingsAlertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        if (!accessibilityConsent) {
            accessibilityConsent = hasFdroid(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            && accessibilityConsent
            && App.getInstance().prefManager.screenshotCount == 0
            && isNewAppInstallation(this)
        ) {
            // On Android Pie - Tiramisu, enable native method on first start
            // Don't do it on Tiramisu+ because of the "restricted settings" dialog
            App.getInstance().prefManager.screenshotCount++
            App.getInstance().prefManager.useNative = true
        }

        val textDescTranslate = binding.textDescTranslate
        textDescTranslate.movementMethod = LinkMovementMethod()
        textDescTranslate.text = Html.fromHtml(
            getString(R.string.translate_this_app_text),
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV
        )

        val switchLegacy = binding.switchLegacy
        val switchNative = binding.switchNative
        val switchAssist = binding.switchAssist
        val switchFloatingButton = binding.switchFloatingButton

        toggleSwitchOnLabel(R.id.switchLegacy, R.id.textTitleLegacy)
        toggleSwitchOnLabel(R.id.switchNative, R.id.textTitleNative)
        toggleSwitchOnLabel(R.id.switchAssist, R.id.textTitleAssist)
        toggleSwitchOnLabel(R.id.switchFloatingButton, R.id.textTitleFloatingButton)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            binding.linearLayoutNative.let {
                val hint = TextView(this)
                it.addView(hint, 1)
                hint.text = getString(
                    R.string.emoji_forbidden,
                    getString(R.string.use_native_screenshot_unsupported)
                )
            }
            switchNative.isEnabled = false
            switchNative.isChecked = false
            switchLegacy.isEnabled = false
            switchLegacy.isChecked = true
            switchFloatingButton.isEnabled = false

            binding.floatingButtonCardView.let {
                (it.parent as ViewGroup).removeView(it)
            }

        }
        binding.textDescNative.text =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                getString(R.string.main_native_method_text).replace(
                    "{main_native_method_text_android_version}",
                    getString(R.string.main_native_method_text_android_pre_11)
                )

            } else {
                getString(R.string.main_native_method_text).replace(
                    "{main_native_method_text_android_version}",
                    getString(R.string.main_native_method_text_android_since_11)
                )
            }


        updateSwitches()

        binding.buttonSettings.setOnClickListener {
            SettingsActivity.start(this)
        }
        binding.buttonSettings2.setOnClickListener {
            SettingsActivity.start(this)
        }
        binding.buttonTutorial.setOnClickListener {
            TutorialActivity.start(this)
        }

        binding.buttonAccessibilitySettings.setOnClickListener {
            // Open Accessibility settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
            }
        }

        binding.buttonFloatingButtonSettings.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setOnClickListener {
                    FloatingButtonSettingsActivity.start(this@MainActivity)
                }
            } else {
                visibility = View.GONE
            }
        }

        binding.buttonPostActions.setOnClickListener {
            startActivity(Intent(this, PostSettingsActivity::class.java))
        }

        binding.buttonHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.textDescGeneral.run {
            makeActivityClickable(this)
        }

        binding.buttonDonateMagen.setOnClickListener {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.mdais.org/en/donation")).apply {
                if (resolveActivity(packageManager) != null) {
                    startActivity(this)
                }
            }
        }
        binding.buttonDonateGiveLively.setOnClickListener {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://secure.givelively.org/donate/the-giving-back-fund-inc/help-israeli-soldiers-return-home")
            ).apply {
                if (resolveActivity(packageManager) != null) {
                    startActivity(this)
                }
            }
        }
        binding.buttonDonateIsraelRescue.setOnClickListener {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://israelrescue.org/campaign/israel-under-attack/")
            ).apply {
                if (resolveActivity(packageManager) != null) {
                    startActivity(this)
                }
            }
        }

        binding.imageButtonDonateClose.setOnClickListener {
            binding.donationLinks.removeAllViews()
            App.getInstance().prefManager.showDonationLinks12600 = false
        }
        if (!App.getInstance().prefManager.showDonationLinks12600) {
            binding.donationLinks.removeAllViews()
            binding.scrollView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                setMargins(0, 0, 0, 0)
            }
        }

        switchLegacy.isChecked = !App.getInstance().prefManager.useNative
        switchNative.isChecked = App.getInstance().prefManager.useNative

        switchLegacy.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == App.getInstance().prefManager.useNative) {
                App.getInstance().prefManager.useNative = !isChecked
                updateFloatButton()
                switchNative.isChecked = App.getInstance().prefManager.useNative
            }
            if (!App.getInstance().prefManager.useNative) {
                hintAccessibilityServiceUnavailable?.let {
                    (it.parent as? ViewGroup)?.removeView(it)
                }
                if (!askedForStoragePermission && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && packageManager.checkPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        packageName
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    askedForStoragePermission = true
                    App.requestStoragePermission(this, false)
                }
            }
        }
        switchNative.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !accessibilityConsent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchNative.isChecked = false
                askToEnableAccessibility()
                return@setOnCheckedChangeListener
            }

            if (isChecked != App.getInstance().prefManager.useNative) {
                App.getInstance().prefManager.useNative = isChecked
                updateFloatButton()
                switchLegacy.isChecked = !App.getInstance().prefManager.useNative
                if (App.getInstance().prefManager.useNative) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ScreenshotAccessibilityService.instance == null) {
                        // Open Accessibility settings
                        ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
                    } else {
                        hintAccessibilityServiceUnavailable?.let {
                            (it.parent as? ViewGroup)?.removeView(it)
                        }
                    }
                }
            }
        }

        switchAssist.setOnCheckedChangeListener { _, _ ->
            MyVoiceInteractionService.openVoiceInteractionSettings(this, TAG)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchFloatingButton.setOnCheckedChangeListener { _, isChecked ->
                App.getInstance().prefManager.floatingButton = isChecked
                if (isChecked && ScreenshotAccessibilityService.instance == null) {
                    if (accessibilityConsent) {
                        // Open Accessibility settings
                        ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
                    } else {
                        askToEnableAccessibility()
                    }
                } else if (ScreenshotAccessibilityService.instance != null) {
                    ScreenshotAccessibilityService.instance!!.updateFloatingButton()
                }
            }
            if (ScreenshotAccessibilityService.instance != null && !App.getInstance().prefManager.floatingButton) {
                // Service is running and floating button is disabled ->  scroll to floating button
                binding.scrollView.postDelayed({
                    binding.scrollView.smoothScrollTo(
                        0,
                        binding.nativeCardView.top
                    )
                }, 1000)
            }
        }

        // Show warning if app is installed on external storage
        try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                ).flags
            } else {
                packageManager.getApplicationInfo(packageName, 0).flags
            }
            if (flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0
            ) {
                toastMessage(
                    "App is installed on external storage, this can cause problems  after a reboot with the floating button and the assistant function.",
                    ToastType.ACTIVITY
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, e.toString())
        }

        // On Android 13 Tiramisu we ask the user to add the tile to the quick settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (App.getInstance().prefManager.screenshotCount == 0 || isNewAppInstallation(this))
        ) {
            askToAddTiles()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun askToAddTiles() {
        if (BuildConfig.TESTING_MODE.value) {
            return
        }
        if (ScreenshotTileService.instance == null) {
            val statusBarManager = getSystemService(StatusBarManager::class.java)
            // Firstly, ask for normal screenshot tile
            statusBarManager.requestAddTileService(
                ComponentName(this, ScreenshotTileService::class.java),
                getString(R.string.tile_label),
                Icon.createWithResource(this, R.drawable.ic_stat_name),
                {
                    it.run()
                },
                {
                    // Secondly, ask for floating button tile
                    if (FloatingTileService.instance == null) {
                        statusBarManager.requestAddTileService(
                            ComponentName(this, FloatingTileService::class.java),
                            getString(R.string.tile_floating),
                            Icon.createWithResource(this, R.drawable.ic_tile_float),
                            {},
                            {})
                    }
                })
        }
    }

    private fun toggleSwitchOnLabel(switchId: Int, labelId: Int) {
        findViewById<View?>(labelId)?.let { label ->
            label.isClickable = true
            label.setOnClickListener {
                findViewById<SwitchMaterial?>(switchId)?.toggle()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun askToEnableAccessibility() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.googleplay_consent_title)
        builder.setMessage(
            "${getString(R.string.googleplay_consent_line_0)} " +
                    "${getString(R.string.googleplay_consent_line_1)} " +
                    "${getString(R.string.googleplay_consent_line_2)}\n" +
                    "${getString(R.string.googleplay_consent_line_3)} " +
                    "${getString(R.string.googleplay_consent_line_4)} " +
                    "${getString(R.string.googleplay_consent_line_5)} " +
                    "${getString(R.string.googleplay_consent_line_6)}\n" +
                    "\n" +
                    getString(R.string.googleplay_consent_line_7)
        )
        builder.setPositiveButton(getString(R.string.googleplay_consent_yes)) { _, _ ->
            accessibilityConsent = true
            if (binding.switchNative.isChecked && ScreenshotAccessibilityService.instance == null) {
                ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
            } else {
                binding.switchNative.isChecked = true
            }
        }
        builder.setNegativeButton(R.string.googleplay_consent_no) { _, _ ->
            val switchLegacy = binding.switchLegacy
            switchLegacy.isChecked = true
        }
        builder.show()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    /**
     * Does nothing until Android 13 Tiramisu
     * Show dialog about restricted settings:
     *   [ Displays screenshot of "App info" ]
     *  - Button to open accessibility settings
     *  - Cancel button
     *  - Button to open "App info" screen with "restricted settings"
     */
    private fun informAboutRestrictedSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        restrictedSettingsAlertDialog?.dismiss()

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.restricted_settings_title)
        builder.setMessage(R.string.restricted_settings_text)
        val imageView = ImageView(this)
        imageView.setImageResource(R.drawable.restricted_settings)
        builder.setView(imageView)
        builder.setNeutralButton(R.string.restricted_settings_open_settings) { _, _ ->
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
        builder.setPositiveButton(R.string.restricted_settings_open_accessibility) { _, _ ->
            ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        restrictedSettingsAlertDialog = builder.show()
    }

    private fun updateFloatButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ScreenshotAccessibilityService.instance?.updateFloatingButton()
        }
    }

    override fun onResume() {
        super.onResume()
        askedForStoragePermission = true // Don't ask again on resume
        updateSwitches()
    }

    override fun onPause() {
        super.onPause()
        hintAccessibilityServiceUnavailable?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }
        hintAccessibilityServiceUnavailable = null

        restrictedSettingsAlertDialog?.dismiss()
    }


    private fun updateSwitches() {
        val switchLegacy = binding.switchLegacy
        val switchNative = binding.switchNative
        val switchAssist = binding.switchAssist
        val switchFloatingButton = binding.switchFloatingButton

        switchLegacy.isChecked = !App.getInstance().prefManager.useNative
        switchNative.isChecked = App.getInstance().prefManager.useNative

        if (App.getInstance().prefManager.useNative && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (ScreenshotAccessibilityService.instance == null && hintAccessibilityServiceUnavailable == null) {
                binding.linearLayoutNative.let {
                    hintAccessibilityServiceUnavailable = TextView(this)
                    it.addView(hintAccessibilityServiceUnavailable, 1)
                    hintAccessibilityServiceUnavailable?.text = getString(
                        R.string.emoji_warning, getString(
                            R.string.use_native_screenshot_unavailable
                        )
                    )
                    hintAccessibilityServiceUnavailable?.setOnClickListener { _ ->
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
                        } else {
                            informAboutRestrictedSettings()
                        }
                    }
                }
            } else if (ScreenshotAccessibilityService.instance != null && hintAccessibilityServiceUnavailable != null) {
                binding.linearLayoutNative.removeView(
                    hintAccessibilityServiceUnavailable
                )
                hintAccessibilityServiceUnavailable = null
            }
            // User might have returned from accessibility settings without activating the service
            // Show dialog about restricted settings
            if (ScreenshotAccessibilityService.instance == null) {
                informAboutRestrictedSettings()
            }
        }

        switchAssist.isChecked = MyVoiceInteractionService.instance != null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchFloatingButton.isChecked =
                ScreenshotAccessibilityService.instance != null && App.getInstance().prefManager.floatingButton
        }
    }

    private fun makeActivityClickable(textView: TextView) {
        textView.apply {
            text = makeActivityClickableFromText(text.toString(), this@MainActivity).builder
            movementMethod = LinkMovementMethod()
            highlightColor = Color.BLUE
        }
    }

}