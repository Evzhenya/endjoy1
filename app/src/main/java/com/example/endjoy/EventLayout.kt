package com.example.endjoy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class EventLayout(context: Context, eventName: String, eventDate: String, imageBitmap: Bitmap) : LinearLayout(context) {


    private var isHighlighted = false
    private val imageView: ImageView
    private val textView: TextView
    private val deleteButton: Button
    private val editButton: Button

    private lateinit var editText: EditText
    private lateinit var datePickerButton: Button

    private var isEditMode = false


    var eventDate: String = eventDate
        private set

    init {
        orientation = HORIZONTAL

        imageView = ImageView(context)
        imageView.setImageBitmap(imageBitmap)
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        params.setMargins(10, 10, 10, 10)
        imageView.layoutParams = params
        addView(imageView)

        val textLayout = LinearLayout(context)
        textLayout.orientation = VERTICAL

        textView = TextView(context)
        textView.text = "Название мероприятия: $eventName\nДата: $eventDate"
        textLayout.addView(textView)

        deleteButton = Button(context)
        deleteButton.text = "Удалить"
        deleteButton.visibility = View.GONE
        deleteButton.setOnClickListener {
            val dbHelper = DBHelper(context)
            val db = dbHelper.writableDatabase


            val selection = "${DBHelper.COLUMN_NAME} = ? AND ${DBHelper.COLUMN_DATE} = ?"
            val selectionArgs = arrayOf(eventName, eventDate)
            val deletedRows = db.delete(DBHelper.TABLE_EVENTS, selection, selectionArgs)

            if (deletedRows > 0) {

                (context as? MainActivity)?.removeEventLayout(this)
            } else {

            }
        }
        textLayout.addView(deleteButton)

        editButton = Button(context)
        editButton.text = "Редактировать"
        editButton.visibility = View.GONE
        editButton.setOnClickListener {

        }
        textLayout.addView(editButton)

        addView(textLayout)

        setOnLongClickListener {
            isHighlighted = true
            updateHighlightState()
            true
        }

        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isHighlighted) {
                        updateHighlightState()
                    }
                }
            }
            false
        }
    }

    private fun updateHighlightState() {
        if (isHighlighted) {
            setBackgroundColor(Color.LTGRAY)
            deleteButton.visibility = View.VISIBLE
            editButton.visibility = View.VISIBLE

            if (isEditMode) {

                textView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                editText.visibility = View.VISIBLE
                datePickerButton.visibility = View.VISIBLE
            }
        } else {
            setBackgroundColor(Color.WHITE)
            deleteButton.visibility = View.GONE
            editButton.visibility = View.GONE


            isEditMode = false
            textView.visibility = View.VISIBLE
            imageView.visibility = View.GONE
            editText.visibility = View.GONE
            datePickerButton.visibility = View.GONE
        }
    }
}