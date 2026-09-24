#!/bin/bash
# ============================================================
# RevConnect — EC2 Instance Setup Script
# Run this ONCE on your new EC2 instance (Amazon Linux 2023)
# Usage: chmod +x setup-ec2.sh && sudo ./setup-ec2.sh
# ============================================================

set -e

echo "========================================="
echo "  RevConnect EC2 Setup (Amazon Linux 2023)"
echo "========================================="

# --- 1. System Update ---
echo "[1/6] Updating system packages..."
dnf update -y

# --- 2. Allocate 2GB Swap Memory (prevents OOM on t3.micro/1GB RAM) ---
echo "[2/6] Configuring 2GB Swap Memory..."
if [ ! -f /swapfile ]; then
    fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    echo "  ✅ 2GB Swap enabled"
else
    echo "  ✅ Swap already exists"
fi

# --- 3. Install Java 21 (Amazon Corretto) ---
echo "[3/6] Installing Java 21 (Amazon Corretto)..."
dnf install -y java-21-amazon-corretto-devel
java -version

# --- 4. Install Nginx ---
echo "[4/6] Installing and starting Nginx..."
dnf install -y nginx
mkdir -p /var/www/html/revconnect-ui/browser
chown -R ec2-user:ec2-user /var/www/html/revconnect-ui
chmod -R 755 /var/www/html/revconnect-ui
systemctl start nginx
systemctl enable nginx

# --- 5. Install & Configure MariaDB / MySQL ---
echo "[5/6] Installing MySQL/MariaDB..."
dnf install -y mariadb105-server
systemctl start mariadb
systemctl enable mariadb

# Configure database & set root password to 'root' (matches application-prod.properties and Jenkinsfile)
echo "  Configuring database and root password..."
mysql -u root <<EOF
CREATE DATABASE IF NOT EXISTS revconnect_db;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
FLUSH PRIVILEGES;
EOF

echo "  ✅ Database 'revconnect_db' ready"
echo "  ✅ Root password set to: root"

# --- 6. Create Application Directories ---
echo "[6/6] Setting up application directories..."
mkdir -p /home/ec2-user/revconnect/uploads
chown -R ec2-user:ec2-user /home/ec2-user/revconnect
chmod -R 755 /home/ec2-user/revconnect

echo ""
echo "========================================="
echo "  ✅ Server Provisioning Complete!"
echo "========================================="
echo "  Java:    $(java -version 2>&1 | head -1)"
echo "  MySQL:   $(mysql --version)"
echo "  Nginx:   $(nginx -v 2>&1)"
echo "  DB Name: revconnect_db"
echo "  DB User: root"
echo "  DB Pass: root"
echo "========================================="
