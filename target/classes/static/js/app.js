/* ============================================================
   Laboratorio Bacterium — app.js  (versión completa corregida)
   ============================================================ */
const API = '';

let token      = localStorage.getItem('jwt') || null;
let paginaPac  = 0;
let paginaOrden = 0;
let pruebasLaboratorio = [];
let medicosList = [];
let pruebasCatalogo = [];

// ── Boot ────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    if (token) {
        mostrarApp();
        cargarDashboard();
    } else {
        document.getElementById('loginOverlay').classList.add('visible');
    }

    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', e => {
            e.preventDefault();
            navegarA(item.dataset.page);
        });
    });

    document.getElementById('searchInput').addEventListener('input', e => {
        const page = document.querySelector('.nav-item.active')?.dataset.page;
        if (page === 'pacientes') cargarPacientes(0, e.target.value);
        if (page === 'ordenes')   cargarOrdenes(0, e.target.value);
        if (page === 'medicos')   cargarMedicos(e.target.value);
    });
});

// ── Auth ─────────────────────────────────────────────────────────────────────
async function doLogin() {
    const user  = document.getElementById('loginUser').value.trim();
    const pass  = document.getElementById('loginPass').value;
    const errEl = document.getElementById('loginError');
    errEl.style.display = 'none';
    try {
        const res = await post('/api/auth/login', { username: user, password: pass }, false);
        token = res.token;
        localStorage.setItem('jwt', token);
        document.getElementById('loginOverlay').classList.remove('visible');
        mostrarApp();
        cargarDashboard();
    } catch {
        errEl.textContent = 'Usuario o contraseña incorrectos.';
        errEl.style.display = 'block';
    }
}

function logout() {
    token = null;
    localStorage.removeItem('jwt');
    location.reload();
}

function mostrarApp() {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const sub  = payload.sub || 'Usuario';
        const role = payload.role || payload.roles || '';
        document.getElementById('userName').textContent    = sub;
        document.getElementById('userRole').textContent    = formatRol(role);
        document.getElementById('userInitials').textContent =
            sub.split(' ').map(p => p[0]).join('').toUpperCase().slice(0, 2);

        // Mostrar/ocultar sección usuarios según rol
        const esAdmin = String(role).includes('ADMIN');
        document.querySelectorAll('[data-role-admin]').forEach(el => {
            el.style.display = esAdmin ? '' : 'none';
        });
    } catch(e) { console.warn('JWT parse error', e); }
}

function formatRol(role) {
    const map = { ADMIN: 'Administrador', LABORATORISTA: 'Laboratorista',
                  RECEPCION: 'Recepción', MEDICO: 'Médico', PACIENTE: 'Paciente' };
    return map[String(role).replace('ROLE_','')] || role || 'Usuario';
}

// ── Navegación ────────────────────────────────────────────────────────────────
function navegarA(page) {
    document.querySelectorAll('.nav-item').forEach(n =>
        n.classList.toggle('active', n.dataset.page === page));
    document.querySelectorAll('.page').forEach(p =>
        p.classList.toggle('active', p.id === `page-${page}`));

    const titles = { dashboard: 'Inicio', pacientes: 'Pacientes',
                     ordenes: 'Órdenes', resultados: 'Resultados',
                     medicos: 'Médicos', usuarios: 'Usuarios',
                     pruebas: 'Pruebas',
                     configuracion: 'Configuracion' };
    document.getElementById('pageTitle').textContent = titles[page] || page;

    const showSearch = ['pacientes','ordenes','medicos'].includes(page);
    document.getElementById('searchBox').style.display   = showSearch ? '' : 'none';
    document.getElementById('btnNuevo').style.display    = (showSearch || page === 'usuarios' || page === 'pruebas') ? '' : 'none';
    document.getElementById('searchInput').value         = '';
    document.getElementById('searchInput').placeholder   =
        page === 'ordenes' ? 'Buscar orden o paciente...' :
        page === 'medicos' ? 'Buscar médico...' : 'Buscar paciente...';

    if (page === 'dashboard')  cargarDashboard();
    if (page === 'pacientes')  cargarPacientes(0);
    if (page === 'ordenes')    cargarOrdenes(0);
    if (page === 'resultados') prepararResultados();
    if (page === 'medicos')    cargarMedicos();
    if (page === 'usuarios')   cargarUsuarios();
    if (page === 'pruebas')    cargarPruebasAdmin();
    if (page === 'configuracion') cargarConfiguracion();
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
async function cargarDashboard() {
    try {
        const [pacientesData, stats, ordenesData] = await Promise.all([
            get('/api/pacientes?size=1'),
            get('/api/ordenes/stats'),
            get('/api/ordenes?size=10&sort=fechaRecepcion,desc')
        ]);
        document.getElementById('totalPacientes').textContent     = pacientesData.totalElements ?? '-';
        document.getElementById('ordenesPendientes').textContent  = stats.pendientes ?? '-';
        document.getElementById('ordenesHoy').textContent         = stats.completadas ?? '-';
        renderOrdenes('tablaOrdenesRecientes', ordenesData.content || []);
    } catch(e) { console.error(e); }
}

// ── Pacientes ─────────────────────────────────────────────────────────────────
async function cargarPacientes(page = 0, query = '') {
    paginaPac = page;
    const url = query
        ? `/api/pacientes?q=${encodeURIComponent(query)}&page=${page}&size=20`
        : `/api/pacientes?page=${page}&size=20`;
    const data  = await get(url);
    const tbody = document.getElementById('tablaPacientes');

    tbody.innerHTML = (data.content || []).map(p => `
        <tr>
            <td><code style="font-size:12px;color:var(--blue)">${p.codigoPaciente}</code></td>
            <td>${esc(p.nombreCompleto)}</td>
            <td>${p.fechaNacimiento ? fecSimple(p.fechaNacimiento) : '-'}</td>
            <td>${esc(p.sexo || '-')}</td>
            <td>${esc(p.telefono || '-')}</td>
            <td style="display:flex;gap:4px;">
                <button class="btn-icon" title="Editar" onclick="editarPaciente(${p.id})">✎</button>
                <button class="btn-icon" title="Nueva orden" onclick="modalNuevaOrden(${p.id},'${escA(p.nombreCompleto)}')">+ Orden</button>
            </td>
        </tr>`).join('');

    renderPaginacion('paginacionPacientes', data, 'pacientes');
}

function modalNuevoPaciente(paciente = null) {
    document.getElementById('modalTitulo').textContent = paciente ? 'Editar paciente' : 'Nuevo paciente';
    document.getElementById('modalBody').innerHTML = `
        <div class="form-group">
            <label class="form-label">Nombre completo *</label>
            <input id="f_nombre" class="form-input" value="${escA(paciente?.nombreCompleto || '')}">
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
            <div class="form-group">
                <label class="form-label">Fecha de nacimiento *</label>
                <input id="f_fecha" type="date" class="form-input" value="${paciente?.fechaNacimiento || ''}">
            </div>
            <div class="form-group">
                <label class="form-label">Sexo</label>
                <select id="f_sexo" class="form-input">
                    <option value="MASCULINO" ${paciente?.sexo==='MASCULINO'?'selected':''}>Masculino</option>
                    <option value="FEMENINO"  ${paciente?.sexo==='FEMENINO' ?'selected':''}>Femenino</option>
                    <option value="OTRO"      ${paciente?.sexo==='OTRO'     ?'selected':''}>Otro</option>
                </select>
            </div>
        </div>
        <div class="form-group"><label class="form-label">Teléfono</label>
            <input id="f_tel" class="form-input" value="${escA(paciente?.telefono||'')}"></div>
        <div class="form-group"><label class="form-label">Correo</label>
            <input id="f_email" type="email" class="form-input" value="${escA(paciente?.email||'')}"></div>
        <div class="form-group"><label class="form-label">Dirección</label>
            <input id="f_dir" class="form-input" value="${escA(paciente?.direccion||'')}"></div>
        <button class="btn-primary" style="width:100%" onclick="guardarPaciente(${paciente?.id||'null'})">
            ${paciente ? 'Guardar cambios' : 'Registrar paciente'}</button>`;
    abrirModal();
}

async function guardarPaciente(id) {
    const body = {
        nombreCompleto:  document.getElementById('f_nombre').value.trim(),
        fechaNacimiento: document.getElementById('f_fecha').value,
        sexo:            document.getElementById('f_sexo').value,
        telefono:        document.getElementById('f_tel').value.trim(),
        email:           document.getElementById('f_email').value.trim(),
        direccion:       document.getElementById('f_dir').value.trim()
    };
    if (id && id !== 'null') await put(`/api/pacientes/${id}`, body);
    else                     await post('/api/pacientes', body);
    cerrarModal();
    cargarPacientes(paginaPac);
}

async function editarPaciente(id) {
    const p = await get(`/api/pacientes/${id}`);
    modalNuevoPaciente(p);
}

// ── Médicos ───────────────────────────────────────────────────────────────────
async function cargarMedicos(query = '') {
    let lista = await get('/api/medicos');
    if (query) {
        const q = query.toLowerCase();
        lista = lista.filter(m => m.nombreCompleto.toLowerCase().includes(q)
                               || (m.especialidad||'').toLowerCase().includes(q));
    }
    medicosList = lista;
    const tbody = document.getElementById('tablaMedicos');
    if (!tbody) return;
    tbody.innerHTML = lista.map(m => `
        <tr>
            <td>${esc(m.nombreCompleto)}</td>
            <td>${esc(m.especialidad||'-')}</td>
            <td>${esc(m.telefono||'-')}</td>
            <td><span class="badge ${m.activo?'badge-completado':'badge-entregado'}">${m.activo?'Activo':'Inactivo'}</span></td>
            <td style="display:flex;gap:4px;">
                <button class="btn-icon" title="Editar" onclick="editarMedico(${m.id})">✎</button>
            </td>
        </tr>`).join('');
}

function modalNuevoMedico(medico = null) {
    document.getElementById('modalTitulo').textContent = medico ? 'Editar médico' : 'Nuevo médico';
    document.getElementById('modalBody').innerHTML = `
        <div class="form-group"><label class="form-label">Nombre completo *</label>
            <input id="fm_nombre" class="form-input" value="${escA(medico?.nombreCompleto||'')}"></div>
        <div class="form-group"><label class="form-label">Especialidad</label>
            <input id="fm_esp" class="form-input" value="${escA(medico?.especialidad||'')}"></div>
        <div class="form-group"><label class="form-label">Teléfono</label>
            <input id="fm_tel" class="form-input" value="${escA(medico?.telefono||'')}"></div>
        <button class="btn-primary" style="width:100%" onclick="guardarMedico(${medico?.id||'null'})">
            ${medico?'Guardar cambios':'Registrar médico'}</button>`;
    abrirModal();
}

async function guardarMedico(id) {
    const body = {
        nombreCompleto: document.getElementById('fm_nombre').value.trim(),
        especialidad:   document.getElementById('fm_esp').value.trim(),
        telefono:       document.getElementById('fm_tel').value.trim(),
        activo: true
    };
    if (id && id !== 'null') await put(`/api/medicos/${id}`, body);
    else                     await post('/api/medicos', body);
    cerrarModal();
    cargarMedicos();
}

async function editarMedico(id) {
    const m = await get(`/api/medicos/${id}`);
    modalNuevoMedico(m);
}

async function cargarUsuarios() {
    let lista;
    try {
        lista = await get('/api/usuarios');
    } catch(e) {
        mostrarToast('Solo administradores pueden ver los usuarios', true);
        return;
    }
    const tbody = document.getElementById('tablaUsuarios');
    if (!tbody) return;
    tbody.innerHTML = lista.map(u => `
        <tr>
            <td><strong>${esc(u.username)}</strong></td>
            <td>${esc(u.nombreCompleto)}</td>
            <td>${esc(u.email || '-')}</td>
            <td>${formatRol(u.rol)}</td>
            <td><span class="badge ${u.activo ? 'badge-completado' : 'badge-entregado'}">${u.activo ? 'Activo' : 'Inactivo'}</span></td>
            <td style="display:flex;gap:4px;">
                <button class="btn-icon" title="Editar" onclick="editarUsuario(${u.id})">✎</button>
                <button class="btn-icon" title="${u.activo ? 'Desactivar' : 'Activar'}" onclick="toggleUsuario(${u.id}, ${!u.activo})">${u.activo ? 'Desactivar' : 'Activar'}</button>
            </td>
        </tr>`).join('');
}

function modalNuevoUsuario(usuario = null) {
    document.getElementById('modalTitulo').textContent = usuario ? 'Editar usuario' : 'Nuevo usuario';
    document.getElementById('modalBody').innerHTML = `
        <div class="form-group"><label class="form-label">Usuario *</label>
            <input id="fu_user" class="form-input" value="${escA(usuario?.username || '')}"></div>
        <div class="form-group"><label class="form-label">Nombre completo *</label>
            <input id="fu_nombre" class="form-input" value="${escA(usuario?.nombreCompleto || '')}"></div>
        <div class="form-group"><label class="form-label">Correo</label>
            <input id="fu_email" type="email" class="form-input" value="${escA(usuario?.email || '')}"></div>
        <div class="form-group"><label class="form-label">Rol</label>
            <select id="fu_rol" class="form-input">
                ${['ADMIN','LABORATORISTA','RECEPCION','MEDICO','PACIENTE'].map(r =>
                    `<option value="${r}" ${usuario?.rol===r?'selected':''}>${formatRol(r)}</option>`).join('')}
            </select></div>
        <div class="form-group"><label class="form-label">${usuario ? 'Nueva contraseña' : 'Contraseña *'}</label>
            <input id="fu_pass" type="password" class="form-input" placeholder="${usuario ? 'Dejar vacía para conservarla' : ''}"></div>
        <label style="display:flex;align-items:center;gap:8px;margin-bottom:14px;font-size:13px;">
            <input id="fu_activo" type="checkbox" ${usuario?.activo === false ? '' : 'checked'}> Activo
        </label>
        <button class="btn-primary" style="width:100%" onclick="guardarUsuario(${usuario?.id || 'null'})">
            ${usuario ? 'Guardar cambios' : 'Crear usuario'}</button>`;
    abrirModal();
}

async function guardarUsuario(id) {
    const body = {
        username: document.getElementById('fu_user').value.trim(),
        nombreCompleto: document.getElementById('fu_nombre').value.trim(),
        email: document.getElementById('fu_email').value.trim(),
        rol: document.getElementById('fu_rol').value,
        password: document.getElementById('fu_pass').value,
        activo: document.getElementById('fu_activo').checked
    };
    try {
        if (id && id !== 'null') await put(`/api/usuarios/${id}`, body);
        else await post('/api/usuarios', body);
        cerrarModal();
        cargarUsuarios();
        mostrarToast('Usuario guardado correctamente');
    } catch {
        mostrarToast('No se pudo guardar el usuario', true);
    }
}

async function editarUsuario(id) {
    const u = await get(`/api/usuarios/${id}`);
    modalNuevoUsuario(u);
}

async function toggleUsuario(id, activo) {
    const u = await get(`/api/usuarios/${id}`);
    await put(`/api/usuarios/${id}`, { ...u, password: '', activo });
    cargarUsuarios();
}

async function cargarConfiguracion() {
    let cfg;
    try {
        cfg = await get('/api/configuracion');
    } catch(e) {
        mostrarToast('Solo administradores pueden ver la configuración', true);
        return;
    }
    const logo = cfg['lab.logo.dataUrl'];
    document.getElementById('configForm').innerHTML = `
        <div class="config-grid">
            ${campoConfig('lab.nombre', 'Nombre', cfg)}
            ${campoConfig('lab.direccion', 'Direccion', cfg)}
            ${campoConfig('lab.telefono', 'Telefono', cfg)}
            ${campoConfig('lab.email', 'Email', cfg)}
            ${campoConfig('lab.web', 'Web', cfg)}
            ${campoConfig('lab.desde', 'Leyenda', cfg)}
            ${campoConfig('lab.director', 'Director tecnico', cfg)}
            ${campoConfig('lab.cedula', 'Cedula', cfg)}
        </div>
        <h2 class="card-title" style="margin-top:18px;">Contenido del PDF</h2>
        <div class="config-checks">
            ${checkConfig('pdf.mostrarLogo', 'Mostrar logo', cfg)}
            ${checkConfig('pdf.mostrarContacto', 'Mostrar dirección, teléfono y email', cfg)}
            ${checkConfig('pdf.mostrarMedico', 'Mostrar médico referente', cfg)}
            ${checkConfig('pdf.mostrarObservaciones', 'Mostrar observaciones', cfg)}
            ${checkConfig('pdf.mostrarFirmas', 'Mostrar firmas', cfg)}
            ${checkConfig('pdf.mostrarReferencias', 'Mostrar rangos de referencia', cfg)}
            ${checkConfig('pdf.soloConResultado', 'Mostrar solo pruebas con resultado', cfg)}
        </div>
        <div class="form-group">
            <label class="form-label">Logo del laboratorio</label>
            <div class="logo-config-row">
                <div class="logo-preview">${logo ? `<img src="${logo}" alt="Logo">` : 'Sin logo'}</div>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                    <input id="labLogo" type="file" accept="image/png,image/jpeg,image/webp" class="form-input" style="max-width:280px;">
                    <button class="btn-icon" onclick="subirLogo()">Subir logo</button>
                    <button class="btn-icon" onclick="eliminarLogo()">Quitar</button>
                </div>
            </div>
        </div>
        <button class="btn-primary" onclick="guardarConfiguracion()">Guardar configuracion</button>`;
}

function campoConfig(clave, label, cfg) {
    return `<div class="form-group"><label class="form-label">${label}</label>
        <input class="form-input config-input" data-clave="${clave}" value="${escA(cfg[clave] || '')}"></div>`;
}

function checkConfig(clave, label, cfg) {
    const checked = cfg[clave] !== 'false';
    return `<label class="config-check"><input type="checkbox" class="config-check-input" data-clave="${clave}" ${checked ? 'checked' : ''}> ${label}</label>`;
}

async function guardarConfiguracion() {
    const body = {};
    document.querySelectorAll('.config-input').forEach(input => body[input.dataset.clave] = input.value.trim());
    document.querySelectorAll('.config-check-input').forEach(input => body[input.dataset.clave] = String(input.checked));
    await put('/api/configuracion', body);
    mostrarToast('Configuracion guardada');
}

async function subirLogo() {
    const file = document.getElementById('labLogo')?.files?.[0];
    if (!file) { mostrarToast('Selecciona un logo', true); return; }
    const form = new FormData();
    form.append('logo', file);
    const res = await fetch('/api/configuracion/logo', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: form
    });
    if (!res.ok) { mostrarToast('No se pudo subir el logo', true); return; }
    mostrarToast('Logo actualizado');
    cargarConfiguracion();
}

async function eliminarLogo() {
    const res = await fetch('/api/configuracion/logo', {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
    });
    if (!res.ok) { mostrarToast('No se pudo quitar el logo', true); return; }
    cargarConfiguracion();
}

async function cargarPruebasAdmin() {
    try {
        pruebasCatalogo = await get('/api/pruebas/todas');
    } catch(e) {
        mostrarToast('Solo administradores pueden gestionar pruebas', true);
        return;
    }
    const tbody = document.getElementById('tablaPruebas');
    if (!tbody) return;
    tbody.innerHTML = pruebasCatalogo.map(p => `
        <tr>
            <td>${esc(p.area || '-')}</td>
            <td><strong>${esc(p.nombre)}</strong></td>
            <td>${esc(p.unidades || '-')}</td>
            <td>${esc(p.intervaloReferencia || '-')}</td>
            <td>${p.orden ?? 0}</td>
            <td><span class="badge ${p.activa ? 'badge-completado' : 'badge-entregado'}">${p.activa ? 'Activa' : 'Inactiva'}</span></td>
            <td><button class="btn-icon" onclick="editarPrueba(${p.id})">✎</button></td>
        </tr>`).join('');
}

function modalNuevaPrueba(prueba = null) {
    document.getElementById('modalTitulo').textContent = prueba ? 'Editar prueba' : 'Nueva prueba';
    document.getElementById('modalBody').innerHTML = `
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
            <div class="form-group"><label class="form-label">Área / categoría *</label>
                <input id="fp_area" class="form-input" value="${escA(prueba?.area || '')}" placeholder="Ej: INMUNOLOGIA"></div>
            <div class="form-group"><label class="form-label">Orden</label>
                <input id="fp_orden" type="number" class="form-input" value="${prueba?.orden ?? 0}"></div>
        </div>
        <div class="form-group"><label class="form-label">Nombre de la prueba *</label>
            <input id="fp_nombre" class="form-input" value="${escA(prueba?.nombre || '')}"></div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
            <div class="form-group"><label class="form-label">Unidades</label>
                <input id="fp_unidades" class="form-input" value="${escA(prueba?.unidades || '')}"></div>
            <div class="form-group"><label class="form-label">Referencia</label>
                <input id="fp_ref" class="form-input" value="${escA(prueba?.intervaloReferencia || '')}"></div>
        </div>
        <label style="display:flex;align-items:center;gap:8px;margin-bottom:14px;font-size:13px;">
            <input id="fp_activa" type="checkbox" ${prueba?.activa === false ? '' : 'checked'}> Activa
        </label>
        <button class="btn-primary" style="width:100%" onclick="guardarPrueba(${prueba?.id || 'null'})">
            ${prueba ? 'Guardar cambios' : 'Crear prueba'}</button>`;
    abrirModal();
}

async function editarPrueba(id) {
    const p = await get(`/api/pruebas/${id}`);
    modalNuevaPrueba(p);
}

async function guardarPrueba(id) {
    const body = {
        area: document.getElementById('fp_area').value.trim(),
        nombre: document.getElementById('fp_nombre').value.trim(),
        unidades: document.getElementById('fp_unidades').value.trim(),
        intervaloReferencia: document.getElementById('fp_ref').value.trim(),
        orden: Number(document.getElementById('fp_orden').value || 0),
        activa: document.getElementById('fp_activa').checked
    };
    if (id && id !== 'null') await put(`/api/pruebas/${id}`, body);
    else await post('/api/pruebas', body);
    cerrarModal();
    cargarPruebasAdmin();
    mostrarToast('Prueba guardada');
}

// ── Órdenes ───────────────────────────────────────────────────────────────────
async function cargarOrdenes(page = 0, query = '') {
    paginaOrden = page;
    const url = query
        ? `/api/ordenes?q=${encodeURIComponent(query)}&page=${page}&size=20&sort=fechaRecepcion,desc`
        : `/api/ordenes?page=${page}&size=20&sort=fechaRecepcion,desc`;
    const data = await get(url);
    renderOrdenes('tablaOrdenes', data.content || []);
    renderPaginacion('paginacionOrdenes', data, 'ordenes');
}

function renderOrdenes(containerId, ordenes) {
    const tbody = document.getElementById(containerId);
    if (!tbody) return;
    tbody.innerHTML = ordenes.map(o => `
        <tr>
            <td><strong>${esc(o.numeroOrden)}</strong></td>
            <td>${esc(o.paciente?.nombreCompleto||'-')}</td>
            <td>${o.medico ? esc(o.medico.nombreCompleto) : '<span style="color:var(--text-muted)">—</span>'}</td>
            <td>${fecLargo(o.fechaRecepcion)}</td>
            <td><span class="badge badge-${(o.estado||'').toLowerCase()}">${labelEstado(o.estado)}</span></td>
            <td style="display:flex;gap:4px;">
                <button class="btn-icon" title="Ingresar resultados" onclick="irAResultados(${o.id})">✓ Resultados</button>
                <button class="btn-icon" title="Generar PDF" onclick="verPdf(${o.id})">PDF</button>
            </td>
        </tr>`).join('');
}

// Modal nueva orden desde botón global
async function abrirModalNuevo() {
    const page = document.querySelector('.nav-item.active')?.dataset.page;
    if (page === 'pacientes') modalNuevoPaciente();
    if (page === 'ordenes')   await modalSeleccionarPacienteOrden();
    if (page === 'medicos')   modalNuevoMedico();
    if (page === 'usuarios')  modalNuevoUsuario();
    if (page === 'pruebas')   modalNuevaPrueba();
}

async function modalSeleccionarPacienteOrden() {
    const [pacsData, meds, pruebas] = await Promise.all([
        get('/api/pacientes?size=200'),
        get('/api/medicos'),
        get('/api/pruebas')
    ]);
    const pacs = pacsData.content || [];
    pruebasLaboratorio = pruebas;
    document.getElementById('modalTitulo').textContent = 'Nueva orden';
    document.getElementById('modalBody').innerHTML = `
        <div class="form-group"><label class="form-label">Paciente *</label>
            <select id="ordenPaciente" class="form-input">
                ${pacs.map(p=>`<option value="${p.id}">${esc(p.nombreCompleto)} — ${esc(p.codigoPaciente)}</option>`).join('')}
            </select></div>
        <div class="form-group"><label class="form-label">Médico referente</label>
            <select id="ordenMedico" class="form-input">
                <option value="">— Sin médico —</option>
                ${meds.map(m=>`<option value="${m.id}">${esc(m.nombreCompleto)}${m.especialidad?' · '+esc(m.especialidad):''}</option>`).join('')}
            </select></div>
            
            <div class="form-group"><label class="form-label">Tipo de muestra</label>
    <input id="ordenMuestra" class="form-input" 
           value="Orina (chorro medio)" 
           placeholder="Ej: Orina (chorro medio), Sangre venosa..."></div>
        <div class="form-group"><label class="form-label">Observaciones</label>
            <input id="ordenObs" class="form-input"></div>
        ${renderSelectorPruebasOrden(pruebas)}
        <button class="btn-primary" style="width:100%" onclick="crearOrdenDesdeModal()">Crear orden</button>`;
    abrirModal();
}

async function modalNuevaOrden(pacienteId, nombre) {
    const [meds, pruebas] = await Promise.all([get('/api/medicos'), get('/api/pruebas')]);
    pruebasLaboratorio = pruebas;
    document.getElementById('modalTitulo').textContent = 'Nueva orden';
    document.getElementById('modalBody').innerHTML = `
        <p style="margin-bottom:14px">Paciente: <strong>${esc(nombre)}</strong></p>
        <div class="form-group"><label class="form-label">Médico referente</label>
            <select id="ordenMedico" class="form-input">
                <option value="">— Sin médico —</option>
                ${meds.map(m=>`<option value="${m.id}">${esc(m.nombreCompleto)}${m.especialidad?' · '+esc(m.especialidad):''}</option>`).join('')}
            </select></div>
            <div class="form-group"><label class="form-label">Tipo de muestra</label>
    <input id="ordenMuestra" class="form-input" 
           value="Orina (chorro medio)" 
           placeholder="Ej: Orina (chorro medio), Sangre venosa..."></div>
        <div class="form-group"><label class="form-label">Observaciones</label>
            <input id="ordenObs" class="form-input"></div>
        ${renderSelectorPruebasOrden(pruebas)}
        <button class="btn-primary" style="width:100%" onclick="crearOrden(${pacienteId})">Crear orden</button>`;
    abrirModal();
}

async function crearOrdenDesdeModal() {
    await crearOrden(Number(document.getElementById('ordenPaciente').value));
}

async function crearOrden(pacienteId) {
    try {
        const medicoId = document.getElementById('ordenMedico')?.value;
        await post('/api/ordenes', {
            pacienteId,
            medicoReferenteId: medicoId ? Number(medicoId) : null,
            tipoMuestra: document.getElementById('ordenMuestra')?.value || 'Orina (chorro medio)',
            observaciones: document.getElementById('ordenObs')?.value || '',
            pruebaIds: pruebasSeleccionadasOrden()
        });
        cerrarModal();
        navegarA('ordenes');
    } catch(e) {
        mostrarToast('Error al crear la orden: ' + e.message, true);
    }
}

function renderSelectorPruebasOrden(pruebas) {
    const porArea = pruebas.reduce((acc, p) => {
        const area = p.area || 'GENERAL';
        acc[area] = acc[area] || [];
        acc[area].push(p);
        return acc;
    }, {});
    return `<div class="form-group"><label class="form-label">Pruebas solicitadas para esta orden</label>
        <div class="test-picker">
            ${Object.entries(porArea).map(([area, items]) => `
                <div class="test-area">
                    <div class="test-area-title">
                        <label><input type="checkbox" checked onchange="toggleAreaPruebas('${escA(area)}', this.checked)"> ${esc(area)}</label>
                    </div>
                    <div class="test-list">
                        ${items.map(p => `<label><input class="orden-prueba-check" data-area="${escA(area)}" type="checkbox" value="${p.id}" checked> ${esc(p.nombre)}</label>`).join('')}
                    </div>
                </div>`).join('')}
        </div></div>`;
}

function toggleAreaPruebas(area, checked) {
    document.querySelectorAll(`.orden-prueba-check[data-area="${CSS.escape(area)}"]`)
        .forEach(chk => chk.checked = checked);
}

function pruebasSeleccionadasOrden() {
    return [...document.querySelectorAll('.orden-prueba-check:checked')].map(chk => Number(chk.value));
}

// ── Resultados ────────────────────────────────────────────────────────────────
async function prepararResultados() {
    const data = await get('/api/ordenes?size=200&sort=fechaRecepcion,desc');
    const select = document.getElementById('ordenResultadoSelect');
    select.innerHTML = (data.content || []).map(o =>
        `<option value="${o.id}">${esc(o.numeroOrden)} — ${esc(o.paciente.nombreCompleto)}</option>`).join('');
    document.getElementById('formResultados').innerHTML = '';
}

async function irAResultados(ordenId) {
    navegarA('resultados');
    await prepararResultados();
    document.getElementById('ordenResultadoSelect').value = String(ordenId);
    cargarFormularioResultados();
}

async function cargarFormularioResultados() {
    const ordenId = document.getElementById('ordenResultadoSelect').value;
    if (!ordenId) return;

    const [solicitadas, existentes, ordenData] = await Promise.all([
        get(`/api/ordenes/${ordenId}/pruebas`),
        get(`/api/ordenes/${ordenId}/resultados`),
        get(`/api/ordenes/${ordenId}`)
    ]);
    pruebasLaboratorio = (solicitadas || []).map(item => item.prueba).filter(Boolean);
    if (pruebasLaboratorio.length === 0) {
        document.getElementById('formResultados').innerHTML = `
            <div class="empty-state">
                Esta orden no tiene pruebas solicitadas. Crea una orden nueva seleccionando pruebas o actualiza la selección desde backend.
            </div>`;
        return;
    }
    const porPrueba = new Map(existentes.map(r => [r.prueba.id, r]));

    const porArea = pruebasLaboratorio.reduce((acc, p) => {
        const area = p.area || 'GENERAL';
        acc[area] = acc[area] || [];
        acc[area].push(p);
        return acc;
    }, {});

    const tablas = Object.entries(porArea).map(([area, pruebas]) => `
        <h3 class="area-title">${esc(area)}</h3>
        <table class="data-table result-table">
            <thead><tr>
                <th>Prueba</th><th style="min-width:180px">Resultado</th>
                <th>Alarma</th><th>Unidades</th><th>Referencia</th>
            </tr></thead>
            <tbody>
                ${pruebas.map(p => {
                    const r = porPrueba.get(p.id);
                    return `<tr>
                        <td style="font-weight:500">${esc(p.nombre)}</td>
                        <td><input class="form-input result-input" data-prueba="${p.id}"
                                   value="${escA(r?.valor || valorSugerido(p.nombre))}"></td>
                        <td style="text-align:center">
                            <input type="checkbox" class="result-alarm" data-prueba="${p.id}" ${r?.alarma?'checked':''}></td>
                        <td style="color:var(--text-muted)">${esc(p.unidades||'')}</td>
                        <td style="color:var(--text-muted);font-size:12px">${esc(p.intervaloReferencia||'')}</td>
                    </tr>`;
                }).join('')}
            </tbody>
        </table>`).join('');

    document.getElementById('formResultados').innerHTML = `
        <div class="form-group" style="margin-bottom:12px;">
            <label class="form-label">Observaciones de la orden</label>
            <input id="obsOrden" class="form-input" value="${escA(ordenData.observaciones||'')}"
                   placeholder="Ej: Muestra tomada en ayunas, paciente neonato...">
        </div>
        ${tablas}
        <div style="display:flex;gap:8px;margin-top:14px;flex-wrap:wrap;">
            <button class="btn-primary" onclick="guardarResultados()">💾 Guardar y completar</button>
            <button class="btn-primary" style="background:var(--green)" onclick="verPdf(${ordenId})">📄 Generar PDF</button>
        </div>`;
}

async function guardarResultados() {
    const ordenId = document.getElementById('ordenResultadoSelect').value;
    const obs     = document.getElementById('obsOrden')?.value || '';
    const resultados = [...document.querySelectorAll('.result-input')].map(input => {
        const pruebaId = Number(input.dataset.prueba);
        const alarma   = document.querySelector(`.result-alarm[data-prueba="${pruebaId}"]`)?.checked || false;
        return { pruebaId, valor: input.value.trim(), alarma };
    });
    try {
        await put(`/api/ordenes/${ordenId}/uroanalisis`, { resultados, observaciones: obs });
        mostrarToast('Resultados guardados correctamente ✓');
        navegarA('ordenes');
    } catch (e) {
        mostrarToast(e.message.includes('permiso') ? 'No tiene permiso para guardar resultados' : 'Error al guardar: ' + e.message, true);
    }
}

function valorSugerido(nombre) {
    const base = {
        'COLOR': 'AMARILLO', 'ASPECTO': 'LIMPIO',
        'DENSIDAD': '1.020', 'PH': '6',
        'LEUCOCITOS': '1-2', 'CELULAS EPITELIALES': 'ESCASAS'
    };
    return base[nombre] || 'NEGATIVO';
}

async function verPdf(ordenId) {
    const res = await fetch(`/api/ordenes/${ordenId}/pdf`, {
        headers: { Authorization: `Bearer ${token}` }
    });
    if (!res.ok) { mostrarToast('No se pudo generar el PDF', true); return; }
    const blob = await res.blob();
    const url  = URL.createObjectURL(blob);
    window.open(url, '_blank');
    setTimeout(() => URL.revokeObjectURL(url), 60000);
}

// ── Modal ─────────────────────────────────────────────────────────────────────
function abrirModal() {
    document.getElementById('modal').classList.add('open');
    document.getElementById('modalOverlay').classList.add('open');
}
function cerrarModal() {
    document.getElementById('modal').classList.remove('open');
    document.getElementById('modalOverlay').classList.remove('open');
}

// ── Toast ─────────────────────────────────────────────────────────────────────
function mostrarToast(msg, error = false) {
    let t = document.getElementById('toast');
    if (!t) {
        t = document.createElement('div');
        t.id = 'toast';
        t.style.cssText = 'position:fixed;bottom:24px;right:24px;padding:10px 18px;border-radius:8px;font-size:13px;font-weight:500;z-index:9999;transition:opacity .3s;';
        document.body.appendChild(t);
    }
    t.textContent   = msg;
    t.style.background = error ? '#fee2e2' : '#e6f5ed';
    t.style.color      = error ? '#b91c1c' : '#1a7a4a';
    t.style.opacity    = '1';
    setTimeout(() => { t.style.opacity = '0'; }, 3000);
}

// ── Fetch helpers ─────────────────────────────────────────────────────────────
async function get(url) {
    const res = await fetch(API + url, { headers: { Authorization: `Bearer ${token}` } });
    if (res.status === 401) { logout(); throw new Error('Sesión expirada'); }
    if (res.status === 403) { throw new Error('Sin permiso para esta acción'); }
    if (!res.ok) throw new Error(`Error ${res.status}`);
    return res.json();
}
async function post(url, body, auth = true) {
    const headers = { 'Content-Type': 'application/json' };
    if (auth) headers.Authorization = `Bearer ${token}`;
    const res = await fetch(API + url, { method: 'POST', headers, body: JSON.stringify(body) });
    if (res.status === 401) { logout(); throw new Error('Sesión expirada'); }
    if (res.status === 403) { throw new Error('Sin permiso para esta acción'); }
    if (!res.ok) throw new Error(`Error ${res.status}`);
    return res.json();
}
async function put(url, body) {
    const res = await fetch(API + url, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body)
    });
    if (res.status === 401) { logout(); throw new Error('Sesión expirada'); }
    if (res.status === 403) { throw new Error('Sin permiso para esta acción'); }
    if (!res.ok) throw new Error(`Error ${res.status}`);
    return res.json();
}
async function del(url) {
    const res = await fetch(API + url, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
    });
    if (res.status === 401) { logout(); throw new Error('Sesión expirada'); }
    if (res.status === 403) { throw new Error('Sin permiso para esta acción'); }
    if (!res.ok) throw new Error(`Error ${res.status}`);
    return res.status === 204 ? null : res.json();
}

// ── Utils ─────────────────────────────────────────────────────────────────────
function fecLargo(iso) {
    if (!iso) return '-';
    return new Date(iso).toLocaleString('es-SV', {
        day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit' });
}
function fecSimple(iso) {
    return new Date(iso + 'T00:00:00').toLocaleDateString('es-SV');
}
function labelEstado(e) {
    return { PENDIENTE:'Pendiente', EN_PROCESO:'En proceso',
             COMPLETADO:'Completado', ENTREGADO:'Entregado' }[e] || e || '-';
}
function renderPaginacion(containerId, pageData, tipo) {
    const cont = document.getElementById(containerId);
    if (!cont || !pageData || pageData.totalPages <= 1) { if(cont) cont.innerHTML=''; return; }
    cont.innerHTML = Array.from({ length: pageData.totalPages }, (_, i) =>
        `<button class="page-btn ${i===pageData.number?'active':''}"
         onclick="${tipo==='ordenes'?`cargarOrdenes(${i})`:`cargarPacientes(${i})`}">${i+1}</button>`).join('');
}
function esc(v) {
    return String(v ?? '').replace(/[&<>"']/g, c =>
        ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
function escA(v) { return esc(v).replace(/`/g,'&#96;'); }
