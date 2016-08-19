package nl.sogeti.android.gpstracker.ng


import android.app.Application
import android.databinding.DataBindingUtil
import android.os.StrictMode
import nl.sogeti.android.gpstracker.ng.binders.CommonBindingComponent
import nl.sogeti.android.gpstracker.v2.BuildConfig
import timber.log.Timber

/**
 * Start app generic services
 */
class GpsTackerApplication : Application() {

    var debug = BuildConfig.DEBUG

    override fun onCreate() {
        super.onCreate()

        val bindingComponent = CommonBindingComponent()
        DataBindingUtil.setDefaultComponent(bindingComponent)

        if (debug) {
            Timber.plant(Timber.DebugTree())
            val builder = StrictMode.ThreadPolicy.Builder()
            if (builder != null) {
                StrictMode.setThreadPolicy(builder
                        .detectAll()
                        .penaltyLog().build())
            }
        }
    }
}