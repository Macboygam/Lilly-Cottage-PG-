package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val parentName: String, // Father / Mother Name
    val parentPhone: String, // Father / Mother Phone Number
    val gender: String, // Male, Female
    val age: Int,
    val dateOfBirth: String, // YYYY-MM-DD
    val mobileNumber: String,
    val emailId: String,
    val aadhaarPhotoUri: String? = null, // Local Uri string of Aadhaar Card photo from picker
    val drivingLicenceNumber: String? = null, // optional
    val address: String,
    val city: String,
    val state: String,
    val block: String, // Block A, Block B, Block C
    val rentAmount: Double,
    val paymentDate: String, // YYYY-MM-DD
    val nextDueDate: String, // YYYY-MM-DD
    val paymentMethod: String, // Cash, UPI, Bank
    val foodIncluded: Boolean, // True = Yes, False = No
    val isActive: Boolean = true,
    val joinDate: String = ""
)
