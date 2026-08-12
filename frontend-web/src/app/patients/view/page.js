"use client";

import React, { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { useDataStore, supabaseClient } from '@/context/DataStoreContext';
import { FileText, ArrowLeft } from 'lucide-react';
import '@/styles/patients.css';

function PatientReportsContent() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const mrn = searchParams.get('mrn');
    const { getReports, getPatients } = useDataStore();

    const [patientName, setPatientName] = useState('Patient');
    const [reportsList, setReportsList] = useState([]);
    const [loading, setLoading] = useState(true);

    const loadReports = async () => {
        setLoading(true);
        if (!mrn) {
            setLoading(false);
            return;
        }

        let name = "Patient";
        const localPatients = (typeof getPatients === 'function' ? getPatients() : []) || [];
        const foundLocalPat = localPatients.find(p => p.id === mrn || p.mrn === mrn);
        if (foundLocalPat) {
            name = foundLocalPat.full_name;
        }

        try {
            // Check Supabase patient metadata by id, mrn, or full_name
            const { data: patientData } = await supabaseClient
                .from('patients')
                .select('full_name, mrn, id')
                .or(`id.eq.${mrn},mrn.eq.${mrn},full_name.eq.${mrn}`);
            if (patientData && patientData.length > 0) {
                name = patientData[0].full_name || name;
            }
        } catch (e) {
            console.warn(e);
        }
        setPatientName(name);

        const matchingLocalReports = (typeof getReports === 'function' ? getReports(mrn) : []) || [];
        let sbReports = [];

        try {
            const filter = `patient_id.eq.${mrn},mrn.eq.${mrn},patient_name.eq.${mrn},id.eq.${mrn}`;
            const { data: reports } = await supabaseClient
                .from('reports')
                .select('*')
                .or(filter)
                .order('analysis_date', { ascending: false });
            if (reports) sbReports = reports;
        } catch (err) {
            console.warn("Supabase reports query error:", err);
        } finally {
            // Combine local & remote reports
            const reportMap = new Map();
            matchingLocalReports.forEach(r => reportMap.set(r.id, r));
            sbReports.forEach(r => reportMap.set(r.id, r));

            setReportsList(Array.from(reportMap.values()));
            setLoading(false);
        }
    };

    useEffect(() => {
        loadReports();
    }, [mrn]);

    const handleReportClick = (report) => {
        sessionStorage.setItem('currentViewReport', JSON.stringify(report));
        router.push(`/reports/view?id=${report.id}`);
    };

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <div style={{ display: 'flex', alignItems: 'center', padding: '20px', fontSize: '20px', fontWeight: '600', borderBottom: '1px solid #1f2c3b', color: '#ffffff' }}>
                <ArrowLeft size={24} style={{ marginRight: '15px', cursor: 'pointer' }} onClick={() => router.push('/patients')} />
                <span>{patientName}'s Reports</span>
            </div>

            <main className="content-area" style={{ flex: 1, paddingBottom: '90px', paddingLeft: '20px', paddingRight: '20px', marginTop: '20px' }}>
                <div style={{ fontSize: '18px', fontWeight: '600', marginBottom: '20px', color: '#ffffff' }}>Saved Reports</div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                    {loading ? (
                        <div style={{ color: '#7b8e9f', textAlign: 'center', padding: '20px' }}>
                            Loading reports...
                        </div>
                    ) : reportsList.length === 0 ? (
                        <div style={{ color: '#7b8e9f', textAlign: 'center', padding: '20px' }}>
                            No reports saved for this patient.
                        </div>
                    ) : (
                        reportsList.map(report => {
                            const dateObj = new Date(report.analysis_date || Date.now());
                            const formattedDate = dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
                            
                            const shortId = (report.id || '').substring(0, 8);
                            const isHigh = report.has_cancer || report.risk_level === 'High';
                            const statusColorVal = isHigh ? '#ff4b4b' : '#10b981';

                            return (
                                <div 
                                    key={report.id} 
                                    style={{ 
                                        display: 'flex', 
                                        alignItems: 'center', 
                                        backgroundColor: '#151e2b', 
                                        borderRadius: '12px', 
                                        padding: '16px', 
                                        cursor: 'pointer',
                                        transition: 'background-color 0.2s' 
                                    }}
                                    onClick={() => handleReportClick(report)}
                                    className="patient-item-hover"
                                >
                                    <div 
                                        style={{ 
                                            width: '50px', 
                                            height: '50px', 
                                            borderRadius: '50%', 
                                            backgroundColor: 'rgba(2, 132, 199, 0.1)', 
                                            color: '#0284c7', 
                                            display: 'flex', 
                                            justifyContent: 'center', 
                                            alignItems: 'center', 
                                            marginRight: '15px' 
                                        }}
                                    >
                                        <FileText size={22} />
                                    </div>
                                    <div style={{ flex: 1 }}>
                                        <div style={{ fontSize: '16px', fontWeight: '600', marginBottom: '4px', color: '#ffffff' }}>{formattedDate}</div>
                                        <div style={{ fontSize: '13px', color: '#7b8e9f' }}>{shortId} ({report.risk_level || 'Scanned'})</div>
                                    </div>
                                    <div 
                                        style={{ 
                                            width: '8px', 
                                            height: '8px', 
                                            borderRadius: '50%', 
                                            backgroundColor: statusColorVal,
                                            marginLeft: '10px' 
                                        }}
                                    ></div>
                                </div>
                            );
                        })
                    )}
                </div>
            </main>

            <BottomNav />
        </div>
    );
}

export default function PatientReportsPage() {
    return (
        <Suspense fallback={<div style={{ backgroundColor: '#0b111a', minHeight: '100vh', color: '#00c6ff' }}>Loading...</div>}>
            <PatientReportsContent />
        </Suspense>
    );
}
