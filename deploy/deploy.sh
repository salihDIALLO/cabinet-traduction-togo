#!/usr/bin/env bash
# ============================================================
# deploy.sh — Script de déploiement manuel sur AWS Lightsail
# Cabinet de Traduction Certifiée — Togo
#
# Usage :
#   chmod +x deploy.sh
#   ./deploy.sh
#
# Prérequis locaux :
#   - Java 17+, Maven, SSH configuré (~/.ssh/config ou clé)
#   - Les variables ci-dessous renseignées
# ============================================================
set -euo pipefail

# ── Configuration — MODIFIER ces valeurs ──────────────────────
SERVER_USER="ubuntu"
SERVER_HOST="XX.XX.XX.XX"          # IP publique de l'instance Lightsail
SSH_KEY="~/.ssh/lightsail-key.pem" # Chemin vers votre clé SSH
REMOTE_DIR="/opt/cabinet-traduction"
SERVICE_NAME="spring-app"
JAR_NAME="cabinet-traduction-togo-1.0.0-SNAPSHOT.jar"
# ──────────────────────────────────────────────────────────────

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║  Cabinet Traduction Togo — Déploiement  ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# ── Étape 1 : Build Maven local ──────────────────────────────
echo "▶ [1/4] Build Maven (tests ignorés)..."
./mvnw clean package -DskipTests -q
echo "   ✅ Build terminé : target/${JAR_NAME}"

# ── Étape 2 : Transfert du JAR vers le serveur ───────────────
echo "▶ [2/4] Transfert du JAR vers ${SERVER_HOST}..."
scp -i "${SSH_KEY}" -o StrictHostKeyChecking=no \
    "target/${JAR_NAME}" \
    "${SERVER_USER}@${SERVER_HOST}:${REMOTE_DIR}/app-new.jar"
echo "   ✅ Transfert terminé"

# ── Étape 3 : Rotation du JAR et redémarrage du service ──────
echo "▶ [3/4] Rotation + redémarrage du service sur le serveur..."
ssh -i "${SSH_KEY}" -o StrictHostKeyChecking=no \
    "${SERVER_USER}@${SERVER_HOST}" << 'REMOTE'

set -e

# Backup de l'ancien JAR
if [ -f /opt/cabinet-traduction/app.jar ]; then
    cp /opt/cabinet-traduction/app.jar /opt/cabinet-traduction/app-backup.jar
    echo "   → Backup créé : app-backup.jar"
fi

# Remplacer par le nouveau
mv /opt/cabinet-traduction/app-new.jar /opt/cabinet-traduction/app.jar
echo "   → Nouveau JAR en place"

# Redémarrer le service
sudo systemctl restart spring-app
echo "   → Service redémarré"

# Attendre le démarrage (max 30s)
for i in $(seq 1 30); do
    sleep 1
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "   ✅ Application démarrée (${i}s)"
        exit 0
    fi
done

echo "   ❌ Timeout : l'application ne répond pas après 30s"
echo "   → Rollback vers app-backup.jar..."
mv /opt/cabinet-traduction/app-backup.jar /opt/cabinet-traduction/app.jar
sudo systemctl restart spring-app
exit 1

REMOTE

# ── Étape 4 : Vérification finale ────────────────────────────
echo "▶ [4/4] Vérification santé de l'application..."
sleep 3
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    "https://${SERVER_HOST}/actuator/health" 2>/dev/null || echo "000")

if [ "$HTTP_STATUS" = "200" ]; then
    echo "   ✅ Application en ligne — HTTP ${HTTP_STATUS}"
else
    echo "   ⚠️  Statut HTTP : ${HTTP_STATUS} (vérifier les logs)"
fi

echo ""
echo "════════════════════════════════════════════"
echo "  Déploiement terminé !"
echo "  Logs : ssh -i ${SSH_KEY} ${SERVER_USER}@${SERVER_HOST}"
echo "         journalctl -u ${SERVICE_NAME} -f"
echo "════════════════════════════════════════════"
echo ""
