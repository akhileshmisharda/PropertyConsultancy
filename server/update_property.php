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
    @file_put_contents($logFile, "[$time] [UPDATE] $msg\n", FILE_APPEND);
}

$config_path = __DIR__ . '/../ERP/config/database.php';
if (!file_exists($config_path)) {
    echo json_encode(["status" => "error", "message" => "Database config missing"]);
    exit;
}
require_once $config_path;

try {
    $pdo = new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4', DB_USER, DB_PASS, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);

    if (!$data || empty($data['property_id'])) { throw new Exception("Invalid data"); }
    $property_id = $data['property_id'];

    $pdo->beginTransaction();

    // 1. Update Details
    $sql = "UPDATE pro_properties SET category_id=:category_id, title=:title, slug=:slug, description=:description, address_line_1=:address_line_1, city=:city, state=:state, price_per_month=:price_per_month, bedrooms=:bedrooms, bathrooms=:bathrooms, area_sqft=:area_sqft, status=:status, updated_at=NOW() WHERE property_id=:pid";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([
        ':category_id' => $data['category_id'] ?? 0, ':title' => $data['title'], ':slug' => strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $data['title']))), ':description' => $data['description'] ?? '', ':address_line_1' => $data['address_line_1'] ?? '', ':city' => $data['city'] ?? '', ':state' => $data['state'] ?? '', ':price_per_month' => $data['price_per_month'] ?? 0, ':bedrooms' => $data['bedrooms'] ?? 0, ':bathrooms' => $data['bathrooms'] ?? 0, ':area_sqft' => $data['area_sqft'] ?? null, ':status' => $data['status'] ?? 'available', ':pid' => $property_id
    ]);

    // 2. Sync Amenities
    $pdo->prepare("DELETE FROM pro_property_amenities WHERE property_id=?")->execute([$property_id]);
    if (!empty($data['amenity_ids'])) {
        $stmt_am = $pdo->prepare("INSERT INTO pro_property_amenities (property_id, amenity_id) VALUES (?, ?)");
        foreach ($data['amenity_ids'] as $aid) { $stmt_am->execute([$property_id, $aid]); }
    }

    // 3. Sync Media (Intelligent update)
    $upload_dir = __DIR__ . '/uploads/';
    $stmt_old = $pdo->prepare("SELECT file_url FROM pro_property_media WHERE property_id=?");
    $stmt_old->execute([$property_id]);
    $old_files = $stmt_old->fetchAll(PDO::FETCH_COLUMN);

    $pdo->prepare("DELETE FROM pro_property_media WHERE property_id=?")->execute([$property_id]);

    $new_active_files = [];
    if (!empty($data['media_urls'])) {
        $stmt_m = $pdo->prepare("INSERT INTO pro_property_media (property_id, media_type, file_url, is_primary, display_order, created_at) VALUES (?, ?, ?, ?, ?, NOW())");
        foreach ($data['media_urls'] as $index => $media_data) {
            $file_url = null;
            $mtype = 'image';

            if (preg_match('/^data:(image|video)\/(\w+);base64,/', $media_data, $matches)) {
                // Decode new Base64
                $ext = $matches[2] == 'jpeg' ? 'jpg' : $matches[2];
                $mtype = $matches[1];
                $filename = "prop_{$property_id}_" . time() . "_{$index}.{$ext}";
                if (file_put_contents($upload_dir . $filename, base64_decode(substr($media_data, strpos($media_data, ',') + 1)))) {
                    $file_url = 'uploads/' . $filename;
                }
            } else if (strpos($media_data, 'uploads/') === 0) {
                // Keep existing relative path
                $file_url = $media_data;
                if (preg_match('/\.(mp4|mkv)$/i', $file_url)) $mtype = 'video';
            }

            if ($file_url) {
                $new_active_files[] = $file_url;
                $stmt_m->execute([$property_id, $mtype, $file_url, ($index === 0 ? 1 : 0), $index]);
            }
        }
    }

    // Physically delete removed files
    foreach ($old_files as $old) {
        if (strpos($old, 'uploads/') === 0 && !in_array($old, $new_active_files)) {
            @unlink(__DIR__ . '/' . $old);
        }
    }

    $pdo->commit();
    echo json_encode(["status" => "success", "message" => "Updated"]);
} catch (Exception $e) {
    if (isset($pdo)) $pdo->rollBack();
    log_queue_debug("ERROR: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
