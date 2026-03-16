export interface SpotifySession {
  accessToken: string;
  refreshToken: string | null;
  expirationDate: number;
  scopes: SpotifyScopes[];
}

export interface SpotifyConfig {
  scopes: SpotifyScopes[];
  tokenSwapURL?: string;
  tokenRefreshURL?: string;
  clientID: string;
  redirectUri: string;
}

export interface SpotifyRemoteOptions {
  accessToken?: string;
  clientID?: string;
  redirectUri?: string;
}

export type SpotifyScopes =
  | "ugc-image-upload"
  | "user-read-playback-state"
  | "user-modify-playback-state"
  | "user-read-currently-playing"
  | "app-remote-control"
  | "streaming"
  | "playlist-read-private"
  | "playlist-read-collaborative"
  | "playlist-modify-private"
  | "playlist-modify-public"
  | "user-follow-modify"
  | "user-follow-read"
  | "user-top-read"
  | "user-read-recently-played"
  | "user-library-modify"
  | "user-library-read"
  | "user-read-email"
  | "user-read-private";

export interface SpotifyTrack {
  name: string;
  uri: string;
  artist: string;
  album: string;
  duration: number;
}

export interface SpotifyPlayerState {
  isPaused: boolean;
  track: SpotifyTrack | null;
  playbackPosition: number;
  playbackSpeed: number;
  playbackOptions: {
    isShuffling: boolean;
    repeatMode: number;
  };
}
