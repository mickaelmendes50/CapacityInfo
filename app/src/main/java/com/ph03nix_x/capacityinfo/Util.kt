package com.ph03nix_x.capacityinfo

import android.content.Intent

class Util {

    companion object {

        var isPowerConnected = false
        var tempCurrentCapacity = 0.0
        var capacityAdded = 0.0
        var tempBatteryLevelWith = 0
        var percentAdded = 0
        var batteryIntent: Intent? = null
    }
}