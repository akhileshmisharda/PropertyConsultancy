<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

$logFile = __DIR__ . '/uploads/php_debug.log';
function writeDebug($msg) {
    global $logFile;
    $time = date('Y-m-d H:i:s');
    file_put_contents($logFile, "[$time] [UPLOAD_MEDIA] " . (is_array($msg) ? json_encode($msg) : $msg) . PHP_EOL, FILE_APPEND);
}

$config_path = __DIR__ . '/../ERP/config/database.php';
if (file_exists($config_path)) {
    require_once $config_path;
} else {
    writeDebug("Error: Database config missing");
    echo json_encode(["status" => "error", "message" => "Database config missing"]);
    exit();
}

try {
    $pdo = new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4', DB_USER, DB_PASS, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);

    writeDebug("Incoming POST data: " . json_encode($_POST));
    writeDebug("Incoming FILES data: " . json_encode($_FILES));

    $property_id = $_POST['property_id'] ?? null;
    $tag_id = $_POST['image_tag_id'] ?? null;
    $media_type = $_POST['media_type'] ?? 'image';

    if (!$property_id) {
        writeDebug("Error: property_id is missing");
        echo json_encode(["status" => "error", "message" => "Property ID is required"]);
        exit();
    }

    if (!isset($_FILES['file'])) {
        writeDebug("Error: No file key in _FILES");
        echo json_encode(["status" => "error", "message" => "No file uploaded"]);
        exit();
    }

    $upload_root = 'uploads/properties/' . $property_id . '/';
    if (!is_dir($upload_root)) {
        if (mkdir($upload_root, 0777, true)) {
            writeDebug("Created directory: $upload_root");
        } else {
            writeDebug("Failed to create directory: $upload_root");
        }
    }

    $file = $_FILES['file'];
    $ext = pathinfo($file['name'], PATHINFO_EXTENSION);
    $filename = uniqid('media_') . '.' . $ext;
    $target_path = $upload_root . $filename;

    if (move_uploaded_file($file['tmp_name'], $target_path)) {
        $file_url = 'server/' . $target_path;
        writeDebug("File moved to: $target_path. URL: $file_url");

        $sql = "INSERT INTO pro_property_media (property_id, image_tag_id, media_type, file_url, is_primary, display_order, created_at)
                VALUES (:pid, :tid, :mtype, :url, 0, (SELECT IFNULL(MAX(display_order), 0) + 1 FROM pro_property_media WHERE property_id = :pid2), NOW())";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':pid' => $property_id,
            ':tid' => $tag_id,
            ':mtype' => $media_type,
            ':url' => $file_url,
            ':pid2' => $property_id
        ]);

        $media_id = $pdo->lastInsertId();
        writeDebug("Database entry created. Media ID: $media_id");

        echo json_encode([
            "status" => "success",
            "message" => $file_url, // Returning URL in message as app expects
            "media_id" => $media_id
        ]);
    } else {
        writeDebug("Error: Failed to move_uploaded_file to $target_path");
        echo json_encode(["status" => "error", "message" => "Failed to move uploaded file"]);
    }

} catch (PDOException $e) {
    writeDebug("PDO Error: " . $e->getMessage());
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
?>
