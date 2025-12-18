package com.example.utils

import com.example.config.TenantDatabaseConfig
import com.example.tenant.TenantContextHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }


suspend fun <T> tenantDbQuery(block: suspend () -> T): T {
    val currentTenant = TenantContextHolder.getTenant()
        ?: error("No tenant context found in coroutine scope")

    // The context MUST combine the dispatcher and the context element
    return newSuspendedTransaction(Dispatchers.IO + TenantContextHolder.threadLocal.asContextElement(currentTenant)) {
        val schema = currentTenant.schemaName
        // Ensure the search path is set for this specific transaction thread
        exec("SET search_path TO $schema")
        block()
    }
}

fun <T> tenantDbQuerySync(block: () -> T): T =
    transaction(TenantDatabaseConfig.getCurrentTenantDatabase()) {
        val schema = TenantContextHolder.getTenant()?.schemaName
            ?: error("No tenant schema")

        exec("SET search_path TO $schema")
        block()
    }
