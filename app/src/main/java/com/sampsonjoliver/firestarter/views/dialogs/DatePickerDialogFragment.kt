package com.sampsonjoliver.firestarter.views.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.support.v4.app.DialogFragment
import java.util.*

class DatePickerDialogFragment : DialogFragment() {
    companion object {
        const val ARG_YEAR = "ARG_YEAR"
        const val ARG_MONTH = "ARG_MONTH"
        const val ARG_DAY = "ARG_DAY"
        const val ARG_MIN_DATE = "ARG_MIN_DATE"

        fun newInstance(year: Int, month: Int, day: Int): DatePickerDialogFragment {
            return DatePickerDialogFragment().apply {
                arguments = Bundle(3)
                arguments.putInt(ARG_YEAR, year)
                arguments.putInt(ARG_MONTH, month)
                arguments.putInt(ARG_DAY, day)
            }
        }

        fun newInstance(year: Int, month: Int, day: Int, minDate: Date): DatePickerDialogFragment {
            return DatePickerDialogFragment().apply {
                arguments = Bundle(4)
                arguments.putInt(ARG_YEAR, year)
                arguments.putInt(ARG_MONTH, month)
                arguments.putInt(ARG_DAY, day)
                arguments.putSerializable(ARG_MIN_DATE, minDate)
            }
        }
    }

    var listener: DatePickerDialog.OnDateSetListener? = null
    private val year by lazy { arguments.getInt(ARG_YEAR) }
    private val month by lazy { arguments.getInt(ARG_MONTH) }
    private val day by lazy { arguments.getInt(ARG_DAY) }
    private val minDate by lazy { arguments.getSerializable(ARG_MIN_DATE) as Date? }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val picker = DatePickerDialog(activity, listener, year, month, day)
        if (minDate != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            picker.datePicker.minDate = minDate?.time ?: 0L
        return picker
    }
}