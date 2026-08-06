# Fix IllegalStateException in PropertyPricingFragment Slider

The application crashes with an `java.lang.IllegalStateException` because the `Slider` in `PropertyPricingFragment` is being set to a value (`100.0`) that is not a multiple of its `stepSize` (`1000.0`).

## Proposed Changes

### UI Components

#### [MODIFY] [fragment_property_pricing.xml](file:///C:/D_Drive/Android_Application/app/src/main/res/layout/fragment_property_pricing.xml)
- Change `app:stepSize` from `1000.0` to `100.0` (or `500.0` depending on preference, but `100.0` makes `100.0` valid).
- This allows for finer price selection and resolves the conflict where `100.0` was an invalid value.

### Logic Improvements

#### [MODIFY] [PropertyPricingFragment.kt](file:///C:/D_Drive/Android_Application/app/src/main/java/com/example/propertyconsultancy/ui/fragments/PropertyPricingFragment.kt)
- Update `setupPricingLogic` to be more robust.
- Ensure that the value set to the slider is always a valid multiple of the step size, even if floating point precision issues occur.
- Add a safety check before setting `sliderPrice.value`.

## Verification Plan

### Automated Tests
- N/A (UI logic verification)

### Manual Verification
1. Open the "Add Property" screen and navigate to the "Pricing" tab.
2. Type "100" in the Price field. Verify it no longer crashes.
3. Move the slider and verify the Price field updates in increments of 100.
4. Type various values in the Price field and verify the slider snaps to the nearest valid increment.
