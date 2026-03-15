<?php
header('Content-Type: application/json');
include("conn.php");

$data = json_decode(file_get_contents("php://input"), true);
$message_id = isset($data['message_id']) ? intval($data['message_id']) : 0;
$sender_id = isset($data['sender_id']) ? intval($data['sender_id']) : 0;

if (!$message_id || !$sender_id) {
    echo json_encode(["status"=>"error","message"=>"Missing fields"]);
    exit();
}

// check owner and time window
$chk = $conn->prepare("SELECT sender_id, timestamp FROM messages WHERE id = ? LIMIT 1");
$chk->bind_param("i", $message_id);
$chk->execute();
$res = $chk->get_result();
$row = $res->fetch_assoc();
if (!$row) { echo json_encode(["status"=>"error","message"=>"Message not found"]); exit(); }
if (intval($row['sender_id']) !== $sender_id) { echo json_encode(["status"=>"error","message"=>"Not owner"]); exit(); }
if ((time() - intval($row['timestamp'])) > (5 * 60)) { echo json_encode(["status"=>"error","message"=>"Delete window expired"]); exit(); }

// update message to "deleted"
$upd = $conn->prepare("UPDATE messages SET message_text = 'Message deleted', deleted = 1 WHERE id = ?");
$upd->bind_param("i", $message_id);
if ($upd->execute()) {
    echo json_encode(["status"=>"success","message"=>"Message deleted"]);
} else {
    echo json_encode(["status"=>"error","message"=>"Delete failed"]);
}

$upd->close();
$conn->close();
?>
