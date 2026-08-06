# Curved Bottom Navigation Implementation

Implement a custom bottom navigation bar that matches the provided design: curved top border with a "hump" in the center, a green "attraction" line, and semi-transparent background.

## Proposed Changes

### [Component Name] UI Components

#### [NEW] [CurvedBottomNavigationView.kt](file:///C:/D_Drive/Android_Application/app/src/main/java/com/example/propertyconsultancy/ui/views/CurvedBottomNavigationView.kt)
- Custom View extending `BottomNavigationView`.
- Overrides `onDraw` to draw a curved background and a green top border.
- Uses `Path` with Bezier curves for the "hump".

#### [MODIFY] [activity_main.xml](file:///C:/D_Drive/Android_Application/app/src/main/res/layout/activity_main.xml)
- Replaced standard `BottomNavigationView` with `CurvedBottomNavigationView`.
- Removed legacy `navIndicator` and `navTopBorder`.
- Adjusted height and padding to accommodate the hump.

#### [MODIFY] [MainActivity.kt](file:///C:/D_Drive/Android_Application/app/src/main/java/com/example/propertyconsultancy/ui/activities/MainActivity.kt)
- Removed manual indicator movement logic.
- Simplified navigation setup.

#### [MODIFY] [themes.xml](file:///C:/D_Drive/Android_Application/app/src/main/res/values/themes.xml)
- Added circular active indicator style (`Widget.App.Indicator.Circle`).

## Verification Plan

### Manual Verification
- Deploy the app and check the bottom navigation bar.
- Verify the "hump" is centered and has a green border.
- Verify the background is semi-transparent.
- Verify items are correctly positioned and clickable.
