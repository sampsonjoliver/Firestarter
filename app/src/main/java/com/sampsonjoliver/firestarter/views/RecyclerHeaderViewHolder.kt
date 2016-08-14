package com.sampsonjoliver.firestarter.views

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.utils.inflate
import kotlinx.android.synthetic.main.view_recycler_header.view.*

class RecyclerHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    companion object {
        fun inflate(container: ViewGroup): View {
            return container.inflate(R.layout.view_recycler_header, false)
        }
    }

    fun bind(title: String) {
        itemView.headerText.text = title
    }
}