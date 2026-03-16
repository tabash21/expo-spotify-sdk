import { ConfigPlugin } from "@expo/config-plugins";

import { withSpotifyAndroidAppBuildGradle } from "./android/withSpotifyAndroidAppBuildGradle";
import { withSpotifyQueryScheme } from "./ios/withSpotifyQueryScheme";
import { SpotifyPluginConfig } from "./types";

export const withSpotifySdkConfig: ConfigPlugin<SpotifyPluginConfig> = (
  config,
  props
) => {
  // Android specific: Build gradle fix (AAR) and manifest placeholders
  config = withSpotifyAndroidAppBuildGradle(config, props);

  // iOS specific: Query scheme for "spotify:" is always needed, and register the specific app scheme
  config = withSpotifyQueryScheme(config, props);

  return config;
};

export default withSpotifySdkConfig;
