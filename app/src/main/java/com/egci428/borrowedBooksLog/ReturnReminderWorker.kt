package com.egci428.borrowedBooksLog

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ReturnReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val userUUID = inputData.getString("userUUID") ?: return Result.failure()
        val dataReference = FirebaseFirestore.getInstance()

        val userCollection = dataReference.collection("users").document(userUUID).collection("dataMessage")
        userCollection.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    val currentDate = Calendar.getInstance().time
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                    for (document in snapshot.documents) {
                        val returnDateStr = document.getString("returnTime") ?: continue
                        val returnDate = dateFormat.parse(returnDateStr)
                        Log.d("ReturnReminder","Reach this step")

                        if (returnDate != null) {
                            val diffInMillis = returnDate.time - currentDate.time
                            val daysLeft = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

                            // Notify if days left are 3 or less
                            if (daysLeft in 1..100) {
                                val bookName = document.getString("bookName") ?: "Unnamed Book"
                                NotificationHelper.sendNotification(
                                    applicationContext,
                                    "Return Reminder",
                                    "Your book '$bookName' is due in $daysLeft days!"
                                )
                                Log.d("Notify","Reach notify steps")
                            }
                        }
                    }
                }
            }
        return Result.success()
    }


}