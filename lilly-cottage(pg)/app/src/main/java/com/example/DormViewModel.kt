package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DormDatabase
import com.example.data.DormRepository
import com.example.data.Member
import com.example.data.Payment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DormViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DormRepository
    private val prefs = application.getSharedPreferences("dorm_prefs", Context.MODE_PRIVATE)

    // Room / Bed Capacities persistence
    val blockACapacity = MutableStateFlow(prefs.getInt("block_a_capacity", 30))
    val blockBCapacity = MutableStateFlow(prefs.getInt("block_b_capacity", 20))
    val blockCCapacity = MutableStateFlow(prefs.getInt("block_c_capacity", 15))
    val reminderDaysConfig = MutableStateFlow(prefs.getInt("reminder_days", 3)) // Days before due date to alert (default 3 days)

    init {
        val db = DormDatabase.getDatabase(application)
        repository = DormRepository(db)

        // Automatically persist updates across launches (localStorage behaviour)
        viewModelScope.launch {
            reminderDaysConfig.collect { days ->
                prefs.edit().putInt("reminder_days", days).apply()
            }
        }
        viewModelScope.launch {
            blockACapacity.collect { cap ->
                prefs.edit().putInt("block_a_capacity", cap).apply()
            }
        }
        viewModelScope.launch {
            blockBCapacity.collect { cap ->
                prefs.edit().putInt("block_b_capacity", cap).apply()
            }
        }
        viewModelScope.launch {
            blockCCapacity.collect { cap ->
                prefs.edit().putInt("block_c_capacity", cap).apply()
            }
        }
    }

    // Today's date YYYY-MM-DD
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Default Next Due Date (1 month from today)
    fun getDefaultNextDueDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    val todayDateStr = MutableStateFlow(getTodayDateString())

    // All Members
    val allMembers = repository.allMembers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All Payments History
    val allPayments = repository.allPayments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search and Filters for members list
    val searchQuery = MutableStateFlow("")
    val selectedBlock = MutableStateFlow("All") // All, Block A, Block B, Block C
    val memberStatusFilter = MutableStateFlow("Active") // All, Active, Left

    val filteredMembers: StateFlow<List<Member>> = combine(
        allMembers,
        searchQuery,
        selectedBlock,
        memberStatusFilter
    ) { members, query, block, status ->
        members.filter { member ->
            val matchesSearch = member.fullName.contains(query, ignoreCase = true) ||
                    member.mobileNumber.contains(query)
            val matchesBlock = if (block == "All") true else member.block == block
            val matchesStatus = when (status) {
                "Active" -> member.isActive
                "Left" -> !member.isActive
                else -> true
            }
            matchesSearch && matchesBlock && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current elected month for viewing income (Format: "YYYY-MM", e.g. "2026-05")
    val currentMonthYear = MutableStateFlow(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))

    // Multi-month selector options based on months present in database or general +/- 3 months
    val monthYearOptions: StateFlow<List<String>> = combine(
        allPayments,
        currentMonthYear
    ) { payments, current ->
        val months = payments.map { it.paymentDate.take(7) }.toMutableSet()
        months.add(current)
        // Add current, previous, and next month to guarantee options
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        months.add(sdf.format(cal.time))
        cal.add(Calendar.MONTH, -1)
        months.add(sdf.format(cal.time))
        cal.add(Calendar.MONTH, 2)
        months.add(sdf.format(cal.time))
        months.sortedDescending()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
    )

    // Income for the selected Month
    val selectedMonthIncome: StateFlow<Double> = combine(
        allPayments,
        currentMonthYear
    ) { payments, targetMonth ->
        payments.filter { it.paymentDate.startsWith(targetMonth) }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Pending Payments List (members whose nextDueDate is prior to today)
    val pendingMembers: StateFlow<List<Member>> = combine(
        allMembers,
        todayDateStr
    ) { members, today ->
        members.filter { it.nextDueDate < today }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // CRUD Members
    fun addMember(member: Member) {
        viewModelScope.launch {
            repository.insertMember(member)
        }
    }

    fun updateMember(member: Member) {
        viewModelScope.launch {
            repository.updateMember(member)
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun deleteMemberById(id: Int) {
        viewModelScope.launch {
            repository.deleteMemberById(id)
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
        }
    }

    // Payments Journal recording
    fun recordPayment(
        memberId: Int,
        amount: Double,
        datePaid: String,
        nextDueDate: String,
        method: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.recordRentPayment(
                memberId = memberId,
                amount = amount,
                datePaid = datePaid,
                nextDueDate = nextDueDate,
                method = method,
                notes = notes
            )
        }
    }

    // Export to Excel (CSV format)
    fun exportToExcel(context: Context): Uri? {
        val membersList = allMembers.value
        val paymentsList = allPayments.value

        val csvBuilder = StringBuilder()
        
        // 1. Members Sheet Section
        csvBuilder.append("--- LILLY COTTAGE(PG) RESIDENTS RECORD ---\n")
        csvBuilder.append("ID,Full Name,Father/Mother Name,Parent Phone,Gender,Age,DOB,Mobile,Email,Aadhaar Photo,DL,Address,City,State,Block,Rent Amount,Payment Date,Next Due Date,Payment Method,Food Included,Active Status,Join Date\n")
        
        for (m in membersList) {
            val foodText = if (m.foodIncluded) "Yes" else "No"
            val activeText = if (m.isActive) "Active" else "Left"
            csvBuilder.append("${m.id},")
                .append("\"${escapeCsv(m.fullName)}\",")
                .append("\"${escapeCsv(m.parentName)}\",")
                .append("\"${escapeCsv(m.parentPhone)}\",")
                .append("${m.gender},")
                .append("${m.age},")
                .append("${m.dateOfBirth},")
                .append("\"${escapeCsv(m.mobileNumber)}\",")
                .append("\"${escapeCsv(m.emailId)}\",")
                .append("\"${escapeCsv(m.aadhaarPhotoUri ?: "")}\",")
                .append("\"${escapeCsv(m.drivingLicenceNumber ?: "")}\",")
                .append("\"${escapeCsv(m.address)}\",")
                .append("\"${escapeCsv(m.city)}\",")
                .append("\"${escapeCsv(m.state)}\",")
                .append("${m.block},")
                .append("${m.rentAmount},")
                .append("${m.paymentDate},")
                .append("${m.nextDueDate},")
                .append("${m.paymentMethod},")
                .append("$foodText,")
                .append("$activeText,")
                .append("\"${escapeCsv(m.joinDate)}\"\n")
        }

        csvBuilder.append("\n\n")

        // 2. Payments Journal Section
        csvBuilder.append("--- LILLY COTTAGE(PG) PAYMENTS REGISTER ---\n")
        csvBuilder.append("Transaction ID,Member ID,Member Name,Block,Amount Paid,Payment Date,Method,Notes\n")
        for (p in paymentsList) {
            csvBuilder.append("${p.id},")
                .append("${p.memberId},")
                .append("\"${escapeCsv(p.memberName)}\",")
                .append("${p.memberBlock},")
                .append("${p.amount},")
                .append("${p.paymentDate},")
                .append("${p.paymentMethod},")
                .append("\"${escapeCsv(p.notes)}\"\n")
        }

        // Write to File in cache dir
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(exportDir, "Lilly_Cottage_Report_$dateStr.csv")
            
            val writer = FileWriter(exportFile)
            writer.write(csvBuilder.toString())
            writer.flush()
            writer.close()

            // Return Uri via FileProvider
            return FileProvider.getUriForFile(
                context,
                "com.aistudio.dormitoryadmin.fileprovider",
                exportFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"").replace("\n", " ")
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DormViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DormViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
