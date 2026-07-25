// OralAI Unified Data Store for Localhost & Supabase & Backend Sync
(function(window) {
    const STORAGE_PATIENTS_KEY = 'oralai_local_patients';
    const STORAGE_REPORTS_KEY = 'oralai_local_reports';
    const STORAGE_ACTIVITIES_KEY = 'oralai_activity_log';

    // Default Seed Data so Localhost is never empty on first load
    const DEFAULT_PATIENTS = [
        {
            id: 'PAT-10482',
            mrn: 'PT-10482',
            full_name: 'Rajesh Kumar',
            age: 48,
            gender: 'Male',
            created_at: new Date(Date.now() - 86400000 * 3).toISOString()
        },
        {
            id: 'PAT-10483',
            mrn: 'PT-10483',
            full_name: 'Priya Sharma',
            age: 36,
            gender: 'Female',
            created_at: new Date(Date.now() - 86400000 * 2).toISOString()
        },
        {
            id: 'PAT-10484',
            mrn: 'PT-10484',
            full_name: 'Amit Patel',
            age: 52,
            gender: 'Male',
            created_at: new Date(Date.now() - 86400000 * 1).toISOString()
        },
        {
            id: 'PAT-10485',
            mrn: 'PT-10485',
            full_name: 'Sunita Verma',
            age: 29,
            gender: 'Female',
            created_at: new Date().toISOString()
        }
    ];

    const DEFAULT_REPORTS = [
        {
            id: 'REP-1001',
            patient_id: 'PAT-10482',
            mrn: 'PT-10482',
            patient_name: 'Rajesh Kumar',
            risk_level: 'High',
            risk_percentage: 94,
            has_cancer: true,
            message: 'High risk squamous cell carcinoma signs detected on lateral tongue.',
            scan_image_url: '',
            analysis_date: new Date(Date.now() - 86400000 * 3).toISOString()
        },
        {
            id: 'REP-1002',
            patient_id: 'PAT-10483',
            mrn: 'PT-10483',
            patient_name: 'Priya Sharma',
            risk_level: 'Low',
            risk_percentage: 4,
            has_cancer: false,
            message: 'Normal oral mucosa. No malignant structures detected.',
            scan_image_url: '',
            analysis_date: new Date(Date.now() - 86400000 * 2).toISOString()
        },
        {
            id: 'REP-1003',
            patient_id: 'PAT-10484',
            mrn: 'PT-10484',
            patient_name: 'Amit Patel',
            risk_level: 'High',
            risk_percentage: 91,
            has_cancer: true,
            message: 'Leukoplakia lesion with elevated risk profile detected.',
            scan_image_url: '',
            analysis_date: new Date(Date.now() - 86400000 * 1).toISOString()
        },
        {
            id: 'REP-1004',
            patient_id: 'PAT-10485',
            mrn: 'PT-10485',
            patient_name: 'Sunita Verma',
            risk_level: 'Low',
            risk_percentage: 3,
            has_cancer: false,
            message: 'Normal oral tissue examination.',
            scan_image_url: '',
            analysis_date: new Date().toISOString()
        }
    ];

    const DEFAULT_ACTIVITIES = [
        {
            id: 'ACT-1',
            title: 'AI Scan: Sunita Verma (PT-10485)',
            description: 'Low Risk (3%) - Normal oral scan',
            timestamp: 'Today, ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            type: 'low_risk'
        },
        {
            id: 'ACT-2',
            title: 'AI Scan: Amit Patel (PT-10484)',
            description: 'High Risk (91%) - Action Recommended',
            timestamp: 'Yesterday',
            type: 'high_risk'
        },
        {
            id: 'ACT-3',
            title: 'AI Scan: Priya Sharma (PT-10483)',
            description: 'Low Risk (4%) - Normal oral scan',
            timestamp: '2 days ago',
            type: 'low_risk'
        }
    ];

    function initStore() {
        let p = localStorage.getItem(STORAGE_PATIENTS_KEY);
        if (!p || p === '[]' || p === 'null' || p === 'undefined') {
            localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify(DEFAULT_PATIENTS));
        }
        let r = localStorage.getItem(STORAGE_REPORTS_KEY);
        if (!r || r === '[]' || r === 'null' || r === 'undefined') {
            localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(DEFAULT_REPORTS));
        }
        let a = localStorage.getItem(STORAGE_ACTIVITIES_KEY);
        if (!a || a === '[]' || a === 'null' || a === 'undefined') {
            localStorage.setItem(STORAGE_ACTIVITIES_KEY, JSON.stringify(DEFAULT_ACTIVITIES));
        }
    }

    function syncWithBackend() {
        const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
        const backendBase = isLocal ? 'http://localhost:8000' : 'https://oral-ai-backend.onrender.com';

        const localPatients = JSON.parse(localStorage.getItem(STORAGE_PATIENTS_KEY) || '[]');
        const localReports = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || '[]');

        // 1. Upload ALL existing local browser reports to Backend Server so Android gets them!
        localReports.forEach(report => {
            fetch(backendBase + '/api/reports', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(report)
            }).catch(e => {});
        });

        // 2. Fetch all reports from Backend Server
        fetch(backendBase + '/api/reports')
            .then(res => res.json())
            .then(remoteReports => {
                if (Array.isArray(remoteReports) && remoteReports.length > 0) {
                    const currentLocal = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || '[]');
                    const map = new Map();
                    currentLocal.forEach(r => map.set(r.id, r));
                    remoteReports.forEach(r => map.set(r.id, r));
                    localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(Array.from(map.values())));
                }
            }).catch(e => {});

        // 3. Fetch all patients from Backend Server
        fetch(backendBase + '/api/patients')
            .then(res => res.json())
            .then(remotePatients => {
                if (Array.isArray(remotePatients) && remotePatients.length > 0) {
                    const currentPatients = JSON.parse(localStorage.getItem(STORAGE_PATIENTS_KEY) || '[]');
                    const map = new Map();
                    currentPatients.forEach(p => map.set(p.mrn || p.id, p));
                    remotePatients.forEach(p => map.set(p.mrn || p.id, p));
                    localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify(Array.from(map.values())));
                }
            }).catch(e => {});
    }

    initStore();
    syncWithBackend();

    const OralAIDataStore = {
        getPatients: function() {
            initStore();
            syncWithBackend();
            return JSON.parse(localStorage.getItem(STORAGE_PATIENTS_KEY) || JSON.stringify(DEFAULT_PATIENTS));
        },

        getReports: function(patientIdOrMrn) {
            initStore();
            syncWithBackend();
            const reports = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || JSON.stringify(DEFAULT_REPORTS));
            if (!patientIdOrMrn) return reports;

            return reports.filter(r => 
                r.patient_id === patientIdOrMrn || 
                r.mrn === patientIdOrMrn ||
                (r.patient_name && patientIdOrMrn && r.patient_name.toLowerCase() === patientIdOrMrn.toLowerCase())
            );
        },

        getActivities: function() {
            initStore();
            return JSON.parse(localStorage.getItem(STORAGE_ACTIVITIES_KEY) || JSON.stringify(DEFAULT_ACTIVITIES));
        },

        getStats: function() {
            const patients = this.getPatients();
            const reports = this.getReports();

            const uniquePatients = new Set();
            patients.forEach(p => uniquePatients.add(p.mrn || p.id));
            reports.forEach(r => uniquePatients.add(r.mrn || r.patient_id || r.patient_name));

            const totalPatients = Math.max(patients.length, uniquePatients.size);
            const totalScans = reports.length;
            const highRiskCount = reports.filter(r => r.has_cancer || r.risk_level === 'High').length;
            const normalCount = totalScans - highRiskCount;

            return {
                totalPatients,
                totalScans,
                highRiskCount,
                normalCount
            };
        },

        addScanReport: function(patientObj, resultObj) {
            initStore();
            const localPatients = this.getPatients();
            const localReports = this.getReports();
            const localActivities = this.getActivities();

            const mrn = patientObj.mrn || patientObj.id || `PT-${Math.floor(10000 + Math.random() * 90000)}`;
            const name = patientObj.name || patientObj.full_name || 'Anonymous Patient';
            const age = parseInt(patientObj.age) || 35;
            const gender = patientObj.gender || 'Unspecified';

            const isoNow = new Date().toISOString();
            const formattedDate = new Date().toLocaleString();

            let patientId = 'PAT-' + Date.now();
            const pIdx = localPatients.findIndex(p => p.mrn === mrn || (p.full_name && p.full_name.toLowerCase() === name.toLowerCase()));

            if (pIdx >= 0) {
                patientId = localPatients[pIdx].id;
                localPatients[pIdx].updated_at = isoNow;
            } else {
                localPatients.unshift({
                    id: patientId,
                    mrn: mrn,
                    full_name: name,
                    age: age,
                    gender: gender,
                    created_at: isoNow
                });
            }
            localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify(localPatients));

            const newReport = {
                id: 'REP-' + Date.now(),
                patient_id: patientId,
                mrn: mrn,
                patient_name: name,
                scan_image_url: resultObj.image_base64 || resultObj.scan_image_url || '',
                risk_level: resultObj.has_cancer ? 'High' : 'Low',
                risk_percentage: resultObj.risk_percentage || (resultObj.has_cancer ? 92 : 5),
                has_cancer: resultObj.has_cancer,
                message: resultObj.message || (resultObj.has_cancer ? "Cancer Detected!" : "Normal Oral Scan"),
                analysis_date: isoNow
            };

            localReports.unshift(newReport);
            localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(localReports));

            localActivities.unshift({
                id: 'ACT-' + Date.now(),
                title: `AI Scan: ${name} (${mrn})`,
                description: resultObj.has_cancer ? `High Risk (${newReport.risk_percentage}%) - Action Recommended` : `Low Risk (${newReport.risk_percentage}%) - Normal Scan`,
                timestamp: formattedDate,
                type: resultObj.has_cancer ? 'high_risk' : 'low_risk',
                patientName: name,
                mrn: mrn
            });
            localStorage.setItem(STORAGE_ACTIVITIES_KEY, JSON.stringify(localActivities.slice(0, 50)));

            // POST TO SHARED BACKEND SERVER FOR ANDROID SYNC
            const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
            const backendBase = isLocal ? 'http://localhost:8000' : 'https://oral-ai-backend.onrender.com';
            
            fetch(backendBase + '/api/reports', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(newReport)
            }).catch(e => console.warn("Backend report sync skipped:", e));

            return { patientId, report: newReport };
        },

        deletePatient: function(patientIdOrMrn) {
            initStore();
            let localPatients = this.getPatients();
            let localReports = this.getReports();

            localPatients = localPatients.filter(p => p.id !== patientIdOrMrn && p.mrn !== patientIdOrMrn);
            localReports = localReports.filter(r => r.patient_id !== patientIdOrMrn && r.mrn !== patientIdOrMrn);

            localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify(localPatients));
            localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(localReports));

            const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
            const backendBase = isLocal ? 'http://localhost:8000' : 'https://oral-ai-backend.onrender.com';
            fetch(backendBase + '/api/patients/' + patientIdOrMrn, { method: 'DELETE' }).catch(e => {});
        },

        resetStore: function() {
            localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify(DEFAULT_PATIENTS));
            localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(DEFAULT_REPORTS));
            localStorage.setItem(STORAGE_ACTIVITIES_KEY, JSON.stringify(DEFAULT_ACTIVITIES));
        }
    };

    window.OralAIDataStore = OralAIDataStore;
})(window);
