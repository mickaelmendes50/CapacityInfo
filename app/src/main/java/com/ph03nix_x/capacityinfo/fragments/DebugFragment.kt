package com.ph03nix_x.capacityinfo.fragments

import android.content.*
import android.os.Bundle
import android.widget.Toast
import androidx.preference.*
import com.ph03nix_x.capacityinfo.MainApp.Companion.isGooglePlay
import com.ph03nix_x.capacityinfo.interfaces.DebugOptionsInterface
import com.ph03nix_x.capacityinfo.R
import com.ph03nix_x.capacityinfo.databases.HistoryDB
import com.ph03nix_x.capacityinfo.helpers.DateHelper
import com.ph03nix_x.capacityinfo.helpers.HistoryHelper
import com.ph03nix_x.capacityinfo.helpers.ServiceHelper
import com.ph03nix_x.capacityinfo.services.CapacityInfoService
import com.ph03nix_x.capacityinfo.services.OverlayService
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.DESIGN_CAPACITY
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.IS_FORCIBLY_SHOW_RATE_THE_APP
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

class DebugFragment : PreferenceFragmentCompat(), DebugOptionsInterface {

    private lateinit var pref: SharedPreferences

    private var forciblyShowRateTheApp: SwitchPreferenceCompat? = null
    private var addSetting: Preference? = null
    private var changeSetting: Preference? = null
    private var resetSetting: Preference? = null
    private var resetSettings: Preference? = null
    private var resetScreenTime: Preference? = null
    private var addCustomHistory: Preference? = null
    private var addHistory: Preference? = null
    private var addTenHistory: Preference? = null
    private var addFiftyHistory: Preference? = null
    private var historyCount: Preference? = null
    private var startCapacityInfoService: Preference? = null
    private var stopCapacityInfoService: Preference? = null
    private var restartCapacityInfoService: Preference? = null
    private var stopOverlayService: Preference? = null
    private var restartOverlayService: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        pref = PreferenceManager.getDefaultSharedPreferences(requireContext())

        addPreferencesFromResource(R.xml.debug_settings)

        forciblyShowRateTheApp = findPreference(IS_FORCIBLY_SHOW_RATE_THE_APP)

        addSetting = findPreference("add_setting")

        changeSetting = findPreference("change_setting")

        resetSetting = findPreference("reset_setting")

        resetSettings = findPreference("reset_settings")

        resetScreenTime = findPreference("reset_screen_time")

        addCustomHistory = findPreference("add_custom_history")

        addHistory = findPreference("add_history")

        addTenHistory = findPreference("add_ten_history")

        addFiftyHistory = findPreference("add_fifty_history")

        historyCount = findPreference("history_count")

        startCapacityInfoService = findPreference("start_capacity_info_service")

        stopCapacityInfoService = findPreference("stop_capacity_info_service")

        restartCapacityInfoService = findPreference("restart_capacity_info_service")

        stopOverlayService = findPreference("stop_overlay_service")

        restartOverlayService = findPreference("restart_overlay_service")

        forciblyShowRateTheApp?.isVisible = !isGooglePlay(requireContext())

        resetScreenTime?.isEnabled = (CapacityInfoService.instance?.screenTime ?: 0) > 0L

        addCustomHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())

        addHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())

        addTenHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())

        addFiftyHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())

        resetScreenTime?.setOnPreferenceClickListener {

            if((CapacityInfoService.instance?.screenTime ?: 0L) > 0L) {

                try {
                    CapacityInfoService.instance!!.screenTime = 0L
                    Toast.makeText(requireContext(), getString(R.string.success),
                        Toast.LENGTH_LONG).show()
                }
                catch (e: KotlinNullPointerException) {
                    Toast.makeText(requireContext(), getString(R.string.error),
                        Toast.LENGTH_LONG).show()
                }
            }
            else Toast.makeText(requireContext(), getString(R.string.error),
                Toast.LENGTH_LONG).show()

            true
        }

        addCustomHistory?.setOnPreferenceClickListener {

            onAddCustomHistory(pref, arrayListOf(it, addHistory, addTenHistory, addFiftyHistory),
                historyCount)

            true
        }

        addHistory?.setOnPreferenceClickListener {

            addCustomHistory?.isEnabled = false
            it.isEnabled = false
            addTenHistory?.isEnabled = false
            addFiftyHistory?.isEnabled = false

            val designCapacity = pref.getInt(DESIGN_CAPACITY, resources.getInteger(
                R.integer.min_design_capacity))
            val date =  DateHelper.getDate((1..31).random(), (1..12).random(),
                DateHelper.getCurrentYear())
            val residualCapacity = if(pref.getString(PreferencesKeys
                    .UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY, "μAh") == "μAh") ((
                    designCapacity * 0.01).toInt() * 1000..(designCapacity + (
                    (designCapacity / 1000) * 5)) * 1000).random()
            else ((designCapacity * 0.01).toInt() * 100..(designCapacity + (
                    (designCapacity / 1000) * 5)) * 100).random()

            HistoryHelper.addHistory(requireContext(), date, residualCapacity)

            val historyDB = HistoryDB(requireContext()).readDB()

            if(historyDB.isNotEmpty() && historyDB[historyDB.size - 1].date == date)
                Toast.makeText(requireContext(), "$date: $residualCapacity",
                    Toast.LENGTH_LONG).show()
            else Toast.makeText(requireContext(), "0.0.0: 0", Toast.LENGTH_LONG).show()

            addCustomHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
            it.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
            addTenHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
            addFiftyHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
            historyCount?.summary = "${HistoryHelper.getHistoryCount(requireContext())}"

            true
        }

        addTenHistory?.setOnPreferenceClickListener {

            addCustomHistory?.isEnabled = false
            addHistory?.isEnabled = false
            it.isEnabled = false
            addFiftyHistory?.isEnabled = false

            CoroutineScope(Dispatchers.Default).launch(Dispatchers.IO) {

                for(i in 1..10) {

                    if(HistoryHelper.isHistoryMax(requireContext())) break

                    val designCapacity = pref.getInt(DESIGN_CAPACITY, resources.getInteger(
                        R.integer.min_design_capacity))
                    val date =  DateHelper.getDate((1..31).random(), (1..12).random(),
                        DateHelper.getCurrentYear())
                    val residualCapacity = if(pref.getString(PreferencesKeys
                            .UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY, "μAh") == "μAh") ((
                            designCapacity * 0.01).toInt() * 1000..(designCapacity + (
                            (designCapacity / 1000) * 5)) * 1000).random()
                    else ((designCapacity * 0.01).toInt() * 100..(designCapacity + (
                            (designCapacity / 1000) * 5)) * 100).random()

                    HistoryHelper.addHistory(requireContext(), date, residualCapacity)

                    val historyDB = HistoryDB(requireContext()).readDB()

                    withContext(Dispatchers.Main) {
                        if(i == 10 && historyDB.isNotEmpty() && historyDB[historyDB.size - 1]
                                .date == date)
                            Toast.makeText(requireContext(), "$date: $residualCapacity",
                                Toast.LENGTH_LONG).show()
                        else if(historyDB.isEmpty() || historyDB[historyDB.size - 1].date != date) {
                            Toast.makeText(requireContext(), "$i: 0.0.0: 0",
                                Toast.LENGTH_LONG).show()
                            delay(3.5.seconds)
                        }
                    }
                }

                withContext(Dispatchers.Main) {

                    addCustomHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
                    addHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
                    it.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
                    addFiftyHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
                    historyCount?.summary = "${HistoryHelper.getHistoryCount(requireContext())}"
                }
            }

            true
        }

        addFiftyHistory?.setOnPreferenceClickListener {

            addCustomHistory?.isEnabled = false
            addHistory?.isEnabled = false
            addTenHistory?.isEnabled = false
            it.isEnabled = false

            CoroutineScope(Dispatchers.Default).launch(Dispatchers.IO) {

                for(i in 1..50) {

                    if(HistoryHelper.isHistoryMax(requireContext())) break

                    val designCapacity = pref.getInt(DESIGN_CAPACITY, resources.getInteger(
                        R.integer.min_design_capacity))
                    val date =  DateHelper.getDate((1..31).random(), (1..12).random(),
                        DateHelper.getCurrentYear())
                    val residualCapacity = if(pref.getString(PreferencesKeys
                            .UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY, "μAh") == "μAh") ((
                            designCapacity * 0.01).toInt() * 1000..(designCapacity + (
                            (designCapacity / 1000) * 5)) * 1000).random()
                    else ((designCapacity * 0.01).toInt() * 100..(designCapacity + (
                            (designCapacity / 1000) * 5)) * 100).random()

                    HistoryHelper.addHistory(requireContext(), date, residualCapacity)

                    val historyDB = HistoryDB(requireContext()).readDB()

                    withContext(Dispatchers.Main) {
                        if(i == 10 && historyDB.isNotEmpty() && historyDB[historyDB.size - 1]
                                .date == date)
                            Toast.makeText(requireContext(), "$date: $residualCapacity",
                                Toast.LENGTH_LONG).show()
                        else if(historyDB.isEmpty() || historyDB[historyDB.size - 1].date != date) {
                            Toast.makeText(requireContext(), "$i: 0.0.0: 0",
                                Toast.LENGTH_LONG).show()
                            delay(3.5.seconds)
                        }
                    }
                }

                withContext(Dispatchers.Main) {

                    addCustomHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
                    addHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
                    addTenHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
                    it.isEnabled = !HistoryHelper.isHistoryMax(requireContext())
                    historyCount?.summary = "${HistoryHelper.getHistoryCount(requireContext())}"
                }
            }

            true
        }

        startCapacityInfoService?.isEnabled = CapacityInfoService.instance == null && !ServiceHelper
            .isStartedCapacityInfoService()

        stopCapacityInfoService?.isEnabled = CapacityInfoService.instance != null

        restartCapacityInfoService?.isEnabled = CapacityInfoService.instance != null

        stopOverlayService?.isEnabled = OverlayService.instance != null

        restartOverlayService?.isEnabled = OverlayService.instance != null

        addSetting?.setOnPreferenceClickListener {

            addSettingDialog(pref)

            true
        }

        changeSetting?.setOnPreferenceClickListener {

            changeSettingDialog(pref)

            true
        }

        resetSetting?.setOnPreferenceClickListener {

            resetSettingDialog(pref)

            true
        }

        resetSettings?.setOnPreferenceClickListener {

            resetSettingsDialog(pref)

            true
        }

        startCapacityInfoService?.setOnPreferenceClickListener {

            it.isEnabled = false

            ServiceHelper.startService(requireContext(), CapacityInfoService::class.java)

            CoroutineScope(Dispatchers.Main).launch {

                delay(3.7.seconds)
                it.isEnabled = CapacityInfoService.instance == null && !ServiceHelper
                    .isStartedCapacityInfoService()

                stopCapacityInfoService?.isEnabled = CapacityInfoService.instance != null

                restartCapacityInfoService?.isEnabled = CapacityInfoService.instance != null
            }

            true
        }

        stopCapacityInfoService?.setOnPreferenceClickListener {

            it.isEnabled = false

            restartCapacityInfoService?.isEnabled = false

            ServiceHelper.stopService(requireContext(), CapacityInfoService::class.java)

            CoroutineScope(Dispatchers.Main).launch {

                delay(2.5.seconds)
                startCapacityInfoService?.isEnabled = CapacityInfoService.instance == null
                        && !ServiceHelper.isStartedCapacityInfoService()

                it.isEnabled = CapacityInfoService.instance != null

                restartCapacityInfoService?.isEnabled = CapacityInfoService.instance != null
            }

            true
        }

        restartCapacityInfoService?.setOnPreferenceClickListener {

            it.isEnabled = false

            stopCapacityInfoService?.isEnabled = false

            ServiceHelper.restartService(requireContext(), CapacityInfoService::class.java)

            CoroutineScope(Dispatchers.Main).launch {

                delay(6.2.seconds)
                startCapacityInfoService?.isEnabled = CapacityInfoService.instance == null
                        && !ServiceHelper.isStartedCapacityInfoService()

                stopCapacityInfoService?.isEnabled = CapacityInfoService.instance != null

                it.isEnabled = CapacityInfoService.instance != null
            }

            true
        }

        stopOverlayService?.setOnPreferenceClickListener {

            it.isEnabled = false

            restartOverlayService?.isEnabled = false

            ServiceHelper.stopService(requireContext(), OverlayService::class.java)

            CoroutineScope(Dispatchers.Main).launch {

                delay(1.5.seconds)
                it.isEnabled = OverlayService.instance != null

                restartOverlayService?.isEnabled = OverlayService.instance != null
            }

            true
        }

        restartOverlayService?.setOnPreferenceClickListener {

            it.isEnabled = false

            stopOverlayService?.isEnabled = false

            ServiceHelper.restartService(requireContext(), OverlayService::class.java)

            CoroutineScope(Dispatchers.Main).launch {

                delay(4.8.seconds)
                stopOverlayService?.isEnabled = OverlayService.instance != null

                it.isEnabled = OverlayService.instance != null
            }

            true
        }
    }

    override fun onResume() {

        super.onResume()

        resetScreenTime?.isEnabled = (CapacityInfoService.instance?.screenTime ?: 0) > 0L

        addCustomHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())

        addHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())

        addTenHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())

        addFiftyHistory?.isEnabled = !HistoryHelper.isHistoryMax(requireContext())

        historyCount?.summary = "${HistoryHelper.getHistoryCount(requireContext())}"

        startCapacityInfoService?.isEnabled = CapacityInfoService.instance == null && !ServiceHelper
            .isStartedCapacityInfoService()

        stopCapacityInfoService?.isEnabled = CapacityInfoService.instance != null

        restartCapacityInfoService?.isEnabled = CapacityInfoService.instance != null

        stopOverlayService?.isEnabled = OverlayService.instance != null

        restartOverlayService?.isEnabled = OverlayService.instance != null

        if(!pref.getBoolean(PreferencesKeys.IS_ENABLED_DEBUG_OPTIONS, resources.getBoolean(R.bool
                .is_enabled_debug_options)))
            requireActivity().onBackPressedDispatcher.onBackPressed()
    }
}