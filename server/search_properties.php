<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// --- DEBUG LOGGING START ---
function writeDebugLog($message, $data = null) {
    $logFile = __DIR__ . '/uploads/search_properties_debug.log';
    $timestamp = date('Y-m-d H:i:s');
    $logContent = "[$timestamp] $message";
    if ($data !== null) {
        $logContent .= " | Data: " . (is_array($data) ? json_encode($data) : print_r($data, true));
    }
    file_put_contents($logFile, $logContent . PHP_EOL, FILE_APPEND);
}

// Handle Preflight OPTIONS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

writeDebugLog("--- Incoming API Request ---");
writeDebugLog("GET Params", $_GET);
// --- DEBUG LOGGING END ---

// Include database configuration
$config_path = __DIR__ . '/../ERP/config/database.php';

if (file_exists($config_path)) {
    require_once $config_path;
} else {
    writeDebugLog("Error: Database config file missing at " . $config_path);
    echo json_encode(["status" => "error", "message" => "Database configuration file not found."]);
    exit();
}

// Extract search parameters
$city         = $_GET['city'] ?? null;
$zip_code     = $_GET['zip_code'] ?? null;
$min_price    = $_GET['min_price'] ?? null;
$max_price    = $_GET['max_price'] ?? null;
$bedrooms     = $_GET['bedrooms'] ?? null;
$bathrooms    = $_GET['bathrooms'] ?? null;
$floor_ids    = $_GET['floor_ids'] ?? null;
$roadsize_ids = $_GET['roadsize_ids'] ?? null;
$facing_ids   = $_GET['facing_ids'] ?? null;
$status_ids   = $_GET['status_ids'] ?? null;
$protype_ids  = $_GET['protype_ids'] ?? null;

// Pagination parameters
$limit  = isset($_GET['limit']) ? (int)$_GET['limit'] : 5;
$offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;

$whereClauses = ["1=1"];
$params = [];

if (isset($city) && $city !== '') {
    $whereClauses[] = "p.city LIKE :city";
    $params[':city'] = "%$city%";
}

if (isset($zip_code) && $zip_code !== '') {
    $whereClauses[] = "p.zip_code = :zip_code";
    $params[':zip_code'] = $zip_code;
}

if (isset($min_price) && $min_price !== '') {
    $whereClauses[] = "p.price_per_month >= :min_price";
    $params[':min_price'] = (float)$min_price;
}

if (isset($max_price) && $max_price !== '') {
    $whereClauses[] = "p.price_per_month <= :max_price";
    $params[':max_price'] = (float)$max_price;
}

if (isset($bedrooms) && $bedrooms !== '') {
    $whereClauses[] = "p.bedrooms = :bedrooms";
    $params[':bedrooms'] = (int)$bedrooms;
}

if (isset($bathrooms) && $bathrooms !== '') {
    $whereClauses[] = "p.bathrooms = :bathrooms";
    $params[':bathrooms'] = (float)$bathrooms;
}

// Multi-select filters
function addInClause($field, $idsString, &$whereClauses, &$params) {
    if (!$idsString || trim($idsString) === '') return;
    $ids = explode(',', $idsString);
    $placeholders = [];
    foreach ($ids as $i => $id) {
        $id = trim($id);
        if ($id === '') continue;
        $placeholder = ":" . $field . "_" . $i;
        $placeholders[] = $placeholder;
        $params[$placeholder] = $id;
    }
    if (!empty($placeholders)) {
        $whereClauses[] = "p.$field IN (" . implode(',', $placeholders) . ")";
    }
}

addInClause('floor_id', $floor_ids, $whereClauses, $params);
addInClause('category_id', $protype_ids, $whereClauses, $params);
addInClause('roadsize_id', $roadsize_ids, $whereClauses, $params);
addInClause('facing_id', $facing_ids, $whereClauses, $params);
addInClause('status_id', $status_ids, $whereClauses, $params);

$whereSql = implode(" AND ", $whereClauses);

try {
    $pdo = new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4', DB_USER, DB_PASS, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);

    // Disable emulation to ensure LIMIT/OFFSET are handled correctly
    $pdo->setAttribute(PDO::ATTR_EMULATE_PREPARES, false);

    // 1. Get Total Count
    $countQuery = "SELECT COUNT(DISTINCT p.property_id) FROM pro_properties p WHERE $whereSql";
    $countStmt = $pdo->prepare($countQuery);
    $countStmt->execute($params);
    $totalCount = (int)$countStmt->fetchColumn();

    // 2. Get Paginated Data
    $query = "SELECT p.*,
              (SELECT GROUP_CONCAT(pm.file_url) FROM pro_property_media pm WHERE pm.property_id = p.property_id) as media_urls,
              (SELECT GROUP_CONCAT(pa.amenity_id) FROM pro_property_amenities pa WHERE pa.property_id = p.property_id) as amenity_ids,
              (SELECT COUNT(*) FROM pro_property_amenities pa WHERE pa.property_id = p.property_id) as amenity_count,
              pe.user_id as executive_id,
              CONCAT(pe.first_name, ' ', pe.last_name) as executive_name,
              pe.phone as executive_mobile
              FROM pro_properties p
              LEFT JOIN pro_landlord_executives ple ON p.landlord_id = ple.landlord_id AND ple.is_active = 1
              LEFT JOIN pro_users pe ON ple.executive_id = pe.user_id
              WHERE $whereSql
              GROUP BY p.property_id
              ORDER BY p.created_at DESC
              LIMIT :limit OFFSET :offset";

    $stmt = $pdo->prepare($query);

    foreach ($params as $key => $val) {
        $stmt->bindValue($key, $val);
    }
    $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
    $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);

    $stmt->execute();
    $properties = $stmt->fetchAll(PDO::FETCH_ASSOC);

    foreach ($properties as &$p) {
        $p['media_urls'] = !empty($p['media_urls']) ? explode(',', $p['media_urls']) : [];
        $p['amenity_ids'] = !empty($p['amenity_ids']) ? array_map('intval', explode(',', $p['amenity_ids'])) : [];
    }

    echo json_encode([
        "status" => "success",
        "count"  => $totalCount,
        "data"   => $properties
    ]);
} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
?>
