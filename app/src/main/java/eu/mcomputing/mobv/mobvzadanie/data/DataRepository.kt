package eu.mcomputing.mobv.mobvzadanie.data

import android.content.Context
import androidx.annotation.RequiresApi
import eu.mcomputing.mobv.mobvzadanie.data.api.ApiService
import eu.mcomputing.mobv.mobvzadanie.data.api.model.GeofenceUpdateRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.UserLoginRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.UserRegistrationRequest
import eu.mcomputing.mobv.mobvzadanie.data.db.AppRoomDatabase
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.GeofenceEntity
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.UserEntity
import eu.mcomputing.mobv.mobvzadanie.data.model.User
import venaka.bioapp.data.db.LocalCache
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.HexFormat
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class DataRepository private constructor(
    private val service: ApiService,
    private val cache: LocalCache
) {
    companion object {
        const val TAG = "DataRepository"

        @Volatile
        private var INSTANCE: DataRepository? = null
        private val lock = Any()

        fun getInstance(context: Context): DataRepository =
            INSTANCE ?: synchronized(lock) {
                INSTANCE
                    ?: DataRepository(
                        ApiService.create(context),
                        LocalCache(AppRoomDatabase.getInstance(context).appDao())
                    ).also { INSTANCE = it }
            }
    }


    suspend fun apiRegisterUser(username: String, email: String, password: String): Pair<String, User?> {
        if (username.isEmpty()) {
            return Pair("Username can not be empty! Please provide it.", null)
        }
        if (email.isEmpty()) {
            return Pair("Email can not be empty! Please provide it.", null)
        }
        if (password.isEmpty()) {
            return Pair("Password can not be empty! Please provide it.", null)
        }
        try {
            val hashedPw = password.toSHA256()
            val response = service.registerUser(UserRegistrationRequest(username, email, hashedPw))
            if (response.isSuccessful) {
                response.body()?.let { json_response ->
                    return Pair(
                        "",
                        User(
                            username,
                            email,
                            json_response.uid,
                            json_response.access,
                            json_response.refresh
                        )
                    )
                }
            }
            return Pair("Failed to create user.", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to create user.", null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to create user.", null)
    }


    suspend fun apiLoginUser(username: String, password: String): Pair<String, User?> {
        if (username.isEmpty()) {
            return Pair("Username can not be empty! Please provide it.", null)
        }
        if (password.isEmpty()) {
            return Pair("Password can not be empty! Please provide it.", null)
        }
        try {
            val hashedPw = password.toSHA256()
            val response = service.loginUser(UserLoginRequest(username, hashedPw))
            if (response.isSuccessful) {
                response.body()?.let { json_response ->
                    if (json_response.uid == "-1") {
                        return Pair("Wrong password or username.", null)
                    }
                    return Pair(
                        "",
                        User(
                            username,
                            "",
                            json_response.uid,
                            json_response.access,
                            json_response.refresh
                        )
                    )
                }
            }
            return Pair("Failed to login user.", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to login user.", null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to login user.", null)
    }

    suspend fun apiGetUser(uid: String): Pair<String, User?> {
        try {
            val response = service.getUser(uid)

            if (response.isSuccessful) {
                response.body()?.let {
                    val user = User(it.name, "", it.id, "", "", it.photo)
                    cache.insertUserItems(
                        listOf(
                            UserEntity(
                                user.id, user.username, "", 48.13393075744861, 17.202611211938684, 0.0, ""
                            )
                        )
                    )
                    return Pair("", user)
                }
            }

            return Pair("Failed to load user", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to load user.", null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to load user.", null)
    }

    suspend fun apiListGeofence(): String {
        try {
            val response = service.listGeofence()

            if (response.isSuccessful) {
                response.body()?.list?.let {
                    val users = it.map {
                        UserEntity(
                            it.uid, it.name, it.updated,
                            0.0,0.0, it.radius, it.photo
                        )
                    }
                    cache.insertUserItems(users)
                    return ""
                }
            }

            return "Failed to load users"
        } catch (ex: IOException) {
            ex.printStackTrace()
            return "Check internet connection. Failed to load users."
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "Fatal error. Failed to load users."
    }

    fun getUsers() = cache.getUsers()

    suspend fun getUsersList() = cache.getUsersList()

    suspend fun insertGeofence(item: GeofenceEntity) {
        cache.insertGeofence(item)
        try {
            val response =
                service.updateGeofence(GeofenceUpdateRequest(item.lat, item.lon, item.radius))

            if (response.isSuccessful) {
                response.body()?.let {

                    item.uploaded = true
                    cache.insertGeofence(item)
                    return
                }
            }

            return
        } catch (ex: IOException) {
            ex.printStackTrace()
            return
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    suspend fun removeGeofence() {
        try {
            val response = service.deleteGeofence()

            if (response.isSuccessful) {
                response.body()?.let {
                    return
                }
            }

            return
        } catch (ex: IOException) {
            ex.printStackTrace()
            return
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun String.toSHA256(): String{
        val HEX_CHARS = "0123456789ABCDEF"
        val digest = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
        return digest.joinToString(
            separator = "",
            transform = { a -> String(
                charArrayOf(
                    HEX_CHARS[a.toInt() shr 4 and 0x0f],
                    HEX_CHARS[a.toInt() and 0x0f]
                )
            )

            }
        )
    }
}