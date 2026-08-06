<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

ini_set('display_errors', 0);
error_reporting(E_ALL);

$logFile = __DIR__ . '/uploads/debug.log';
function log_queue_debug($msg) {
    global $logFile;
    $time = date('Y-m-d H:i:s');
    if (!is_dir(__DIR__ . '/uploads')) { mkdir(__DIR__ . '/uploads', 0777, true); }
    @file_put_contents($logFile, "[$time] [SUBMIT] $msg\n", FILE_APPEND);
}

$config_path = __DIR__ . '/../ERP/config/database.php';
if (!file_exists($config_path)) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Database config missing"]);
    exit;
}
require_once $config_path;

try {
    $pdo = new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4', DB_USER, DB_PASS, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);

    if (!$data) { throw new Exception("Invalid JSON"); }

    // LOG ONLY THE ID OR TITLE, NOT THE FULL DATA (avoids memory crash)
    log_queue_debug("Submitting new property: " . ($data['title'] ?? 'Untitled'));

    $pdo->beginTransaction();

    // 1. Insert Property
    $sql = "INSERT INTO pro_properties (landlord_id, category_id, title, slug, description, address_line_1, city, state, country, price_per_month, bedrooms, bathrooms, max_guests, status, created_at, updated_at)
            VALUES (:landlord_id, :category_id, :title, :slug, :description, :address_line_1, :city, :state, :country, :price_per_month, :bedrooms, :bathrooms, :max_guests, :status, NOW(), NOW())";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([
        ':landlord_id' => $data['landlord_id'] ?? 1,
        ':category_id' => $data['category_id'] ?? 0,
        ':title' => $data['title'],
        ':slug' => strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $data['title']))),
        ':description' => $data['description'] ?? '',
        ':address_line_1' => $data['address_line_1'] ?? '',
        ':city' => $data['city'] ?? '',
        ':state' => $data['state'] ?? '',
        ':country' => $data['country'] ?? 'India',
        ':price_per_month' => $data['price_per_month'] ?? 0,
        ':bedrooms' => $data['bedrooms'] ?? 0,
        ':bathrooms' => $data['bathrooms'] ?? 0,
        ':max_guests' => $data['max_guests'] ?? 1,
        ':status' => $data['status'] ?? 'available'
    ]);

    $property_id = $pdo->lastInsertId();

    // 2. Amenities
    if (!empty($data['amenity_ids'])) {
        $stmt_am = $pdo->prepare("INSERT INTO pro_property_amenities (property_id, amenity_id) VALUES (?, ?)");
        foreach ($data['amenity_ids'] as $aid) { $stmt_am->execute([$property_id, $aid]); }
    }

    // 3. Media (Decode Base64 and save as physical files)
    if (!empty($data['media_urls'])) {
        $stmt_m = $pdo->prepare("INSERT INTO pro_property_media (property_id, media_type, file_url, is_primary, display_order, created_at) VALUES (?, ?, ?, ?, ?, NOW())");
        $upload_dir = __DIR__ . '/uploads/';
        if (!is_dir($upload_dir)) { mkdir($upload_dir, 0777, true); }

        foreach ($data['media_urls'] as $index => $media_data) {
            $file_url = null;
            $mtype = 'image';

            if (preg_match('/^data:(image|video)\/(\w+);base64,/', $media_data, $matches)) {
                $mtype = $matches[1];
                $ext = $matches[2] == 'jpeg' ? 'jpg' : $matches[2];
                $filename = "prop_{$property_id}_" . time() . "_{$index}.{$ext}";

                if (file_put_contents($upload_dir . $filename, base64_decode(substr($media_data, strpos($media_data, ',') + 1)))) {
                    $file_url = 'uploads/' . $filename;
                }
            }

            if ($file_url) {
                $stmt_m->execute([$property_id, $mtype, $file_url, ($index === 0 ? 1 : 0), $index]);
            }
        }
    }

    $pdo->commit();
    echo json_encode(["status" => "success", "message" => "Property submitted successfully", "property_id" => $property_id]);

} catch (Exception $e) {
    if (isset($pdo)) $pdo->rollBack();
    log_queue_debug("ERROR: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
