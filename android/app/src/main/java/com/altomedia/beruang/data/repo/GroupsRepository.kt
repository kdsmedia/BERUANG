package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Group
import com.altomedia.beruang.data.model.GroupMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupsRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val auth: Auth get() = client.auth
    private val postgrest: Postgrest get() = client.postgrest
    private val groups get() = postgrest.from("groups")
    private val members get() = postgrest.from("group_members")
    private fun uid() = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")

    suspend fun allGroups(): List<Group> =
        runCatching { groups.select { }.decodeList<Group>() }.getOrDefault(emptyList())

    suspend fun myMemberships(): List<GroupMember> =
        runCatching {
            members.select { filter { eq("user_id", uid()) } }.decodeList<GroupMember>()
        }.getOrDefault(emptyList())

    suspend fun create(name: String, description: String?): Group {
        val g = Group(name = name, description = description?.ifBlank { null }, created_by = uid())
        val created = groups.insert(g) { select() }.decodeSingle<Group>()
        members.insert(GroupMember(group_id = created.id, user_id = uid(), role = "admin"))
        return created
    }

    suspend fun join(groupId: String) {
        members.insert(GroupMember(group_id = groupId, user_id = uid(), role = "member"))
    }

    suspend fun leave(groupId: String) {
        val rows = runCatching {
            members.select { filter { eq("group_id", groupId); eq("user_id", uid()) } }.decodeList<GroupMember>()
        }.getOrDefault(emptyList())
        rows.forEach { m -> runCatching { members.delete { filter { eq("id", m.id) } } } }
    }
}
