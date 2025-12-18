package com.example.tenant

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TenantContext(
    val id: String,
    val name: String,
    val subDomain: String,
    val schemaName: String
)

object TenantContextHolder {
    val threadLocal = ThreadLocal<TenantContext>()

    fun setTenant(tenant: TenantContext) {
        threadLocal.set(tenant)
    }

    fun getTenant(): TenantContext? = threadLocal.get()

    fun clear() {
        threadLocal.remove()
    }
}