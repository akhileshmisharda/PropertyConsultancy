<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

// Debugging function
function debug_log($message) {
    error_log("[php_debug] " . (is_array($message) || is_object($message) ? json_encode($message) : $message));
}

debug_log("--- Incoming Login Request ---");

$host = "localhost";
$db_name = "rishya";
$username = "root";
$password = "";

try {
    $conn = new PDO("mysql:host=$host;dbname=$db_name", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    debug_log("Database connected successfully");
} catch(PDOException $e) {
    debug_log("Connection failed: " . $e->getMessage());
    echo json_encode(["status" => "error", "message" => "Connection failed: " . $e->getMessage()]);
    exit();
}

$raw_input = file_get_contents("php://input");
debug_log("Raw Input: " . $raw_input);

$data = json_decode($raw_input);
debug_log("Decoded Data: " . json_encode($data));

if (!empty($data->email) && !empty($data->password)) {

    debug_log("Searching for user: " . $data->email);
    // Support login via email or phone
    $query = "SELECT * FROM pro_users WHERE email = :email OR phone = :email LIMIT 1";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(":email", $data->email);
    $stmt->execute();

    if ($stmt->rowCount() > 0) {
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        debug_log("User found, verifying password...");

        // Verify password hash
        if (password_verify($data->password, $user['password_hash'])) {
            debug_log("Password verified successfully for user: " . $user['user_id']);
            unset($user['password_hash']); // Don't return the hash
            echo json_encode([
                "status" => "success",
                "message" => "Login successful",
                "user" => $user
            ]);
        } else {
            debug_log("Password verification failed");
            echo json_encode(["status" => "error", "message" => "Invalid credentials."]);
        }
    } else {
        debug_log("User not found in database");
        echo json_encode(["status" => "error", "message" => "User not found."]);
    }
} else {
    debug_log("Incomplete login data provided");
    echo json_encode(["status" => "error", "message" => "Please provide email and password."]);
}
?>
