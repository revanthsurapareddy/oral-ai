// OralAI Unified Data Store for Localhost & Supabase & Backend Sync
(function(window) {
    const STORAGE_PATIENTS_KEY = 'oralai_local_patients';
    const STORAGE_REPORTS_KEY = 'oralai_local_reports';
    const STORAGE_ACTIVITIES_KEY = 'oralai_activity_log';

    const DEFAULT_PATIENTS = [];
    const DEFAULT_REPORTS = [];
    const DEFAULT_ACTIVITIES = [];

    function initStore() {
        let p = localStorage.getItem(STORAGE_PATIENTS_KEY);
        if (p && (p.includes('PAT-10482') || p.includes('Rajesh Kumar'))) {
            localStorage.removeItem(STORAGE_PATIENTS_KEY);
            localStorage.removeItem(STORAGE_REPORTS_KEY);
            localStorage.removeItem(STORAGE_ACTIVITIES_KEY);
        }
        
        p = localStorage.getItem(STORAGE_PATIENTS_KEY);
        if (!p || p === 'null' || p === 'undefined') {
            localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify([]));
        }
        let r = localStorage.getItem(STORAGE_REPORTS_KEY);
        if (!r || r === 'null' || r === 'undefined') {
            localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify([]));
        }
        let a = localStorage.getItem(STORAGE_ACTIVITIES_KEY);
        if (!a || a === 'null' || a === 'undefined') {
            localStorage.setItem(STORAGE_ACTIVITIES_KEY, JSON.stringify([]));
        }
    }

    function unpackReportContours(report) {
        if (report && report.message && report.message.includes(" ||CONTOURS||")) {
            const parts = report.message.split(" ||CONTOURS||");
            report.message = parts[0];
            try {
                const cdata = JSON.parse(parts[1]);
                report.inner_lesion_pts = cdata.inner || [];
                report.outer_safety_pts = cdata.outer || [];
            } catch(e){}
        }
        return report;
    }

    function syncWithBackend() {
        const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1' || window.location.hostname.startsWith('192.168.') || window.location.hostname.startsWith('172.');
        const backendBase = isLocal ? 'http://' + window.location.hostname + ':8000' : 'https://49d6b344d4e664.lhr.life';

        const localPatients = JSON.parse(localStorage.getItem(STORAGE_PATIENTS_KEY) || '[]');
        const localReports = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || '[]');

        // 1. Upload unsynced local reports to Backend
        const unsyncedReports = localReports.filter(r => r.synced !== true);
        unsyncedReports.forEach(report => {
            fetch(backendBase + '/api/reports', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(report)
            }).then(res => {
                if (res.ok) {
                    const currentLocal = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || '[]');
                    const idx = currentLocal.findIndex(r => r.id === report.id);
                    if (idx >= 0) {
                        currentLocal[idx].synced = true;
                        localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(currentLocal));
                    }
                }
            }).catch(e => {});
        });

        // 2. Fetch all reports from Backend / Supabase
        fetch(backendBase + '/api/reports')
            .then(res => res.json())
            .then(remoteReports => {
                if (Array.isArray(remoteReports) && remoteReports.length > 0) {
                    remoteReports.forEach(r => {
                        unpackReportContours(r);
                        r.synced = true;
                    });
                    const currentLocal = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || '[]');
                    const map = new Map();
                    currentLocal.forEach(r => map.set(r.id, r));
                    remoteReports.forEach(r => map.set(r.id, r));
                    localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(Array.from(map.values())));
                }
            }).catch(e => {});

        // 3. Fetch all patients from Backend / Supabase
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
            const list = JSON.parse(localStorage.getItem(STORAGE_PATIENTS_KEY) || '[]');
            return list;
        },

        getReports: function(patientIdOrMrn) {
            initStore();
            syncWithBackend();
            const reports = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || '[]');
            
            if (!patientIdOrMrn) return reports;

            return reports.filter(r => 
                r.patient_id === patientIdOrMrn || 
                r.mrn === patientIdOrMrn ||
                (r.patient_name && patientIdOrMrn && r.patient_name.toLowerCase() === patientIdOrMrn.toLowerCase())
            );
        },

        getActivities: function() {
            initStore();
            const acts = JSON.parse(localStorage.getItem(STORAGE_ACTIVITIES_KEY) || '[]');
            return acts.filter(a => !a.title.includes('PT-1048') && !['Rajesh Kumar', 'Priya Sharma', 'Amit Patel', 'Sunita Verma'].some(n => a.title.includes(n)));
        },

        getStats: function() {
            const patients = this.getPatients();
            const reports = this.getReports();

            const uniquePatients = new Set();
            patients.forEach(p => {
                const key = (p.mrn || p.id || p.full_name || '').toString().trim().toLowerCase();
                if (key) uniquePatients.add(key);
            });
            reports.forEach(r => {
                const key = (r.mrn || r.patient_id || r.patient_name || '').toString().trim().toLowerCase();
                if (key) uniquePatients.add(key);
            });

            const totalPatients = uniquePatients.size;
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
                analysis_date: isoNow,
                inner_lesion_pts: resultObj.inner_lesion_pts || [],
                outer_safety_pts: resultObj.outer_safety_pts || [],
                synced: false
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
            const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1' || window.location.hostname.startsWith('192.168.') || window.location.hostname.startsWith('172.');
            const backendBase = isLocal ? 'http://' + window.location.hostname + ':8000' : 'https://49d6b344d4e664.lhr.life';
            
            fetch(backendBase + '/api/reports', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(newReport)
            }).then(res => {
                if (res.ok) {
                    const currentLocal = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || '[]');
                    const idx = currentLocal.findIndex(r => r.id === newReport.id);
                    if (idx >= 0) {
                        currentLocal[idx].synced = true;
                        localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(currentLocal));
                    }
                }
            }).catch(e => console.warn("Backend report sync skipped:", e));

            return { patientId, report: newReport };
        },

        deletePatient: function(patientIdOrMrn) {
            initStore();
            if (!patientIdOrMrn) return;
            const key = String(patientIdOrMrn).trim();

            let localPatients = JSON.parse(localStorage.getItem(STORAGE_PATIENTS_KEY) || '[]');
            let localReports = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || '[]');
            let localActivities = JSON.parse(localStorage.getItem(STORAGE_ACTIVITIES_KEY) || '[]');

            localPatients = localPatients.filter(p => p.id !== key && p.mrn !== key && p.full_name !== key);
            localReports = localReports.filter(r => r.patient_id !== key && r.mrn !== key && r.patient_name !== key);
            localActivities = localActivities.filter(a => !a.title.includes(key));

            localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify(localPatients));
            localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(localReports));
            localStorage.setItem(STORAGE_ACTIVITIES_KEY, JSON.stringify(localActivities));

            if (window.supabase) {
                try {
                    const SUPABASE_URL = 'https://gduqgsxwcnrzdjqkextl.supabase.co';
                    const SUPABASE_ANON_KEY = 'sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo';
                    const client = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
                    client.from('patients').delete().or('id.eq.' + key + ',mrn.eq.' + key).then(() => {});
                    client.from('reports').delete().or('patient_id.eq.' + key + ',mrn.eq.' + key).then(() => {});
                } catch(e){}
            }

            const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
            const backendBase = isLocal ? 'http://' + window.location.hostname + ':8000' : 'https://oral-ai-backend.onrender.com';
            fetch(backendBase + '/api/patients/' + encodeURIComponent(key), { method: 'DELETE' }).catch(e => {});
        },

        resetStore: function() {
            localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify(DEFAULT_PATIENTS));
            localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(DEFAULT_REPORTS));
            localStorage.setItem(STORAGE_ACTIVITIES_KEY, JSON.stringify(DEFAULT_ACTIVITIES));
        }
    };

    window.OralAIDataStore = OralAIDataStore;
})(window);
