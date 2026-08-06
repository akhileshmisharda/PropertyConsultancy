# Expand Shared Element Transition for Property Explore

Expand the shared element transition between `SearchFragment` and `PropertyExploreFragment` to include rent, location, BHK, facing, road size, furnished status, area, and property type.

## Proposed Changes

### [Component Name] UI Layouts

#### [MODIFY] [item_zigzag_property.xml](file:///C:/D_Drive/Application_Mobile/PropertyConsultancy/app/src/main/res/layout/item_zigzag_property.xml)
- Add `transitionName` to:
    - `tvPriceLeft/Right`
    - `tvLocationLeft/Right`
    - `tvBhkLeft/Right`
    - `tvFacingLeft/Right`
    - `tvRoadSizeLeft/Right`
    - `tvFurnishedLeft/Right`
    - `tvAreaLeft/Right`
    - `tvPropertyTypeLeft/Right`

#### [MODIFY] [fragment_property_explore.xml](file:///C:/D_Drive/Application_Mobile/PropertyConsultancy/app/src/main/res/layout/fragment_property_explore.xml)
- Add missing views for:
    - Facing
    - Road Size
    - Furnished Status
    - Property Type
- Add `transitionName` to all relevant views:
    - `vpExploreMedia`
    - `tvExploreTitle`
    - `tvExplorePrice`
    - `tvExploreLocation`
    - `tvExploreBhk`
    - `tvExploreArea`
    - `tvExploreFacing`
    - `tvExploreRoadSize`
    - `tvExploreFurnished`
    - `tvExplorePropertyType`

### [Component Name] Adapters & Fragments

#### [MODIFY] [SearchPropertyAdapter.kt](file:///C:/D_Drive/Application_Mobile/PropertyConsultancy/app/src/main/java/com/example/propertyconsultancy/ui/adapters/SearchPropertyAdapter.kt)
- Update `onItemClick` lambda signature to accept a `Map<String, View>` of shared elements.
- In `onBindViewHolder`, set unique dynamic `transitionName` for all shared views.
- Collect all shared views into a map and pass it to `onItemClick`.

#### [MODIFY] [MainActivity.kt](file:///C:/D_Drive/Application_Mobile/PropertyConsultancy/app/src/main/java/com/example/propertyconsultancy/ui/activities/MainActivity.kt)
- Update `openPropertyExplore` to accept a `Map<String, View>` of shared elements.
- Pass all shared elements to the fragment transaction using `addSharedElement`.
- Pass the transition names in the bundle to `PropertyExploreFragment`.

#### [MODIFY] [SearchFragment.kt](file:///C:/D_Drive/Application_Mobile/PropertyConsultancy/app/src/main/java/com/example/propertyconsultancy/ui/fragments/SearchFragment.kt)
- Update the adapter initialization to handle the new `onItemClick` signature.

#### [MODIFY] [PropertyExploreFragment.kt](file:///C:/D_Drive/Application_Mobile/PropertyConsultancy/app/src/main/java/com/example/propertyconsultancy/ui/fragments/PropertyExploreFragment.kt)
- Retrieve all transition names from the bundle.
- Set `transitionName` on all corresponding views in `onCreateView`.
- Populate the new UI fields in `onViewCreated`.

## Verification Plan

### Automated Tests
- Build the app and ensure no compilation errors.

### Manual Verification
- Deploy the app to the device.
- Navigate to the search results.
- Click on a property item.
- Verify that all specified fields (image, title, price, etc.) animate smoothly to their new positions in the explore screen.
- Verify that the "missing" fields (facing, road size, etc.) are correctly displayed in the explore screen.
