<?php
/**
 * لوحة التحكم الإدارية لتطبيق أطباء زمار (Admin Panel PHP Backend)
 * تدعم الرفع المباشر عبر PHP $_FILES وكذلك مزامنة Firebase Realtime Database
 */

// Enable error reporting during development
error_reporting(E_ALL);
ini_set('display_errors', 0);

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Upload directory for banner files if handled by PHP
$uploadDir = __DIR__ . '/uploads/banners/';
if (!is_dir($uploadDir)) {
    @mkdir($uploadDir, 0755, true);
}

// Handle AJAX Image Upload POST
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_FILES['banner_image'])) {
    header('Content-Type: application/json; charset=utf-8');
    
    $file = $_FILES['banner_image'];
    if ($file['error'] !== UPLOAD_ERR_OK) {
        echo json_encode([
            'success' => false,
            'message' => 'فشل في رفع الملف، رمز الخطأ: ' . $file['error']
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $allowedExtensions = ['jpg', 'jpeg', 'png', 'webp', 'gif'];
    $fileInfo = pathinfo($file['name']);
    $ext = strtolower($fileInfo['extension'] ?? '');

    if (!in_array($ext, $allowedExtensions)) {
        echo json_encode([
            'success' => false,
            'message' => 'نوع الملف غير مدعوم. يرجى اختيار صورة بصيغة JPG أو PNG أو WEBP.'
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $newFileName = 'banner_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
    $targetFilePath = $uploadDir . $newFileName;

    if (move_uploaded_file($file['tmp_name'], $targetFilePath)) {
        $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https://" : "http://";
        $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
        $scriptPath = dirname($_SERVER['SCRIPT_NAME']);
        $publicUrl = rtrim($protocol . $host . $scriptPath, '/') . '/uploads/banners/' . $newFileName;

        echo json_encode([
            'success' => true,
            'url' => $publicUrl,
            'fileName' => $newFileName,
            'message' => 'تم رفع الصورة بنجاح!'
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'تعذر حفظ الصورة على الخادم.'
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }
}
?>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>لوحة التحكم الإدارية - أطباء زمار (PHP Edition)</title>
    <!-- Redirect to or include admin.html -->
    <meta http-equiv="refresh" content="0; url=admin.html">
</head>
<body class="bg-gray-100 flex items-center justify-center min-h-screen">
    <div class="text-center p-8 bg-white rounded-2xl shadow">
        <h1 class="text-xl font-bold text-gray-800 mb-2">جاري فتح لوحة التحكم...</h1>
        <p class="text-sm text-gray-500 mb-4">إذا لم يتم تحويلك تلقائياً، اضغط على الرابط أدناه:</p>
        <a href="admin.html" class="inline-block bg-indigo-600 text-white font-bold px-6 py-2.5 rounded-xl hover:bg-indigo-700">فتح لوحة التحكم (admin.html)</a>
    </div>
</body>
</html>
