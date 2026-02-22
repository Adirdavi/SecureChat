# SecureChat 🛡️

A highly secure, real-time Android messaging application featuring **Military-Grade End-to-End (E2E) RSA Encryption** and **Self-Destructing Messages**. Built entirely with **Kotlin** and **Jetpack Compose**, backed by Firebase.

## ✨ Key Features

* **True End-to-End Encryption (RSA):** Messages are encrypted using asymmetric RSA cryptography. Private keys are generated and locked securely inside the device's hardware (Android KeyStore) and never leave the user's phone.
* **Self-Destructing Messages (Secret Mode):** Send highly sensitive messages that automatically delete themselves from both devices and the database 20 seconds after being read.
* **Real-Time Messaging:** Instant message delivery and UI synchronization powered by Firebase Firestore.
* **Modern UI/UX:** A sleek, fully dark-themed interface built with Jetpack Compose, featuring colorful dynamic avatars, unread message indicators, and smart chat sorting.
* **Secure Authentication:** User registration and login managed via Firebase Auth.

## 🏗️ Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material Design 3)
* **Backend:** Firebase Authentication & Cloud Firestore
* **Security:** `java.security` API, Android KeyStore, RSA/ECB/PKCS1Padding
* **Architecture:** Repository Pattern (Separation of Concerns)
* **Asynchrony:** Kotlin Coroutines & Flows

## 🔐 How the Encryption Works

SecureChat does not rely on hardcoded secrets. It uses a robust Public Key Infrastructure (PKI):
1. **Key Generation:** When a user registers or logs in, the app generates a unique RSA Public/Private key pair. 
2. **Key Storage:** The Private Key is safely stored in the `AndroidKeyStore` (hardware-backed). The Public Key is uploaded to Firestore.
3. **Sending a Message:** When Alice sends a message to Bob, the app fetches Bob's Public Key and encrypts the message. The app also creates a secondary copy of the message encrypted with Alice's own Public Key (so she can read her own sent messages).
4. **Receiving a Message:** Only Bob's device, using his locally stored Private Key, can decrypt the incoming message. Not even the database administrator can read the contents.

## 📸 Screenshots
*(Add screenshots of your app here)*
| Login Screen | Chat List | Secure Chat | Secret Mode |
| :---: | :---: | :---: | :---: |
| <img src="link_to_image" width="200"/> | <img src="link_to_image" width="200"/> | <img src="link_to_image" width="200"/> | <img src="link_to_image" width="200"/> |

## 🚀 Getting Started

### Prerequisites
* Android Studio (Latest Version)
* A Firebase Project

### Installation
1. Clone the repository:
   ```bash
   git clone (https://github.com/Adirdavi/SecureChat)
