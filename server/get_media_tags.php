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

    $query = "SELECT tag_id, tag_name, allowed_media_type, is_required, display_order FROM pro_property_media_tags WHERE is_active = 1 ORDER BY display_order ASC";
    $stmt = $pdo->prepare($query);
    $stmt->execute();
    $tags = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "status" => "success",
        "data" => $tags
    ]);

} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
?>
