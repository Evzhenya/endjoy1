package com.example.endjoy


import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    private var myCalendar = Calendar.getInstance()


    private lateinit var scrollView: ScrollView
    private lateinit var upperLayout: LinearLayout
    private lateinit var lowerLayout: LinearLayout
    private lateinit var btnDatePicker: Button
    private lateinit var tvDatePicker: TextView
    private lateinit var editTextText: EditText
    private lateinit var button: Button
    private lateinit var imageView: ImageView
    private lateinit var createEventButton: Button

    private val eventLayoutList: MutableList<EventLayout> = mutableListOf()

    private var selectedImageBitmap: Bitmap? = null

    companion object {
        const val IMAGE_REQUEST_CODE = 100
        const val MIN_DAYS_OFFSET = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        dbHelper = DBHelper(this)

        scrollView = findViewById(R.id.scrollView)
        upperLayout = findViewById(R.id.upperLayout)
        lowerLayout = findViewById(R.id.lowerLayout)
        btnDatePicker = findViewById(R.id.btnDatePicker)
        tvDatePicker = findViewById(R.id.tvDatePicker)
        editTextText = findViewById(R.id.editTextText)
        button = findViewById(R.id.button)
        imageView = findViewById(R.id.imageView)
        createEventButton = findViewById(R.id.create_event_button)


        loadEventsFromDatabase()

        


        val datePicker = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateLabel(myCalendar)
            updateCreateEventButtonState()
        }


        btnDatePicker.setOnClickListener {
            DatePickerDialog(
                this, datePicker, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        button.setOnClickListener {
            pickImageGallery()
        }

        createEventButton.setOnClickListener {
            val eventName = editTextText.text.toString().trim()
            val eventDate = tvDatePicker.text.toString().trim()

            if (selectedImageBitmap != null && eventName.isNotEmpty() && eventDate.isNotEmpty()) {
                showEventDetails(eventName, eventDate, selectedImageBitmap!!)
                clearFields()
            } else {
                Toast.makeText(
                    this,
                    "Пожалуйста, выберите изображение, введите название мероприятия и выберите дату",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        tvDatePicker.addTextChangedListener { updateCreateEventButtonState() }
    }

    fun removeEventLayout(eventLayout: EventLayout) {
        lowerLayout.removeView(eventLayout)
        eventLayoutList.remove(eventLayout)
    }

    private fun saveEventToDatabase(eventName: String, eventDate: String, imageBitmap: Bitmap) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(DBHelper.COLUMN_NAME, eventName)
            put(DBHelper.COLUMN_DATE, eventDate)
            put(DBHelper.COLUMN_IMAGE, bitmapToByteArray(imageBitmap))
        }

        val newRowId = db.insert(DBHelper.TABLE_EVENTS, null, values)

        if (newRowId != -1L) {

        } else {

        }
    }

    private fun loadEventsFromDatabase() {
        val db = dbHelper.readableDatabase

        val projection = arrayOf(
            DBHelper.COLUMN_ID,
            DBHelper.COLUMN_NAME,
            DBHelper.COLUMN_DATE,
            DBHelper.COLUMN_IMAGE
        )

        val sortOrder = "${DBHelper.COLUMN_DATE} DESC"

        val cursor = db.query(
            DBHelper.TABLE_EVENTS,
            projection,
            null,
            null,
            null,
            null,
            sortOrder
        )

        val eventList = mutableListOf<Event>()

        with(cursor) {
            while (moveToNext()) {
                val eventId = getLong(getColumnIndexOrThrow(DBHelper.COLUMN_ID))
                val eventName = getString(getColumnIndexOrThrow(DBHelper.COLUMN_NAME))
                val eventDate = getString(getColumnIndexOrThrow(DBHelper.COLUMN_DATE))
                val eventImageBytes = getBlob(getColumnIndexOrThrow(DBHelper.COLUMN_IMAGE))
                val eventImageBitmap = BitmapFactory.decodeByteArray(eventImageBytes, 0, eventImageBytes.size)

                val event = Event(eventId, eventName, eventDate, eventImageBitmap)
                eventList.add(event)
            }
        }

        for (event in eventList) {
            val eventLayout = createEventLayout(event.name, event.date, event.image)
            eventLayoutList.add(eventLayout)

            eventLayout.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lowerLayout.addView(eventLayout)
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun updateLabel(myCalendar: Calendar) {
        val myFormat = "dd-MM-yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        tvDatePicker.text = sdf.format(myCalendar.time)
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            val imageUri = resultData?.data
            if (imageUri != null) {
                val inputStream = contentResolver.openInputStream(imageUri)

                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true


                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()


                val reqWidth = 500
                val reqHeight = 800


                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)


                options.inJustDecodeBounds = false
                val inputStreamAgain = contentResolver.openInputStream(imageUri)
                selectedImageBitmap = BitmapFactory.decodeStream(inputStreamAgain, null, options)
                inputStreamAgain?.close()


                selectedImageBitmap = Bitmap.createScaledBitmap(selectedImageBitmap!!, reqWidth, reqHeight, false)

                imageView.setImageBitmap(selectedImageBitmap)
                updateCreateEventButtonState()
            }
        }
    }


    private fun updateCreateEventButtonState() {
        val isDateSelected = tvDatePicker.text.toString().isNotEmpty()
        val isDateWithinRange = isDateSelectedWithinRange(myCalendar)
        createEventButton.isEnabled = selectedImageBitmap != null && isDateWithinRange

        if (!isDateWithinRange) {
            Toast.makeText(
                this,
                "Пожалуйста, выберите дату, отстоящую от текущей как минимум на 3 дня",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }


    private fun isDateSelectedWithinRange(selectedDate: Calendar): Boolean {
        val currentDate = Calendar.getInstance()
        currentDate.add(Calendar.DAY_OF_MONTH, MIN_DAYS_OFFSET)

        return selectedDate.after(currentDate) || isSameDay(selectedDate, currentDate)
    }

    private fun isSameDay(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
                date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH)
    }

    private fun showEventDetails(eventName: String, eventDate: String, imageBitmap: Bitmap) {
        val eventLayout = createEventLayout(eventName, eventDate, imageBitmap)
        eventLayoutList.add(eventLayout)

        saveEventToDatabase(eventName, eventDate, imageBitmap)


        eventLayout.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lowerLayout.addView(eventLayout)


        eventLayoutList.sortByDescending { getDateFromString(it.eventDate) }

        scrollView.post {
            scrollView.smoothScrollTo(0, scrollView.bottom)
        }
    }

    private fun createEventLayout(eventName: String, eventDate: String, imageBitmap: Bitmap): EventLayout {
        val eventLayout = EventLayout(this, eventName, eventDate, imageBitmap)

        return eventLayout
    }

    private fun clearFields() {
        editTextText.text.clear()
        imageView.setImageBitmap(null)
        selectedImageBitmap = null
    }

    private fun getDateFromString(dateString: String): Date {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        return sdf.parse(dateString)!!
    }

}











