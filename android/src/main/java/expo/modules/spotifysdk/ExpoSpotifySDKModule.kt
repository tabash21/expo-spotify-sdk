package expo.modules.spotifysdk

import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.CallResult
import com.spotify.protocol.client.ErrorCallback
import com.spotify.protocol.client.CallResult.ResultCallback

import android.content.pm.PackageManager
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.app.SpotifyNativeAuthUtil
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

import okhttp3.OkHttpClient
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Call
import okhttp3.Response

import java.io.IOException
import org.json.JSONObject

class SpotifyConfigOptions : Record {
  @Field
  val scopes: List<String> = emptyList()

  @Field
  val tokenSwapURL: String? = null

  @Field
  val tokenRefreshURL: String? = null
}

class ExpoSpotifySDKModule : Module() {

  private val requestCode = 2095
  private var requestConfig: SpotifyConfigOptions? = null
  private var authPromise: Promise? = null
  private var spotifyRemote: SpotifyAppRemote? = null
  private var clientId: String? = null
  private var redirectUri: String? = null
  private var accessToken: String? = null

  private val context
    get() = appContext.reactContext ?: throw Exceptions.ReactContextLost()
  private val currentActivity
    get() = appContext.currentActivity ?: throw Exceptions.MissingActivity()

  private fun fetchConfigFromManifest(): Pair<String?, String?> {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_META_DATA)
        val metaData = packageInfo.applicationInfo?.metaData
        Pair(metaData?.getString("spotifyClientId"), metaData?.getString("spotifyRedirectUri"))
    } catch (e: Exception) {
        Pair(null, null)
    }
  }

  override fun definition() = ModuleDefinition {

    Name("ExpoSpotifySDK")

    Function("isAvailable") {
      return@Function SpotifyNativeAuthUtil.isSpotifyInstalled(context)
    }

    AsyncFunction("authenticateAsync") { config: SpotifyConfigOptions, promise: Promise ->
      try {
        val packageInfo =
          context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_META_DATA)
        val applicationInfo = packageInfo.applicationInfo
        val metaData = applicationInfo?.metaData
        clientId = metaData?.getString("spotifyClientId")
        redirectUri = metaData?.getString("spotifyRedirectUri")

        requestConfig = config

        if (clientId == null || redirectUri == null) {
          promise.reject(
            "ERR_EXPO_SPOTIFY_SDK",
            "Missing Spotify configuration in AndroidManifest.xml. Ensure SPOTIFY_CLIENT_ID and SPOTIFY_REDIRECT_URI are set.",
            null
          )
          return@AsyncFunction
        }

        val responseType = if (config.tokenSwapURL != null || config.tokenRefreshURL != null) {
          AuthorizationResponse.Type.CODE
        } else {
          AuthorizationResponse.Type.TOKEN
        }

        val request = AuthorizationRequest.Builder(
          clientId,
          responseType,
          redirectUri
        )
          .setScopes(config.scopes.toTypedArray())
          .build()

        authPromise = promise
        AuthorizationClient.openLoginActivity(currentActivity, requestCode, request)

      } catch (e: PackageManager.NameNotFoundException) {
        promise.reject(
          "ERR_EXPO_SPOTIFY_SDK",
          "Missing Spotify configuration in AndroidManifest.xml",
          e
        )
      }
    }

    OnActivityResult { _, payload ->
      if (payload.requestCode == requestCode) {
        val authResponse = AuthorizationClient.getResponse(payload.resultCode, payload.data)

        when (authResponse.type) {
          AuthorizationResponse.Type.TOKEN -> {
            val expirationDate = System.currentTimeMillis() + authResponse.expiresIn * 1000

            authPromise?.resolve(
              mapOf(
                "accessToken" to authResponse.accessToken,
                "refreshToken" to null, // Spotify SDK does not return refresh token
                "expirationDate" to expirationDate,
                "scope" to requestConfig?.scopes
              )
            )
          }

          AuthorizationResponse.Type.CODE -> {
            val client = OkHttpClient()
            val requestBody = FormBody.Builder()
              .add("code", authResponse.code)
              .build()

            val request = Request.Builder()
              .url(requestConfig?.tokenSwapURL!!)
              .post(requestBody)
              .header("Content-Type", "application/x-www-form-urlencoded")
              .build()

            client.newCall(request).enqueue(object : Callback {
              override fun onFailure(call: Call, e: IOException) {
                authPromise?.reject("ERR_EXPO_SPOTIFY_SDK", e.message, e)
                authPromise = null
              }

              override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                  authPromise?.reject("ERR_EXPO_SPOTIFY_SDK", "Failed to swap code for token", null)
                  authPromise = null
                  return
                }

                response.body?.string()?.let { body ->
                  val json = JSONObject(body)
                  val accessToken = json.getString("access_token")
                  val refreshToken = json.getString("refresh_token")
                  val expiresIn = json.getInt("expires_in")
                  val scope = json.getString("scope")
                  val expirationDate = System.currentTimeMillis() + expiresIn * 1000

                  authPromise?.resolve(
                    mapOf(
                      "accessToken" to accessToken,
                      "refreshToken" to refreshToken,
                      "expirationDate" to expirationDate,
                      "scope" to scope.split(' ')
                    )
                  )
                } ?: run {
                  authPromise?.reject("ERR_EXPO_SPOTIFY_SDK", "Empty response body", null)
                }
                authPromise = null
              }
            })
          }

          AuthorizationResponse.Type.ERROR -> {
            authPromise?.reject("ERR_EXPO_SPOTIFY_SDK", authResponse.error, null)
            authPromise = null
          }

          else -> {
            authPromise?.reject("ERR_EXPO_SPOTIFY_SDK", "Unknown response type", null)
            authPromise = null
          }
        }
      }
    }

    AsyncFunction("connectToRemote") { token: String?, promise: Promise ->
        accessToken = token
        
        val (currentClientId, currentRedirectUri) = fetchConfigFromManifest()
        if (currentClientId == null || currentRedirectUri == null) {
            promise.reject("ERR_CONFIG", "Spotify Client ID or Redirect URI not found in Manifest", null)
            return@AsyncFunction
        }

        val connectionParams = ConnectionParams.Builder(currentClientId)
            .setRedirectUri(currentRedirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                // Store the remote for later use
                spotifyRemote = appRemote
                promise.resolve(true)
            }

            override fun onFailure(throwable: Throwable) {
                promise.reject("ERR_SPOTIFY_REMOTE", throwable.message, throwable)
            }
        })
    }

    AsyncFunction("disconnectFromRemote") { promise: Promise ->
        if (spotifyRemote != null) {
            SpotifyAppRemote.disconnect(spotifyRemote)
            spotifyRemote = null
        }
        promise.resolve(true)
    }

    AsyncFunction("playURI") { uri: String, promise: Promise ->
        val (currentClientId, currentRedirectUri) = fetchConfigFromManifest()
        
        if (currentClientId == null || currentRedirectUri == null) {
            promise.reject("ERR_CONFIG", "Spotify Client ID or Redirect URI not found in Manifest", null)
            return@AsyncFunction
        }

        val connectionParams = ConnectionParams.Builder(currentClientId)
            .setRedirectUri(currentRedirectUri)
            .showAuthView(true)
            .apply {
                if (accessToken != null) {
                    promise.reject("ERR_SPOTIFY_REMOTE", "Spotify access token is required", null) 
                    return@AsyncFunction
                }
            }
            .build()

        val connectionListener = object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyRemote = appRemote
                appRemote.playerApi.play(uri)
                    .setResultCallback {
                        promise.resolve(true)
                    }
                    .setErrorCallback { throwable ->
                        promise.reject("ERR_PLAYBACK", throwable.message, throwable)
                    }
            }

            override fun onFailure(throwable: Throwable) {
                promise.reject("ERR_SPOTIFY_REMOTE", throwable.message, throwable)
            }
        }

        val remote = spotifyRemote
        if (remote != null && remote.isConnected) {
            remote.playerApi.play(uri)
                .setResultCallback {
                    promise.resolve(true)
                }
                .setErrorCallback { throwable ->
                    promise.reject("ERR_PLAYBACK", throwable.message, throwable)
                }
        } else {
            SpotifyAppRemote.connect(context, connectionParams, connectionListener)
        }
    }

    AsyncFunction("pause") { promise: Promise ->
        val remote = spotifyRemote
        if (remote != null && remote.isConnected) {
            remote.playerApi.pause()
                .setResultCallback { promise.resolve(true) }
                .setErrorCallback { promise.reject("ERR_PAUSE", it.message, it) }
        } else {
            promise.reject("ERR_NOT_CONNECTED", "Spotify Remote not connected", null)
        }
    }

    AsyncFunction("resume") { promise: Promise ->
        val remote = spotifyRemote
        if (remote != null && remote.isConnected) {
            remote.playerApi.resume()
                .setResultCallback { promise.resolve(true) }
                .setErrorCallback { promise.reject("ERR_RESUME", it.message, it) }
        } else {
            promise.reject("ERR_NOT_CONNECTED", "Spotify Remote not connected", null)
        }
    }

    AsyncFunction("skipToNext") { promise: Promise ->
        val remote = spotifyRemote
        if (remote != null && remote.isConnected) {
            remote.playerApi.skipNext()
                .setResultCallback { promise.resolve(true) }
                .setErrorCallback { promise.reject("ERR_SKIP_NEXT", it.message, it) }
        } else {
            promise.reject("ERR_NOT_CONNECTED", "Spotify Remote not connected", null)
        }
    }

    AsyncFunction("skipToPrevious") { promise: Promise ->
        val remote = spotifyRemote
        if (remote != null && remote.isConnected) {
            remote.playerApi.skipPrevious()
                .setResultCallback { promise.resolve(true) }
                .setErrorCallback { promise.reject("ERR_SKIP_PREVIOUS", it.message, it) }
        } else {
            promise.reject("ERR_NOT_CONNECTED", "Spotify Remote not connected", null)
        }
    }
  }
}
