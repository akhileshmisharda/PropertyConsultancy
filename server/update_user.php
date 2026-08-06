<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

require_once __DIR__ . '/../ERP/config/database.php';

try {
    $conn = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME, DB_USER, DB_PASS);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch(PDOException $e) {
    echo json_encode(["status" => "error", "message" => "Connection failed: " . $e->getMessage()]);
    exit();
}

$raw_input = file_get_contents("php://input");
$data = json_decode($raw_input);

if (!empty($data->user_id) && !empty($data->first_name) && !empty($data->last_name) && !empty($data->email) && !empty($data->phone)) {

    $query = "UPDATE pro_users SET
                first_name = :first_name,
                last_name = :last_name,
                email = :email,
                phone = :phone,
                profile_image_url = :profile_image_url,
                role = :role
              WHERE user_id = :user_id";

    $stmt = $conn->prepare($query);

    $stmt->bindParam(":first_name", $data->first_name);
    $stmt->bindParam(":last_name", $data->last_name);
    $stmt->bindParam(":email", $data->email);
    $stmt->bindParam(":phone", $data->phone);
    $stmt->bindParam(":profile_image_url", $data->profile_image_url);
    $stmt->bindParam(":role", $data->role);
    $stmt->bindParam(":user_id", $data->user_id);

    if ($stmt->execute()) {
        // Fetch updated user data to return
        $fetch_query = "SELECT user_id, first_name, last_name, email, phone, role, profile_image_url, is_verified, status FROM pro_users WHERE user_id = :user_id";
        $fetch_stmt = $conn->prepare($fetch_query);
        $fetch_stmt->bindParam(":user_id", $data->user_id);
        $fetch_stmt->execute();
        $user = $fetch_stmt->fetch(PDO::FETCH_ASSOC);

        echo json_encode([
            "status" => "success",
            "message" => "Profile updated successfully.",
            "user" => $user
        ]);
    } else {
        echo json_encode(["status" => "error", "message" => "Failed to update profile."]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Incomplete data provided."]);
}
?>
