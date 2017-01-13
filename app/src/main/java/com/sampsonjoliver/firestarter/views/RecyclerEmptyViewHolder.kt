package com.sampsonjoliver.firestarter.views

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.utils.inflate
import kotlinx.android.synthetic.main.row_recycler_empty.view.*

class RecyclerEmptyViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
    companion object {
        fun inflate(container: ViewGroup?): View? {
            return container?.inflate(R.layout.row_recycler_empty, false)
        }
    }

    fun bind(emptyString: String) {
        itemView.emptyText.text = emptyString
    }
}