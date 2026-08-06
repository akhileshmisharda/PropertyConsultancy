<?php
include 'db_config.php';

$sql = "SELECT p.*, (SELECT file_url FROM property_media pm WHERE pm.property_id = p.property_id LIMIT 1) as primary_image
        FROM properties p";
$result = $conn->query($sql);

$properties = [];
if ($result->num_size > 0) {
    while($row = $result->fetch_assoc()) {
        $properties[] = $row;
    }
}

echo json_encode($properties);
?>
