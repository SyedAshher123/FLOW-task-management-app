<?php
header('Content-Type: application/json');
include 'conn.php';

// Read JSON input
$data = json_decode(file_get_contents("php://input"), true);

// Required fields
$required = ['name', 'email', 'password'];
foreach ($required as $field) {
    if (empty($data[$field])) {
        echo json_encode([
            "success" => false,
            "message" => "Field '$field' is required"
        ]);
        exit;
    }
}

$name = $data['name'];
$email = $data['email'];
$password = $data['password'];
$profile_photo = $data['profile_photo'] ?? null; // Base64 string or null

// Check if email exists
$check = $conn->prepare("SELECT * FROM users WHERE email=?");
$check->bind_param("s", $email);
$check->execute();
$result = $check->get_result();

if ($result->num_rows > 0) {
    echo json_encode([
        "success" => false,
        "message" => "Email already exists"
    ]);
    exit;
}

// Insert new user
$stmt = $conn->prepare("INSERT INTO users (name, email, password, profile_photo) VALUES (?, ?, ?, ?)");
$stmt->bind_param("ssss", 
    $name, 
    $email, 
    $password,   // you can replace this later with a hash
    $profile_photo
);

if ($stmt->execute()) {
    echo json_encode([
        "success" => true,
        "message" => "Account created successfully!",
        "user_id" => $stmt->insert_id
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $stmt->error
    ]);
}

$stmt->close();
$conn->close();
?>
