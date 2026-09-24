pipeline {
    agent any

    parameters {
        string(name: 'EC2_IP', defaultValue: '13.234.67.90', description: 'Target AWS EC2 Public IPv4 Address')
        string(name: 'SSH_CREDENTIAL_ID', defaultValue: 'aws-ec2-ssh-key', description: 'Jenkins SSH Private Key Credential ID')
    }

    environment {
        TARGET_IP = "${params.EC2_IP ?: '13.234.67.90'}"
        CRED_ID   = "${params.SSH_CREDENTIAL_ID ?: 'aws-ec2-ssh-key'}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend') {
            steps {
                script {
                    def backendDir = fileExists('backend') ? 'backend' : 'Downloads/REVCONNECT-main/REVCONNECT-main/backend'
                    echo "Building backend in: ${backendDir}"
                    dir(backendDir) {
                        bat 'mvnw.cmd clean package -DskipTests'
                    }
                }
            }
        }

        stage('Build Frontend') {
            steps {
                script {
                    def frontendDir = fileExists('frontend') ? 'frontend' : 'Downloads/REVCONNECT-main/REVCONNECT-main/frontend'
                    echo "Building frontend in: ${frontendDir}"
                    dir(frontendDir) {
                        bat 'npm install --legacy-peer-deps'
                        bat 'if exist .angular\\cache rmdir /s /q .angular\\cache'
                        bat 'set NODE_OPTIONS=--max_old_space_size=4096 && npm run build'
                    }
                }
            }
        }

        stage('Deploy Full Stack to EC2') {
            steps {
                script {
                    def projectDir = fileExists('backend') ? '.' : 'Downloads/REVCONNECT-main/REVCONNECT-main'
                    dir(projectDir) {
                        withCredentials([sshUserPrivateKey(credentialsId: env.CRED_ID, keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
                            powershell """
                            \$ErrorActionPreference = "Stop"
                            \$targetIp = "${env.TARGET_IP}"
                            \$sshUser = "${env.SSH_USER}"
                            \$keyPath = "\$env:WORKSPACE\\jenkins-key-\${env:BUILD_NUMBER}.pem"
                            Copy-Item -Path \$env:SSH_KEY -Destination \$keyPath -Force

                            icacls \$keyPath /inheritance:r /grant:r "\${env:USERNAME}:F" /grant:r "NT AUTHORITY\\SYSTEM:F" /grant:r "BUILTIN\\Administrators:F"

                            # Absorb the first-time SSH known_hosts warning to prevent PowerShell from throwing a fatal stderr exception
                            \$ErrorActionPreference = "Continue"
                            ssh -o StrictHostKeyChecking=accept-new -i \$keyPath \${sshUser}@\${targetIp} "echo 'Initializing SSH known_hosts...'" 2>&1 | Out-Null
                            \$ErrorActionPreference = "Stop"

                            # Allocate 2GB Swap Memory to prevent t3.micro (1GB RAM) from freezing (OOM death)
                            ssh -o StrictHostKeyChecking=accept-new -i \$keyPath \${sshUser}@\${targetIp} "sudo fallocate -l 2G /swapfile 2>/dev/null || true; sudo chmod 600 /swapfile 2>/dev/null || true; sudo mkswap /swapfile 2>/dev/null || true; sudo swapon /swapfile 2>/dev/null || true" 2>&1

                            # Database Migration — ensure notifications type column supports all enum values
                            ssh -o StrictHostKeyChecking=accept-new -i \$keyPath \${sshUser}@\${targetIp} "sudo systemctl start mysqld 2>/dev/null || sudo systemctl start mariadb 2>/dev/null || true; sudo systemctl enable mysqld 2>/dev/null || true; sudo systemctl enable mariadb 2>/dev/null || true" 2>&1
                            ssh -o StrictHostKeyChecking=accept-new -i \$keyPath \${sshUser}@\${targetIp} "mysql -u root -proot revconnect_db -e 'ALTER TABLE notifications MODIFY COLUMN type VARCHAR(50) NOT NULL;' 2>/dev/null || true" 2>&1

                            # Backend Deployment
                            scp -o StrictHostKeyChecking=accept-new -i \$keyPath backend/target/revconnect-1.0.0.jar \${sshUser}@\${targetIp}:/home/\${sshUser}/revconnect-1.0.0.jar 2>&1
                            scp -o StrictHostKeyChecking=accept-new -i \$keyPath backend/deploy/revconnect-backend.service \${sshUser}@\${targetIp}:/tmp/ 2>&1
                            ssh -o StrictHostKeyChecking=accept-new -i \$keyPath \${sshUser}@\${targetIp} "mkdir -p /home/\${sshUser}/revconnect/uploads; sudo mv /tmp/revconnect-backend.service /etc/systemd/system/revconnect-backend.service; sudo chown root:root /etc/systemd/system/revconnect-backend.service; sudo systemctl daemon-reload; sudo systemctl enable revconnect-backend; sudo systemctl restart revconnect-backend" 2>&1

                            # Frontend Deployment
                            ssh -o StrictHostKeyChecking=accept-new -i \$keyPath \${sshUser}@\${targetIp} "mkdir -p /tmp/frontend" 2>&1
                            scp -o StrictHostKeyChecking=accept-new -i \$keyPath -pr frontend/dist/revconnect-ui/browser/* \${sshUser}@\${targetIp}:/tmp/frontend/ 2>&1
                            ssh -o StrictHostKeyChecking=accept-new -i \$keyPath \${sshUser}@\${targetIp} "sudo rm -f /etc/nginx/conf.d/revconnect-ssl.conf; sudo rm -rf /var/www/html/revconnect-ui/browser/*; sudo mkdir -p /var/www/html/revconnect-ui/browser/; sudo cp -r /tmp/frontend/* /var/www/html/revconnect-ui/browser/; sudo chown -R \${sshUser}:\${sshUser} /var/www/html/revconnect-ui; sudo chmod -R 755 /var/www/html/revconnect-ui/browser; sudo systemctl restart nginx" 2>&1

                            # SSL Certificate Setup - run via script to avoid quoting issues
                            scp -o StrictHostKeyChecking=accept-new -i \$keyPath backend/deploy/ssl-setup.sh \${sshUser}@\${targetIp}:/tmp/ssl-setup.sh 2>&1
                            ssh -o StrictHostKeyChecking=accept-new -i \$keyPath \${sshUser}@\${targetIp} "sed -i 's/\\r//' /tmp/ssl-setup.sh && chmod +x /tmp/ssl-setup.sh && bash /tmp/ssl-setup.sh" 2>&1

                            icacls \$keyPath /reset
                            Remove-Item -Path \$keyPath -Force -ErrorAction SilentlyContinue
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Deployment finished for target host: ${env.TARGET_IP}"
        }
    }
}
