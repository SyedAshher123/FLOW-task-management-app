<?php
header('Content-Type: application/json');
include "conn.php";

$currentUserId = isset($_GET['current_user_id']) ? intval($_GET['current_user_id']) : 0;

if ($currentUserId <= 0) {
    echo json_encode(["status" => "error", "message" => "Missing or invalid current_user_id"]);
    exit();
}

// Fetch all users except current user
$stmt = $conn->prepare("SELECT id, name, email, profile_photo FROM users WHERE id != ?");
$stmt->bind_param("i", $currentUserId);
$stmt->execute();
$result = $stmt->get_result();

$users = [];
while ($row = $result->fetch_assoc()) {
    // Keep key names consistent for Android parsing
    $users[] = [
        "id" => $row['id'],
        "name" => $row['name'],
        "email" => $row['email'],
        "profile_photo" => $row['profile_photo']
    ];
}

echo json_encode(["status" => "success", "users" => $users]);

$stmt->close();
$conn->close();
?>
