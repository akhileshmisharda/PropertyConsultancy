<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

// Database configuration
$host = "localhost";
$db_name = "rishya";
$username = "root";
$password = "";

try {
    $conn = new PDO("mysql:host=$host;dbname=$db_name", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch(PDOException $e) {
    echo json_encode(["status" => "error", "message" => "Connection failed: " . $e->getMessage()]);
    exit();
}

$landlord_id = $_GET['landlord_id'] ?? null;

if ($landlord_id) {
    $query = "SELECT * FROM pro_properties WHERE landlord_id = :landlord_id ORDER BY created_at DESC";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(":landlord_id", $landlord_id);
    $stmt->execute();

    $properties = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode($properties);
} else {
    // If no landlord_id, maybe return all available properties (for tenants)
    $query = "SELECT * FROM pro_properties WHERE status = 'available' ORDER BY created_at DESC";
    $stmt = $conn->prepare($query);
    $stmt->execute();

    $properties = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode($properties);
}
?>
