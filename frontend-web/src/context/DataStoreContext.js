"use client";

import React, { createContext, useContext, useState, useEffect } from 'react';
import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = 'https://gduqgsxwcnrzdjqkextl.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo';

export const supabaseClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const STORAGE_PATIENTS_KEY = 'oralai_local_patients';
const STORAGE_REPORTS_KEY = 'oralai_local_reports';
const STORAGE_ACTIVITIES_KEY = 'oralai_activity_log';

const DataStoreContext = createContext(null);

export function DataStoreProvider({ children }) {
    const [patients, setPatients] = useState([]);
    const [reports, setReports] = useState([]);
    const [activities, setActivities] = useState([]);
    const [stats, setStats] = useState({ totalPatients: 0, totalScans: 0, highRiskCount: 0, normalCount: 0 });
    const [isInitialized, setIsInitialized] = useState(false);

    // Initialize Store
    useEffect(() => {
        if (typeof window === 'undefined') return;

        let p = localStorage.getItem(STORAGE_PATIENTS_KEY);
        if (p && (p.includes('PAT-10482') || p.includes('Rajesh Kumar'))) {
            localStorage.removeItem(STORAGE_PATIENTS_KEY);
            localStorage.removeItem(STORAGE_REPORTS_KEY);
            localStorage.removeItem(STORAGE_ACTIVITIES_KEY);
        }

        if (!localStorage.getItem(STORAGE_PATIENTS_KEY)) {
            localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify([]));
        }
        if (!localStorage.getItem(STORAGE_REPORTS_KEY)) {
            localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify([]));
        }
        if (!localStorage.getItem(STORAGE_ACTIVITIES_KEY)) {
            localStorage.setItem(STORAGE_ACTIVITIES_KEY, JSON.stringify([]));
        }

        const localPatients = JSON.parse(localStorage.getItem(STORAGE_PATIENTS_KEY) || '[]');
        const localReports = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || '[]');
        const localActivities = JSON.parse(localStorage.getItem(STORAGE_ACTIVITIES_KEY) || '[]');

        setPatients(localPatients);
        setReports(localReports);
        setActivities(localActivities);
        setIsInitialized(true);
    }, []);

    // Recalculate stats whenever patients or reports change
    useEffect(() => {
        if (!isInitialized) return;

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

        setStats({ totalPatients, totalScans, highRiskCount, normalCount });
    }, [patients, reports, isInitialized]);

    function getBackendBase() {
        if (typeof window !== 'undefined') {
            const hostname = window.location.hostname || 'localhost';
            if (hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '0.0.0.0' || hostname.startsWith('192.168.') || hostname.startsWith('172.') || hostname.startsWith('10.')) {
                return `http://${hostname}:8000`;
            }
        }
        return 'http://localhost:8000';
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
        if (typeof window === 'undefined') return;
        const backendBase = getBackendBase();

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
            }).catch(() => {});
        });

        // 2. Fetch all reports from Backend
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
                    const merged = Array.from(map.values());
                    localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(merged));
                    setReports(merged);
                }
            }).catch(() => {});

        // 3. Fetch all patients from Backend
        fetch(backendBase + '/api/patients')
            .then(res => res.json())
            .then(remotePatients => {
                if (Array.isArray(remotePatients) && remotePatients.length > 0) {
                    const currentPatients = JSON.parse(localStorage.getItem(STORAGE_PATIENTS_KEY) || '[]');
                    const map = new Map();
                    currentPatients.forEach(p => map.set(p.mrn || p.id, p));
                    remotePatients.forEach(p => map.set(p.mrn || p.id, p));
                    const merged = Array.from(map.values());
                    localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify(merged));
                    setPatients(merged);
                }
            }).catch(() => {});

        // 4. Direct Supabase sync fallback
        const deletedBlacklist = new Set(JSON.parse(localStorage.getItem('oralai_deleted_blacklist') || '[]'));

        supabaseClient.from('patients').select('id, mrn, full_name, age, gender, created_at').order('created_at', { ascending: false }).range(0, 2000).then(({ data }) => {
            if (data && data.length > 0) {
                const currentPatients = JSON.parse(localStorage.getItem(STORAGE_PATIENTS_KEY) || '[]');
                const map = new Map();
                currentPatients.forEach(p => {
                    const k = (p.mrn || p.id || p.full_name || '').toString().toLowerCase();
                    if (k && !deletedBlacklist.has(k)) map.set(k, p);
                });
                data.forEach(p => {
                    const k = (p.mrn || p.id || p.full_name || '').toString().toLowerCase();
                    if (k && !deletedBlacklist.has(k)) map.set(k, p);
                });
                const merged = Array.from(map.values());
                localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify(merged));
                setPatients(merged);
            }
        }).catch((e) => console.warn("Supabase patients sync error:", e));

        supabaseClient.from('reports').select('id, patient_id, mrn, patient_name, age, gender, risk_level, risk_percentage, has_cancer, message, analysis_date').order('analysis_date', { ascending: false }).range(0, 2000).then(({ data }) => {
            if (data && data.length > 0) {
                data.forEach(r => {
                    unpackReportContours(r);
                    r.synced = true;
                });
                const currentLocal = JSON.parse(localStorage.getItem(STORAGE_REPORTS_KEY) || '[]');
                const map = new Map();
                currentLocal.forEach(r => {
                    const k = (r.id || '').toString().toLowerCase();
                    const pKey = (r.mrn || r.patient_id || r.patient_name || '').toString().toLowerCase();
                    if (k && !deletedBlacklist.has(k) && !deletedBlacklist.has(pKey)) map.set(k, r);
                });
                data.forEach(r => {
                    const k = (r.id || '').toString().toLowerCase();
                    const pKey = (r.mrn || r.patient_id || r.patient_name || '').toString().toLowerCase();
                    if (k && !deletedBlacklist.has(k) && !deletedBlacklist.has(pKey)) map.set(k, r);
                });
                const merged = Array.from(map.values());
                localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(merged));
                setReports(merged);
            }
        }).catch((e) => console.warn("Supabase reports sync error:", e));
    }

    // Run sync on initialization
    useEffect(() => {
        if (isInitialized) {
            syncWithBackend();
        }
    }, [isInitialized]);

    const addScanReport = async (patientObj, resultObj) => {
        const mrn = patientObj.mrn || patientObj.id || `PT-${Math.floor(10000 + Math.random() * 90000)}`;
        const name = patientObj.name || patientObj.full_name || 'Anonymous Patient';
        const age = parseInt(patientObj.age) || 35;
        const gender = patientObj.gender || 'Unspecified';

        const isoNow = new Date().toISOString();
        const formattedDate = new Date().toLocaleString();

        let patientId = 'PAT-' + Date.now();
        const localPatients = [...patients];
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
        setPatients(localPatients);

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

        const localReports = [newReport, ...reports];
        localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(localReports));
        setReports(localReports);

        const localActivities = [
            {
                id: 'ACT-' + Date.now(),
                title: `AI Scan: ${name} (${mrn})`,
                description: resultObj.has_cancer ? `High Risk (${newReport.risk_percentage}%) - Action Recommended` : `Low Risk (${newReport.risk_percentage}%) - Normal Scan`,
                timestamp: formattedDate,
                type: resultObj.has_cancer ? 'high_risk' : 'low_risk',
                patientName: name,
                mrn: mrn
            },
            ...activities
        ].filter(a => !a.title.includes('PT-1048') && !['Rajesh Kumar', 'Priya Sharma', 'Amit Patel', 'Sunita Verma'].some(n => a.title.includes(n)));

        localStorage.setItem(STORAGE_ACTIVITIES_KEY, JSON.stringify(localActivities.slice(0, 50)));
        setActivities(localActivities.slice(0, 50));

        // POST TO SHARED BACKEND SERVER FOR ANDROID SYNC
        const backendBase = getBackendBase();
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
    };

    const deletePatient = async (patientIdOrMrn) => {
        if (!patientIdOrMrn) return;
        const key = String(patientIdOrMrn).trim();
        const keyLower = key.toLowerCase();

        const deletedList = JSON.parse(localStorage.getItem('oralai_deleted_blacklist') || '[]');
        if (!deletedList.includes(keyLower)) {
            deletedList.push(keyLower);
            localStorage.setItem('oralai_deleted_blacklist', JSON.stringify(deletedList));
        }

        const updatedPatients = patients.filter(p => (p.id || '').toLowerCase() !== keyLower && (p.mrn || '').toLowerCase() !== keyLower && (p.full_name || '').toLowerCase() !== keyLower);
        const updatedReports = reports.filter(r => (r.patient_id || '').toLowerCase() !== keyLower && (r.mrn || '').toLowerCase() !== keyLower && (r.patient_name || '').toLowerCase() !== keyLower && (r.id || '').toLowerCase() !== keyLower);
        const updatedActivities = activities.filter(a => !a.title.toLowerCase().includes(keyLower));

        localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify(updatedPatients));
        localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify(updatedReports));
        localStorage.setItem(STORAGE_ACTIVITIES_KEY, JSON.stringify(updatedActivities));

        setPatients(updatedPatients);
        setReports(updatedReports);
        setActivities(updatedActivities);

        // Direct Supabase table deletion so patient & reports NEVER recur!
        try {
            await supabaseClient.from('patients').delete().or(`id.eq.${key},mrn.eq.${key},full_name.eq.${key}`);
            await supabaseClient.from('reports').delete().or(`patient_id.eq.${key},mrn.eq.${key},patient_name.eq.${key},id.eq.${key}`);
        } catch (err) {
            console.warn("Supabase patient delete error:", err);
        }

        const backendBase = getBackendBase();
        fetch(backendBase + '/api/patients/' + encodeURIComponent(key), { method: 'DELETE' }).catch(() => {});
    };

    const resetStore = () => {
        localStorage.setItem(STORAGE_PATIENTS_KEY, JSON.stringify([]));
        localStorage.setItem(STORAGE_REPORTS_KEY, JSON.stringify([]));
        localStorage.setItem(STORAGE_ACTIVITIES_KEY, JSON.stringify([]));

        setPatients([]);
        setReports([]);
        setActivities([]);
    };

    const getPatients = () => {
        return patients;
    };

    const getReports = (patientIdOrMrn) => {
        if (!patientIdOrMrn) return reports;
        return reports.filter(r => 
            r.patient_id === patientIdOrMrn || 
            r.mrn === patientIdOrMrn ||
            (r.patient_name && patientIdOrMrn && r.patient_name.toLowerCase() === patientIdOrMrn.toLowerCase())
        );
    };

    const value = {
        patients,
        reports,
        getPatients,
        getReports,
        activities: activities.filter(a => !a.title.includes('PT-1048') && !['Rajesh Kumar', 'Priya Sharma', 'Amit Patel', 'Sunita Verma'].some(n => a.title.includes(n))),
        stats,
        syncWithBackend,
        addScanReport,
        deletePatient,
        resetStore,
        getBackendBase
    };

    return (
        <DataStoreContext.Provider value={value}>
            {children}
        </DataStoreContext.Provider>
    );
}

export function useDataStore() {
    const context = useContext(DataStoreContext);
    if (!context) {
        throw new Error('useDataStore must be used within a DataStoreProvider');
    }
    return context;
}
