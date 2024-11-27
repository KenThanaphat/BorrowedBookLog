package com.egci428.borrowedBooksLog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class BooksAdapter(val mContext : Context, val layoutResID: Int, val bookList : List<Books>) :
    ArrayAdapter<Books>(mContext, layoutResID, bookList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val book = bookList[position]
        val layoutInflater = LayoutInflater.from(mContext)
        val view = layoutInflater.inflate(layoutResID, null)
        val BookName = view.findViewById<TextView>(R.id.bookName)
        val returnDate = view.findViewById<TextView>(R.id.returnDate)
        val borrowDate = view.findViewById<TextView>(R.id.borrowDate)

        val booksImage = view.findViewById<ImageView>(R.id.Image)
        val imageres = loadBitmapFromFile(book.imagePath)

        booksImage.setImageBitmap(imageres)
        BookName.text = book.bookName
        returnDate.text = book.returnTime
        borrowDate.text = book.timeBorrowed

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
}