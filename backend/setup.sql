ALTER USER 'root'@'localhost' IDENTIFIED BY 'RevConnect2024!';
UNINSTALL COMPONENT 'file://component_validate_password';
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
CREATE DATABASE IF NOT EXISTS revconnect_db;
FLUSH PRIVILEGES;
