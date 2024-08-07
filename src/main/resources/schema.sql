CREATE TABLE translation_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ip_address VARCHAR(255),
    input_text TEXT,
    translated_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);