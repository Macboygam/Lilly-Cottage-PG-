package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY fullName ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    fun getMemberById(id: Int): Flow<Member?>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberByIdDirect(id: Int): Member?

    @Query("SELECT * FROM members WHERE fullName LIKE '%' || :query || '%' ORDER BY fullName ASC")
    fun searchMembers(query: String): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE block = :block ORDER BY fullName ASC")
    fun getMembersByBlock(block: String): Flow<List<Member>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMemberById(id: Int)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC, id DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE paymentDate LIKE :yearMonth || '%'")
    fun getPaymentsByMonth(yearMonth: String): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Delete
    suspend fun deletePayment(payment: Payment)
}

@Database(entities = [Member::class, Payment::class], version = 3, exportSchema = false)
abstract class DormDatabase : RoomDatabase() {
    abstract val memberDao: MemberDao
    abstract val paymentDao: PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: DormDatabase? = null

        fun getDatabase(context: Context): DormDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DormDatabase::class.java,
                    "dorm_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
