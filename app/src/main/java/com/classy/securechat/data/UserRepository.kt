package com.classy.securechat.data

import android.util.Log
import com.classy.securechat.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getAllUsers(onResult: (List<User>) -> Unit) {
        val myId = auth.currentUser?.uid ?: ""

        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                val users = mutableListOf<User>()
                for (doc in snapshot.documents) {
                    try {
                        val user = doc.toObject(User::class.java)

                        if (user != null && user.userId != myId && user.email.isNotBlank()) {
                            users.add(user)
                        }
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Skipped corrupted user: ${doc.id}", e)
                    }
                }
                onResult(users)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}