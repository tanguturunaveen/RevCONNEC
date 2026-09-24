# 🚀 Deploy RevConnect to AWS (Free Tier)

This guide walks you through deploying the RevConnect backend to an **AWS EC2 t2.micro** instance.

---

## Prerequisites

- An [AWS Free Tier account](https://aws.amazon.com/free/)
- Git Bash or WSL installed on your Windows machine (for running shell scripts)
- Maven installed locally (or use the included `mvnw` wrapper)

---

## Step 1: Launch an EC2 Instance

1. Log in to [AWS Console](https://console.aws.amazon.com/) → **EC2** → **Launch Instance**

2. Configure as follows:

   | Setting | Value |
   |---|---|
   | **Name** | `revconnect-server` |
   | **AMI** | Amazon Linux 2023 (Free tier eligible) |
   | **Instance type** | `t2.micro` (Free tier eligible) |
   | **Key pair** | Create new → name it `revconnect-key` → Download `.pem` file |
   | **Network** | Allow SSH (port 22) from "My IP" |
   | **Storage** | 20 GiB gp3 |

3. Click **Launch Instance**

---

## Step 2: Configure Security Group

1. Go to **EC2** → **Instances** → click your instance → **Security** tab → click the Security Group link

2. Click **Edit inbound rules** → **Add rule**:

   | Type | Port Range | Source | Description |
   |---|---|---|---|
   | SSH | 22 | My IP | SSH access |
   | Custom TCP | 8080 | 0.0.0.0/0 | Backend API |

3. Click **Save rules**

---

## Step 3: Connect & Set Up the Instance

1. **Find your EC2 Public IP**: Go to **EC2** → **Instances** → copy the **Public IPv4 address**

2. **SSH into the instance** (from Git Bash / WSL):
   ```bash
   chmod 400 ~/revconnect-key.pem
   ssh -i ~/revconnect-key.pem ec2-user@<YOUR_EC2_IP>
   ```

3. **Upload and run the setup script**:
   ```bash
   # From your LOCAL machine, upload the script:
   scp -i ~/revconnect-key.pem deploy/setup-ec2.sh ec2-user@<YOUR_EC2_IP>:/home/ec2-user/

   # SSH in and run it:
   ssh -i ~/revconnect-key.pem ec2-user@<YOUR_EC2_IP>
   sudo chmod +x setup-ec2.sh
   sudo ./setup-ec2.sh
   ```

   This installs **Java 21**, **MySQL (MariaDB)**, and creates the `revconnect_db` database.

---

## Step 4: Deploy the Application

### Option A: Using the deploy script (recommended)

From your **local machine** (Git Bash / WSL), run:

```bash
cd /c/Users/navee/OneDrive/Desktop/Git\ FOlder/REVCONNECT
chmod +x deploy/deploy.sh
./deploy/deploy.sh <YOUR_EC2_IP> ~/revconnect-key.pem
```

This will:
1. Build the JAR locally with Maven
2. Upload it to your EC2 instance via SCP
3. Start the application with the production profile

### Option B: Manual deployment

```bash
# 1. Build locally
./mvnw clean package -DskipTests

# 2. Upload JAR
scp -i ~/revconnect-key.pem target/revconnect-1.0.0.jar ec2-user@<YOUR_EC2_IP>:/home/ec2-user/revconnect/app.jar

# 3. SSH in and start
ssh -i ~/revconnect-key.pem ec2-user@<YOUR_EC2_IP>

export DB_PASSWORD="RevConnect2024!"
cd /home/ec2-user/revconnect
nohup java -jar app.jar --spring.profiles.active=prod -Xmx512m > /dev/null 2>&1 &
```

---

## Step 5: Verify

| Check | Command / URL |
|---|---|
| **Swagger UI** | `http://<EC2_IP>:8080/swagger-ui.html` |
| **API Health** | `curl http://<EC2_IP>:8080/api/auth/login` |
| **App Process** | `ssh ... 'ps aux \| grep java'` |
| **App Logs** | `ssh ... 'tail -f /home/ec2-user/revconnect/app.log'` |
| **MySQL Status** | `ssh ... 'sudo systemctl status mariadb'` |

---

## Environment Variables (Production)

These are set in `deploy.sh` automatically. Change them as needed:

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `RevConnect2024!` | MySQL password |
| `JWT_SECRET` | (long key) | JWT signing key |
| `MAIL_USERNAME` | (gmail address) | SMTP email |
| `MAIL_PASSWORD` | (app password) | Gmail app password |

---

## Useful Commands

```bash
# View logs
tail -f /home/ec2-user/revconnect/app.log

# Restart app
pkill -f "java -jar" && nohup java -jar /home/ec2-user/revconnect/app.jar --spring.profiles.active=prod -Xmx512m > /dev/null 2>&1 &

# Stop app
pkill -f "java -jar"

# Check disk space
df -h

# Check memory
free -m
```

---

## ⚠️ Important Notes

- **Free Tier Limits**: `t2.micro` has 1 GB RAM. The `-Xmx512m` flag limits Java to 512 MB, leaving room for MySQL and OS.
- **No HTTPS**: Traffic is plain HTTP on port 8080. For SSL, you'd need an Application Load Balancer + ACM certificate.
- **Elastic IP**: By default, your public IP changes when you restart the instance. To get a fixed IP, allocate an **Elastic IP** (free while attached to a running instance).
- **Auto-start on reboot**: To make the app start automatically after instance reboot, add a systemd service or add the startup command to `/etc/rc.local`.
