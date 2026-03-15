<?php
header('Content-Type: application/json');
include "conn.php";

$data = json_decode(file_get_contents("php://input"), true);
$currentUserId = $data['current_user_id'] ?? '';

if (empty($currentUserId)) {
    echo json_encode(["status" => "error", "message" => "Missing current_user_id"]);
    exit();
}

// user_chats table structure same as socially
$stmt = $conn->prepare("
    SELECT uc.chat_id, 
           CASE WHEN uc.user1_id = ? THEN uc.user2_id ELSE uc.user1_id END AS partner_id
    FROM user_chats uc
    WHERE uc.user1_id = ? OR uc.user2_id = ?
");
$stmt->bind_param("iii", $currentUserId, $currentUserId, $currentUserId);
$stmt->execute();
$result = $stmt->get_result();

$chats = [];

while ($row = $result->fetch_assoc()) {
    $partnerId = $row['partner_id'];
    $chatId = $row['chat_id'];

    $stmtUser = $conn->prepare("SELECT name, profile_photo FROM users WHERE id=?");
    $stmtUser->bind_param("i", $partnerId);
    $stmtUser->execute();
    $userRes = $stmtUser->get_result()->fetch_assoc();
    $stmtUser->close();

    $stmtMsg = $conn->prepare("SELECT message_text, message_type, timestamp FROM chat_messages WHERE chat_id=? ORDER BY timestamp DESC LIMIT 1");
    $stmtMsg->bind_param("i", $chatId);
    $stmtMsg->execute();
    $msgRes = $stmtMsg->get_result()->fetch_assoc();
    $stmtMsg->close();

    $lastMessage = isset($msgRes['message_type']) && $msgRes['message_type'] == 'image' ? "Photo" : ($msgRes['message_text'] ?? "Tap to start chatting");
    $lastMessageTime = $msgRes['timestamp'] ?? "";

    $chats[] = [
        "chat_id" => $chatId,
        "user_id" => $partnerId,
        "username" => $userRes['name'] ?? "",
        "display_name" => $userRes['name'] ?? "",
        "profile_picture_url" => $userRes['profile_photo'] ?? "",
        "last_message" => $lastMessage,
        "last_message_time" => $lastMessageTime
    ];
}

echo json_encode(["status" => "success", "chats" => $chats]);

$stmt->close();
$conn->close();
?>
