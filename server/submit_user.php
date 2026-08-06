<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

// Debugging function
function debug_log($message) {
    error_log("[php_debug] " . (is_array($message) || is_object($message) ? json_encode($message) : $message));
}

debug_log("--- Incoming Registration Request ---");

// Database configuration
$host = "localhost";
$db_name = "rishya";
$username = "root"; // Update with your DB username
$password = "";     // Update with your DB password

try {
    $conn = new PDO("mysql:host=$host;dbname=$db_name", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    debug_log("Database connected successfully");
} catch(PDOException $e) {
    debug_log("Connection failed: " . $e->getMessage());
    echo json_encode(["status" => "error", "message" => "Connection failed: " . $e->getMessage()]);
    exit();
}

// Get POST data
$raw_input = file_get_contents("php://input");
debug_log("Raw Input: " . $raw_input);

$data = json_decode($raw_input);
debug_log("Decoded Data: " . json_encode($data));

if (!empty($data->first_name) && !empty($data->last_name) && !empty($data->email) && !empty($data->phone) && !empty($data->password)) {

    // Check if email already exists
    debug_log("Checking for existing user with email: " . $data->email . " or phone: " . $data->phone);
    $check_query = "SELECT user_id FROM pro_users WHERE email = :email OR phone = :phone";
    $check_stmt = $conn->prepare($check_query);
    $check_stmt->bindParam(":email", $data->email);
    $check_stmt->bindParam(":phone", $data->phone);
    $check_stmt->execute();

    if ($check_stmt->rowCount() > 0) {
        debug_log("User already exists");
        echo json_encode(["status" => "error", "message" => "User with this email or phone already exists."]);
    } else {
        debug_log("User does not exist, proceeding with registration");

        // Hash password
        $password_hash = password_hash($data->password, PASSWORD_BCRYPT);

        // Prepare insert query
        $query = "INSERT INTO pro_users (first_name, last_name, email, phone, password_hash, role, profile_image_url, is_verified, status)
                  VALUES (:first_name, :last_name, :email, :phone, :password_hash, :role, :profile_image_url, :is_verified, :status)";

        $stmt = $conn->prepare($query);

        // Set defaults for meta fields
        $is_verified = 0;
        $status = "active";
        $profile_image_url = $data->profile_image_url ?? null;
        $role = $data->role ?? "tenant";

        debug_log("Binding parameters: Role=" . $role . ", Verified=" . $is_verified . ", Status=" . $status);

        $stmt->bindParam(":first_name", $data->first_name);
        $stmt->bindParam(":last_name", $data->last_name);
        $stmt->bindParam(":email", $data->email);
        $stmt->bindParam(":phone", $data->phone);
        $stmt->bindParam(":password_hash", $password_hash);
        $stmt->bindParam(":role", $role);
        $stmt->bindParam(":profile_image_url", $profile_image_url);
        $stmt->bindParam(":is_verified", $is_verified);
        $stmt->bindParam(":status", $status);

        try {
            if ($stmt->execute()) {
                debug_log("Data inserted successfully. User ID: " . $conn->lastInsertId());
                echo json_encode(["status" => "success", "message" => "Account created successfully."]);
            } else {
                $error_info = $stmt->errorInfo();
                debug_log("Execution failed: " . json_encode($error_info));
                echo json_encode(["status" => "error", "message" => "Failed to create account."]);
            }
        } catch (Exception $e) {
            debug_log("Exception during execute: " . $e->getMessage());
            echo json_encode(["status" => "error", "message" => "Database error: " . $e->getMessage()]);
        }
    }
} else {
    debug_log("Incomplete data. Missing fields detected.");
    echo json_encode(["status" => "error", "message" => "Incomplete data provided."]);
}
?>
