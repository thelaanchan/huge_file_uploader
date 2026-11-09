// State
let selectedFile = null;
let currentFileId = null;
let chunkSize = 5242880; // 5 MB
let totalChunks = 0;
let isPaused = false;
let isUploading = false;
let abortController = null;
let uploadedChunksSet = new Set();
let uploadStartTime = null;
let bytesUploadedSoFar = 0;

// DOM Elements
const dropzone = document.getElementById('dropzone');
const fileInput = document.getElementById('fileInput');
const fileInfoPanel = document.getElementById('fileInfoPanel');
const displayFileName = document.getElementById('displayFileName');
const displayFileSize = document.getElementById('displayFileSize');
const chunkSizeSelect = document.getElementById('chunkSizeSelect');
const totalChunksEstimated = document.getElementById('totalChunksEstimated');
const progressSection = document.getElementById('progressSection');
const statusLabel = document.getElementById('statusLabel');
const percentageLabel = document.getElementById('percentageLabel');
const progressBar = document.getElementById('progressBar');
const transferredBytesLabel = document.getElementById('transferredBytesLabel');
const totalBytesLabel = document.getElementById('totalBytesLabel');
const chunksProgressLabel = document.getElementById('chunksProgressLabel');
const speedLabel = document.getElementById('speedLabel');
const startBtn = document.getElementById('startBtn');
const pauseBtn = document.getElementById('pauseBtn');
const resumeBtn = document.getElementById('resumeBtn');
const cancelBtn = document.getElementById('cancelBtn');
const logConsole = document.getElementById('logConsole');
const clearLogBtn = document.getElementById('clearLogBtn');
const refreshFilesBtn = document.getElementById('refreshFilesBtn');
const filesTableBody = document.getElementById('filesTableBody');

// Event Listeners
fileInput.addEventListener('change', (e) => handleFileSelected(e.target.files[0]));

dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('dragover');
});
dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) {
        handleFileSelected(e.dataTransfer.files[0]);
    }
});

chunkSizeSelect.addEventListener('change', () => {
    chunkSize = parseInt(chunkSizeSelect.value, 10);
    updateChunkEstimate();
});

startBtn.addEventListener('click', startUpload);
pauseBtn.addEventListener('click', pauseUpload);
resumeBtn.addEventListener('click', resumeUpload);
cancelBtn.addEventListener('click', cancelUpload);
clearLogBtn.addEventListener('click', () => logConsole.innerHTML = '');
refreshFilesBtn.addEventListener('click', fetchFilesList);

// Initialize
fetchFilesList();

// Functions
function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function log(msg, type = 'info') {
    const time = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.className = `log-entry ${type}`;
    entry.textContent = `[${time}] ${msg}`;
    logConsole.appendChild(entry);
    logConsole.scrollTop = logConsole.scrollHeight;
}

function handleFileSelected(file) {
    if (!file) return;
    selectedFile = file;
    currentFileId = null;
    isPaused = false;
    isUploading = false;
    uploadedChunksSet.clear();

    displayFileName.textContent = file.name;
    displayFileSize.textContent = formatBytes(file.size);
    totalBytesLabel.textContent = formatBytes(file.size);

    fileInfoPanel.classList.remove('hidden');
    progressSection.classList.remove('hidden');

    updateChunkEstimate();
    resetProgressUI();
    setUIState('READY');

    log(`File selected: "${file.name}" (${formatBytes(file.size)})`, 'info');
}

function updateChunkEstimate() {
    if (!selectedFile) return;
    chunkSize = parseInt(chunkSizeSelect.value, 10);
    totalChunks = Math.ceil(selectedFile.size / chunkSize);
    if (totalChunks === 0) totalChunks = 1;
    totalChunksEstimated.textContent = `Estimated chunks: ${totalChunks} (~${formatBytes(chunkSize)} each)`;
}

function resetProgressUI() {
    progressBar.style.width = '0%';
    percentageLabel.textContent = '0%';
    transferredBytesLabel.textContent = '0 B';
    chunksProgressLabel.textContent = `0 / ${totalChunks}`;
    speedLabel.textContent = '0 MB/s';
}

function setUIState(state) {
    statusLabel.className = 'status-badge';
    switch (state) {
        case 'READY':
            statusLabel.textContent = 'Ready';
            statusLabel.classList.add('status-idle');
            startBtn.classList.remove('hidden');
            pauseBtn.classList.add('hidden');
            resumeBtn.classList.add('hidden');
            cancelBtn.classList.add('hidden');
            chunkSizeSelect.disabled = false;
            break;
        case 'UPLOADING':
            statusLabel.textContent = 'Uploading to MySQL';
            statusLabel.classList.add('status-uploading');
            startBtn.classList.add('hidden');
            pauseBtn.classList.remove('hidden');
            resumeBtn.classList.add('hidden');
            cancelBtn.classList.remove('hidden');
            chunkSizeSelect.disabled = true;
            break;
        case 'PAUSED':
            statusLabel.textContent = 'Paused / Interrupted';
            statusLabel.classList.add('status-paused');
            startBtn.classList.add('hidden');
            pauseBtn.classList.add('hidden');
            resumeBtn.classList.remove('hidden');
            cancelBtn.classList.remove('hidden');
            break;
        case 'COMPLETED':
            statusLabel.textContent = 'Completed';
            statusLabel.classList.add('status-completed');
            startBtn.classList.add('hidden');
            pauseBtn.classList.add('hidden');
            resumeBtn.classList.add('hidden');
            cancelBtn.classList.add('hidden');
            chunkSizeSelect.disabled = false;
            break;
        case 'FAILED':
            statusLabel.textContent = 'Failed';
            statusLabel.classList.add('status-failed');
            startBtn.classList.add('hidden');
            pauseBtn.classList.add('hidden');
            resumeBtn.classList.remove('hidden');
            cancelBtn.classList.remove('hidden');
            break;
    }
}

async function startUpload() {
    if (!selectedFile) return;

    isPaused = false;
    isUploading = true;
    setUIState('UPLOADING');
    uploadStartTime = Date.now();

    const fileIdentifier = `${selectedFile.name}_${selectedFile.size}_${selectedFile.lastModified}`;

    log(`Initializing upload for file "${selectedFile.name}"...`, 'info');

    try {
        // Step 1: Init or check existing upload for resume
        const initResponse = await fetch('/api/files/init', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fileName: selectedFile.name,
                fileSize: selectedFile.size,
                contentType: selectedFile.type || 'application/octet-stream',
                chunkSize: chunkSize,
                fileIdentifier: fileIdentifier
            })
        });

        if (!initResponse.ok) {
            throw new Error(`Init failed: ${initResponse.statusText}`);
        }

        const initData = await initResponse.json();
        currentFileId = initData.fileId;
        chunkSize = initData.chunkSize;
        totalChunks = initData.totalChunks;

        uploadedChunksSet = new Set(initData.uploadedChunks || []);

        if (initData.resumed) {
            log(`[RESUME DETECTED] Previous upload found in MySQL! ${uploadedChunksSet.size}/${totalChunks} chunks already saved.`, 'warning');
            log(`Resuming upload directly from chunk index ${initData.nextChunkIndex}...`, 'info');
        } else {
            log(`Upload registered in MySQL with ID: ${currentFileId}. Starting chunk 0...`, 'info');
        }

        updateProgress();

        if (initData.status === 'COMPLETED' || uploadedChunksSet.size >= totalChunks) {
            log('File is already 100% uploaded in database!', 'success');
            setUIState('COMPLETED');
            fetchFilesList();
            return;
        }

        await uploadRemainingChunks();

    } catch (err) {
        log(`Upload initialization error: ${err.message}`, 'error');
        setUIState('FAILED');
    }
}

async function uploadRemainingChunks() {
    abortController = new AbortController();

    for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
        if (isPaused) {
            log(`Upload halted at chunk ${chunkIndex}. Data up to chunk ${chunkIndex - 1} is safely in MySQL.`, 'warning');
            setUIState('PAUSED');
            return;
        }

        // Skip chunks already persisted in MySQL
        if (uploadedChunksSet.has(chunkIndex)) {
            continue;
        }

        const start = chunkIndex * chunkSize;
        const end = Math.min(start + chunkSize, selectedFile.size);
        const chunkBlob = selectedFile.slice(start, end);

        const formData = new FormData();
        formData.append('chunkIndex', chunkIndex);
        formData.append('file', chunkBlob, `chunk_${chunkIndex}.bin`);

        const chunkStartTime = Date.now();

        try {
            const resp = await fetch(`/api/files/${currentFileId}/chunks`, {
                method: 'POST',
                body: formData,
                signal: abortController.signal
            });

            if (!resp.ok) {
                const errText = await resp.text();
                throw new Error(`Server returned ${resp.status}: ${errText}`);
            }

            const data = await resp.json();
            uploadedChunksSet.add(chunkIndex);

            // Compute speed
            const chunkDurationSec = (Date.now() - chunkStartTime) / 1000;
            if (chunkDurationSec > 0) {
                const speedMBs = ((chunkBlob.size / 1024 / 1024) / chunkDurationSec).toFixed(2);
                speedLabel.textContent = `${speedMBs} MB/s`;
            }

            updateProgress();

            if (data.fileCompleted) {
                log(`Success! All ${totalChunks} chunks stored in MySQL. File fully uploaded!`, 'success');
                setUIState('COMPLETED');
                fetchFilesList();
                return;
            }

        } catch (err) {
            if (err.name === 'AbortError') {
                log(`Chunk ${chunkIndex} transfer aborted.`, 'warning');
            } else {
                log(`Error uploading chunk ${chunkIndex}: ${err.message}. Ready to resume.`, 'error');
                setUIState('PAUSED');
            }
            return;
        }
    }
}

function updateProgress() {
    const uploadedCount = uploadedChunksSet.size;
    const percentage = totalChunks > 0 ? ((uploadedCount / totalChunks) * 100).toFixed(1) : 0;

    progressBar.style.width = `${percentage}%`;
    percentageLabel.textContent = `${percentage}%`;
    chunksProgressLabel.textContent = `${uploadedCount} / ${totalChunks}`;

    const approxTransferred = Math.min(uploadedCount * chunkSize, selectedFile ? selectedFile.size : 0);
    transferredBytesLabel.textContent = formatBytes(approxTransferred);
}

function pauseUpload() {
    isPaused = true;
    if (abortController) {
        abortController.abort();
    }
    log('[PAUSE TRIGGERED] Network drop simulated / upload paused.', 'warning');
    setUIState('PAUSED');
}

async function resumeUpload() {
    if (!selectedFile || !currentFileId) {
        startUpload();
        return;
    }

    log(`Querying MySQL for current status of file "${selectedFile.name}"...`, 'info');
    setUIState('UPLOADING');
    isPaused = false;

    try {
        const resp = await fetch(`/api/files/${currentFileId}/status`);
        if (!resp.ok) {
            throw new Error(`Failed to query file status: ${resp.statusText}`);
        }

        const statusData = await resp.json();
        uploadedChunksSet = new Set(statusData.uploadedChunks || []);

        log(`[RESUMING] MySQL has ${uploadedChunksSet.size}/${totalChunks} chunks. Next chunk: ${statusData.nextChunkIndex}`, 'info');
        updateProgress();

        if (statusData.status === 'COMPLETED' || uploadedChunksSet.size >= totalChunks) {
            log('File already completed in MySQL!', 'success');
            setUIState('COMPLETED');
            fetchFilesList();
            return;
        }

        await uploadRemainingChunks();

    } catch (err) {
        log(`Resume failed: ${err.message}`, 'error');
        setUIState('FAILED');
    }
}

function cancelUpload() {
    isPaused = true;
    if (abortController) {
        abortController.abort();
    }
    resetProgressUI();
    setUIState('READY');
    log('Upload cancelled by user.', 'info');
}

// Fetch list of files from MySQL
async function fetchFilesList() {
    try {
        const resp = await fetch('/api/files');
        if (!resp.ok) return;

        const files = await resp.json();
        renderFilesTable(files);
    } catch (err) {
        console.error('Failed to load files:', err);
    }
}

function renderFilesTable(files) {
    if (!files || files.length === 0) {
        filesTableBody.innerHTML = `<tr><td colspan="6" class="text-center empty-state">No files uploaded yet.</td></tr>`;
        return;
    }

    filesTableBody.innerHTML = files.map(f => {
        const statusBadgeClass = f.status === 'COMPLETED' ? 'status-completed' : 'status-uploading';
        const dateStr = f.createdAt ? new Date(f.createdAt).toLocaleString() : '-';

        return `
            <tr>
                <td style="font-weight: 500; font-family: var(--font-mono);">${escapeHtml(f.fileName)}</td>
                <td>${formatBytes(f.fileSize)}</td>
                <td>${f.uploadedChunksCount} / ${f.totalChunks}</td>
                <td><span class="status-badge ${statusBadgeClass}">${f.status}</span></td>
                <td>${dateStr}</td>
                <td>
                    <div class="table-actions">
                        ${f.status === 'COMPLETED' ? `
                            <a href="/api/files/${f.fileId}/download" class="btn btn-sm btn-download-sm">Download</a>
                        ` : ''}
                        <button onclick="deleteFileRecord('${f.fileId}')" class="btn btn-sm btn-danger-sm">Delete</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

async function deleteFileRecord(fileId) {
    if (!confirm('Are you sure you want to delete this file and all its chunks from MySQL?')) return;

    try {
        const resp = await fetch(`/api/files/${fileId}`, { method: 'DELETE' });
        if (resp.ok) {
            log(`Deleted file ${fileId} from MySQL.`, 'info');
            fetchFilesList();
        } else {
            alert('Failed to delete file');
        }
    } catch (err) {
        alert('Error deleting file: ' + err.message);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

