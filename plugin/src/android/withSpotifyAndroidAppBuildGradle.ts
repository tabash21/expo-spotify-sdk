import { ConfigPlugin, withAppBuildGradle } from "@expo/config-plugins";



export const withSpotifyAndroidAppBuildGradle: ConfigPlugin = (config) => {
  return withAppBuildGradle(config, (config) => {
    // Add local AAR support for the main app
    const aarFix = `
// Added by @tabash21/expo-spotify-sdk to fix local AAR bundling
repositories {
    flatDir {
        dirs project(':tabash21-expo-spotify-sdk').file('libs')
    }
}

android {
    defaultConfig {
        manifestPlaceholders += [
            "redirectSchemeName": "redirectSchemeName",
            "redirectHostName": "redirectHostName"
        ]
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
