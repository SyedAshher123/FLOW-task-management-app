<?php
$host = "localhost";
$user = "root";
$pass = "";
$db = "flow";

$conn = new mysqli($host, $user, $pass, $db);

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
// Ensure UTF-8
//$conn->set_charset("utf8mb4");
?>

