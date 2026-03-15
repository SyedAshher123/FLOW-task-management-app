<?php
header('Content-Type: application/json');
include("conn.php");

$data = json_decode(file_get_contents("php://input"), true);

$chat_id = isset($data['chat_id']) ? intval($data['chat_id']) : 0;
$limit = isset($data['limit']) ? intval($data['limit']) : 1000;
$vanish_mode = isset($data['vanish_mode']) ? intval($data['vanish_mode']) : 0;

if (!$chat_id) {
    echo json_encode(["status"=>"error","message"=>"Missing chat_id"]);
    exit();
}

if ($vanish_mode == 1) {
    $sql = "SELECT id AS message_id, chat_id, sender_id, message_text, image_base64,
            message_type, edited, edited_at, deleted, timestamp, is_vanish
            FROM messages WHERE chat_id = ? ORDER BY timestamp ASC LIMIT ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ii", $chat_id, $limit);
} else {
    $sql = "SELECT id AS message_id, chat_id, sender_id, message_text, image_base64,
            message_type, edited, edited_at, deleted, timestamp, is_vanish
            FROM messages WHERE chat_id = ? AND is_vanish = 0 ORDER BY timestamp ASC LIMIT ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ii", $chat_id, $limit);
}

$stmt->execute();
$res = $stmt->get_result();

$messages = [];
while ($row = $res->fetch_assoc()) {
    $messages[] = $row;
}

echo json_encode(["status"=>"success","messages"=>$messages]);

$stmt->close();
$conn->close();
?>
