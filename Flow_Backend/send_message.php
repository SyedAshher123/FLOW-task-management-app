<?php
header('Content-Type: application/json');
include("conn.php");

$data = json_decode(file_get_contents("php://input"), true);

$chat_id = isset($data['chat_id']) ? intval($data['chat_id']) : 0;
$sender_id = isset($data['sender_id']) ? intval($data['sender_id']) : 0;
$message_text = isset($data['message_text']) ? $data['message_text'] : '';
$image_base64 = isset($data['image_base64']) ? $data['image_base64'] : null;
$message_type = isset($data['message_type']) ? $data['message_type'] : 'text';
$timestamp = isset($data['timestamp']) ? intval($data['timestamp']) : time();
$is_vanish = isset($data['is_vanish']) ? intval($data['is_vanish']) : 0;

if (!$chat_id || !$sender_id) {
    echo json_encode(["status"=>"error","message"=>"Missing chat_id or sender_id"]);
    exit();
}

$stmt = $conn->prepare("INSERT INTO messages 
    (chat_id, sender_id, message_text, image_base64, message_type, timestamp, is_vanish) 
    VALUES (?, ?, ?, ?, ?, ?, ?)");

$stmt->bind_param("issssii", 
    $chat_id, 
    $sender_id, 
    $message_text, 
    $image_base64, 
    $message_type, 
    $timestamp,
    $is_vanish
);

if ($stmt->execute()) {
    $msgId = $stmt->insert_id;
    echo json_encode(["status"=>"success","message"=>"Message sent","message_id"=>$msgId]);
} else {
    echo json_encode(["status"=>"error","message"=>"Send failed: ".$conn->error]);
}

$stmt->close();
$conn->close();
?>
