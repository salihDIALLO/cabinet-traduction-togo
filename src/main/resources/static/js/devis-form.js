/**
 * devis-form.js — Formulaire de devis multi-étapes
 * Cabinet de Traduction Certifiée — Togo
 * Vanilla JS, Bootstrap 5, fetch() vers POST /api/devis
 */
(function () {
  'use strict';

  const TOTAL_STEPS = 3;
  const MAX_FILES = 5;
  const MAX_SIZE_MB = 10;

  // ── Références DOM ──────────────────────────────────────────
  const form        = document.getElementById('devis-form');
  const btnNext     = document.getElementById('btn-next');
  const btnPrev     = document.getElementById('btn-prev');
  const btnSubmit   = document.getElementById('btn-submit');
  const progressBar = document.getElementById('progress-bar');
  const stepLabel   = document.getElementById('step-label');
  const stepPct     = document.getElementById('step-pct');
  const successCard = document.getElementById('success-card');
  const successMsg  = document.getElementById('success-msg');
  const uploadProg  = document.getElementById('upload-progress');
  const uploadBar   = document.getElementById('upload-bar');
  const serverError = document.getElementById('server-error');
  const fileInput   = document.getElementById('fichiers');
  const fileList    = document.getElementById('file-list');
  const fileError   = document.getElementById('file-error');
  const dropZone    = document.getElementById('drop-zone');

  let currentStep = 1;
  let selectedFiles = [];

  // ── Initialisation ──────────────────────────────────────────
  function init() {
    if (!form) return;
    updateUI();
    bindEvents();
  }

  // ── Mise à jour de l'interface ──────────────────────────────
  function updateUI() {
    const pct = Math.round((currentStep / TOTAL_STEPS) * 100);
    progressBar.style.width = pct + '%';
    stepPct.textContent = pct + '%';

    // Libellé localisé (cherche la valeur rendue par Thymeleaf)
    const labelEl = document.getElementById('step-label');
    if (labelEl) {
      // Thymeleaf a déjà rendu le texte ; on met juste à jour le numéro
      labelEl.textContent = labelEl.dataset.template
        ? labelEl.dataset.template.replace('{0}', currentStep)
        : 'Étape ' + currentStep + ' sur ' + TOTAL_STEPS;
    }

    // Indicateurs de dots
    for (let i = 1; i <= TOTAL_STEPS; i++) {
      const dot = document.getElementById('dot-' + i);
      if (!dot) continue;
      dot.className = 'badge rounded-pill step-dot ' + (i <= currentStep ? 'bg-success' : 'bg-secondary');
    }

    // Afficher/masquer les étapes
    for (let i = 1; i <= TOTAL_STEPS; i++) {
      const el = document.getElementById('step-' + i);
      if (el) el.classList.toggle('d-none', i !== currentStep);
    }

    // Boutons
    btnPrev.classList.toggle('d-none', currentStep === 1);
    btnNext.classList.toggle('d-none', currentStep === TOTAL_STEPS);
    btnSubmit.classList.toggle('d-none', currentStep !== TOTAL_STEPS);
  }

  // ── Validation d'une étape ──────────────────────────────────
  function validerEtape(step) {
    const stepEl = document.getElementById('step-' + step);
    if (!stepEl) return true;

    const inputs = stepEl.querySelectorAll('input[required], select[required], textarea[required]');
    let valide = true;

    inputs.forEach(function (input) {
      if (!input.value.trim()) {
        input.classList.add('is-invalid');
        valide = false;
      } else {
        input.classList.remove('is-invalid');
        input.classList.add('is-valid');
      }
    });

    // Validation email spécifique
    const emailEl = document.getElementById('email');
    if (emailEl && step === 1) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(emailEl.value)) {
        emailEl.classList.add('is-invalid');
        valide = false;
      }
    }

    return valide;
  }

  // ── Validation des fichiers ─────────────────────────────────
  function validerFichiers(files) {
    fileError.textContent = '';
    const MIME_OK = ['application/pdf', 'image/jpeg', 'image/png',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];

    if (files.length > MAX_FILES) {
      fileError.textContent = 'Maximum ' + MAX_FILES + ' fichiers autorisés.';
      return false;
    }

    for (const f of files) {
      if (f.size > MAX_SIZE_MB * 1024 * 1024) {
        fileError.textContent = 'Le fichier "' + f.name + '" dépasse ' + MAX_SIZE_MB + ' Mo.';
        return false;
      }
      if (!MIME_OK.includes(f.type)) {
        fileError.textContent = 'Type non autorisé : "' + f.name + '". Formats : PDF, JPG, PNG, DOCX.';
        return false;
      }
    }
    return true;
  }

  // ── Rendu de la liste de fichiers ───────────────────────────
  function renderFileList(files) {
    fileList.innerHTML = '';
    Array.from(files).forEach(function (f) {
      const badge = document.createElement('span');
      badge.className = 'badge bg-light text-dark border me-2 mb-1 py-2 px-3';
      badge.innerHTML = '<span class="fa fa-file-o me-1"></span>' + escapeHtml(f.name)
        + ' <small class="text-muted">(' + (f.size / 1024).toFixed(1) + ' Ko)</small>';
      fileList.appendChild(badge);
    });
  }

  // ── Envoi du formulaire ─────────────────────────────────────
  async function soumettre() {
    serverError.classList.add('d-none');
    uploadProg.classList.remove('d-none');
    uploadBar.style.width = '10%';
    btnSubmit.disabled = true;
    btnSubmit.innerHTML = '<span class="fa fa-spinner fa-spin me-2"></span>Envoi en cours...';

    const formData = new FormData();

    // Champs texte
    ['nom', 'email', 'telephone', 'typeClient', 'serviceId',
      'langueSource', 'langueCible', 'description'].forEach(function (name) {
      const el = document.getElementById(name);
      if (el) formData.append(name, el.value);
    });

    // Fichiers
    selectedFiles.forEach(function (f) {
      formData.append('fichiers', f);
    });

    // Simulation progression (pas d'API XHR progress sur fetch natif)
    let pct = 10;
    const timer = setInterval(function () {
      pct = Math.min(pct + 15, 85);
      uploadBar.style.width = pct + '%';
    }, 300);

    try {
      const resp = await fetch('/api/devis', {
        method: 'POST',
        body: formData
      });

      clearInterval(timer);
      uploadBar.style.width = '100%';

      const data = await resp.json();

      if (resp.ok) {
        // Succès
        setTimeout(function () {
          uploadProg.classList.add('d-none');
          form.classList.add('d-none');
          successCard.classList.remove('d-none');

          if (successMsg) {
            successMsg.textContent = 'Référence #' + data.id + '. ' + (data.message || '');
          }

          // Lien WhatsApp suivi
          const waLink = document.getElementById('whatsapp-suivi');
          if (waLink) {
            const msg = encodeURIComponent('Bonjour, je souhaite suivre ma demande #' + data.id);
            waLink.href = 'https://wa.me/228XXXXXXXX?text=' + msg;
          }

          // Scroll vers succès
          successCard.scrollIntoView({ behavior: 'smooth' });
        }, 500);

      } else {
        showServerError(data.erreur || 'Une erreur est survenue.');
        resetSubmitBtn();
      }

    } catch (err) {
      clearInterval(timer);
      uploadProg.classList.add('d-none');
      showServerError('Impossible de contacter le serveur. Vérifiez votre connexion.');
      resetSubmitBtn();
    }
  }

  function showServerError(msg) {
    serverError.textContent = msg;
    serverError.classList.remove('d-none');
    serverError.scrollIntoView({ behavior: 'smooth' });
  }

  function resetSubmitBtn() {
    btnSubmit.disabled = false;
    btnSubmit.innerHTML = '<span class="fa fa-paper-plane me-2"></span>Envoyer';
  }

  function escapeHtml(str) {
    return str.replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  // ── Événements ─────────────────────────────────────────────
  function bindEvents() {

    // Suivant
    btnNext.addEventListener('click', function () {
      if (!validerEtape(currentStep)) return;
      currentStep++;
      updateUI();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });

    // Précédent
    btnPrev.addEventListener('click', function () {
      currentStep--;
      updateUI();
    });

    // Soumission
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      if (!validerEtape(currentStep)) return;
      if (!validerFichiers(selectedFiles)) return;
      soumettre();
    });

    // Click sur zone drop → ouvre le picker
    dropZone.addEventListener('click', function () {
      fileInput.click();
    });

    // Sélection via picker
    fileInput.addEventListener('change', function () {
      selectedFiles = Array.from(fileInput.files);
      if (validerFichiers(selectedFiles)) {
        renderFileList(selectedFiles);
      }
    });

    // Drag & Drop
    dropZone.addEventListener('dragover', function (e) {
      e.preventDefault();
      dropZone.classList.add('bg-light');
    });
    dropZone.addEventListener('dragleave', function () {
      dropZone.classList.remove('bg-light');
    });
    dropZone.addEventListener('drop', function (e) {
      e.preventDefault();
      dropZone.classList.remove('bg-light');
      selectedFiles = Array.from(e.dataTransfer.files);
      if (validerFichiers(selectedFiles)) {
        renderFileList(selectedFiles);
      }
    });

    // Retirer classe invalide dès que l'utilisateur tape
    form.querySelectorAll('input, select, textarea').forEach(function (el) {
      el.addEventListener('input', function () {
        el.classList.remove('is-invalid');
      });
    });
  }

  // ── Démarrage ───────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', init);

})();
