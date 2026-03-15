<?php
header("Content-Type: application/json; charset=UTF-8");
include "conn.php";

$currentUserId = $_GET['user_id'] ?? '';

if (!$currentUserId) {
    echo json_encode(["status" => "error", "message" => "Missing user_id"]);
    exit();
}

// Flow table: users(id, name, email, password, profile_photo, created_at)
$stmt = $conn->prepare("SELECT name, profile_photo FROM users WHERE id = ?");
$stmt->bind_param("i", $currentUserId);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    echo json_encode(["status" => "success", "user" => $row]);
} else {
    echo json_encode(["status" => "error", "message" => "User not found"]);
}

$stmt->close();
$conn->close();
?>
