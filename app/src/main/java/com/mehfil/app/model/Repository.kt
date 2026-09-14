package com.mehfil.app.model

import android.content.Context
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Date

/** Storage behind the app. Firestore in production; a JSON file on device when the build has no Firebase config. */
interface Repository {
    val isCloud: Boolean
    suspend fun profile(uid: String): VendorProfile?
    suspend fun saveProfile(uid: String, profile: VendorProfile)
    /** Starts streaming the vendor's data; `onChange` receives the full picture after every change. */
    fun observe(uid: String, onChange: (VendorData) -> Unit)
    fun stopObserving()
    suspend fun write(uid: String, ops: List<WriteOp>)
    suspend fun deleteEverything(uid: String)
}

// MARK: - Local repository (device only): documents kept as JSON in app storage.

class LocalRepository(context: Context) : Repository {
    override val isCloud = false
    private val root = File(context.filesDir, "mehfil")
    private val docs = mutableMapOf<String, MutableMap<String, Doc>>()
    private var onChange: ((VendorData) -> Unit)? = null

    private fun dir(uid: String) = File(root, uid).apply { mkdirs() }

    override suspend fun profile(uid: String): VendorProfile? = withContext(Dispatchers.IO) {
        val f = File(dir(uid), "profile.json")
        if (!f.exists()) null else Mapping.profile(fromJson(JSONObject(f.readText())))
    }
    override suspend fun saveProfile(uid: String, profile: VendorProfile) = withContext(Dispatchers.IO) {
        File(dir(uid), "profile.json").writeText(toJson(Mapping.of(profile)).toString())
    }
    override fun observe(uid: String, onChange: (VendorData) -> Unit) {
        this.onChange = onChange
        docs.clear()
        val f = File(dir(uid), "data.json")
        if (f.exists()) {
            val o = JSONObject(f.readText())
            for (c in Coll.all) {
                val arr = o.optJSONArray(c) ?: continue
                val m = mutableMapOf<String, Doc>()
                for (i in 0 until arr.length()) { val d = fromJson(arr.getJSONObject(i)); m[d["id"] as String] = d }
                docs[c] = m
            }
        }
        onChange(snapshot())
    }
    override fun stopObserving() { onChange = null }

    override suspend fun write(uid: String, ops: List<WriteOp>) {
        for (op in ops) {
            val m = docs.getOrPut(op.collection) { mutableMapOf() }
            when (op) { is WriteOp.Set -> m[op.id] = op.doc; is WriteOp.Delete -> m.remove(op.id) }
        }
        withContext(Dispatchers.IO) {
            val o = JSONObject()
            for ((c, m) in docs) o.put(c, JSONArray().apply { m.values.forEach { put(toJson(it)) } })
            File(dir(uid), "data.json").writeText(o.toString())
        }
        onChange?.invoke(snapshot())
    }
    override suspend fun deleteEverything(uid: String) = withContext(Dispatchers.IO) {
        dir(uid).deleteRecursively(); docs.clear(); Unit
    }

    private fun snapshot() = Mapping.vendorData(docs.mapValues { it.value.values.toList() })

    // JSON with dates as epoch millis (the mapping layer accepts Long for any date field).
    private fun toJson(d: Doc): JSONObject = JSONObject().apply { for ((k, v) in d) put(k, jsonValue(v)) }
    private fun jsonValue(v: Any?): Any? = when (v) {
        null -> JSONObject.NULL
        is Date -> v.time
        is com.google.firebase.Timestamp -> v.toDate().time
        is Map<*, *> -> JSONObject().apply { for ((k, x) in v) put(k as String, jsonValue(x)) }
        is List<*> -> JSONArray().apply { v.forEach { put(jsonValue(it)) } }
        else -> v
    }
    private fun fromJson(o: JSONObject): Doc = o.keys().asSequence().associateWith { k -> plainValue(o.get(k)) }
    private fun plainValue(v: Any?): Any? = when (v) {
        JSONObject.NULL, null -> null
        is JSONObject -> v.keys().asSequence().associateWith { plainValue(v.get(it)) }
        is JSONArray -> (0 until v.length()).map { plainValue(v.get(it)) }
        else -> v
    }
}

// MARK: - Cloud Firestore: vendors/{uid} + one subcollection per Coll.* name, offline persistence on.

class FirestoreRepository : Repository {
    override val isCloud = true
    private val db = FirebaseFirestore.getInstance().apply {
        firestoreSettings = firestoreSettings { setLocalCacheSettings(PersistentCacheSettings.newBuilder().build()) }
    }
    private val listeners = mutableListOf<ListenerRegistration>()
    private val docs = mutableMapOf<String, List<Doc>>()

    private fun vendor(uid: String): DocumentReference = db.collection("vendors").document(uid)

    override suspend fun profile(uid: String): VendorProfile? {
        val snap = vendor(uid).get().await()
        return if (snap.exists()) Mapping.profile(snap.data.orEmpty()) else null
    }
    override suspend fun saveProfile(uid: String, profile: VendorProfile) { vendor(uid).set(Mapping.of(profile)).await() }

    override fun observe(uid: String, onChange: (VendorData) -> Unit) {
        stopObserving(); docs.clear()
        for (c in Coll.all) {
            listeners += vendor(uid).collection(c).addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                docs[c] = snap.documents.map { it.data.orEmpty() + ("id" to it.id) }
                onChange(Mapping.vendorData(docs))
            }
        }
    }
    override fun stopObserving() { listeners.forEach { it.remove() }; listeners.clear() }

    override suspend fun write(uid: String, ops: List<WriteOp>) {
        for (chunk in ops.chunked(450)) {   // Firestore batches cap at 500 operations
            val batch = db.batch()
            for (op in chunk) {
                val ref = vendor(uid).collection(op.collection).document(op.id)
                when (op) { is WriteOp.Set -> batch.set(ref, op.doc); is WriteOp.Delete -> batch.delete(ref) }
            }
            batch.commit().await()
        }
    }
    override suspend fun deleteEverything(uid: String) {
        for (c in Coll.all) {
            val snap = vendor(uid).collection(c).get().await()
            for (chunk in snap.documents.chunked(450)) { val b = db.batch(); chunk.forEach { b.delete(it.reference) }; b.commit().await() }
        }
        vendor(uid).delete().await()
    }
}
