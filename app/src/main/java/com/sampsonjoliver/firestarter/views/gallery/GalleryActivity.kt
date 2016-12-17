package com.sampsonjoliver.firestarter.views.gallery

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sampsonjoliver.firestarter.FirebaseActivity
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Message
import com.sampsonjoliver.firestarter.views.GridSpacingItemDecoration
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.android.synthetic.main.item_photo_thumbnail.view.*
import java.util.*

class GalleryActivity : FirebaseActivity() {
    companion object {
        val EXTRA_TITLE = "EXTRA_TITLE"
        val EXTRA_MESSAGES = "EXTRA_MESSAGES"
    }
    val messages by lazy { intent.getParcelableArrayListExtra<Message>(EXTRA_MESSAGES) }
    val title by lazy { intent.getStringExtra(EXTRA_TITLE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_gallery)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = title

        val spacingPx = 5
        val manager = GridLayoutManager(this, 4)
        recycler.addItemDecoration(GridSpacingItemDecoration(4, spacingPx, true))

        recycler?.layoutManager = manager
        recycler?.adapter = PhotoAdapter(messages)
    }

    inner class PhotoAdapter(val messages: List<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            holder?.itemView?.drawee?.setImageURI(messages[position].contentThumbUri)
            holder?.itemView?.setOnClickListener {
                GalleryItemFragment.newInstance(ArrayList(messages), position).show(fragmentManager, "")
            }
        }

        override fun getItemCount(): Int = messages.size

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent?.context)?.inflate(R.layout.item_photo_thumbnail, parent, false)

            return PhotoThumbnailHolder(view)
        }
    }

    inner class PhotoThumbnailHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {

    }
}