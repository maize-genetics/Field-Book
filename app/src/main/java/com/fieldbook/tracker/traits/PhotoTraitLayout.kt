package com.fieldbook.tracker.traits

import android.app.Activity
import android.content.Context
import com.fieldbook.tracker.utilities.DocumentTreeUtil.Companion.getFieldMediaDirectory
import com.fieldbook.tracker.utilities.DocumentTreeUtil.Companion.getPlotMedia
import org.phenoapps.utils.BaseDocumentTreeUtil.Companion.getStem
import com.fieldbook.tracker.traits.BaseTraitLayout
import android.graphics.Bitmap
import android.widget.Gallery
import com.fieldbook.tracker.adapters.GalleryImageAdapter
import android.widget.ImageButton
import com.fieldbook.tracker.R
import com.fieldbook.tracker.traits.PhotoTraitLayout.PhotoTraitOnClickListener
import android.widget.EditText
import androidx.documentfile.provider.DocumentFile
import com.fieldbook.tracker.utilities.DocumentTreeUtil
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView
import android.graphics.BitmapFactory
import org.phenoapps.utils.BaseDocumentTreeUtil
import android.provider.MediaStore
import android.content.Intent
import com.fieldbook.tracker.objects.TraitObject
import com.fieldbook.tracker.preferences.GeneralKeys
import com.fieldbook.tracker.activities.ConfigActivity
import android.content.DialogInterface
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.fieldbook.tracker.utilities.DialogUtils
import com.fieldbook.tracker.traits.PhotoTraitLayout
import com.fieldbook.tracker.utilities.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PhotoTraitLayout : BaseTraitLayout {

    private var scope = CoroutineScope(Dispatchers.IO)

    private var drawables: ArrayList<Bitmap>? = null
    private var uris = arrayListOf<Uri>()

    private var photo: Gallery? = null
    private var photoAdapter: GalleryImageAdapter? = null
    private var mCurrentPhotoPath: String? = null
    private var activity: Activity? = null

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    override fun setNaTraitsText() {}
    override fun type(): String {
        return "photo"
    }

    override fun init() {

    }

    override fun init(act: Activity?) {
        super.init(act)
        val capture = findViewById<ImageButton>(R.id.capture)
        capture.setOnClickListener(PhotoTraitOnClickListener())
        photo = findViewById(R.id.photo)
        activity = act
    }

    override fun loadLayout() {
        etCurVal.removeTextChangedListener(cvText)
        etCurVal.visibility = GONE
        etCurVal.isEnabled = false
        loadLayoutWork()
    }

    fun loadLayoutWork() {

        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO)
        scope.launch {

            // Always set to null as default, then fill in with trait value
            drawables = ArrayList()
            uris = arrayListOf()

            val photosDir = getFieldMediaDirectory(context, "thumbnails")

            //back down to the photos directory if thumbnails don't exist
            if (photosDir == null || photosDir.listFiles().isEmpty()) {
                generateThumbnails()
            }
            if (photosDir != null) {
                val plot = cRange.plot_id
                val locations = getPlotMedia(photosDir, plot, ".jpg")

                if (locations.isNotEmpty()) {
                    locations.forEach { image ->
                        if (image.exists()) {

                            val name = image.name

                            if (name != null) {

                                if (plot in name) {

                                    val bmp = decodeBitmap(image.uri)

                                    if (bmp != null) {

                                        if (image.uri !in uris) {
                                            uris.add(image.uri)
                                            drawables?.add(bmp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                loadGallery()

            }

            scope.cancel()
        }
    }

    private fun loadGallery() {
        val photosDir = getFieldMediaDirectory(context, "photos")
        if (photosDir != null) {
            val photos = getPlotMedia(photosDir, cRange.plot_id, ".jpg")

            activity?.runOnUiThread {

                photoAdapter = GalleryImageAdapter(context as Activity, drawables)
                photo?.adapter = photoAdapter

                if (photos.isNotEmpty()) {

                    photo?.setSelection((photo?.count ?: 1) - 1)
                    photo?.onItemClickListener =
                        OnItemClickListener { arg0: AdapterView<*>?, arg1: View?, pos: Int, arg3: Long ->
                            displayPlotImage(
                                photos[pos].uri
                            )
                        }
                }

                photoAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return try {
            val input = context.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(input)
            input?.close()
            bmp
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun generateThumbnails() {
        val photosDir = getFieldMediaDirectory(context, "photos")
        if (photosDir != null) {
            val files = photosDir.listFiles()
            for (doc in files) {
                createThumbnail(doc.uri)
            }
        }
    }

    private fun createThumbnail(uri: Uri) {

        //create thumbnail
        try {
            val thumbsDir = getFieldMediaDirectory(context, "thumbnails")
            val name: String = uri.getStem(context)
            if (thumbsDir != null) {
                var bmp = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                bmp = Bitmap.createScaledBitmap(bmp, 256, 256, true)
                val thumbnail = thumbsDir.createFile("image/*", "$name.jpg")
                if (thumbnail != null) {
                    val output = context.contentResolver.openOutputStream(thumbnail.uri)
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, output)
                    output?.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteTraitListener() {
        deletePhotoWarning(false, null)
    }

    fun brapiDelete(newTraits: MutableMap<String, String>?) {
        deletePhotoWarning(true, newTraits)
    }

    private fun displayPlotImage(path: Uri) {
        try {
            Log.w("Display path", path.toString())
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(path, "image/*")
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun makeImage(currentTrait: TraitObject, newTraits: MutableMap<String, String>?) {
        val photosDir = getFieldMediaDirectory(context, "photos")
        val thumbsDir = getFieldMediaDirectory(context, "thumbnails")
        if (photosDir != null && thumbsDir != null) {
            mCurrentPhotoPath?.let { path ->
                val file = photosDir.findFile(path)
                if (file != null) {
                    try {
                        Utils.scanFile(context, file.uri.toString(), "image/*")
                        createThumbnail(file.uri)
                        updateTraitAllowDuplicates(
                            currentTrait.trait,
                            "photo",
                            path,
                            null,
                            newTraits
                        )
                        loadLayoutWork()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun updateTraitAllowDuplicates(
        parent: String,
        trait: String,
        value: String?,
        newValue: String?,
        newTraits: MutableMap<String, String>?
    ) {
        if (value != newValue) {
            if (cRange == null || cRange.plot_id.isEmpty()) {
                return
            }

            value?.let { v ->

                Log.d("Field Book", "$trait $v")
                newTraits?.remove(parent)
                newTraits?.set(parent, v)
                val expId = prefs.getInt(GeneralKeys.SELECTED_FIELD_ID, 0).toString()
                val observation =
                    ConfigActivity.dt.getObservationByValue(expId, cRange.plot_id, parent, v)
                ConfigActivity.dt.deleteTraitByValue(expId, cRange.plot_id, parent, v)
                ConfigActivity.dt.insertUserTraits(
                    cRange.plot_id,
                    parent,
                    trait,
                    newValue ?: v,
                    prefs.getString(
                        GeneralKeys.FIRST_NAME,
                        ""
                    ) + " " + prefs.getString(GeneralKeys.LAST_NAME, ""),
                    prefs.getString(GeneralKeys.LOCATION, ""),
                    "",
                    expId,
                    observation.dbId,
                    observation.lastSyncedTime
                ) //TODO add notes and exp_id
            }
        }
    }

    private fun deletePhotoWarning(brapiDelete: Boolean, newTraits: MutableMap<String, String>?) {
        val expId = prefs.getInt(GeneralKeys.SELECTED_FIELD_ID, 0).toString()
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.dialog_warning))
        builder.setMessage(context.getString(R.string.trait_delete_warning_photo))
        builder.setPositiveButton(context.getString(R.string.dialog_yes)) { dialog, which ->
            dialog.dismiss()
            if (brapiDelete) {
                Toast.makeText(
                    context.applicationContext,
                    context.getString(R.string.brapi_delete_message),
                    Toast.LENGTH_SHORT
                ).show()
                //updateTrait(parent, currentTrait.getFormat(), getString(R.string.brapi_na));
            }
            if ((photo?.count ?: 0) > 0) {
                val photosDir = getFieldMediaDirectory(context, "photos")
                val thumbsDir = getFieldMediaDirectory(context, "thumbnails")
                val photosList = getPlotMedia(photosDir, cRange.plot_id, ".jpg").toMutableList()
                val thumbsList = getPlotMedia(thumbsDir, cRange.plot_id, ".jpg")
                val index = photo?.selectedItemPosition ?: 0
                val selected = photosList[index]
                val thumbSelected = thumbsList[index]
                val item = selected.uri
                if (!brapiDelete) {
                    selected.delete()
                    thumbSelected.delete()
                    photosList.removeAt(index)
                }
                val file = DocumentFile.fromSingleUri(context, item)
                if (file != null && file.exists()) {
                    file.delete()
                }

                // Remove individual images
                if (brapiDelete) {
                    updateTraitAllowDuplicates(
                        currentTrait.trait,
                        "photo",
                        item.toString(),
                        "NA",
                        newTraits
                    )
                    loadLayout()
                } else {
                    ConfigActivity.dt.deleteTraitByValue(
                        expId,
                        cRange.plot_id,
                        currentTrait.trait,
                        item.toString()
                    )
                }

                // Only do a purge by trait when there are no more images left
                if (!brapiDelete) {
                    if (photosList.size == 0) removeTrait(currentTrait.trait)
                }
            } else {

                // If an NA exists, delete it
                ConfigActivity.dt.deleteTraitByValue(
                    expId,
                    cRange.plot_id,
                    currentTrait.trait,
                    "NA"
                )
            }
            loadLayoutWork()
        }
        builder.setNegativeButton(context.getString(R.string.dialog_no)) { dialog, _ -> dialog.dismiss() }
        val alert = builder.create()
        alert.show()
        DialogUtils.styleDialogs(alert)
    }

    private fun takePicture() {
        val timeStamp = SimpleDateFormat(
            "yyyy-MM-dd-hh-mm-ss", Locale.getDefault()
        )
        val dir = getFieldMediaDirectory(context, "photos")
        if (dir != null) {
            val generatedName =
                cRange.plot_id + "_" + currentTrait.trait + "_" + rep + "_" + timeStamp.format(
                    Calendar.getInstance().time
                ) + ".jpg"
            mCurrentPhotoPath = generatedName
            Log.w("File", dir.uri.toString() + generatedName)
            val file = dir.createFile("image/jpg", generatedName)
            if (file != null) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                // Ensure that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(context.packageManager) != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, file.uri)
                    (context as Activity).startActivityForResult(
                        takePictureIntent,
                        PICTURE_REQUEST_CODE
                    )
                }
            }
        }
    }

    private val rep: String
        get() {
            val repInt = ConfigActivity.dt.getRep(cRange.plot_id, currentTrait.trait)
            return repInt.toString()
        }

    private inner class PhotoTraitOnClickListener : OnClickListener {
        override fun onClick(view: View) {
            try {
                val m: Int = try {
                    currentTrait.details.toInt()
                } catch (n: Exception) {
                    0
                }
                val photosDir = getFieldMediaDirectory(context, "photos")
                val plot = cRange.plot_id
                val locations = getPlotMedia(photosDir, plot, ".jpg")
                if (photosDir != null) {
                    // Do not take photos if limit is reached
                    if (m == 0 || locations.size < m) {
                        takePicture()
                    } else Utils.makeToast(
                        context,
                        context.getString(R.string.traits_create_photo_maximum)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Utils.makeToast(context, context.getString(R.string.trait_error_hardware_missing))
            }
        }
    }

    companion object {
        const val PICTURE_REQUEST_CODE = 252
    }
}