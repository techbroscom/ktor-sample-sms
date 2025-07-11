package com.example.tenant

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TenantContext(
    val id: String,
    val name: String,
    val schemaName: String
)

object TenantContextHolder {
    private val tenantContext = ThreadLocal<TenantContext>()

    fun setTenant(context: TenantContext) {
        tenantContext.set(context)
    }

    fun getTenant(): TenantContext? {
        return tenantContext.get()
    }

    fun clear() {
        tenantContext.remove()
    }
}