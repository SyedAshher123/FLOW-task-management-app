<?php
header("Content-Type: application/json; charset=UTF-8");
include "conn.php";

// Read JSON POST body
$data = json_decode(file_get_contents("php://input"), true);

$currentUserId = isset($data['current_user_id']) ? intval($data['current_user_id']) : 0;
$selectedUserId = isset($data['selected_user_id']) ? intval($data['selected_user_id']) : 0;

if ($currentUserId <= 0 || $selectedUserId <= 0) {
    echo json_encode(["status" => "error", "message" => "Missing or invalid fields"]);
    exit();
}

// Ensure consistent order to avoid duplicate chats
if ($currentUserId < $selectedUserId) {
    $user1 = $currentUserId;
    $user2 = $selectedUserId;
} else {
    $user1 = $selectedUserId;
    $user2 = $currentUserId;
}

// Check if chat already exists
$check = $conn->prepare("SELECT chat_id FROM user_chats WHERE user1_id = ? AND user2_id = ? LIMIT 1");
$check->bind_param("ii", $user1, $user2);
$check->execute();
$check->store_result();

if ($check->num_rows > 0) {
    $check->close();
    echo json_encode(["status" => "error", "message" => "Chat already exists"]);
    exit();
}
$check->close();

// Insert new chat
$stmt = $conn->prepare("INSERT INTO user_chats (user1_id, user2_id) VALUES (?, ?)");
$stmt->bind_param("ii", $user1, $user2);

if ($stmt->execute()) {
    $chat_id = $conn->insert_id;

    // Insert an initial placeholder message (optional)
    $initialMessage = "Tap to start chatting";
    $timestamp = time();

    $stmtMsg = $conn->prepare("INSERT INTO chat_messages (chat_id, sender_id, receiver_id, message_text, timestamp) VALUES (?, ?, ?, ?, ?)");
    $stmtMsg->bind_param("iiisi", $chat_id, $currentUserId, $selectedUserId, $initialMessage, $timestamp);
    $stmtMsg->execute();
    $stmtMsg->close();

    echo json_encode(["status" => "success", "message" => "Chat added successfully"]);
} else {
    echo json_encode(["status" => "error", "message" => "Failed to add chat: " . $conn->error]);
}

$stmt->close();
$conn->close();
?>
