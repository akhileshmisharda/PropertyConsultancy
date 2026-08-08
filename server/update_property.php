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
    @file_put_contents($logFile, "[$time] [UPDATE] " . (is_array($msg) ? json_encode($msg) : $msg) . "\n", FILE_APPEND);
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

    if (!$data || empty($data['property_id'])) {
        throw new Exception("Invalid data or property_id missing");
    }

    $property_id = $data['property_id'];
    log_queue_debug("Updating property ID: $property_id");

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

    $slug = strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $data['title'])));
    $stmt = $pdo->prepare($sql);
    $stmt->execute([
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
        ':protype_id'       => $data['protype_id'] ?? null,
        ':property_id'      => $property_id
    ]);

    // 2. Update Amenities (Delete old, Insert new)
    $stmt_del_amenities = $pdo->prepare("DELETE FROM pro_property_amenities WHERE property_id = ?");
    $stmt_del_amenities->execute([$property_id]);

    if (!empty($data['amenity_ids']) && is_array($data['amenity_ids'])) {
        $stmt_amenity = $pdo->prepare("INSERT INTO pro_property_amenities (property_id, amenity_id) VALUES (?, ?)");
        foreach ($data['amenity_ids'] as $aid) {
            $stmt_amenity->execute([$property_id, $aid]);
        }
    }

    // 3. Update Media (Physically sync files)
    $relative_dir = 'uploads/properties/';
    $absolute_dir = __DIR__ . '/' . $relative_dir;

    // a. Get current media from DB to know what to potentially delete
    $stmt_old_media = $pdo->prepare("SELECT file_url FROM pro_property_media WHERE property_id = ?");
    $stmt_old_media->execute([$property_id]);
    $old_media_files = $stmt_old_media->fetchAll(PDO::FETCH_COLUMN);

    // b. Wipe existing media records for this property
    $stmt_del_media = $pdo->prepare("DELETE FROM pro_property_media WHERE property_id = ?");
    $stmt_del_media->execute([$property_id]);

    $new_active_files = [];
    $stmt_ins_media = $pdo->prepare("INSERT INTO pro_property_media (property_id, media_type, file_url, is_primary, display_order, created_at) VALUES (?, ?, ?, ?, ?, NOW())");

    if (!empty($data['media_urls']) && is_array($data['media_urls'])) {
        foreach ($data['media_urls'] as $index => $media_data) {
            $file_url = $media_data;
            $mtype = 'image';

            // If it's new Base64 data from Android
            if (preg_match('/^data:(image|video)\/(\w+);base64,/', $media_data, $matches)) {
                $mtype = $matches[1];
                $extension = strtolower($matches[2]);
                if ($extension == 'jpeg') $extension = 'jpg';

                $base64_str = substr($media_data, strpos($media_data, ',') + 1);
                $filename = 'prop_' . $property_id . '_' . time() . '_' . $index . '.' . $extension;

                if (file_put_contents($absolute_dir . $filename, base64_decode($base64_str))) {
                    $file_url = $relative_dir . $filename;
                }
            } else if (strpos($media_data, 'uploads/') === 0) {
                // Existing relative path - strip any query params like ?v=...
                $file_url = strtok($media_data, '?');
                if (preg_match('/\.(mp4|mkv|avi)$/i', $file_url)) $mtype = 'video';
            }

            if ($file_url) {
                $new_active_files[] = $file_url;
                $stmt_ins_media->execute([$property_id, $mtype, $file_url, ($index === 0 ? 1 : 0), $index]);
            }
        }
    }

    // c. Physically delete orphaned files
    foreach ($old_media_files as $old_file_raw) {
        $old_file = strtok($old_file_raw, '?'); // Clean path for comparison and deletion
        if (strpos($old_file, 'uploads/') === 0 && !in_array($old_file, $new_active_files)) {
            $full_path = __DIR__ . '/' . $old_file;
            if (file_exists($full_path)) {
                @unlink($full_path);
                log_queue_debug("Removed orphaned file: $old_file");
            }
        }
    }

    $pdo->commit();
    echo json_encode(["status" => "success", "message" => "Property updated successfully"]);

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    log_queue_debug("ERROR: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
