package com.classy.securechat.data

import com.classy.securechat.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun signUp(email: String, pass: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Email and Password are required")
            return
        }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                val myPublicKey = RSAKeyManager.generateKeyPair() ?: ""

                val newUser = User(
                    userId = uid,
                    displayName = name,
                    email = email,
                    publicKey = myPublicKey
                )

                db.collection("users").document(uid).set(newUser)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError("Failed to save user") }
            }
            .addOnFailureListener { onError(it.message ?: "Sign up failed") }
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Email and Password are required")
            return
        }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if(uid != null) {
                    val newPublicKey = RSAKeyManager.generateKeyPair() ?: ""
                    db.collection("users").document(uid).update("publicKey", newPublicKey)
                }
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Login failed") }
    }

    fun getUserId(): String? = auth.currentUser?.uid
}