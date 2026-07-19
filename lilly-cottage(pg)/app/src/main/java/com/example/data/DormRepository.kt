package com.example.data

import kotlinx.coroutines.flow.Flow

class DormRepository(private val db: DormDatabase) {
    private val memberDao = db.memberDao
    private val paymentDao = db.paymentDao

    // Members
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()

    fun getMemberById(id: Int): Flow<Member?> = memberDao.getMemberById(id)

    suspend fun getMemberByIdDirect(id: Int): Member? = memberDao.getMemberByIdDirect(id)

    fun searchMembers(query: String): Flow<List<Member>> = memberDao.searchMembers(query)

    fun getMembersByBlock(block: String): Flow<List<Member>> = memberDao.getMembersByBlock(block)

    suspend fun insertMember(member: Member): Long = memberDao.insertMember(member)

    suspend fun updateMember(member: Member) = memberDao.updateMember(member)

    suspend fun deleteMember(member: Member) = memberDao.deleteMember(member)

    suspend fun deleteMemberById(id: Int) = memberDao.deleteMemberById(id)

    // Payments
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments()

    fun getPaymentsByMonth(yearMonth: String): Flow<List<Payment>> = paymentDao.getPaymentsByMonth(yearMonth)

    suspend fun insertPayment(payment: Payment): Long = paymentDao.insertPayment(payment)

    suspend fun deletePayment(payment: Payment) = paymentDao.deletePayment(payment)

    // Synchronize payment and updates member
    suspend fun recordRentPayment(
        memberId: Int,
        amount: Double,
        datePaid: String,
        nextDueDate: String,
        method: String,
        notes: String
    ): Boolean {
        val member = memberDao.getMemberByIdDirect(memberId) ?: return false
        val updatedMember = member.copy(
            paymentDate = datePaid,
            nextDueDate = nextDueDate,
            paymentMethod = method
        )
        memberDao.updateMember(updatedMember)
        
        val payment = Payment(
            memberId = memberId,
            memberName = member.fullName,
            memberBlock = member.block,
            amount = amount,
            paymentDate = datePaid,
            paymentMethod = method,
            notes = notes
        )
        paymentDao.insertPayment(payment)
        return true
    }
}
