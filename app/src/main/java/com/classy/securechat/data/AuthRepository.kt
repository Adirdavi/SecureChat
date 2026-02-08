package com.classy.securechat.data

import com.classy.securechat.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // הרשמה + שמירת משתמש בדאטה בייס
    fun signUp(email: String, pass: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Email and Password are required")
            return
        }

        // 1. יצירת משתמש באותנטיקציה
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                // 2. יצירת אובייקט משתמש
                val newUser = User(
                    userId = uid,
                    displayName = name,
                    email = email
                )

                // 3. שמירה ב-Firestore באוסף "users"
                db.collection("users").document(uid).set(newUser)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onSuccess() } // גם אם השמירה נכשלה, המשתמש נוצר
            }
            .addOnFailureListener { onError(it.message ?: "Sign up failed") }
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Email and Password are required")
            return
        }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Login failed") }
    }

    fun getUserId(): String? = auth.currentUser?.uid
}