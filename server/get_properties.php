<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

$config_path = __DIR__ . '/../ERP/config/database.php';
if (file_exists($config_path)) {
    require_once $config_path;
} else {
    echo json_encode(["status" => "error", "message" => "Database config missing"]);
    exit();
}

try {
    $pdo = new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4', DB_USER, DB_PASS, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);

    $landlord_id = $_GET['landlord_id'] ?? null;
    $property_id = $_GET['property_id'] ?? null;

    $where = "1=1";
    $params = [];

    if ($property_id) {
        $where = "p.property_id = :property_id";
        $params[':property_id'] = $property_id;
    } else if ($landlord_id) {
        $where = "p.landlord_id = :landlord_id";
        $params[':landlord_id'] = $landlord_id;
    } else {
        $where = "p.status = 'available'";
    }

    $query = "SELECT p.*,
              (SELECT GROUP_CONCAT(pm.file_url) FROM pro_property_media pm WHERE pm.property_id = p.property_id) as media_urls,
              (SELECT COUNT(*) FROM pro_property_amenities pa WHERE pa.property_id = p.property_id) as amenity_count,
              pe.user_id as executive_id,
              CONCAT(pe.first_name, ' ', pe.last_name) as executive_name,
              pe.phone as executive_mobile
              FROM pro_properties p
              LEFT JOIN pro_landlord_executives ple ON p.landlord_id = ple.landlord_id AND ple.is_active = 1
              LEFT JOIN pro_users pe ON ple.executive_id = pe.user_id
              WHERE $where
              GROUP BY p.property_id
              ORDER BY p.created_at DESC";

    $stmt = $pdo->prepare($query);
    $stmt->execute($params);
    $properties = $stmt->fetchAll(PDO::FETCH_ASSOC);

    foreach ($properties as &$p) {
        if ($p['media_urls']) {
            $p['media_urls'] = explode(',', $p['media_urls']);
        } else {
            $p['media_urls'] = [];
        }
    }

    echo json_encode([
        "status" => "success",
        "count" => count($properties),
        "data" => $properties
    ]);

} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
?>
