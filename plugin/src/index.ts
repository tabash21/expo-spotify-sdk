import { ConfigPlugin } from "@expo/config-plugins";

import { withSpotifyAndroidAppBuildGradle } from "./android/withSpotifyAndroidAppBuildGradle";
import { withSpotifyQueryScheme } from "./ios/withSpotifyQueryScheme";

export const withSpotifySdkConfig: ConfigPlugin = (config) => {
  // Android specific: Build gradle fix (AAR) is always needed
  config = withSpotifyAndroidAppBuildGradle(config);

  // iOS specific: Query scheme for "spotify:" is always needed
  config = withSpotifyQueryScheme(config);

  return config;
};

export default withSpotifySdkConfig;
