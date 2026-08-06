# Fix `java.lang.IllegalArgumentException: bad base-64` in Font Loading

The application crashes when trying to load downloadable fonts because the font certificate strings in `font_certs.xml` are invalid or truncated. Specifically, the production certificate for Google Fonts is incomplete, leading to a Base64 decoding error.

## User Review Required

> [!IMPORTANT]
> This change updates the Google Fonts provider certificates to the standard ones required by Google Play Services. No other changes are needed in the app logic.

## Proposed Changes

### Resources

#### [MODIFY] [font_certs.xml](file:///C:/D_Drive/Android_Application/app/src/main/res/values/font_certs.xml)
Update the `com_google_android_gms_fonts_certs_prod` string-array with the correct, full certificate string. I will also ensure the `dev` certificate is correct.

## Verification Plan

### Automated Tests
- I will attempt to build the project to ensure no resource errors are introduced.
- Since this is a runtime crash related to font loading, manual verification on a device with Google Play Services is the best way to confirm the fix.

### Manual Verification
- Deploy the app to a device or emulator.
- Verify that the fonts (e.g., "Arimo") load correctly and the crash no longer occurs.
