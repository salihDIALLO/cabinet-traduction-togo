# run-dev.ps1 - Lance l'application en mode developpement local
# Usage : .\run-dev.ps1

Set-Location $PSScriptRoot
$env:JAVA_HOME             = "C:\Program Files\Java\jdk-21.0.11"
$env:PATH                  = "$env:JAVA_HOME\bin;$env:PATH"
$env:JWT_SECRET            = "dev-secret-key-for-local-testing-minimum-64-bytes-long-ok-123456"
$env:AWS_ACCESS_KEY_ID     = "dummy"
$env:AWS_SECRET_ACCESS_KEY = "dummy"
$env:AWS_S3_BUCKET         = "dummy-bucket"
$env:AWS_REGION            = "eu-west-3"
$env:MAIL_HOST             = "localhost"
$env:MAIL_PORT             = "25"
$env:MAIL_USERNAME         = "test@test.com"
$env:MAIL_PASSWORD         = "dummy"
$env:FEDAPAY_API_KEY       = "dummy"
$env:FEDAPAY_WEBHOOK_SECRET= "dummy"
$env:FEDAPAY_CALLBACK_URL  = "http://localhost:8080/paiement/retour"
$env:MYSQL_URL              = "jdbc:mysql://localhost:3306/cabinet_traduction?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:MYSQL_USER             = "root"
$env:MYSQL_PASS             = ""

# CinetPay (Flooz / Yass / Moov Money)
# Obtenir vos clés sur : https://sandbox.cinetpay.com → Paramètres → Clés API
$env:CINETPAY_API_KEY       = "CHANGE_ME"
$env:CINETPAY_SITE_ID       = "CHANGE_ME"
$env:CINETPAY_WEBHOOK_SECRET= ""
$env:CINETPAY_NOTIFY_URL    = "http://localhost:8080/api/paiement/notify"
$env:CINETPAY_RETURN_URL    = "http://localhost:8080/paiement/retour"

# URL de base (liens dans les emails)
$env:APP_BASE_URL           = "http://localhost:8080"

# Mode stockage local pour le dev (pas besoin d'AWS S3)
# Les fichiers vont dans uploads-dev/ à la racine du projet
$env:APP_STORAGE_MODE       = "local"

Write-Host "Cabinet Traduction Togo - Mode DEV" -ForegroundColor Green
Write-Host "http://localhost:8080" -ForegroundColor Cyan
Write-Host ""

.\mvnw.cmd spring-boot:run "-Dspring-javaformat.skip=true" "-Dspring-boot.run.profiles=mysql" "-Dspring-boot.run.arguments=--spring.sql.init.mode=never --app.storage.mode=local"