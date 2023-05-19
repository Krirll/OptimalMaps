package ru.krirll.optimalmaps.data

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import androidx.core.location.LocationListenerCompat
import java.lang.NullPointerException

class LocationService : Service(), LocationListenerCompat {

    private val locationManager by lazy {
        applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private var locationListener: ((Location) -> Unit)? = null
    private var providerDisabledListener: (() -> Unit)? = null
    private var providerEnabledListener: (() -> Unit)? = null
    private var permissionDeniedListener: (() -> Unit)? = null
    private var getProviderErrorHandler: (() -> Unit)? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        tryUpdateLocation()
        return START_NOT_STICKY
    }

    fun tryUpdateLocation() {
        val criteria = Criteria().apply { accuracy = Criteria.ACCURACY_FINE }
        locationManager.removeUpdates(this)
        try {
            val provider = locationManager.getBestProvider(criteria, true)
            locationManager.requestLocationUpdates(
                provider ?: LocationManager.GPS_PROVIDER,
                500L,
                0.5F,
                this
            )
        } catch (_: SecurityException) {
            permissionDeniedListener?.invoke()
        } catch (_: NullPointerException) {
            getProviderErrorHandler?.invoke()
        }
    }

    fun setOnLocationChangedListener(listener: (Location) -> Unit) {
        locationListener = listener
    }

    fun setOnProviderEnabledListener(listener: () -> Unit) {
        providerEnabledListener = listener
    }

    fun setOnProviderDisabledListener(listener: () -> Unit) {
        providerDisabledListener = listener
    }

    fun setOnPermissionDeniedListener(listener: () -> Unit) {
        permissionDeniedListener = listener
    }

    fun setBestProviderErrorHandler(handler: () -> Unit) {
        getProviderErrorHandler = handler
    }

    override fun onLocationChanged(location: Location) {
        locationListener?.invoke(location)
    }

    override fun onProviderEnabled(provider: String) {
        providerEnabledListener?.invoke()
    }

    override fun onProviderDisabled(provider: String) {
        providerDisabledListener?.invoke()
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@LocationService
    }

    override fun onBind(intent: Intent?) = LocalBinder()

    override fun onUnbind(intent: Intent?): Boolean {
        locationListener = null
        providerDisabledListener = null
        providerEnabledListener = null
        permissionDeniedListener = null
        getProviderErrorHandler = null
        return super.onUnbind(intent)
    }

    companion object {

        fun createIntent(context: Context) = Intent(context, LocationService::class.java)
    }
}