# Property Rental App Implementation Plan

We will build an Android application for a Property Rental System based on the provided PHP/MySQL schema. The app will feature two main flows: **Tenant** (browsing and booking) and **Landlord** (uploading and managing properties).

## User Review Required

> [!IMPORTANT]
> This plan assumes a REST API will be available to interact with the PHP/MySQL backend. I will define the necessary Kotlin Data Classes and Retrofit interfaces matching your table structure.

> [!NOTE]
> I will use Jetpack Compose for the UI, Retrofit for networking, and Navigation Compose for app flow.

## Proposed Changes

### 1. Configuration & Dependencies
Update `libs.versions.toml` and `build.gradle.kts` to include:
- Navigation Compose
- Retrofit & Gson (for API)
- Coil (for image loading)
- ViewModel & Lifecycle components

### 2. Data Models (DTOs)
Create Kotlin data classes in a `data` package:
- `User`, `Property`, `Category`, `Amenity`, `PropertyMedia`, `Booking`, etc.
- Matching the fields in `pro_users`, `pro_properties`, etc.

### 3. API & Networking
- Define `ApiService.kt` with endpoints for:
    - Auth: `login`, `register`
    - Properties: `getProperties`, `getPropertyDetail`, `addProperty`
    - Metadata: `getCategories`, `getAmenities`
- Implement `RetrofitInstance.kt` for networking setup.

### 4. UI Screens (Compose)
- **Auth**: `LoginScreen`, `RegisterScreen`.
- **Tenant Flow**:
    - `PropertyListScreen`: Displaying properties with search and filters.
    - `PropertyDetailScreen`: Showing images, description, amenities, and "Book Now".
- **Landlord Flow**:
    - `LandlordDashboard`: View uploaded properties.
    - `AddPropertyScreen`: Multi-step form to upload property details, select amenities, and upload images.

### 5. Navigation
- Implement `NavHost` to handle transitions between Auth, Tenant, and Landlord flows.

## Verification Plan

### Automated Tests
- Unit tests for Data Model parsing.
- UI tests for screen transitions.

### Manual Verification
- Deploy to emulator/device.
- Verify "Upload Property" form validation.
- Verify "Property Search" UI layout.
- (Optional) Test against a mock API or actual backend if provided.
