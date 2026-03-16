import ExpoModulesCore
import SpotifyiOS
import PromiseKit

enum SessionManagerError: Error {
    case notInitialized
    case invalidConfiguration
}

final class ExpoSpotifySessionManager: NSObject {
    weak var module: ExpoSpotifySDKModule?
    var authPromiseSeal: Resolver<SPTSession>?
    var connectPromiseSeal: Resolver<Bool>?
    var currentSession: SPTSession?
    
    lazy var appRemote: SPTAppRemote = {
        let appRemote = SPTAppRemote(configuration: configuration!, logLevel: .debug)
        appRemote.delegate = self
        return appRemote
    }()

    static let shared = ExpoSpotifySessionManager()

    var sessionManager: SPTSessionManager?

    func setConfiguration(clientID: String, redirectUri: String) {
        guard let redirectURL = URL(string: redirectUri) else { return }
        let newConfig = SPTConfiguration(clientID: clientID, redirectURL: redirectURL)
        
        // Preserve URLs if they were already set
        if let oldConfig = self.configuration {
            newConfig.tokenSwapURL = oldConfig.tokenSwapURL
            newConfig.tokenRefreshURL = oldConfig.tokenRefreshURL
        }
        
        self.configuration = newConfig
        // Re-initialize appRemote with new configuration
        self.appRemote = SPTAppRemote(configuration: newConfig, logLevel: .debug)
        self.appRemote.delegate = self
    }


    func authenticate(scopes: [String], tokenSwapURL: String?, tokenRefreshURL: String?, clientID: String?, redirectUri: String?) -> PromiseKit.Promise<SPTSession> {
        return Promise { seal in
            guard let finalClientID = clientID,
                  let finalRedirectUri = redirectUri,
                  let finalRedirectURL = URL(string: finalRedirectUri) else {
                NSLog("Invalid Spotify configuration. Provide clientID and redirectUri.")
                seal.reject(SessionManagerError.invalidConfiguration)
                return
            }
            
            let configuration = SPTConfiguration(clientID: finalClientID, redirectURL: finalRedirectURL)

            if (tokenSwapURL != nil) {
                configuration.tokenSwapURL = URL(string: tokenSwapURL ?? "")
            }

            if (tokenRefreshURL != nil) {
                configuration.tokenRefreshURL = URL(string: tokenRefreshURL ?? "")
            }

            self.authPromiseSeal = seal
            self.configuration = configuration
            self.sessionManager = SPTSessionManager(configuration: configuration, delegate: self)

            DispatchQueue.main.sync {
                sessionManager?.initiateSession(with: SPTScopeSerializer.deserializeScopes(scopes), options: .default, campaign: nil)
            }
        }
    }

    func connectRemote() -> PromiseKit.Promise<Bool> {
        return Promise { seal in
            guard configuration != nil else {
                seal.reject(SessionManagerError.notInitialized)
                return
            }
            self.connectPromiseSeal = seal
            appRemote.connect()
        }
    }

    func spotifyAppInstalled() -> Bool {
        guard let sessionManager = sessionManager else {
            NSLog("SPTSessionManager not initialized")
            return false
        }

        var isInstalled = false

        DispatchQueue.main.sync {
            isInstalled = sessionManager.isSpotifyAppInstalled
        }

        return isInstalled
    }
}
