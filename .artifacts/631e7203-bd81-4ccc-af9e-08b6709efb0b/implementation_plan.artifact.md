# App Refactor: XML Migration, Maps, Media Upload, and PHP Backend

This plan outlines the steps to migrate the current Jetpack Compose UI to XML layouts, implement map-based location selection, enable media uploading from the gallery, and set up a PHP backend for data persistence.

## User Review Required

> [!IMPORTANT]
> Migrating from Jetpack Compose back to XML layouts is a significant structural change. This will replace modern declarative UI with traditional Imperative UI (Activities/Fragments and XML).

> [!WARNING]
> Implementing the PHP backend requires you to have a web server (like XAMPP, WAMP, or a remote server) to host the PHP files and a MySQL database.

## Proposed Changes

### 1. XML Migration & Activity Setup
We will convert each Compose screen into a traditional Android Activity with an XML layout.

- **Dependencies:** Add `appcompat` and `material` components.
- **Layouts:** Create `activity_login.xml`, `activity_register.xml`, `activity_landlord_dashboard.xml`, `activity_add_property.xml`, `activity_property_list.xml`, `activity_property_detail.xml`.
- **Activities:** Create corresponding Kotlin classes.
- **Navigation:** Replace `AppNavigation.kt` with Activity transitions (Intents).

### 2. Map Integration
We will integrate Google Maps for property location selection.

- **Dependencies:** Add `play-services-maps`.
- **Implementation:** Add a `MapActivity` or integrate a MapView into `activity_add_property.xml`. Use a marker to let the user select a location and return the coordinates.

### 3. Media Upload (Image & Video)
We will implement gallery access using `ActivityResultContracts`.

- **Implementation:** Update the "Add Photo/Video" buttons in `AddPropertyActivity` to launch the system photo picker. Handle the returned URIs to display thumbnails and prepare for upload.

### 4. PHP Backend Implementation
We will create a REST API using PHP and MySQL.

- **Database:** Create a `properties` table and a `users` table.
- **Scripts:**
    - `db_config.php`: Database connection.
    - `auth.php`: Login and Registration logic.
    - `add_property.php`: Handle POST requests with multipart/form-data for media.
    - `get_properties.php`: Retrieve property listings.
- **Retrofit:** Update `ApiService.kt` to point to the new PHP endpoints.

## Verification Plan

### Automated Tests
- Build the project to ensure all XML layouts and Activities are correctly linked.
- Unit tests for the new PHP API (using tools like Postman or curl).

### Manual Verification
- Launch the app and verify the Login/Register flow.
- Navigate to the Landlord Dashboard and test adding a property.
- Verify that clicking "Select on Map" opens a map.
- Verify that clicking "Add Photo/Video" opens the gallery.
- Check the MySQL database to ensure data is saved correctly.
