<?php
header('Content-Type: application/json');

// Database configuration
$host = "localhost";
$username = "your_db_username";
$password = "your_db_password";
$dbname = "your_db_name";

// Create connection
$conn = new mysqli($host, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Connection failed: " . $conn->connect_error]));
}

$sql = "SELECT category_id, name, `option`, slug, description, is_active, created_at FROM pro_categories WHERE is_active = 1";
$result = $conn->query($sql);

$categories = [];

if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $categories[] = $row;
    }
    echo json_encode(["status" => "success", "data" => $categories]);
} else {
    echo json_encode(["status" => "success", "data" => []]);
}

$conn->close();
?>
