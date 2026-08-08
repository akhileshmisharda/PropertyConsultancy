<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Enable error logging for debugging
ini_set('display_errors', 0);
error_reporting(E_ALL);

// Handle Preflight OPTIONS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Database configuration path
$config_path = __DIR__ . '/../ERP/config/database.php';
if (!file_exists($config_path)) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Database config file not found"]);
    exit;
}
require_once $config_path;

try {
    $pdo = new PDO(
        'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
        DB_USER,
        DB_PASS,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );

    $json = file_get_contents('php://input');
    $data = json_decode($json, true);

    if (!$data) {
        throw new Exception("Invalid JSON received");
    }

    $pdo->beginTransaction();

    // 1. Insert Property
    $sql = "INSERT INTO pro_properties (
                landlord_id, category_id, title, slug, description,
                address_line_1, address_line_2, city, state, country, zip_code,
                latitude, longitude, price_per_month, cleaning_fee, security_deposit,
                bedrooms, bathrooms, max_guests, area_sqft, furnishing, status,
                is_featured, floor_id, roadsize_id, facing_id, status_id, status_date, protype_id,
                created_at, updated_at
            ) VALUES (
                :landlord_id, :category_id, :title, :slug, :description,
                :address_line_1, :address_line_2, :city, :state, :country, :zip_code,
                :latitude, :longitude, :price_per_month, :cleaning_fee, :security_deposit,
                :bedrooms, :bathrooms, :max_guests, :area_sqft, :furnishing, :status,
                :is_featured, :floor_id, :roadsize_id, :facing_id, :status_id, :status_date, :protype_id,
                NOW(), NOW()
            )";

    $slug = strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $data['title'])));
    $stmt = $pdo->prepare($sql);
    $stmt->execute([
        ':landlord_id'      => $data['landlord_id'] ?? 1,
        ':category_id'      => $data['category_id'] ?? 0,
        ':title'            => $data['title'] ?? 'Untitled',
        ':slug'             => $slug,
        ':description'      => $data['description'] ?? '',
        ':address_line_1'   => $data['address_line_1'] ?? '',
        ':address_line_2'   => $data['address_line_2'] ?? null,
        ':city'             => $data['city'] ?? '',
        ':state'            => $data['state'] ?? '',
        ':country'          => $data['country'] ?? 'India',
        ':zip_code'         => $data['zip_code'] ?? '',
        ':latitude'         => $data['latitude'] ?? null,
        ':longitude'        => $data['longitude'] ?? null,
        ':price_per_month'  => $data['price_per_month'] ?? 0,
        ':cleaning_fee'     => $data['cleaning_fee'] ?? 0,
        ':security_deposit' => $data['security_deposit'] ?? 0,
        ':bedrooms'         => $data['bedrooms'] ?? 0,
        ':bathrooms'        => $data['bathrooms'] ?? 0,
        ':max_guests'       => $data['max_guests'] ?? 1,
        ':area_sqft'        => $data['area_sqft'] ?? null,
        ':furnishing'       => $data['furnishing'] ?? 'Unfurnished',
        ':status'           => $data['status'] ?? 'available',
        ':is_featured'      => $data['is_featured'] ?? 0,
        ':floor_id'         => $data['floor_id'] ?? null,
        ':roadsize_id'      => $data['roadsize_id'] ?? null,
        ':facing_id'        => $data['facing_id'] ?? null,
        ':status_id'        => $data['status_id'] ?? null,
        ':status_date'      => $data['status_date'] ?? null,
        ':protype_id'       => $data['protype_id'] ?? null
    ]);

    $property_id = $pdo->lastInsertId();

    // 2. Amenities
    if (!empty($data['amenity_ids']) && is_array($data['amenity_ids'])) {
        $stmt_amenity = $pdo->prepare("INSERT INTO pro_property_amenities (property_id, amenity_id) VALUES (?, ?)");
        foreach ($data['amenity_ids'] as $aid) {
            $stmt_amenity->execute([$property_id, $aid]);
        }
    }

    // 3. Media (Skipped if not explicitly provided in main payload)
    // New system uses Direct Upload from MediaFragment.

    $pdo->commit();
    echo json_encode([
        "status"      => "success",
        "message"     => "Property initialized",
        "property_id" => $property_id
    ]);

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) { $pdo->rollBack(); }
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
?>
