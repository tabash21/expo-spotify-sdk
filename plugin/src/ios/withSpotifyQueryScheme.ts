import { ConfigPlugin, withInfoPlist } from "@expo/config-plugins";
import { SpotifyPluginConfig } from "../types";

const SPOTIFY_SCHEME = "spotify";

export const withSpotifyQueryScheme: ConfigPlugin<SpotifyPluginConfig> = (
  config,
  props
) =>
  withInfoPlist(config, (config) => {
    // 1. Add to LSApplicationQueriesSchemes for isAvailable check
    if (!config.modResults.LSApplicationQueriesSchemes) {
      config.modResults.LSApplicationQueriesSchemes = [];
    }

    if (
      !config.modResults.LSApplicationQueriesSchemes.includes(SPOTIFY_SCHEME)
    ) {
      config.modResults.LSApplicationQueriesSchemes.push(SPOTIFY_SCHEME);
    }

    // 2. Add to CFBundleURLTypes for redirects
    if (!config.modResults.CFBundleURLTypes) {
      config.modResults.CFBundleURLTypes = [];
    }

    const hasScheme = config.modResults.CFBundleURLTypes.some((type: any) =>
      type.CFBundleURLSchemes?.includes(props.scheme)
    );

    if (!hasScheme) {
      config.modResults.CFBundleURLTypes.push({
        CFBundleURLSchemes: [props.scheme],
        CFBundleURLName: props.host, // Using host as the name
      });
    }

    return config;
  });
