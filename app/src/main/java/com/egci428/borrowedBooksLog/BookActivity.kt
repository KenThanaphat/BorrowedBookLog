package com.egci428.borrowedBooksLog

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.DatePicker
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
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class BookActivity : AppCompatActivity(), SensorEventListener {

    lateinit var userUUID: String
    lateinit var bookname: EditText
    lateinit var borrowdate: TextView

    lateinit var setborrowdate: Button
    lateinit var setreturndate: Button
    lateinit var returndate: TextView
    lateinit var cameraBtn: Button
    lateinit var saveResult: Button

    private var borrowstring = ""
    private var returnstring = ""

    private val calendar = Calendar.getInstance()

    lateinit var bookImageView: ImageView
    lateinit var dataReference: FirebaseFirestore
    lateinit var filePath: String

    private var sensorManager: SensorManager? = null
    private var lastUpdate: Long = 0

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
        borrowdate = findViewById(R.id.borrowtext)
        setborrowdate = findViewById(R.id.SetBorrowDate)
        setreturndate = findViewById(R.id.setReturnDate)

        returndate = findViewById(R.id.returntext)
        cameraBtn = findViewById(R.id.cameraBtn)
        bookImageView = findViewById(R.id.BookImageview)
        saveResult = findViewById(R.id.saveButton)

        dataReference = FirebaseFirestore.getInstance()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lastUpdate = System.currentTimeMillis()

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

        setborrowdate.setOnClickListener{
            openDialog(0)
        }
        setreturndate.setOnClickListener{
            openDialog(1)
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
            bookname.error = "Please submit a name"
            return
        }

        val userCollection = dataReference.collection("users").document(userUUID).collection("dataMessage")
        val bookId = userCollection.document().id
        val bookData = Books(bookId, book, borrowstring, returnstring ,filePath)

        userCollection.document(bookId).set(bookData)
    }

    fun openDialog(choice : Int){
        var dialog = DatePickerDialog(this, {DatePicker, year: Int,monthOfYear: Int, dayOfMonth: Int ->
            val selectedData = Calendar.getInstance()
            selectedData.set(year,monthOfYear,dayOfMonth)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(selectedData.time)
            if(choice == 0) {
                borrowdate.setText("Borrow Date: " + formattedDate)
                borrowstring = formattedDate
            }
            if(choice == 1){
                returndate.setText("Return Date: "+ formattedDate)
                returnstring = formattedDate
            }
        },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun resetData() {
        bookname.text.clear() // Clears the text in the EditText
        borrowdate.text = "Borrow Date:" // Resets the TextView to its default value
        returndate.text = "Return Date:" // Resets the TextView to its default value
        bookImageView.setImageResource(0) // Clears the image
        filePath = "" // Resets the file path
        borrowstring = "" // Resets the borrow string
        returnstring = "" // Resets the return string

        Toast.makeText(this, "Data has been reset", Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER){
            getAccelerometer(event)
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun getAccelerometer(event: SensorEvent) {

        val values = event.values
        val x = values[0]
        val y = values[1]
        val z = values[2]

        val accel = (x*x + y*y + z*z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH)

        val actualTime = System.currentTimeMillis()
        if (accel >= 2) {
            if (actualTime-lastUpdate < 200){
                return // Exit from function
            }

            lastUpdate = actualTime

            resetData()
        }


    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }


}