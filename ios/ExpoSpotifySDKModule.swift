import ExpoModulesCore
import SpotifyiOS

public class ExpoSpotifySDKModule: Module {

    public func definition() -> ModuleDefinition {

        let spotifySession = ExpoSpotifySessionManager.shared

        Name("ExpoSpotifySDK")

        Function("isAvailable") {
            return spotifySession.spotifyAppInstalled()
        }

        AsyncFunction("authenticateAsync") { (config: [String: Any], promise: Promise) in
            guard let scopes = config["scopes"] as? [String] else {
                promise.reject("INVALID_CONFIG", "Invalid SpotifyConfig object")
                return
            }

            let tokenSwapURL = config["tokenSwapURL"] as? String
            let tokenRefreshURL = config["tokenRefreshURL"] as? String

            spotifySession.authenticate(scopes: scopes, tokenSwapURL: tokenSwapURL, tokenRefreshURL: tokenRefreshURL).done { session in
                promise.resolve([
                    "accessToken": session.accessToken,
                    "refreshToken": session.refreshToken,
                    "expirationDate": Int(session.expirationDate.timeIntervalSince1970 * 1000),
                    "scopes": SPTScopeSerializer.serializeScopes(session.scope)
                ])
            }.catch { error in
                promise.reject(error)
            }
        }

        AsyncFunction("connectToRemote") { (accessToken: String?, promise: Promise) in
            if let accessToken = accessToken {
                spotifySession.appRemote.connectionParameters.accessToken = accessToken
            } else if let session = spotifySession.currentSession {
                spotifySession.appRemote.connectionParameters.accessToken = session.accessToken
            } else {
                promise.reject("NO_SESSION", "No active Spotify session or access token provided")
                return
            }

            spotifySession.connectRemote()
            promise.resolve(true)
        }

        AsyncFunction("playURI") { (uri: String, promise: Promise) in
            if spotifySession.appRemote.isConnected {
                spotifySession.appRemote.playerAPI?.play(uri) { _, error in
                    if let error = error {
                        promise.reject("ERR_SPOTIFY_REMOTE", error.localizedDescription)
                    } else {
                        promise.resolve(true)
                    }
                }
            } else {
                // If not connected, use authorizeAndPlayURI which handles waking up/opening the app if needed
                spotifySession.appRemote.authorizeAndPlayURI(uri)
                promise.resolve(true)
            }
        }

        AsyncFunction("disconnectFromRemote") { promise: Promise in
            spotifySession.appRemote.disconnect()
            promise.resolve(true)
        }

        AsyncFunction("pause") { (promise: Promise) in
            if spotifySession.appRemote.isConnected {
                spotifySession.appRemote.playerAPI?.pause { _, error in
                    if let error = error {
                        promise.reject("ERR_SPOTIFY_REMOTE", error.localizedDescription)
                    } else {
                        promise.resolve(true)
                    }
                }
            } else {
                promise.reject("ERR_NOT_CONNECTED", "Spotify Remote not connected")
            }
        }

        AsyncFunction("resume") { (promise: Promise) in
            if spotifySession.appRemote.isConnected {
                spotifySession.appRemote.playerAPI?.resume { _, error in
                    if let error = error {
                        promise.reject("ERR_SPOTIFY_REMOTE", error.localizedDescription)
                    } else {
                        promise.resolve(true)
                    }
                }
            } else {
                promise.reject("ERR_NOT_CONNECTED", "Spotify Remote not connected")
            }
        }

        AsyncFunction("skipToNext") { (promise: Promise) in
            if spotifySession.appRemote.isConnected {
                spotifySession.appRemote.playerAPI?.skip(toNext: { _, error in
                    if let error = error {
                        promise.reject("ERR_SPOTIFY_REMOTE", error.localizedDescription)
                    } else {
                        promise.resolve(true)
                    }
                })
            } else {
                promise.reject("ERR_NOT_CONNECTED", "Spotify Remote not connected")
            }
        }

        AsyncFunction("skipToPrevious") { (promise: Promise) in
            if spotifySession.appRemote.isConnected {
                spotifySession.appRemote.playerAPI?.skip(toPrevious: { _, error in
                    if let error = error {
                        promise.reject("ERR_SPOTIFY_REMOTE", error.localizedDescription)
                    } else {
                        promise.resolve(true)
                    }
                })
            } else {
                promise.reject("ERR_NOT_CONNECTED", "Spotify Remote not connected")
            }
        }
    }
}
