import { ConfigPlugin, withAppBuildGradle } from "@expo/config-plugins";
import { SpotifyPluginConfig } from "../types";

export const withSpotifyAndroidAppBuildGradle: ConfigPlugin<SpotifyPluginConfig> = (
  config,
  props
) => {
  return withAppBuildGradle(config, (config) => {
    const defaultConfigPattern = /(defaultConfig\s*{[\s\S]*?)(})/s;
    const manifestPlaceholders = `
        manifestPlaceholders += [
          redirectSchemeName: "${props.scheme}",
          redirectHostName: "${props.host}"
        ]
    `;

    if (defaultConfigPattern.test(config.modResults.contents)) {
      // If the defaultConfig block exists, add the manifestPlaceholders to it
      config.modResults.contents = config.modResults.contents.replace(
        defaultConfigPattern,
        `$1${manifestPlaceholders}$2`
      );
    } else {
      // If the defaultConfig block doesn't exist, add it to the android block
      config.modResults.contents += `
android {
    defaultConfig {
        ${manifestPlaceholders}
    }
}`;
    }

    // Add local AAR support for the main app
    const aarFix = `
// Added by @tabash21/expo-spotify-sdk to fix local AAR bundling
repositories {
    flatDir {
        dirs project(':tabash21-expo-spotify-sdk').file('libs')
    }
}

dependencies {
    implementation(name: 'spotify-app-remote-release-0.8.0', ext: 'aar')
}
`;
    // Append to the end of the file
    config.modResults.contents += aarFix;

    return config;
  });
};
