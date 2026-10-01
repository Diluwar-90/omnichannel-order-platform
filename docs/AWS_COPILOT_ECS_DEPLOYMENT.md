# AWS Copilot & Amazon ECS Fargate Deployment Guide

This guide covers deploying the **Omnichannel Order Platform** to **Amazon ECS Fargate** with **Aurora Serverless v2 PostgreSQL** and **Amazon MSK Serverless (Kafka)** using **AWS Copilot CLI**, coupled with automated continuous deployment via **GitHub Actions**.

---

## 1. Architecture & Cost-Optimized Sizing

```
                               ┌──────────────────────────────────────────────────┐
                               │       AWS Application Load Balancer (ALB)        │
                               └────────────────────────┬─────────────────────────┘
                                                        │ Port 80 / 443
                                                        ▼
                                       ┌──────────────────────────────────┐
                                       │     api-gateway (Fargate)        │
                                       │       Port 8080 (Public)         │
                                       └────────────────┬─────────────────┘
                                                        │ Service Discovery (.local)
       ┌──────────────────┬──────────────────┬──────────┴───────┬──────────────────┬──────────────────┐
       ▼                  ▼                  ▼                  ▼                  ▼                  ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ auth-service │   │product-servic│   │inventory-svc │   │ order-service│   │payment-servic│   │notification-s│
│  Port 8081   │   │  Port 8082   │   │  Port 8083   │   │  Port 8084   │   │  Port 8085   │   │  Port 8086   │
└──────┬───────┘   └──────┬───────┘   └──────┬───────┘   └──────┬───┬───┘   └──────┬───┬───┘   └──────┬───┬───┘
       │                  │                  │                  │   │          │   │          │   │
       ▼                  ▼                  ▼                  ▼   │          ▼   │          ▼   │
┌─────────────────────────────────────────────────────────────┐     │  ┌───────┴───┴──────────┴───┴───────┐
│     Aurora PostgreSQL Serverless v2 (0.5 – 1.0 ACU)         │     │  │       Amazon MSK Serverless      │
│  (Databases: auth, product, inventory, order, payment, etc) │     └──┼─▶ Topics: order.created, etc     │
└─────────────────────────────────────────────────────────────┘        └──────────────────────────────────┘
```

### Cost Optimization Features in this Setup
1. **Single NAT Gateway**: In `copilot/environments/dev/manifest.yml`, `nat: 1` is configured. This reduces AWS VPC charges by ~$65/month compared to default 3 Multi-AZ NAT Gateways.
2. **Aurora Serverless v2**: Configured in `copilot/environments/dev/addons/database.yml` with `MinCapacity: 0.5 ACU` (~1 GB RAM, scales down when idle to ~0.5 ACU).
3. **Fargate Task Sizing**: Sized at `cpu: 256` (0.25 vCPU) and `memory: 512` (512 MB) per container to minimize ECS compute costs.
4. **Amazon MSK Serverless**: Pay only for partitions and data volume without paying for 3 constantly running EC2 broker instances.

---

## 2. Directory Layout Created for Copilot & ECS

```
omnichannel-order-platform/
├── copilot/
│   ├── .workspace                              # Copilot app name ("omnichannel")
│   ├── environments/
│   │   └── dev/
│   │       ├── manifest.yml                    # Cost-optimized VPC & NAT config
│   │       └── addons/
│   │           ├── database.yml                # Aurora PostgreSQL Serverless v2
│   │           └── kafka.yml                   # Amazon MSK Serverless cluster
│   ├── api-gateway/manifest.yml                # Load Balanced Web Service (Port 8080)
│   ├── auth-service/manifest.yml               # Backend Service (Port 8081)
│   ├── product-service/manifest.yml            # Backend Service (Port 8082)
│   ├── inventory-service/manifest.yml          # Backend Service (Port 8083)
│   ├── order-service/manifest.yml              # Backend Service (Port 8084)
│   ├── payment-service/manifest.yml            # Backend Service (Port 8085)
│   └── notification-service/manifest.yml       # Backend Service (Port 8086)
├── infrastructure/
│   └── ecs/
│       └── task-definitions/                   # 7 Fargate task definition templates
│           ├── api-gateway.json
│           ├── auth-service.json
│           ├── product-service.json
│           ├── inventory-service.json
│           ├── order-service.json
│           ├── payment-service.json
│           └── notification-service.json
└── .github/workflows/ci-cd.yml                 # Automated build, ECR push & ECS rolling update
```

---

## 3. Step-by-Step Initial AWS Deployment

### Step 1: Install AWS Copilot CLI

On macOS:
```bash
brew install aws/tap/copilot-cli
```

On Linux:
```bash
curl -Lo copilot https://github.com/aws/copilot-cli/releases/latest/download/copilot-linux && chmod +x copilot && sudo mv copilot /usr/local/bin/copilot
```

Verify installation:
```bash
copilot --version
```

---

### Step 2: Configure AWS Credentials

Ensure your terminal has active AWS credentials configured:
```bash
aws configure
# Enter AWS Access Key ID, Secret Access Key, and Region (e.g. us-east-1)
```

Verify connection:
```bash
aws sts get-caller-identity
```

---

### Step 3: Deploy the Dev Environment (VPC + RDS + MSK)

Deploy the `dev` environment with the database and Kafka addons:

```bash
cd /path/to/omnichannel-order-platform

# Deploy the dev environment infrastructure
copilot env deploy --name dev
```

> **What AWS Copilot provisions during this step:**
> - A secure Virtual Private Cloud (VPC) with public and private subnets across 2 AZs.
> - 1 NAT Gateway and Internet Gateway.
> - An Application Load Balancer (ALB) for public HTTP ingress.
> - Aurora PostgreSQL Serverless v2 cluster (`omnichannel-dev-postgres`).
> - Amazon MSK Serverless Kafka cluster.
> - AWS Cloud Map private DNS namespace (`omnichannel.local`) for internal service discovery.

---

### Step 4: Deploy the Microservices

Once the environment is active, deploy the services:

```bash
# 1. Deploy the API Gateway (Public Load Balanced Service)
copilot svc deploy --name api-gateway --env dev

# 2. Deploy the Backend Microservices
copilot svc deploy --name auth-service --env dev
copilot svc deploy --name product-service --env dev
copilot svc deploy --name inventory-service --env dev
copilot svc deploy --name order-service --env dev
copilot svc deploy --name payment-service --env dev
copilot svc deploy --name notification-service --env dev
```

When `api-gateway` finishes deploying, Copilot prints the **Public Load Balancer URL**:
```bash
copilot svc show --name api-gateway
```
Example: `http://omnic-Publi-1ABCDEF12345.us-east-1.elb.amazonaws.com`

---

## 4. Automated CI/CD with GitHub Actions

You don't need to manually run `copilot svc deploy` for every code update. Your repository's GitHub Actions workflow ([`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml)) is preconfigured with:

1. **`aws-actions/configure-aws-credentials@v4`**: Authenticates to AWS via temporary OIDC credentials.
2. **`aws-actions/amazon-ecr-login@v2`**: Logs into Amazon ECR.
3. **Parallel Build & Push**: Builds Gradle JARs and pushes Docker containers tagged with the commit SHA and `latest`.
4. **`aws-actions/amazon-ecs-render-task-definition@v1`**: Injects the new image SHA into the task definition.
5. **`aws-actions/amazon-ecs-deploy-task-definition@v2`**: Dispatches a zero-downtime rolling deployment to Amazon ECS.

### GitHub Repository Settings to Configure

In your GitHub repository (**Settings** > **Secrets and variables** > **Actions**):

| Type | Name | Value Example | Description |
| :--- | :--- | :--- | :--- |
| **Secret** | `AWS_ROLE_TO_ASSUME` | `arn:aws:iam::123456789012:role/GitHubActions-OmnichannelDeployRole` | IAM Role assumed via OIDC |
| **Variable** | `AWS_REGION` | `us-east-1` | Target AWS region (Default: `us-east-1`) |
| **Variable** | `ECS_CLUSTER` | `omnichannel-dev` | Name of the Copilot ECS cluster (Default: `omnichannel-dev`) |

---

## 5. Verifying & Inspecting Deployed Services

Using AWS Copilot CLI:

```bash
# Check status of all tasks
copilot svc status --name api-gateway --env dev

# Stream logs in real-time
copilot svc logs --name api-gateway --follow
copilot svc logs --name order-service --follow

# Open an interactive shell inside a running container (AWS ECS Exec)
copilot svc exec --name api-gateway --env dev
```

Via cURL through the public ALB endpoint:
```bash
ALB_URL=$(copilot svc show --name api-gateway --json | jq -r .routes[0].url)

# Health check
curl $ALB_URL/actuator/health

# Register user via Auth Service
curl -X POST $ALB_URL/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@example.com","password":"Password123!","role":"ADMIN"}'

# Place an Order
curl -X POST $ALB_URL/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"productId":1,"quantity":2,"totalAmount":199.98}'
```

---

## 6. Teardown / Destroy (Clean Up AWS Resources)

To stop all resources and avoid ongoing AWS charges:

```bash
# Delete individual services
copilot svc delete --name api-gateway --env dev --yes

# Delete the entire dev environment (VPC, ALB, RDS, MSK)
copilot env delete --name dev --yes

# Delete the application
copilot app delete --yes
```
