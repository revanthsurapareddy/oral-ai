"use client";

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { useDataStore, supabaseClient } from '@/context/DataStoreContext';
import { Search, ArrowUpDown, Trash2 } from 'lucide-react';
import '@/styles/patients.css';

export default function PatientsPage() {
    const router = useRouter();
    const { deletePatient: deleteLocalPatient, getPatients } = useDataStore();
    const [patientsList, setPatientsList] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [sortAsc, setSortAsc] = useState(true);
    const [loading, setLoading] = useState(true);

    const avatars = ['🧑🏼‍🦲', '🧑🏽', '👩🏻‍🦰', '🧔🏾‍♂️', '👱🏼‍♀️'];
    const colors = ['#ffcdd2', '#e0f2f1', '#fff9c4', '#e1bee7', '#bbdefb'];
    const statusColors = ['status-green', 'status-yellow', 'status-red'];

    const loadPatients = async () => {
        setLoading(true);
        let sbPatients = [];
        let reportPatients = [];

        try {
            const { data: patients } = await supabaseClient
                .from('patients')
                .select('id, mrn, full_name, age, gender, created_at')
                .order('created_at', { ascending: false })
                .range(0, 2000);
            if (patients) {
                sbPatients = patients;
            }

            const { data: reps } = await supabaseClient
                .from('reports')
                .select('mrn, patient_name, age, gender')
                .order('analysis_date', { ascending: false })
                .range(0, 2000);
            if (reps) {
                const seenMrns = new Set();
                reps.forEach(r => {
                    if (r.mrn && !seenMrns.has(r.mrn)) {
                        seenMrns.add(r.mrn);
                        reportPatients.push({
                            id: r.mrn,
                            mrn: r.mrn,
                            full_name: r.patient_name || 'Patient',
                            age: r.age || 30,
                            gender: r.gender || 'Unspecified'
                        });
                    }
                });
            }
        } catch (err) {
            console.warn("Supabase fetch error:", err);
        } finally {
            const deletedBlacklist = new Set(JSON.parse(localStorage.getItem('oralai_deleted_blacklist') || '[]'));
            const localPatients = (typeof getPatients === 'function' ? getPatients() : []) || [];
            const combinedMap = new Map();

            const isAllowed = (p) => {
                const idKey = (p.id || '').toString().toLowerCase();
                const mrnKey = (p.mrn || '').toString().toLowerCase();
                const nameKey = (p.full_name || '').toString().toLowerCase();
                return (idKey === '' || !deletedBlacklist.has(idKey)) &&
                       (mrnKey === '' || !deletedBlacklist.has(mrnKey)) &&
                       (nameKey === '' || !deletedBlacklist.has(nameKey));
            };

            localPatients.forEach(p => { if (isAllowed(p)) combinedMap.set((p.mrn || p.id).toString().toLowerCase(), p); });
            reportPatients.forEach(p => { if (isAllowed(p)) combinedMap.set((p.mrn || p.id).toString().toLowerCase(), p); });
            sbPatients.forEach(p => { if (isAllowed(p)) combinedMap.set((p.mrn || p.id).toString().toLowerCase(), p); });

            setPatientsList(Array.from(combinedMap.values()));
            setLoading(false);
        }
    };

    useEffect(() => {
        loadPatients();
    }, []);

    const handleDelete = async (e, patientObj) => {
        e.stopPropagation();
        if (confirm("Are you sure you want to permanently delete this patient and all their historical reports?")) {
            try {
                const pId = typeof patientObj === 'object' ? (patientObj.id || patientObj.mrn) : patientObj;
                const pMrn = typeof patientObj === 'object' ? patientObj.mrn : patientObj;
                const pName = typeof patientObj === 'object' ? patientObj.full_name : '';

                // Instantly blacklist all identifiers locally
                const blacklist = JSON.parse(localStorage.getItem('oralai_deleted_blacklist') || '[]');
                [pId, pMrn, pName].forEach(k => {
                    if (k) {
                        const kl = String(k).trim().toLowerCase();
                        if (kl && !blacklist.includes(kl)) blacklist.push(kl);
                    }
                });
                localStorage.setItem('oralai_deleted_blacklist', JSON.stringify(blacklist));

                setPatientsList(prev => prev.filter(p => {
                    const idL = (p.id || '').toString().toLowerCase();
                    const mrnL = (p.mrn || '').toString().toLowerCase();
                    const nameL = (p.full_name || '').toString().toLowerCase();
                    const targetIdL = String(pId || '').toLowerCase();
                    const targetMrnL = String(pMrn || '').toLowerCase();
                    const targetNameL = String(pName || '').toLowerCase();
                    return idL !== targetIdL && mrnL !== targetMrnL && (targetNameL ? nameL !== targetNameL : true);
                }));

                if (typeof deleteLocalPatient === 'function') {
                    if (pId) deleteLocalPatient(pId);
                    if (pMrn && pMrn !== pId) deleteLocalPatient(pMrn);
                    if (pName) deleteLocalPatient(pName);
                }

                try {
                    if (pId) await supabaseClient.from('patients').delete().or(`id.eq.${pId},mrn.eq.${pId}`);
                    if (pMrn && pMrn !== pId) await supabaseClient.from('patients').delete().or(`id.eq.${pMrn},mrn.eq.${pMrn}`);
                    if (pName) await supabaseClient.from('patients').delete().eq('full_name', pName);

                    if (pId) await supabaseClient.from('reports').delete().or(`patient_id.eq.${pId},mrn.eq.${pId},id.eq.${pId}`);
                    if (pMrn && pMrn !== pId) await supabaseClient.from('reports').delete().or(`patient_id.eq.${pMrn},mrn.eq.${pMrn}`);
                    if (pName) await supabaseClient.from('reports').delete().eq('patient_name', pName);
                } catch (e) {
                    console.warn("Supabase remote delete skipped:", e);
                }
            } catch (err) {
                console.error("Error deleting patient:", err);
                alert("Failed to delete patient.");
            }
        }
    };

    const handleSort = () => {
        const nextSort = !sortAsc;
        setSortAsc(nextSort);
        const sorted = [...patientsList].sort((a, b) => {
            const nameA = (a.full_name || '').toLowerCase();
            const nameB = (b.full_name || '').toLowerCase();
            return nextSort ? nameA.localeCompare(nameB) : nameB.localeCompare(nameA);
        });
        setPatientsList(sorted);
    };

    const filteredPatients = patientsList.filter(p => 
        (p.full_name && p.full_name.toLowerCase().includes(searchQuery.toLowerCase())) ||
        ((p.mrn || p.id) && (p.mrn || p.id).toLowerCase().includes(searchQuery.toLowerCase()))
    );

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <main className="content-area" style={{ flex: 1, paddingBottom: '90px' }}>
                <section className="welcome-section">
                    <h1 className="welcome-title">Patients Directory</h1>
                    <p className="welcome-subtitle">Search and manage diagnostic patient history logs.</p>
                </section>

                <div className="patients-card">
                    <div className="search-filter-container">
                        <div className="search-bar">
                            <Search size={18} />
                            <input 
                                type="text" 
                                placeholder="Search by name or MRN..." 
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                            />
                        </div>
                        <button className="filter-btn" onClick={handleSort} title="Sort Alphabetically">
                            <ArrowUpDown size={18} />
                        </button>
                    </div>

                    <div className="patients-divider"></div>

                    <div className="patients-list">
                        {loading ? (
                            <div style={{ textAlignment: 'center', color: '#7B8E9F', padding: '20px', textAlign: 'center' }}>
                                Loading patients...
                            </div>
                        ) : filteredPatients.length === 0 ? (
                            <div style={{ color: '#7B8E9F', padding: '20px', textAlign: 'center' }}>
                                No matching patients found.
                            </div>
                        ) : (
                            filteredPatients.map((patient, index) => {
                                const avatar = avatars[index % avatars.length];
                                const color = colors[index % colors.length];
                                const statusClass = statusColors[index % statusColors.length];
                                const mrnKey = patient.mrn || patient.id;

                                return (
                                    <div 
                                        key={mrnKey} 
                                        className="patient-item" 
                                        style={{ cursor: 'pointer' }}
                                        onClick={() => router.push(`/patients/view?mrn=${mrnKey}`)}
                                    >
                                        <div className="patient-avatar" style={{ backgroundColor: color }}>
                                            {avatar}
                                        </div>
                                        <div className="patient-info" style={{ flex: 1 }}>
                                            <div className="patient-name">{patient.full_name || 'Patient'}</div>
                                            <div className="patient-mrn">MRN: {mrnKey}</div>
                                        </div>
                                        <div className={`status-dot ${statusClass}`} style={{ marginRight: '15px' }}></div>
                                        <div 
                                            className="delete-patient-btn" 
                                            style={{ color: '#ff4b4b', padding: '10px', borderRadius: '8px', cursor: 'pointer' }}
                                            onClick={(e) => handleDelete(e, patient)}
                                        >
                                            <Trash2 size={16} />
                                        </div>
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>
            </main>

            <BottomNav />
        </div>
    );
}
