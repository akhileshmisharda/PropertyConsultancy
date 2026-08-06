<?php
include 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $landlord_id = $_POST['landlord_id'];
    $title = $_POST['title'];
    $description = $_POST['description'];
    $city = $_POST['city'];
    $state = $_POST['state'];
    $price_per_month = $_POST['price_per_month'];
    $status = 'available';

    $sql = "INSERT INTO properties (landlord_id, title, description, city, state, price_per_month, status)
            VALUES ('$landlord_id', '$title', '$description', '$city', '$state', '$price_per_month', '$status')";

    if ($conn->query($sql) === TRUE) {
        $property_id = $conn->insert_id;

        // Handle Media Upload (Simplified)
        if (isset($_FILES['media'])) {
            $target_dir = "uploads/";
            if (!file_exists($target_dir)) {
                mkdir($target_dir, 0777, true);
            }
            $file_name = time() . "_" . basename($_FILES["media"]["name"]);
            $target_file = $target_dir . $file_name;
            if (move_uploaded_data($_FILES["media"]["tmp_name"], $target_file)) {
                $conn->query("INSERT INTO property_media (property_id, file_url) VALUES ('$property_id', '$target_file')");
            }
        }

        echo json_encode(["status" => "success", "message" => "Property added successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => $conn->error]);
    }
}
?>
