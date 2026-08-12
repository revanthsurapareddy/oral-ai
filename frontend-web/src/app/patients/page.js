"use client";

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { useDataStore, supabaseClient } from '@/context/DataStoreContext';
import { Search, ArrowUpDown, Trash2, LayoutGrid, Table, Eye, ChevronLeft, ChevronRight } from 'lucide-react';
import '@/styles/patients.css';

const avatars = ['👨‍💼', '👩‍💼', '👨‍🔬', '👩‍🔬', '👨‍⚕️', '👩‍⚕️', '👴', '👵'];
const colors = ['#2A81F6', '#00C6FF', '#10B981', '#F59E0B', '#8B5CF6', '#EC4899'];

export default function PatientsPage() {
    const router = useRouter();
    const { deletePatient: deleteLocalPatient, getPatients } = useDataStore();
    const [patientsList, setPatientsList] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [sortAsc, setSortAsc] = useState(true);
    const [loading, setLoading] = useState(true);
    const [viewMode, setViewMode] = useState('table'); // 'table' or 'grid'
    const [currentPage, setCurrentPage] = useState(1);
    const itemsPerPage = 12;

    useEffect(() => {
        async function loadPatients() {
            setLoading(true);
            try {
                // Fetch patients directly from Supabase
                const { data: cloudPatients, error } = await supabaseClient
                    .from('patients')
                    .select('*')
                    .order('created_at', { ascending: false });

                let combinedList = [];
                if (!error && cloudPatients && cloudPatients.length > 0) {
                    combinedList = cloudPatients;
                } else {
                    combinedList = getPatients();
                }

                // Deduplicate by MRN or ID
                const seen = new Set();
                const uniquePatients = [];

                combinedList.forEach(p => {
                    const identifier = (p.mrn || p.id || p.full_name || '').trim().toLowerCase();
                    if (identifier && !seen.has(identifier)) {
                        seen.add(identifier);
                        uniquePatients.push(p);
                    }
                });

                setPatientsList(uniquePatients);
            } catch (err) {
                console.error("Error loading patients:", err);
                setPatientsList(getPatients());
            } finally {
                setLoading(false);
            }
        }
        loadPatients();
    }, []);

    const handleDelete = async (e, patient) => {
        e.stopPropagation();
        const mrnKey = patient.mrn || patient.id;
        if (!confirm(`Are you sure you want to delete patient ${patient.full_name || mrnKey}?`)) return;

        try {
            await deleteLocalPatient(mrnKey);
            try {
                if (patient.id) {
                    await supabaseClient.from('patients').delete().eq('id', patient.id);
                }
                if (patient.mrn) {
                    await supabaseClient.from('patients').delete().eq('mrn', patient.mrn);
                    await supabaseClient.from('reports').delete().eq('mrn', patient.mrn);
                }
            } catch (cloudErr) {
                console.warn("Cloud delete notice:", cloudErr);
            }
            setPatientsList(prev => prev.filter(p => (p.mrn || p.id) !== mrnKey));
        } catch (err) {
            alert("Failed to delete patient. Please try again.");
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

    // Pagination calculations
    const totalPages = Math.ceil(filteredPatients.length / itemsPerPage) || 1;
    const paginatedPatients = filteredPatients.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <main className="content-area" style={{ flex: 1, paddingBottom: '40px' }}>
                <section className="welcome-section" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                    <div>
                        <h1 className="welcome-title">Patients Directory</h1>
                        <p className="welcome-subtitle">Search and manage diagnostic patient records ({filteredPatients.length} total).</p>
                    </div>
                </section>

                <div className="patients-card">
                    <div className="search-filter-container">
                        <div className="search-bar">
                            <Search size={18} />
                            <input 
                                type="text" 
                                placeholder="Search by patient name or MRN..." 
                                value={searchQuery}
                                onChange={(e) => { setSearchQuery(e.target.value); setCurrentPage(1); }}
                            />
                        </div>
                        <button className="filter-btn" onClick={handleSort} title="Sort Alphabetically">
                            <ArrowUpDown size={16} /> Sort Name
                        </button>
                        <button className="filter-btn" onClick={() => setViewMode(viewMode === 'table' ? 'grid' : 'table')} title="Toggle Layout">
                            {viewMode === 'table' ? <LayoutGrid size={16} /> : <Table size={16} />}
                            {viewMode === 'table' ? 'Grid View' : 'Table View'}
                        </button>
                    </div>

                    <div className="patients-divider"></div>

                    {loading ? (
                        <div style={{ color: '#7B8E9F', padding: '40px', textAlign: 'center' }}>
                            Loading patient records...
                        </div>
                    ) : filteredPatients.length === 0 ? (
                        <div style={{ color: '#7B8E9F', padding: '40px', textAlign: 'center' }}>
                            No matching patient records found.
                        </div>
                    ) : viewMode === 'table' ? (
                        /* Desktop Data Table View */
                        <div className="patients-table-wrapper">
                            <table className="patients-table">
                                <thead>
                                    <tr>
                                        <th>Patient Name</th>
                                        <th>MRN / ID</th>
                                        <th>Gender / Age</th>
                                        <th>Status</th>
                                        <th style={{ textAlign: 'right' }}>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {paginatedPatients.map((patient, index) => {
                                        const avatar = avatars[index % avatars.length];
                                        const color = colors[index % colors.length];
                                        const mrnKey = patient.mrn || patient.id;

                                        return (
                                            <tr key={mrnKey} style={{ cursor: 'pointer' }} onClick={() => router.push(`/patients/view?mrn=${mrnKey}`)}>
                                                <td>
                                                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                                        <div className="patient-avatar-box" style={{ backgroundColor: color }}>
                                                            {avatar}
                                                        </div>
                                                        <div style={{ fontWeight: 600, color: '#ffffff' }}>{patient.full_name || 'Patient'}</div>
                                                    </div>
                                                </td>
                                                <td>
                                                    <span style={{ background: '#0b111a', border: '1px solid #2a3b4c', padding: '4px 10px', borderRadius: '6px', fontSize: '13px', color: '#00c6ff' }}>
                                                        {mrnKey}
                                                    </span>
                                                </td>
                                                <td style={{ color: '#a0aec0' }}>
                                                    {patient.gender || 'N/A'} {patient.age ? `(${patient.age} yrs)` : ''}
                                                </td>
                                                <td>
                                                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#10b981', background: 'rgba(16, 185, 129, 0.1)', padding: '4px 10px', borderRadius: '6px', fontSize: '12px', fontWeight: 600 }}>
                                                        <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#10b981' }}></span> Active Log
                                                    </span>
                                                </td>
                                                <td style={{ textAlign: 'right' }}>
                                                    <div style={{ display: 'inline-flex', gap: '8px' }} onClick={(e) => e.stopPropagation()}>
                                                        <button 
                                                            style={{ background: '#0b111a', border: '1px solid #2a3b4c', color: '#00c6ff', padding: '6px 12px', borderRadius: '6px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', fontWeight: 600 }}
                                                            onClick={() => router.push(`/patients/view?mrn=${mrnKey}`)}
                                                        >
                                                            <Eye size={14} /> View
                                                        </button>
                                                        <button 
                                                            style={{ background: '#0b111a', border: '1px solid #2a3b4c', color: '#ff4b4b', padding: '6px 12px', borderRadius: '6px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', fontWeight: 600 }}
                                                            onClick={(e) => handleDelete(e, patient)}
                                                        >
                                                            <Trash2 size={14} /> Delete
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
                    ) : (
                        /* Grid Card View (Max 3-4 per row) */
                        <div className="patients-grid">
                            {paginatedPatients.map((patient, index) => {
                                const avatar = avatars[index % avatars.length];
                                const color = colors[index % colors.length];
                                const mrnKey = patient.mrn || patient.id;

                                return (
                                    <div 
                                        key={mrnKey} 
                                        className="patient-card-item"
                                        onClick={() => router.push(`/patients/view?mrn=${mrnKey}`)}
                                    >
                                        <div className="patient-avatar-box" style={{ backgroundColor: color }}>
                                            {avatar}
                                        </div>
                                        <div style={{ flex: 1 }}>
                                            <div style={{ fontWeight: 600, color: '#ffffff', fontSize: '15px', marginBottom: '2px' }}>{patient.full_name || 'Patient'}</div>
                                            <div style={{ color: '#00c6ff', fontSize: '13px' }}>MRN: {mrnKey}</div>
                                        </div>
                                        <div 
                                            style={{ color: '#ff4b4b', padding: '8px', cursor: 'pointer' }}
                                            onClick={(e) => handleDelete(e, patient)}
                                        >
                                            <Trash2 size={16} />
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}

                    {/* Pagination Footer */}
                    <div className="pagination-container">
                        <div className="pagination-text">
                            Showing {Math.min((currentPage - 1) * itemsPerPage + 1, filteredPatients.length)} - {Math.min(currentPage * itemsPerPage, filteredPatients.length)} of {filteredPatients.length} Patients
                        </div>
                        <div className="pagination-btns">
                            <button 
                                className="page-btn" 
                                disabled={currentPage === 1}
                                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                            >
                                <ChevronLeft size={14} style={{ verticalAlign: 'middle' }} /> Prev
                            </button>
                            <span style={{ display: 'flex', alignItems: 'center', padding: '0 8px', color: '#a0aec0', fontSize: '13px', fontWeight: 600 }}>
                                Page {currentPage} of {totalPages}
                            </span>
                            <button 
                                className="page-btn" 
                                disabled={currentPage === totalPages}
                                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                            >
                                Next <ChevronRight size={14} style={{ verticalAlign: 'middle' }} />
                            </button>
                        </div>
                    </div>
                </div>
            </main>

            <BottomNav />
        </div>
    );
}
