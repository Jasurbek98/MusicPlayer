package uz.jsoft.mediaplayer.receivers

/**
 * Created by Jasurbek Kurganbaev on 1/7/2022 4:56 PM
 **/
/*
class PlayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        (intent?.extras?.get(KEY_SERVICE_COMMAND) as? ServiceCommand)?.let { serviceCommand ->
            Intent(context, SimplePlayerService::class.java).also { serviceIntent ->
                serviceIntent.putExtra(KEY_SERVICE_COMMAND, serviceCommand)
                context?.startService(serviceIntent)
            }
        }
    }

}*/
