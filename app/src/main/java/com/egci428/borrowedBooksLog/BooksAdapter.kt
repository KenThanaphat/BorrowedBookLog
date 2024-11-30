package com.egci428.borrowedBooksLog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import java.util.Locale

class BooksAdapter(val mContext : Context, val layoutResID: Int, val bookList : List<Books>) :
    ArrayAdapter<Books>(mContext, layoutResID, bookList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val book = bookList[position]
        val layoutInflater = LayoutInflater.from(mContext)
        val view = layoutInflater.inflate(layoutResID, null)
        val BookName = view.findViewById<TextView>(R.id.bookName)
        val returnDate = view.findViewById<TextView>(R.id.returnDate)
        val borrowDate = view.findViewById<TextView>(R.id.borrowDate)
        val timeLeftView = view.findViewById<TextView>(R.id.timeLeft)

        val booksImage = view.findViewById<ImageView>(R.id.Image)
        val imageres = loadBitmapFromFile(book.imagePath)

        booksImage.setImageBitmap(imageres)
        BookName.text = book.bookName
        returnDate.text = "Return Date: "+ book.returnTime
        borrowDate.text = "Borrow Date: "+book.timeBorrowed

        val daysLeft = calculateDaysLeft(book.returnTime)
        timeLeftView.text = if (daysLeft >= 0) {
            "$daysLeft days left"
        } else {
            "Overdue!"
        }

        return view

    }

    private fun loadBitmapFromFile(filePath: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(filePath)
        } catch (e: Exception) {
            Log.e("Load Image", "Error loading image", e)
            null
        }
    }

    private fun calculateDaysLeft(returnDate: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val returnDateParsed = dateFormat.parse(returnDate)
            val currentDate = Calendar.getInstance().time

            // Calculate difference in milliseconds
            val diffInMillis = returnDateParsed.time - currentDate.time

            // Convert milliseconds to days
            (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            Log.e("Date Calculation", "Error calculating days left", e)
            -1
        }
    }
}