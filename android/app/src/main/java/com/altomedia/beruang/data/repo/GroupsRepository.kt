package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Group
import com.altomedia.beruang.data.model.GroupMember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupsRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val groups = db.collection("groups")
    private val members = db.collection("group_members")
    private fun uid() = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    suspend fun allGroups(): List<Group> {
        val snap = groups.get().await()
        return snap.documents.mapNotNull { it.toObject(Group::class.java)?.copy(id = it.id) }
    }

    suspend fun myMemberships(): List<GroupMember> {
        val snap = members.whereEqualTo("user_id", uid()).get().await()
        return snap.documents.mapNotNull { it.toObject(GroupMember::class.java)?.copy(id = it.id) }
    }

    suspend fun create(name: String, description: String?): Group {
        val g = Group(name = name, description = description?.ifBlank { null }, created_by = uid())
        val ref = groups.add(g).await()
        members.add(GroupMember(group_id = ref.id, user_id = uid(), role = "admin")).await()
        return g.copy(id = ref.id)
    }

    suspend fun join(groupId: String) {
        members.add(GroupMember(group_id = groupId, user_id = uid(), role = "member")).await()
    }

    suspend fun leave(groupId: String) {
        val snap = members.whereEqualTo("group_id", groupId).whereEqualTo("user_id", uid()).limit(1).get().await()
        snap.documents.firstOrNull()?.reference?.delete()?.await()
    }
}
