package com.ethiostat.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ethiostat.app.data.local.dao.*
import com.ethiostat.app.data.local.entity.*

@Database(
    entities = [
        BalancePackageEntity::class,
        TransactionEntity::class,
        AppConfigEntity::class,
        SmsLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EthioStatDatabase : RoomDatabase() {
    
    abstract fun balanceDao(): BalanceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun configDao(): ConfigDao
    abstract fun smsLogDao(): SmsLogDao
    
    companion object {
        @Volatile
        private var INSTANCE: EthioStatDatabase? = null
        
        fun getDatabase(context: Context): EthioStatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EthioStatDatabase::class.java,
                    "ethiostat_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
