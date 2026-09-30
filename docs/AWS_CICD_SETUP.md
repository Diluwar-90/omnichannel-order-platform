# AWS CI/CD Pipeline Setup Guide (GitHub Actions OIDC)

This guide walks you through setting up secure, keyless continuous deployment to **Amazon Web Services (AWS)** using **GitHub Actions OpenID Connect (OIDC)**.

---

## 1. Architecture Overview

Rather than storing long-lived AWS IAM access keys (`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`) inside GitHub, this pipeline uses **OpenID Connect (OIDC)**:

```
[ GitHub Actions Runner ]
        │
        ▼ 1. Request OIDC Token (JWT)
[ GitHub OIDC Provider ]
        │
        ▼ 2. Exchange JWT via AssumeRoleWithWebIdentity
[ AWS STS (Security Token Service) ]
        │
        ▼ 3. Issues short-lived, temporary AWS credentials
[ Amazon ECR / ECS Deployment ]
```

### Key Security Benefits:
- **Zero Long-Lived Credentials:** No secrets that can leak or expire unexpectedly.
- **Strictly Scoped Permissions:** The IAM Role only trusts requests from your specific GitHub repository (`Diluwar-90/omnichannel-order-platform`).
- **Auditability:** Every assume-role action is recorded in AWS CloudTrail with full commit and run metadata.

---

## 2. Microservices Containerized by the Pipeline

The CI/CD workflow manages 7 microservices in parallel:
1. `api-gateway` (Port 8080)
2. `auth-service` (Port 8081)
3. `product-service` (Port 8082)
4. `inventory-service` (Port 8083)
5. `order-service` (Port 8084)
6. `payment-service` (Port 8085)
7. `notification-service` (Port 8086)

---

## 3. Step-by-Step AWS Setup

### Step 1: Create GitHub OIDC Provider in AWS IAM (One-time per AWS account)

If you haven't already configured GitHub as an OIDC Identity Provider in your AWS account:

1. Open the **AWS IAM Console** > **Identity providers** > **Add provider**.
2. Select **OpenID Connect**.
3. **Provider URL**: `https://token.actions.githubusercontent.com`
4. **Audience**: `sts.amazonaws.com`
5. Click **Get thumbprint** and then click **Add provider**.

*Or via AWS CLI:*
```bash
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 1c5824a6f5a4e1795021533448f066ec2f86e3d1
```

---

### Step 2: Create IAM Role for GitHub Actions

Create an IAM Role that GitHub Actions will assume.

#### 1. Trust Relationship Policy
Save the following as `github-trust-policy.json` (replace `<YOUR_AWS_ACCOUNT_ID>` with your AWS Account ID):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<YOUR_AWS_ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:Diluwar-90/omnichannel-order-platform:*"
        }
      }
    }
  ]
}
```

> [!NOTE]
> The `StringLike` condition restricts this IAM role exclusively to runs triggered from the `Diluwar-90/omnichannel-order-platform` repository.

#### 2. Create the IAM Role via AWS CLI:
```bash
aws iam create-role \
  --role-name GitHubActions-OmnichannelDeployRole \
  --assume-role-policy-document file://github-trust-policy.json \
  --description "Role assumed by GitHub Actions for omnichannel-order-platform CI/CD"
```

---

### Step 3: Attach Permissions Policy to the Role

Create an IAM Policy allowing GitHub Actions to authenticate and push images to Amazon ECR.

Save as `ecr-push-policy.json`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ECRAuthToken",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECRPushPullImages",
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "arn:aws:ecr:*:<YOUR_AWS_ACCOUNT_ID>:repository/*"
    }
  ]
}
```

#### Create and attach policy:
```bash
# 1. Create policy
aws iam create-policy \
  --policy-name GitHubActions-ECRPushPolicy \
  --policy-document file://ecr-push-policy.json

# 2. Attach policy to the role
aws iam attach-role-policy \
  --role-name GitHubActions-OmnichannelDeployRole \
  --policy-arn arn:aws:iam::<YOUR_AWS_ACCOUNT_ID>:policy/GitHubActions-ECRPushPolicy
```

---

### Step 4: Create Amazon ECR Repositories

Create an ECR repository for each of the 7 microservices in your target region (e.g. `us-east-1`):

```bash
SERVICES=(
  "api-gateway"
  "auth-service"
  "product-service"
  "inventory-service"
  "order-service"
  "payment-service"
  "notification-service"
)

for svc in "${SERVICES[@]}"; do
  echo "Creating ECR repository: $svc"
  aws ecr create-repository \
    --repository-name "$svc" \
    --image-scanning-configuration scanOnPush=true \
    --region us-east-1 || true
done
```

---

### Step 5: Configure GitHub Repository Secrets & Variables

In your GitHub repository (`Diluwar-90/omnichannel-order-platform`):
1. Navigate to **Settings** > **Secrets and variables** > **Actions**.
2. Under **Repository secrets**, click **New repository secret**:
   - **Name**: `AWS_ROLE_TO_ASSUME`
   - **Value**: `arn:aws:iam::<YOUR_AWS_ACCOUNT_ID>:role/GitHubActions-OmnichannelDeployRole`
3. *(Optional)* Under **Repository variables**, click **New repository variable**:
   - **Name**: `AWS_REGION`
   - **Value**: `us-east-1` (or your chosen AWS region, defaults to `us-east-1` if omitted).

---

## 4. Pipeline Behavior

- **On Pull Requests & Branches**:
  - Runs the test matrix across all 7 microservices.
  - Builds Spring Boot JARs and validates Docker image packaging.
  - Deployment step is skipped on pull requests.
- **On Push to `main`**:
  - Executes tests and builds.
  - If `AWS_ROLE_TO_ASSUME` is configured:
    - Authenticates to AWS using temporary OIDC credentials.
    - Logs into Amazon ECR.
    - Builds and pushes all 7 container images tagged with the commit SHA and `latest`.
  - If `AWS_ROLE_TO_ASSUME` is not yet configured:
    - Gracefully skips AWS push without failing the workflow, logging a clear informational status in the GitHub Actions summary.

---

## 5. Next Phase: Deploying to AWS Compute (ECS Fargate / EKS)

Once images are in Amazon ECR, you can deploy them using:
1. **Amazon ECS (Fargate)**:
   - Use `aws-actions/amazon-ecs-deploy-task-definition@v2` in the workflow to update task definitions and services.
2. **Amazon EKS**:
   - Use `azure/k8s-set-context@v3` or `aws eks update-kubeconfig` with `kubectl rollout restart deployment/<service>`.
