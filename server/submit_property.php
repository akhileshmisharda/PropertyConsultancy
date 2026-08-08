<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Enable error logging for debugging
ini_set('display_errors', 0);
error_reporting(E_ALL);

// Debug logger helper
$logFile = __DIR__ . '/uploads/debug.log';
function log_queue_debug($msg) {
    global $logFile;
    $time = date('Y-m-d H:i:s');
    if (!is_dir(__DIR__ . '/uploads')) {
        mkdir(__DIR__ . '/uploads', 0777, true);
    }
    @file_put_contents($logFile, "[$time] [SUBMIT] " . (is_array($msg) ? json_encode($msg) : $msg) . "\n", FILE_APPEND);
}

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

    // Support both FormData ($_POST) and JSON Body
    if (!empty($_POST)) {
        $data = $_POST;
        if (isset($data['amenity_ids']) && is_string($data['amenity_ids'])) {
            $data['amenity_ids'] = json_decode($data['amenity_ids'], true) ?? [];
        }
    } else {
        $json = file_get_contents('php://input');
        $data = json_decode($json, true);
    }

    if (!$data) {
        throw new Exception("Invalid JSON or Form data received");
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
        ':title'            => $data['title'],
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

    // 3. Media Upload (Physical file storage with single path reference)
    $relative_dir = 'uploads/properties/';
    $absolute_dir = __DIR__ . '/' . $relative_dir;

    if (!is_dir($absolute_dir)) {
        mkdir($absolute_dir, 0755, true);
    }

    $stmt_media = $pdo->prepare("INSERT INTO pro_property_media (property_id, media_type, file_url, is_primary, display_order, created_at) VALUES (?, ?, ?, ?, ?, NOW())");

    // A. Handle Multipart File Uploads ($_FILES)
    if (!empty($_FILES['media_files']['name'][0])) {
        $files = $_FILES['media_files'];
        $count = count($files['name']);

        for ($i = 0; $i < $count; $i++) {
            if ($files['error'][$i] === UPLOAD_ERR_OK) {
                $tmpName  = $files['tmp_name'][$i];
                $origName = basename($files['name'][$i]);
                $ext      = strtolower(pathinfo($origName, PATHINFO_EXTENSION));
                $allowed  = ['jpg', 'jpeg', 'png', 'webp', 'gif'];

                if (in_array($ext, $allowed)) {
                    $filename         = "prop_" . $property_id . "_" . time() . "_" . $i . "." . $ext;
                    $targetPath       = $absolute_dir . $filename;
                    $relativeReference = $relative_dir . $filename;

                    if (move_uploaded_file($tmpName, $targetPath)) {
                        $stmt_media->execute([$property_id, 'image', $relativeReference, ($i === 0 ? 1 : 0), $i]);
                    }
                }
            }
        }
    }

    // B. Handle Base64 strings or existing URL array passed in $data['media_urls']
    if (!empty($data['media_urls']) && is_array($data['media_urls'])) {
        foreach ($data['media_urls'] as $index => $media_data) {
            $file_reference = $media_data;
            $mtype = 'image';

            // Check if input is Base64 encoded file string
            if (preg_match('/^data:(image|video)\/(\w+);base64,/', $media_data, $type_matches)) {
                $mtype     = $type_matches[1];
                $extension = strtolower($type_matches[2]);
                if ($extension === 'jpeg') $extension = 'jpg';

                $base64_str = substr($media_data, strpos($media_data, ',') + 1);
                $filename   = 'prop_' . $property_id . '_' . time() . '_' . $index . '.' . $extension;

                $targetFilePath    = $absolute_dir . $filename;
                $relativeReference = $relative_dir . $filename;

                if (file_put_contents($targetFilePath, base64_decode($base64_str))) {
                    $file_reference = $relativeReference;
                }
            } else if (strpos($media_data, 'uploads/') === 0) {
                // Existing relative path - strip any query params
                $file_reference = strtok($media_data, '?');
                if (preg_match('/\.(mp4|mkv|avi)$/i', $file_reference)) $mtype = 'video';
            }

            $stmt_media->execute([$property_id, $mtype, $file_reference, ($index === 0 ? 1 : 0), $index]);
        }
    }

    $pdo->commit();
    echo json_encode([
        "status"      => "success",
        "message"     => "Property submitted successfully",
        "property_id" => $property_id
    ]);

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    log_queue_debug("ERROR: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
?>
