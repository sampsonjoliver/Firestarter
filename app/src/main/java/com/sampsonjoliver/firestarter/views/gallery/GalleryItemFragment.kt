package com.sampsonjoliver.firestarter.views.gallery

import android.app.DialogFragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.View
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Message
import kotlinx.android.synthetic.main.fragment_gallery.view.*
import kotlinx.android.synthetic.main.item_gallery.view.*
import java.util.*

class GalleryItemFragment : DialogFragment() {
    companion object {
        fun newInstance(photoUrls: ArrayList<Message>, position: Int): GalleryItemFragment {
            return GalleryItemFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("images", photoUrls)
                    putInt("position", position)
                }
            }
        }
    }

    val photos by lazy { arguments.getParcelableArrayList<Message>("images") }
    val selectedPosition by lazy { arguments.getInt("position") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_gallery, container, false)

        val adapter = GalleryViewPager()
        view?.viewpager?.adapter = adapter
        view?.viewpager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {

            }

        })

        setCurrentItem(view, selectedPosition)

        return view
    }

    fun setCurrentItem(view: View?, position: Int) {
        view?.viewpager?.setCurrentItem(position, false)
    }

    inner class GalleryViewPager : PagerAdapter() {
        override fun instantiateItem(container: ViewGroup?, position: Int): Any {
            val layoutInflater = LayoutInflater.from(container?.context)
            val view = layoutInflater.inflate(R.layout.item_gallery, container, false)
            view?.drawee?.setImageURI(photos[position].contentUri)

            container?.addView(view)

            return view
        }

        override fun getCount(): Int {
            return photos.size
        }

        override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
            return view == `object`
        }

        override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
            container?.removeView(`object` as View)
        }
    }
}