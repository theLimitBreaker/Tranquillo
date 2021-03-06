package com.pervysage.thelimitbreaker.foco.services

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.RemoteException
import android.support.v4.app.JobIntentService
import com.crashlytics.android.Crashlytics
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.pervysage.thelimitbreaker.foco.R
import com.pervysage.thelimitbreaker.foco.database.Repository
import com.pervysage.thelimitbreaker.foco.database.entities.PlacePrefs
import com.pervysage.thelimitbreaker.foco.utils.initCrashlytics
import com.pervysage.thelimitbreaker.foco.utils.sendGeofenceNotification


class GeoActionsIntentService : JobIntentService() {

    companion object {
        private val JOB_ID = 2505

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, GeoActionsIntentService::class.java, JOB_ID, intent)
        }
    }


    override fun onHandleWork(intent: Intent) {

        initCrashlytics(baseContext)

        val geofenceEvent = GeofencingEvent.fromIntent(intent)
        if (geofenceEvent.hasError()) {

            val errorCode = geofenceEvent.errorCode
            when (errorCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> {
                    Crashlytics.log("Geoactions : Geofence Not available")
                }
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> {
                    Crashlytics.log("Geoactions : Too many geofences")

                }
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> {
                    Crashlytics.log("Geoactions: Too many pending intents ")
                }
            }
            return
        }
        if (geofenceEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val repo = Repository.getInstance(application)
            val triggeringGeofences = geofenceEvent.triggeringGeofences
            for (geofence in triggeringGeofences) {
                val geoID = geofence.requestId
                val (latStr, lngStr) = geoID.split(",")
                val lat = latStr.toDouble()
                val lng = lngStr.toDouble()
                try {
                    val placePrefs = repo.getPlacePref(lat, lng)
                    val notifyMsg = "Entered : ${placePrefs.name}"
                    toggleService(true,placePrefs)
                    sendGeofenceNotification(notifyMsg, Geofence.GEOFENCE_TRANSITION_ENTER, baseContext)
                }catch (e:Exception){
                    Crashlytics.logException(e)
                }

                break
            }
        }else if(geofenceEvent.geofenceTransition==Geofence.GEOFENCE_TRANSITION_EXIT){
            toggleService(false,null)
            sendGeofenceNotification("Exit", Geofence.GEOFENCE_TRANSITION_EXIT, baseContext)
        }
    }



    private fun toggleService(doStart: Boolean,placePrefs:PlacePrefs?){
        val sharedPref = baseContext.getSharedPreferences(getString(R.string.SHARED_PREF_KEY),Context.MODE_PRIVATE)
        val dmStatus = sharedPref.getBoolean(baseContext.getString(R.string.DM_STATUS),false)

        toggleRingerVolume(doStart,dmStatus)

        with(sharedPref.edit()){
            putBoolean(getString(R.string.GEO_STATUS),doStart)
            putString(getString(R.string.ACTIVE_NAME),placePrefs?.name?:"")
            putString(getString(R.string.GEO_ACTIVE_GROUP),placePrefs?.contactGroup?:"")
            putString(getString(R.string.ACTIVE_LAT),placePrefs?.latitude?.toString()?:"")
            putString(getString(R.string.ACTIVE_LNG),placePrefs?.longitude?.toString()?:"")
        }.commit()
    }

    private fun toggleRingerVolume(doStart: Boolean, dmStatus:Boolean){
        val am = baseContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            if (doStart){
                am.setStreamVolume(AudioManager.STREAM_RING,0,AudioManager.FLAG_PLAY_SOUND)
            }else if (!dmStatus){
                am.setStreamVolume(AudioManager.STREAM_RING,am.getStreamMaxVolume(AudioManager.STREAM_RING),AudioManager.FLAG_PLAY_SOUND)
            }
        }catch (re: RemoteException){
            Crashlytics.logException(re)
        }catch (e:Exception){
            Crashlytics.logException(e)
        }
    }


}
