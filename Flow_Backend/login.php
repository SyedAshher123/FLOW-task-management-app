<?php
header('Content-Type: application/json');
include 'conn.php';

// Read JSON input
$data = json_decode(file_get_contents("php://input"), true);

$email = $data['email'] ?? '';
$password = $data['password'] ?? '';

if (empty($email) || empty($password)) {
    echo json_encode([
        "success" => false,
        "message" => "Email and password are required"
    ]);
    exit;
}

// Search user by email
$stmt = $conn->prepare("SELECT * FROM users WHERE email=?");
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows == 0) {
    echo json_encode([
        "success" => false,
        "message" => "Email not found"
    ]);
    exit;
}

$user = $result->fetch_assoc();

// Verify password (not hashed in your sample)
if ($password !== $user['password']) {
    echo json_encode([
        "success" => false,
        "message" => "Incorrect password"
    ]);
    exit;
}

// SUCCESS
echo json_encode([
    "success" => true,
    "message" => "Login successful",
    "user" => [
        "id" => $user['id'],
        "name" => $user['name'],
        "email" => $user['email'],
        "profile_photo" => $user['profile_photo']
    ]
]);

$stmt->close();
$conn->close();
?>
