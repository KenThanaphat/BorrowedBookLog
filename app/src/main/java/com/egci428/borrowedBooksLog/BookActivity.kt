package com.egci428.borrowedBooksLog

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class BookActivity : AppCompatActivity() {

    lateinit var userUUID: String
    lateinit var bookname: EditText
    lateinit var borrowdate: EditText
    lateinit var returndate: EditText
    lateinit var cameraBtn: Button
    lateinit var saveResult: Button


    lateinit var bookImageView: ImageView
    lateinit var dataReference: FirebaseFirestore
    lateinit var filePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_book)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bookname = findViewById(R.id.bookname)
        borrowdate = findViewById(R.id.borrowdate)
        returndate = findViewById(R.id.returndate)
        cameraBtn = findViewById(R.id.cameraBtn)
        bookImageView = findViewById(R.id.BookImageview)
        saveResult = findViewById(R.id.saveButton)

        dataReference = FirebaseFirestore.getInstance()

        val bundle = intent.extras

        if(bundle!=null){
            userUUID = bundle.getString("uuid").toString()
            Log.d("BookActivity", "Send UUID Successfully")
            Log.d("userUUID",userUUID)
        }

        saveResult.setOnClickListener{
            submitData()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


    }

    fun takePhoto(view: View){
        requestCameraPermission.launch(android.Manifest.permission.CAMERA)
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            isSuccess : Boolean ->
        if(isSuccess) {
            Log.d("Take Photo", "Permission Granted")
            takePicture.launch(null)
        }
        else{
            Toast.makeText(applicationContext, "Camera has no permission", Toast.LENGTH_SHORT).show()
        }

    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){
            bitmap : Bitmap? ->
//        Log.d("Take Picture", "Show Bitmap Picture")
//        photoImageView.setImageBitmap(bitmap)
        bitmap?.let {
            // Save the bitmap
            val filePath = saveBitmapToInternalStorage(it)

            // Optionally, display it in the ImageView
            bookImageView.setImageBitmap(it)

            Toast.makeText(this, "Image saved to: $filePath", Toast.LENGTH_LONG).show()
        } ?: run {
            Log.e("Take Picture", "Bitmap is null")
        }
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap): String {
        val filename = "captured_image_"+bookname.text+".png"
        return try {
            openFileOutput(filename, MODE_PRIVATE).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            filePath = "${filesDir.absolutePath}/$filename"
            Log.d("Save Image", "Image saved at $filePath")
            filePath
        } catch (e: Exception) {
            Log.e("Save Image", "Error saving image", e)
            ""
        }
    }

    private fun submitData() {
        val book = bookname.text.toString()
        if (book.isEmpty()) {
            bookname.error = "Please submit a message"
            return
        }

        val userCollection = dataReference.collection("users").document(userUUID).collection("dataMessage")
        val bookId = userCollection.document().id
        val bookData = Books(bookId, book, borrowdate.text.toString(), returndate.text.toString(),filePath)

        userCollection.add(bookData).addOnSuccessListener {
            Toast.makeText(applicationContext, "Message saved successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(applicationContext, "Failed to save message", Toast.LENGTH_SHORT).show()
        }
    }

}