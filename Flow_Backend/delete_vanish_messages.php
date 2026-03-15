<?php
header('Content-Type: application/json');
include("conn.php");

$data = json_decode(file_get_contents("php://input"), true);
$chat_id = isset($data["chat_id"]) ? intval($data["chat_id"]) : 0;

if (!$chat_id) {
    echo json_encode(["status"=>"error","message"=>"Missing chat_id"]);
    exit();
}

$stmt = $conn->prepare("DELETE FROM messages WHERE chat_id = ? AND is_vanish = 1");
$stmt->bind_param("i", $chat_id);

if ($stmt->execute()) {
    echo json_encode(["status"=>"success"]);
} else {
    echo json_encode(["status"=>"error"]);
}

$stmt->close();
$conn->close();
?>
