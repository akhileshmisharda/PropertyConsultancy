<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

$logFile = __DIR__ . '/uploads/php_debug.log';
function writeDebug($msg) {
    global $logFile;
    $time = date('Y-m-d H:i:s');
    if (!is_dir(__DIR__ . '/uploads')) {
        mkdir(__DIR__ . '/uploads', 0777, true);
    }
    file_put_contents($logFile, "[$time] [UPDATE_PROPERTY] " . (is_array($msg) ? json_encode($msg) : $msg) . PHP_EOL, FILE_APPEND);
}

// Handle Preflight OPTIONS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

$config_path = __DIR__ . '/../ERP/config/database.php';
if (!file_exists($config_path)) {
    echo json_encode(["status" => "error", "message" => "Database config file not found"]);
    exit;
}
require_once $config_path;

try {
    $pdo = new PDO('mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4', DB_USER, DB_PASS, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);

    $json = file_get_contents('php://input');
    $data = json_decode($json, true);

    writeDebug("Incoming JSON data: " . $json);

    if (!$data || empty($data['property_id'])) {
        throw new Exception("Invalid data or property_id missing");
    }

    $property_id = $data['property_id'];
    $pdo->beginTransaction();

    // 1. Update Property details
    $sql = "UPDATE pro_properties SET
            category_id = :category_id,
            title = :title,
            slug = :slug,
            description = :description,
            address_line_1 = :address_line_1,
            address_line_2 = :address_line_2,
            city = :city,
            state = :state,
            country = :country,
            zip_code = :zip_code,
            latitude = :latitude,
            longitude = :longitude,
            price_per_month = :price_per_month,
            cleaning_fee = :cleaning_fee,
            security_deposit = :security_deposit,
            bedrooms = :bedrooms,
            bathrooms = :bathrooms,
            max_guests = :max_guests,
            area_sqft = :area_sqft,
            furnishing = :furnishing,
            status = :status,
            is_featured = :is_featured,
            floor_id = :floor_id,
            roadsize_id = :roadsize_id,
            facing_id = :facing_id,
            status_id = :status_id,
            status_date = :status_date,
            protype_id = :protype_id,
            updated_at = NOW()
            WHERE property_id = :property_id";

    $slug = strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $data['title'] ?? 'untitled')));
    $stmt = $pdo->prepare($sql);
    $stmt->execute([
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
        ':status'           => ($data['status'] === 'draft' ? 'available' : $data['status']) ?? 'available',
        ':is_featured'      => $data['is_featured'] ?? 0,
        ':floor_id'         => $data['floor_id'] ?? null,
        ':roadsize_id'      => $data['roadsize_id'] ?? null,
        ':facing_id'        => $data['facing_id'] ?? null,
        ':status_id'        => $data['status_id'] ?? null,
        ':status_date'      => $data['status_date'] ?? null,
        ':protype_id'       => $data['protype_id'] ?? null,
        ':property_id'      => $property_id
    ]);

    // 2. Update Amenities
    $stmt_del_amenities = $pdo->prepare("DELETE FROM pro_property_amenities WHERE property_id = ?");
    $stmt_del_amenities->execute([$property_id]);

    if (!empty($data['amenity_ids']) && is_array($data['amenity_ids'])) {
        $stmt_amenity = $pdo->prepare("INSERT INTO pro_property_amenities (property_id, amenity_id) VALUES (?, ?)");
        foreach ($data['amenity_ids'] as $aid) {
            $stmt_amenity->execute([$property_id, $aid]);
        }
    }

    // 3. Media handling removed.
    // Media is now handled by dedicated direct-upload API.

    $pdo->commit();
    writeDebug("Property $property_id updated successfully.");
    echo json_encode(["status" => "success", "message" => "Property updated successfully"]);

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) { $pdo->rollBack(); }
    writeDebug("ERROR: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
?>
