#!/bin/bash
# ============================================================
# RevConnect — Deploy to EC2
# Run this from your LOCAL machine (Windows Git Bash / WSL)
# Usage: ./deploy.sh <EC2_PUBLIC_IP> <PEM_KEY_PATH>
# Example: ./deploy.sh 54.123.45.67 ~/revconnect-key.pem
# ============================================================

set -e

EC2_IP=$1
PEM_KEY=$2

if [ -z "$EC2_IP" ] || [ -z "$PEM_KEY" ]; then
    echo "Usage: ./deploy.sh <EC2_PUBLIC_IP> <PEM_KEY_PATH>"
    echo "Example: ./deploy.sh 54.123.45.67 ~/revconnect-key.pem"
    exit 1
fi

EC2_USER="ec2-user"
APP_DIR="/home/ec2-user/revconnect"
JAR_NAME="revconnect-1.0.0.jar"

echo "========================================="
echo "  RevConnect — Deploying to AWS"
echo "  Target: $EC2_USER@$EC2_IP"
echo "========================================="

# --- 1. Build the JAR locally ---
echo "[1/4] Building JAR (skipping tests)..."
cd "$(dirname "$0")/.."
./mvnw clean package -DskipTests -B
echo "  ✅ JAR built successfully"

# --- 2. Upload JAR to EC2 ---
echo "[2/4] Uploading JAR to EC2..."
scp -i "$PEM_KEY" -o StrictHostKeyChecking=no \
    target/$JAR_NAME \
    $EC2_USER@$EC2_IP:$APP_DIR/app.jar
echo "  ✅ JAR uploaded"

# --- 3. Upload setup script (if first time) ---
echo "[3/4] Uploading setup script..."
scp -i "$PEM_KEY" -o StrictHostKeyChecking=no \
    deploy/setup-ec2.sh \
    $EC2_USER@$EC2_IP:$APP_DIR/setup-ec2.sh

# --- 4. Restart the application ---
echo "[4/4] Restarting application on EC2..."
ssh -i "$PEM_KEY" -o StrictHostKeyChecking=no $EC2_USER@$EC2_IP << 'REMOTE'
    # Stop existing instance
    pkill -f "java -jar" || true
    sleep 2

    cd /home/ec2-user/revconnect

    # Set environment variables
    export DB_USERNAME=root
    export DB_PASSWORD="RevConnect2024!"
    export JWT_SECRET="RevConnectSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong2024"

    # Start the app in background
    nohup java -jar app.jar \
        --spring.profiles.active=prod \
        -Xmx512m \
        > /dev/null 2>&1 &

    sleep 5
    if pgrep -f "java -jar" > /dev/null; then
        echo "  ✅ Application started successfully!"
    else
        echo "  ❌ Application failed to start. Check logs:"
        echo "     tail -f /home/ec2-user/revconnect/app.log"
    fi
REMOTE

echo ""
echo "========================================="
echo "  ✅ Deployment Complete!"
echo "========================================="
echo "  🌐 API:     http://$EC2_IP:8080"
echo "  📖 Swagger: http://$EC2_IP:8080/swagger-ui.html"
echo "  📋 Logs:    ssh -i $PEM_KEY $EC2_USER@$EC2_IP 'tail -f $APP_DIR/app.log'"
echo "========================================="
