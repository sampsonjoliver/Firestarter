package com.sampsonjoliver.firestarter.views.channel.create

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.format.DateUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.sampsonjoliver.firestarter.FirebaseActivity
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Session
import com.sampsonjoliver.firestarter.service.FirebaseService
import com.sampsonjoliver.firestarter.service.References
import com.sampsonjoliver.firestarter.service.SessionManager
import com.sampsonjoliver.firestarter.utils.*
import com.sampsonjoliver.firestarter.utils.IntentUtils.dispatchPickPhotoIntent
import com.sampsonjoliver.firestarter.utils.IntentUtils.dispatchTakePictureIntent
import com.sampsonjoliver.firestarter.views.dialogs.DatePickerDialogFragment
import com.sampsonjoliver.firestarter.views.dialogs.FormDialog
import com.sampsonjoliver.firestarter.views.dialogs.TimePickerDialogFragment
import kotlinx.android.synthetic.main.activity_create_channel.*
import java.io.ByteArrayOutputStream
import java.util.*

class CreateChannelActivity : FirebaseActivity() {
    companion object {
        val BUNDLE_TAG_TAG = "BUNDLE_TAG_TAG"
    }

    val calendarHolder = Calendar.getInstance()

    var currentPhotoPath: String? = null
    var photoUploadPending: Boolean = false

    val session = Session().apply {
        startDate = Date().time
        username = SessionManager.getUsername()
        userId = SessionManager.getUid()
    }

    var startDateSet = false
    var endDateSet = false

    val progressDialog by lazy { ProgressDialog(this) }

    fun setStartTime(timeInMillis: Long) {
        val currentDuration = session.durationMs

        startDate.text = DateUtils.formatDateTime(this, timeInMillis, DateUtils.FORMAT_SHOW_DATE)
        startTime.text = DateUtils.formatDateTime(this, timeInMillis, DateUtils.FORMAT_SHOW_TIME)

        if (!endDateSet) {
            setEndTime(timeInMillis + currentDuration)
            endDateSet = false
        } else {
            session.durationMs = timeInMillis - session.endDate
        }
        session.startDate = timeInMillis

        startDateSet = true
    }

    fun setEndTime(timeInMillis: Long) {
        endDate.text = DateUtils.formatDateTime(this, timeInMillis, DateUtils.FORMAT_SHOW_DATE)
        endTime.text = DateUtils.formatDateTime(this, timeInMillis, DateUtils.FORMAT_SHOW_TIME)

        session.durationMs = timeInMillis - session.startDate

        endDateSet = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_channel)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setStartTime(Date().time)
        setEndTime(session.startDate + 1000 * 60 * 60 * 2)
        endDateSet = false

        startDate.setOnClickListener {
            calendarHolder.time = session.startDateAsDate
            DatePickerDialogFragment.newInstance(
                    calendarHolder.get(Calendar.YEAR),
                    calendarHolder.get(Calendar.MONTH),
                    calendarHolder.get(Calendar.DAY_OF_MONTH))
                    .apply { listener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                        calendarHolder.time = session.startDateAsDate
                        calendarHolder.set(Calendar.YEAR, year)
                        calendarHolder.set(Calendar.MONTH, month)
                        calendarHolder.set(Calendar.DAY_OF_MONTH, day)

                        setStartTime(calendarHolder.timeInMillis)
                    } }
                    .show(supportFragmentManager, DatePickerDialogFragment.TAG)
        }

        startTime.setOnClickListener {
            calendarHolder.time = session.startDateAsDate
            TimePickerDialogFragment.newInstance(
                    calendarHolder.get(Calendar.HOUR),
                    calendarHolder.get(Calendar.MINUTE))
                    .apply { listener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                        calendarHolder.time = session.startDateAsDate
                        calendarHolder.set(Calendar.HOUR_OF_DAY, hour)
                        calendarHolder.set(Calendar.MINUTE, minute)

                        setStartTime(calendarHolder.timeInMillis)
                    } }
                    .show(supportFragmentManager, TimePickerDialogFragment.TAG)
        }

        endDate.setOnClickListener {
            calendarHolder.time = session.endDateAsDate
            DatePickerDialogFragment.newInstance(
                    calendarHolder.get(Calendar.YEAR),
                    calendarHolder.get(Calendar.MONTH),
                    calendarHolder.get(Calendar.DAY_OF_MONTH),
                    session.startDateAsDate)
                    .apply { listener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                        calendarHolder.time = session.endDateAsDate
                        calendarHolder.set(Calendar.YEAR, year)
                        calendarHolder.set(Calendar.MONTH, month)
                        calendarHolder.set(Calendar.DAY_OF_MONTH, day)

                        setEndTime(calendarHolder.timeInMillis)
                    } }
                    .show(supportFragmentManager, DatePickerDialogFragment.TAG)
            endDateSet = true
        }

        endTime.setOnClickListener {
            calendarHolder.time = session.endDateAsDate
            TimePickerDialogFragment.newInstance(
                    calendarHolder.get(Calendar.HOUR),
                    calendarHolder.get(Calendar.MINUTE))
                    .apply { listener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                        calendarHolder.time = session.endDateAsDate
                        calendarHolder.set(Calendar.HOUR_OF_DAY, hour)
                        calendarHolder.set(Calendar.MINUTE, minute)

                        setEndTime(calendarHolder.timeInMillis)
                    } }
                    .show(supportFragmentManager, TimePickerDialogFragment.TAG)
        }

        location.setOnClickListener {
            // Construct an intent for the place picker
            try {
                val intent = PlacePicker.IntentBuilder().build(this)
                startActivityForResult(intent, IntentUtils.REQUEST_PLACE_PICKER)

            } catch (e: GooglePlayServicesRepairableException) {
                // ...
            } catch (e: GooglePlayServicesNotAvailableException) {
                // ...
            }
        }

        addTag.setOnClickListener {
            progressDialog.setMessage(getString(R.string.loading))
            progressDialog.show()
            FirebaseService.getReference(References.tags).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError?) {
                    progressDialog.dismiss()
                }

                override fun onDataChange(p0: DataSnapshot?) {
                    progressDialog.dismiss()
                    val tags = p0?.children?.map { it.key }?.toTypedArray()
                    val selectedTags = tags?.map { Pair(it, session.tags[it] ?: false) }.orEmpty().toMap(mutableMapOf())
                    val checkedIndexes = selectedTags.values.toBooleanArray()
                    AlertDialog.Builder(this@CreateChannelActivity)
                            .setMultiChoiceItems(tags, checkedIndexes, { dialogInterface, i, b ->
                                selectedTags.set(tags?.get(i).orEmpty(), b)
                            })
                            .setPositiveButton(getString(R.string.save), { dialogInterface, i ->
                                session.tags = selectedTags
                                tagList.text = session.tags.entries.filter { it.value == true }.map { it.key }.joinToString()
                            })
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show()
                }
            })
        }

        banner.setOnClickListener {
            AlertDialog.Builder(this@CreateChannelActivity)
                    .setItems(arrayOf(getString(R.string.take_photo), getString(R.string.upload_photo)), { dialogInterface, index ->
                if (index == 0) {
                    // Take photo
                    currentPhotoPath = dispatchTakePictureIntent(this)
                } else if (index == 1) {
                    // Upload photo
                    dispatchPickPhotoIntent(this)
                }
            }).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            IntentUtils.REQUEST_PLACE_PICKER -> {
                resultCode.whenEqual(Activity.RESULT_OK) {
                    // The user has selected a place. Extract the name and address.
                    val place = PlacePicker.getPlace(this, data)

                    val name = place.name
                    val address = place.address

                    session.address = address.toString()
                    session.setLocation(place.latLng)
                    location.text = address
                }
            }
            IntentUtils.REQUEST_IMAGE_PICKER -> {
                photoUploadPending = false
                currentPhotoPath = null
                resultCode.whenEqual(Activity.RESULT_OK) {
                    data?.data?.run {
                        photoUploadPending = true
                        currentPhotoPath = this.toString()
                    }
                }
                banner.setImageURI(currentPhotoPath ?: "")
            }
            IntentUtils.REQUEST_IMAGE_CAPTURE -> {
                photoUploadPending = false
                resultCode.whenEqual(Activity.RESULT_OK) {
                    Uri.parse(currentPhotoPath)?.run {
                        photoUploadPending = true
                    }
                }
                banner.setImageURI(currentPhotoPath)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_create_channel, menu)
        return super.onCreateOptionsMenu(menu)
    }

    fun saveChannel() {
        val progressDialog = ProgressDialog(this@CreateChannelActivity)
        progressDialog.setMessage(getString(R.string.creating_channel))
        progressDialog.show()

        if (photoUploadPending) {
            Uri.parse(currentPhotoPath)?.run {
                progressDialog.setMessage(getString(R.string.uploading_image, 0f))

                val thumb = BitmapUtils.decodeSampledBitmap(currentPhotoPath!!, 820, 312)
                val bos = ByteArrayOutputStream()
                thumb.compress(Bitmap.CompressFormat.PNG, 100, bos)
                val thumbData = bos.toByteArray()

                FirebaseStorage.getInstance().getReference("${References.Images}/banners/${this.lastPathSegment}")
                        .putBytes(thumbData, StorageMetadata.Builder()
                                .setContentType("image/jpg")
                                .setCustomMetadata("uid", SessionManager.getUid())
                                .build()
                        ).addOnFailureListener {
                            Log.d(TAG, "Upload Failed: " + it.message)
                            progressDialog.dismiss()
                        }.addOnProgressListener {
                            Log.d(TAG, "Upload Progress: ${it.bytesTransferred} / ${it.totalByteCount}")
                            progressDialog.setMessage(getString(R.string.uploading_image, (it.bytesTransferred.toFloat() / it.totalByteCount.toFloat()) * 100f))
                        }.addOnSuccessListener { photoIt ->
                            progressDialog.setMessage(getString(R.string.creating_channel))
                            session.bannerUrl = photoIt.downloadUrl.toString()
                            FirebaseService.createSession(session,
                                    { finish() },
                                    { Snackbar.make(toolbar, R.string.create_session_save_error, Snackbar.LENGTH_LONG).show() }
                            )
                        }
            }
        } else {
            FirebaseService.createSession(session,
                    { finish() },
                    { Snackbar.make(toolbar, R.string.create_session_save_error, Snackbar.LENGTH_LONG).show() }
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                if (validate()) {
                    saveChannel()
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun validate(): Boolean {
        session.topic = topic.text.toString()
        session.description = description.text.toString()

        return session.topic.isNullOrBlank().not() &&
                session.address.isNullOrBlank().not() &&
                session.username.isNullOrBlank().not() &&
                session.startDate > 0 &&
                session.durationMs > 0
    }
}