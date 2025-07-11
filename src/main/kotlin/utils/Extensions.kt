package com.example.utils

import com.example.config.TenantDatabaseConfig
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

suspend fun <T> tenantDbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(db = TenantDatabaseConfig.getCurrentTenantDatabase()) { block() }

fun <T> tenantDbQuerySync(block: () -> T): T =
    transaction(TenantDatabaseConfig.getCurrentTenantDatabase()) { block() }