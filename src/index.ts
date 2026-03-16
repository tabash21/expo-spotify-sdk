import {
  SpotifyConfig,
  SpotifySession,
  SpotifyRemoteOptions,
  SpotifyPlayerState,
  SpotifyScopes,
  SpotifyTrack,
} from "./ExpoSpotifySDK.types";
import ExpoSpotifySDKModule from "./ExpoSpotifySDKModule";

function isAvailable(): boolean {
  return ExpoSpotifySDKModule.isAvailable();
}

function authenticateAsync(config: SpotifyConfig): Promise<SpotifySession> {
  if (!config.scopes || config.scopes?.length === 0) {
    throw new Error("scopes are required");
  }

  if (!config.clientID) {
    throw new Error("clientID is required");
  }

  if (!config.redirectUri) {
    throw new Error("redirectUri is required");
  }

  return ExpoSpotifySDKModule.authenticateAsync(config);
}

function connectToRemote(config?: SpotifyRemoteOptions): Promise<boolean> {
  return ExpoSpotifySDKModule.connectToRemote(config);
}

function disconnectFromRemote(): Promise<boolean> {
  return ExpoSpotifySDKModule.disconnectFromRemote();
}

function playURI(uri: string): Promise<boolean> {
  return ExpoSpotifySDKModule.playURI(uri);
}

function pause(): Promise<boolean> {
  return ExpoSpotifySDKModule.pause();
}

function resume(): Promise<boolean> {
  return ExpoSpotifySDKModule.resume();
}

function skipToNext(): Promise<boolean> {
  return ExpoSpotifySDKModule.skipToNext();
}

function skipToPrevious(): Promise<boolean> {
  return ExpoSpotifySDKModule.skipToPrevious();
}

function getPlayerState(): Promise<SpotifyPlayerState> {
  return ExpoSpotifySDKModule.getPlayerState();
}

const Remote = {
  connectToRemote,
  disconnectFromRemote,
  playURI,
  pause,
  resume,
  skipToNext,
  skipToPrevious,
  getPlayerState,
};

export {
  isAvailable,
  authenticateAsync,
  Remote,
  SpotifyConfig,
  SpotifySession,
  SpotifyRemoteOptions,
  SpotifyPlayerState,
  SpotifyScopes,
  SpotifyTrack,
};
