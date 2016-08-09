package com.sampsonjoliver.firestarter.views.dialogs

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v4.app.DialogFragment

class TimePickerDialogFragment : DialogFragment() {
    companion object {
        const val ARG_HOUR = "ARG_HOUR"
        const val ARG_MINUTE = "ARG_MINUTE"

        fun newInstance(hour: Int, minute: Int): TimePickerDialogFragment {
            return TimePickerDialogFragment().apply {
                arguments = Bundle(2)
                arguments.putInt(ARG_HOUR, hour)
                arguments.putInt(ARG_MINUTE, minute)
            }
        }
    }

    var listener: TimePickerDialog.OnTimeSetListener? = null
    val hour by lazy { arguments.getInt(ARG_HOUR) }
    val minute by lazy { arguments.getInt(ARG_MINUTE) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val picker = TimePickerDialog(activity, listener, hour, minute, false)

        return picker
    }
}