# expo-spotify-sdk

An Expo Package for the native iOS and Android Spotify SDK

## Supported Features

- **Authentication**
- **Remote Playback & Control**
  - Connect to/Disconnect from Spotify App Remote
  - Wake up spotify app (automatic app opening in background if needed)
  - Play, Pause, Resume, Skip Next, and Skip Previous controls
  - Get current player state

## Installation

```sh
npm install @tabash21/expo-spotify-sdk
```

## Configuration (Required)

### 1. URL Scheme

You **must** register a URL scheme for your app in `app.json` so Spotify can redirect back after authentication.

```json
{
  "expo": {
    "scheme": "my-spotify-app",
    ...
  }
}
```

### 2. Expo Plugin

The Expo plugin is **required** to handle native dependencies.
Add the plugin to your `app.json` file:

```json
"plugins": [
  "@tabash21/expo-spotify-sdk"
]
```

## Spotify Dashboard Configuration

### 1. Client ID and Redirect URI

You **must** register a Client ID and Redirect URI for your app in the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).

### Android

For the **App Remote** (playback controls) to work on Android, you **must** also configure your Android Package and Fingerprints in the Spotify Dashboard:

1. Go to your app in the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
2. Click **Edit Details**.
3. Under **Android Packages**, add:
   - **Package Name**: Your Android package name (e.g., `com.yourcompany.yourapp`).
   - **SHA1 Fingerprint**: Your app's SHA1 fingerprint.
     - For development, get it with: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
     - For Expo EAS builds: `eas credentials`

### iOS

For the **App Remote** (playback controls) to work on iOS, you **must** also configure your iOS Bundle ID in the Spotify Dashboard:

1. Go to your app in the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
2. Click **Edit Details**.
3. Under **iOS Bundle IDs**, add:
   - **Bundle ID**: Your iOS bundle ID (e.g., `com.yourcompany.yourapp`).

---

## Authentication Options: 

In order to get access token to use spotify remote, you have two options:

1. Use the authenticateAsync method to get access token.
2. Use external package to get access token.

---

## Base API Reference

```typescript
isAvailable(): boolean`
```

Determines if the Spotify app is installed on the target device.

## Remote Control API

These methods are available under the `Remote` object.

```typescript
Remote.connectToRemote(config?: SpotifyRemoteOptions): Promise<boolean>`
```

#### Parameters - SpotifyRemoteOptions

- `accessToken`: <string> The access token.
- `clientId`: <string> The client ID.
- `redirectUri`: <string> The redirect URI.

Connects to the Spotify App Remote. If an `accessToken`, `clientId`, and `redirectUri` are provided, it will be used for the connection. If not, it will try to use the current session's token.

```typescript
Remote.disconnectFromRemote(): Promise<boolean>`
```

Disconnects from the Spotify App Remote.

```typescript
Remote.playURI(uri: string): Promise<boolean>`
```

Plays a Spotify URI (track, album, playlist). If the app is not connected to the Remote, it will attempt to "wake up" the Spotify app (on iOS, this involves opening the Spotify app; on Android, it can often happen in the background).

```typescript
Remote.pause(): Promise<boolean>`
```

Pauses current playback.

```typescript
Remote.resume(): Promise<boolean>`
```

Resumes current playback.

```typescript
Remote.skipToNext(): Promise<boolean>`
```

Skips to the next track in the queue.

```typescript
Remote.skipToPrevious(): Promise<boolean>`
```

Skips to the previous track in the queue.

```typescript
Remote.getPlayerState(): Promise<SpotifyPlayerState>`
```

Returns the current player state.

#### Returns - SpotifyPlayerState

- `isPaused`: <boolean> Whether the track is playing.
- `track`: <SpotifyTrack> The current track.
- `playbackPosition`: <number> The current progress in seconds.
- `playbackSpeed`: <number> The total duration in seconds.
- `playbackOptions`: <SpotifyPlaybackOptions> The playback options (shuffling, repeat mode).

---

## Authentication API

```typescript
authenticateAsync(config: SpotifyConfig): Promise<SpotifySession>`
```

Starts the authentication process. Requires an array of OAuth scopes. If the Spotify app is installed on the target device it will interact directly with it, otherwise it will open a web view to authenticate with the Spotify website.

#### Parameters - SpotifyConfig

- `scopes`: <string[]> Array of OAuth scopes (Required).
- `clientID`: <string> Your Spotify Client ID (Required).
- `redirectUri`: <string> Your registered Redirect URI (Required). Must match the scheme registered in `app.json` (e.g., `my-spotify-app://authenticate`).
- `tokenSwapURL` (optional): The URL for swapping auth code for access token.
- `tokenRefreshURL` (optional): The URL for renewing access token.

#### Returns - SpotifySession

- `accessToken`: <string> The access token.
- `refreshToken`: <string> The refresh token.
- `expirationDate`: <number> The expiration date of the access token.
- `scopes`: <string[]> The scopes that were granted.

**Note for Android:** If not providing a token swap or refresh URL, the Spotify session response access token will expire after 60 minutes and will not include a refresh token. This is due to a limitation in the Android Spotify SDK. It's recommended to implement a token swap endpoint for this reason.

### Parameters

- `tokenSwapURL` (optional): &lt;string&gt; The URL to use for attempting to swap an authorization code for an access token
- `tokenRefreshURL` (optional): &lt;string&gt; The URL to use for attempting to renew an access token with a refresh token
- `scopes`: An array of OAuth scopes that declare how your app wants to access a user's account. See [Spotify Scopes](https://developer.spotify.com/web-api/using-scopes/) for more information.

**Note:** The following scopes are not available to Expo Spotify SDK:

- user-read-playback-position
- user-soa-link
- user-soa-unlink
- user-manage-entitlements
- user-manage-partner
- user-create-partner

---

### Types

```typescript
interface SpotifyConfig {
  scopes: SpotifyScope[];
  tokenSwapURL?: string;
  tokenRefreshURL?: string;
  clientID: string;
  redirectUri: string;
}

interface SpotifySession {
  accessToken: string;
  refreshToken: string | null;
  expirationDate: number;
  scopes: SpotifyScopes[];
}


interface SpotifyRemoteOptions {
  accessToken?: string;
  clientID?: string;
  redirectUri?: string;
}

type SpotifyScopes =
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

interface SpotifyTrack {
  name: string;
  uri: string;
  artist: string;
  album: string;
  duration: number;
}

interface SpotifyPlayerState {
  isPaused: boolean;
  track: SpotifyTrack | null;
  playbackPosition: number;
  playbackSpeed: number;
  playbackOptions: {
    isShuffling: boolean;
    repeatMode: number;
  };
}

```

## Token Swap Example

1. Open the `server.js` file and add your client details:

```javascript
const CLIENT_ID = "<your-client-id>";
const CLIENT_SECRET = "<your-client-secret>";
```

These values can be found in your [Spotify Developer Dashboard](https://developer.spotify.com/dashboard). You will need an existing Spotify app for this.

2. Run the server

```sh
node server.js
```

3. Set the `tokenSwapURL` value in your `authenticateAsync` call:

```javascript
const session = await authenticateAsync({
  tokenSwapURL: "http://192.168.1.120:3000/swap",
  scopes: [
    ...
  ]
});
```

All authentication requests will now be sent through the token swap server.

## License

MIT
