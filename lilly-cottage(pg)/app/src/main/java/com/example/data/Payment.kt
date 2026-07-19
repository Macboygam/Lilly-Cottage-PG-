package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memberId: Int,
    val memberName: String,
    val memberBlock: String,
    val amount: Double,
    val paymentDate: String, // YYYY-MM-DD
    val paymentMethod: String, // Cash, UPI, Bank
    val notes: String = ""
)
