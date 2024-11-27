package com.egci428.borrowedBooksLog

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainActivity : AppCompatActivity() {
    lateinit var userUUID : String
    lateinit var dataReference: FirebaseFirestore
    lateinit var adapter: BooksAdapter
    lateinit var bookList: MutableList<Books>

    lateinit var bookView: ListView

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

        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userUUID = sharedPreferences.getString("UUID", null) ?: generateAndSaveUUID(sharedPreferences)

        bookView = findViewById(R.id.bookView)
        dataReference = FirebaseFirestore.getInstance()
        bookList = mutableListOf()
        readFirestoreData()

        button.setOnClickListener{
            val intent = Intent(this, BookActivity::class.java)
            intent.putExtra("uuid",userUUID)
            startActivity(intent)
            Toast.makeText(this, "Activated", Toast.LENGTH_SHORT).show()
        }



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


}