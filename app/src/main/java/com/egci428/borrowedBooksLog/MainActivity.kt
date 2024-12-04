package com.egci428.borrowedBooksLog

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import android.Manifest


class MainActivity : AppCompatActivity() {
    lateinit var userUUID : String
    lateinit var dataReference: FirebaseFirestore
    lateinit var adapter: BooksAdapter
    lateinit var bookList: MutableList<Books>

    lateinit var bookView: ListView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, proceed with notification setup
        } else {
            // Permission denied, handle accordingly
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var button = findViewById<Button>(R.id.button)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userUUID = sharedPreferences.getString("UUID", null) ?: generateAndSaveUUID(sharedPreferences)

        bookView = findViewById(R.id.bookView)
        dataReference = FirebaseFirestore.getInstance()
        bookList = mutableListOf()
        readFirestoreData()

        val gestureDetector = GestureDetector(this, GestureListener() )

        bookView.setOnTouchListener { _, event ->
            gestureDetector!!.onTouchEvent(event)
            false
        }

        button.setOnClickListener{
            val intent = Intent(this, BookActivity::class.java)
            intent.putExtra("uuid",userUUID)
            startActivity(intent)
        }


        scheduleReturnReminderWorker(this, userUUID)
        NotificationHelper.createNotificationChannel(this)

    }

    private fun generateAndSaveUUID(sharedPreferences: SharedPreferences): String {
        val uuid = UUID.randomUUID().toString()
        val editor = sharedPreferences.edit()
        editor.putString("UUID", uuid)
        editor.apply()
        return uuid
    }

    private fun readFirestoreData() {
        val userCollection = dataReference.collection("users").document(userUUID).collection("dataMessage")
        userCollection.orderBy("timeBorrowed").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    bookList.clear()
                    val messages = snapshot.toObjects(Books::class.java)
                    bookList.addAll(messages)
                    adapter = BooksAdapter(applicationContext, R.layout.item_books, bookList)
                    bookView.adapter = adapter
                }
            }
            .addOnFailureListener {
                Toast.makeText(applicationContext, "Failed to read messages", Toast.LENGTH_SHORT).show()
            }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private val MAX_SWIPE_DISTANCE_X = 100
        private val MAX_VELOCITY = 100

        //private val MIN_SWIPE_DISTANCE_Y = 100
        //private val MAX_SWIPE_DISTANCE_Y = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null || e2 == null) return false

            val deltaX = e2.x - e1.x
            if (Math.abs(deltaX) > MAX_SWIPE_DISTANCE_X && Math.abs(velocityX) > MAX_VELOCITY) {
                val position = findItemPositionByCoordinates(e1)
                position?.let {
                    val view = bookView.getChildAt(it - bookView.firstVisiblePosition)
                    view?.animate()
                        ?.alpha(0f)
                        ?.setDuration(300)
                        ?.withEndAction {
                            val bookref = dataReference.collection("users").document(userUUID).collection("dataMessage").document(bookList[it].bookId)

                            bookref.delete()
                            bookList.removeAt(it)

                            adapter.notifyDataSetChanged()

                            view.alpha = 1f
                        }
                        ?.start()
                }
                return true
            }
            return false
        }


    }

    private fun findItemPositionByCoordinates(e: MotionEvent): Int? {
        val position = bookView.pointToPosition(e.x.toInt(), e.y.toInt())
        return if (position != ListView.INVALID_POSITION) position else null
    }

    fun scheduleReturnReminderWorker(context: Context, userUUID: String) {
        val workManager = WorkManager.getInstance(context)

        val inputData = Data.Builder()
            .putString("userUUID", userUUID)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ReturnReminderWorker>(
            1, // Repeat interval
            java.util.concurrent.TimeUnit.DAYS
        )
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "ReturnReminderWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }



}