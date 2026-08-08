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
    $user_id     = $_GET['user_id'] ?? null;

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
              (SELECT GROUP_CONCAT(pa.amenity_id) FROM pro_property_amenities pa WHERE pa.property_id = p.property_id) as amenity_ids,
              (SELECT COUNT(*) FROM pro_property_amenities pa WHERE pa.property_id = p.property_id) as amenity_count,
              (SELECT is_favorite FROM pro_interactions pi WHERE pi.property_id = p.property_id AND pi.customer_id = :user_id LIMIT 1) as is_favorite,
              pe.user_id as executive_id,
              CONCAT(pe.first_name, ' ', pe.last_name) as executive_name,
              pe.phone as executive_mobile,
              pe.profile_image_url as executive_image
              FROM pro_properties p
              LEFT JOIN pro_landlord_executives ple ON p.landlord_id = ple.landlord_id AND ple.is_active = 1
              LEFT JOIN pro_users pe ON ple.executive_id = pe.user_id
              WHERE $where
              GROUP BY p.property_id
              ORDER BY p.created_at DESC";

    $stmt = $pdo->prepare($query);
    if ($user_id) $params[':user_id'] = $user_id; else $params[':user_id'] = 0;

    $stmt->execute($params);
    $properties = $stmt->fetchAll(PDO::FETCH_ASSOC);

    foreach ($properties as &$p) {
        // Fetch Media as Objects
        $m_stmt = $pdo->prepare("SELECT media_id, property_id, image_tag_id, media_type, file_url, is_primary, display_order FROM pro_property_media WHERE property_id = ? ORDER BY display_order ASC");
        $m_stmt->execute([$p['property_id']]);
        $p['media'] = $m_stmt->fetchAll(PDO::FETCH_ASSOC);

        // Backward compatibility
        $p['media_urls'] = array_column($p['media'], 'file_url');

        // Handle Amenity IDs
        if (!empty($p['amenity_ids'])) {
            $p['amenity_ids'] = array_map('intval', explode(',', $p['amenity_ids']));
        } else {
            $p['amenity_ids'] = [];
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
