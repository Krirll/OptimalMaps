package ru.krirll.optimalmaps.presentation.fragments

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.data.LocationService
import ru.krirll.optimalmaps.presentation.enums.TitleAlertDialog

abstract class BaseFragmentLocationSupport: Fragment() {

    protected var alertDialog: AlertDialog? = null
    protected var alertDialogTitle: TitleAlertDialog? = null

    protected val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fine = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION]
            val coarse = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION]
            when {
                fine != null && fine == true ->
                    locationService?.tryUpdateLocation()
                coarse != null && coarse == false ->
                    createAlertDialogByTitle(TitleAlertDialog.GEO_POSITION_DISABLED)
                else ->
                    createAlertDialogByTitle(TitleAlertDialog.LOCATION_PERMISSION_DENIED)
            }
        }

    protected var locationService: LocationService? = null
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = (service as? LocationService.LocalBinder) ?: return
            locationService = binder.getService()
            locationService?.apply {
                setOnLocationChangedListener {
                    updateLocation(it)
                }
                setOnProviderDisabledListener {
                    setStateProviderDisabled()
                }
                setOnProviderEnabledListener {
                    setStateProviderEnabled()
                }
                setOnPermissionDeniedListener {
                    if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION))
                        createAlertDialogByTitle(TitleAlertDialog.LOCATION_PERMISSION_DENIED)
                    else
                        requestPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                }
                setBestProviderErrorHandler {
                    createAlertDialogByTitle(TitleAlertDialog.GEO_POSITION_DISABLED)
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    protected fun startService() {
        val intent = LocationService.createIntent(requireContext())
        requireContext().startService(intent)
        bindService(intent)
    }

    private fun bindService(intent: Intent) {
        requireContext().bindService(intent, serviceConnection, 0)
    }

    abstract fun updateLocation(location: Location)
    open fun setStateProviderDisabled() { }
    open fun setStateProviderEnabled() { }

    override fun onPause() {
        super.onPause()
        requireContext().unbindService(serviceConnection)
        requireContext().stopService(LocationService.createIntent(requireContext()))
        locationService = null
    }

    fun createAlertDialogByTitle(title: TitleAlertDialog) {
        when (title) {
            TitleAlertDialog.GEO_POSITION_DISABLED -> createAlertDialogGeoPositionDisabled()
            TitleAlertDialog.LOCATION_PERMISSION_DENIED -> createAlertDialogLocationPermissionDenied()
            else -> throw RuntimeException("Unknown title $title")
        }
    }

    private fun createAlertDialogGeoPositionDisabled() {
        createAlertDialog(
            title = TitleAlertDialog.GEO_POSITION_DISABLED,
            message = getString(R.string.location_disabled),
            positiveFunction = {
                //open location settings
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        )
    }

    private fun createAlertDialogLocationPermissionDenied() {
        createAlertDialog(
            title = TitleAlertDialog.LOCATION_PERMISSION_DENIED,
            message = getString(R.string.location_rationale),
            positiveFunction = {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts(
                            "package",
                            requireActivity().packageName,
                            null
                        ) //uri of this application settings
                    )
                )
            }
        )
    }

    private fun createAlertDialog(
        title: TitleAlertDialog,
        message: String,
        positiveFunction: (() -> Unit)? = null
    ) {
        alertDialog =
            AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setTitle(title.stringRes)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    alertDialogTitle = null
                    positiveFunction?.invoke()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    alertDialogTitle = null
                }
                .create()
        alertDialog?.show()
        alertDialogTitle = title
    }

    fun createSnackbar(message: String) {
        view?.let {
            Snackbar.make(
                it,
                message,
                Snackbar.LENGTH_LONG
            ).apply {
                animationMode = Snackbar.ANIMATION_MODE_SLIDE
            }.show()
        }
    }
}