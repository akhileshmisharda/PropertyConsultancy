# Revert Custom Font Changes

Revert all custom font configurations (`fontFamily`) to restore default system font styling and fix layout inflation errors.

## Proposed Changes

### [Component Name] Layouts and Styles

#### [MODIFY] [themes.xml](file:///C:/D_Drive/Android_Application/app/src/main/res/values/themes.xml)
- Remove `android:fontFamily` from `Theme.PropertyConsultancy`.
- Remove `android:fontFamily` from `App.EditText.Style`.
- Remove `android:fontFamily` from `App.BottomNav.TextAppearance`.

#### [MODIFY] [activity_login.xml](file:///C:/D_Drive/Android_Application/app/src/main/res/layout/activity_login.xml)
- Remove `android:fontFamily="sans-serif"` from `tvWelcome`.

#### [MODIFY] [item_property.xml](file:///C:/D_Drive/Android_Application/app/src/main/res/layout/item_property.xml)
- Remove `android:fontFamily="sans-serif"` from all `TextView` and `MaterialButton` elements.

#### [MODIFY] [fragment_property_details.xml](file:///C:/D_Drive/Android_Application/app/src/main/res/layout/fragment_property_details.xml)
- Remove `android:fontFamily="@font/open_sans"` from all input fields and labels.

#### [DELETE] [open_sans.xml](file:///C:/D_Drive/Android_Application/app/src/main/res/font/open_sans.xml)
- Delete the downloadable font configuration file.

#### [DELETE] [font_certs.xml](file:///C:/D_Drive/Android_Application/app/src/main/res/values/font_certs.xml)
- Delete the font provider certificates.

## Verification Plan

### Automated Tests
- Run `./gradlew app:assembleDebug` to ensure the project builds correctly without the font resources.

### Manual Verification
- Deploy the app and verify that the `InvocationTargetException` no longer occurs during navigation between Login, Main, and Dashboard/Details screens.
- Verify that the app uses the default system font (Sans Serif).
