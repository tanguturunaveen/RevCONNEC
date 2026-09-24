param (
    [Parameter(Mandatory=$true)]
    [string]$Ec2Ip,

    [Parameter(Mandatory=$true)]
    [string]$PemKeyPath,

    [Parameter(Mandatory=$false)]
    [string]$Ec2User = "ec2-user"
)

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  RevConnect — Direct AWS Deployment" -ForegroundColor Cyan
Write-Host "  Target: $Ec2User@$Ec2Ip" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# Resolve paths
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir

# 1. Build Backend
Write-Host "`n[1/4] Building Backend JAR..." -ForegroundColor Yellow
Push-Location "$projectRoot\..\backend"
cmd /c "mvnw.cmd clean package -DskipTests"
Pop-Location

# 2. Build Frontend
Write-Host "`n[2/4] Building Frontend..." -ForegroundColor Yellow
Push-Location "$projectRoot\..\frontend"
cmd /c "npm install --legacy-peer-deps && set NODE_OPTIONS=--max_old_space_size=4096 && npm run build"
Pop-Location

# 3. Transfer and configure EC2
Write-Host "`n[3/4] Uploading artifacts to EC2 ($Ec2Ip)..." -ForegroundColor Yellow

$jarPath = "$projectRoot\..\backend\target\revconnect-1.0.0.jar"
$servicePath = "$projectRoot\..\backend\deploy\revconnect-backend.service"
$sslPath = "$projectRoot\..\backend\deploy\ssl-setup.sh"
$frontendDist = "$projectRoot\..\frontend\dist\revconnect-ui\browser\*"

scp -o StrictHostKeyChecking=accept-new -i "$PemKeyPath" "$jarPath" "${Ec2User}@${Ec2Ip}:/home/${Ec2User}/revconnect-1.0.0.jar"
scp -o StrictHostKeyChecking=accept-new -i "$PemKeyPath" "$servicePath" "${Ec2User}@${Ec2Ip}:/tmp/revconnect-backend.service"
scp -o StrictHostKeyChecking=accept-new -i "$PemKeyPath" "$sslPath" "${Ec2User}@${Ec2Ip}:/tmp/ssl-setup.sh"

ssh -o StrictHostKeyChecking=accept-new -i "$PemKeyPath" "${Ec2User}@${Ec2Ip}" "mkdir -p /tmp/frontend /home/${Ec2User}/revconnect/uploads"
scp -o StrictHostKeyChecking=accept-new -i "$PemKeyPath" -pr $frontendDist "${Ec2User}@${Ec2Ip}:/tmp/frontend/"

# 4. Restart Services
Write-Host "`n[4/4] Restarting Backend & Nginx on EC2..." -ForegroundColor Yellow
ssh -o StrictHostKeyChecking=accept-new -i "$PemKeyPath" "${Ec2User}@${Ec2Ip}" @"
sudo mv /tmp/revconnect-backend.service /etc/systemd/system/revconnect-backend.service
sudo chown root:root /etc/systemd/system/revconnect-backend.service
sudo systemctl daemon-reload
sudo systemctl enable revconnect-backend
sudo systemctl restart revconnect-backend

sudo mkdir -p /var/www/html/revconnect-ui/browser/
sudo rm -rf /var/www/html/revconnect-ui/browser/*
sudo cp -r /tmp/frontend/* /var/www/html/revconnect-ui/browser/
sudo chown -R ${Ec2User}:${Ec2User} /var/www/html/revconnect-ui
sudo chmod -R 755 /var/www/html/revconnect-ui/browser
sudo systemctl restart nginx
"@

Write-Host "`n=========================================" -ForegroundColor Green
Write-Host "  ✅ Deployment Completed Successfully!" -ForegroundColor Green
Write-Host "  🌐 HTTP:  http://$Ec2Ip" -ForegroundColor Green
Write-Host "  📖 Docs:  http://$Ec2Ip:8080/swagger-ui.html" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
