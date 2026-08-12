"use client";

import React, { useEffect, useState } from 'react';
import { useDataStore, supabaseClient } from '@/context/DataStoreContext';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { Users as UsersIcon, Activity, CheckCircle, AlertTriangle, Clock } from 'lucide-react';
import '@/styles/dashboard.css';

const formatDate = (dateString) => {
    if (!dateString) return 'Recently';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
};

export default function Dashboard() {
    const { stats, activities } = useDataStore();
    const [welcomeName, setWelcomeName] = useState('Doctor');
    const [livePatientCount, setLivePatientCount] = useState(null);
    const [liveScanCount, setLiveScanCount] = useState(null);
    const [liveActivities, setLiveActivities] = useState([]);
    const [loadingLive, setLoadingLive] = useState(true);

    useEffect(() => {
        async function fetchDashboardStats() {
            setLoadingLive(true);
            try {
                let doctorName = localStorage.getItem('oralai_user_name');
                try {
                    const { data: { session } } = await supabaseClient.auth.getSession();
                    if (session && session.user) {
                        doctorName = session.user.user_metadata?.full_name || session.user.email?.split('@')[0] || doctorName;
                    }
                } catch(e){}
                
                if (!doctorName || doctorName.trim() === '') {
                    doctorName = 'Doctor';
                }
                const displayName = doctorName.toLowerCase().startsWith('dr') ? doctorName : `Dr. ${doctorName}`;
                setWelcomeName(displayName);

                // Fetch live counts directly from Supabase (Patients table + Reports table union)
                try {
                    const { count: pCount } = await supabaseClient
                        .from('patients')
                        .select('*', { count: 'exact', head: true });

                    const { data: patMrns } = await supabaseClient
                        .from('patients')
                        .select('mrn, id, full_name');

                    const { data: repMrns } = await supabaseClient
                        .from('reports')
                        .select('mrn, patient_id, patient_name');

                    const uniquePatientsSet = new Set();
                    if (patMrns) patMrns.forEach(p => { const k = (p.mrn || p.id || p.full_name || '').trim().toLowerCase(); if (k) uniquePatientsSet.add(k); });
                    if (repMrns) repMrns.forEach(r => { const k = (r.mrn || r.patient_id || r.patient_name || '').trim().toLowerCase(); if (k) uniquePatientsSet.add(k); });

                    const totalUniquePatients = Math.max(pCount || 0, uniquePatientsSet.size);
                    setLivePatientCount(totalUniquePatients);

                    const { count: rCount } = await supabaseClient.from('reports').select('*', { count: 'exact', head: true });
                    if (rCount !== null && rCount !== undefined) {
                        setLiveScanCount(rCount);
                    }

                    // Fetch latest 20 reports from Supabase for activity feed
                    const { data: recentReps } = await supabaseClient
                        .from('reports')
                        .select('id, mrn, patient_name, risk_level, risk_percentage, has_cancer, message, analysis_date')
                        .order('analysis_date', { ascending: false })
                        .limit(20);
                    if (recentReps) {
                        setLiveActivities(recentReps);
                    }
                } catch(e){
                    console.warn('Supabase fetch error:', e);
                }
            } finally {
                setLoadingLive(false);
            }
        }
        fetchDashboardStats();
    }, []);

    const displayPatients = (livePatientCount !== null && livePatientCount > 0) ? livePatientCount : stats.totalPatients;
    const displayScans = (liveScanCount !== null && liveScanCount > 0) ? liveScanCount : stats.totalScans;

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <main className="content-area" style={{ flex: 1, paddingBottom: '90px' }}>
                <section className="welcome-section">
                    <h1 className="welcome-title">Welcome back, {welcomeName}</h1>
                    <p className="welcome-subtitle">Here's your clinical overview for today.</p>
                </section>

                <div className="stats-grid">
                    <div className="stat-card">
                        <div className="stat-icon-wrapper" style={{ background: 'rgba(0, 198, 255, 0.1)', color: '#00c6ff' }}>
                            <UsersIcon size={24} />
                        </div>
                        <div className="stat-value">{displayPatients.toLocaleString()}</div>
                        <div className="stat-label">Total Patients</div>
                        <div className="stat-sublabel">Synced with Supabase</div>
                    </div>

                    <div className="stat-card">
                        <div className="stat-icon-wrapper" style={{ background: 'rgba(0, 230, 118, 0.1)', color: '#00e676' }}>
                            <Activity size={24} />
                        </div>
                        <div className="stat-value">{displayScans.toLocaleString()}</div>
                        <div className="stat-label">Scans Diagnosed</div>
                        <div className="stat-sublabel">U-Net AI & Expert Matches</div>
                    </div>

                    <div className="stat-card">
                        <div className="stat-icon-wrapper" style={{ background: 'rgba(255, 193, 7, 0.1)', color: '#ffc107' }}>
                            <Activity size={24} />
                        </div>
                        <div className="stat-value">96.5%</div>
                        <div className="stat-label">Detection Accuracy</div>
                        <div className="stat-sublabel">YOLOv8 Model</div>
                    </div>
                </div>

                <div style={{ background: '#151e2b', borderRadius: '12px', padding: '20px', border: '1px solid #1f2c3b', marginTop: '25px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
                        <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#ffffff', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <Clock size={18} /> Recent Activity Logs
                        </h3>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {liveActivities.length > 0 ? (
                            liveActivities.map(rep => {
                                const isHigh = rep.has_cancer || rep.risk_level === 'High';
                                const dateStr = formatDate(rep.analysis_date);
                                return (
                                    <div key={rep.id} style={{ display: 'flex', alignItems: 'center', padding: '10px 12px', background: '#0b111a', borderRadius: '8px', fontSize: '13px', justifyContent: 'space-between' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                            {isHigh ? (
                                                <AlertTriangle size={18} style={{ color: '#ff4b4b' }} />
                                            ) : (
                                                <CheckCircle size={18} style={{ color: '#10b981' }} />
                                            )}
                                            <div>
                                                <div style={{ color: '#ffffff', fontWeight: 600 }}>AI Scan: {rep.patient_name || 'Patient'} (MRN: {rep.mrn || '?'})</div>
                                                <div style={{ color: '#7b8e9f', fontSize: '12px' }}>
                                                    {isHigh ? `High Risk (${rep.risk_percentage || '?' }%) - Action Recommended` : `Low Risk (${rep.risk_percentage || '?' }%) - Normal Scan`}
                                                </div>
                                            </div>
                                        </div>
                                        <div style={{ color: '#526377', fontSize: '11px', flexShrink: 0, marginLeft: '10px' }}>{dateStr}</div>
                                    </div>
                                );
                            })
                        ) : activities.length > 0 ? (
                            activities.slice(0, 10).map(act => (
                                <div key={act.id} style={{ display: 'flex', alignItems: 'center', padding: '10px 12px', background: '#0b111a', borderRadius: '8px', fontSize: '13px', justifyContent: 'space-between' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                        {act.type === 'high_risk' ? (
                                            <AlertTriangle size={18} style={{ color: '#ff4b4b' }} />
                                        ) : (
                                            <CheckCircle size={18} style={{ color: '#10b981' }} />
                                        )}
                                        <div>
                                            <div style={{ color: '#ffffff', fontWeight: 600 }}>{act.title}</div>
                                            <div style={{ color: '#7b8e9f', fontSize: '12px' }}>{act.description}</div>
                                        </div>
                                    </div>
                                    <div style={{ color: '#526377', fontSize: '11px', flexShrink: 0, marginLeft: '10px' }}>{formatDate(act.timestamp)}</div>
                                </div>
                            ))
                        ) : (
                            <div style={{ color: '#7b8e9f', fontSize: '13px', textAlign: 'center', padding: '10px' }}>
                                {loadingLive ? "Loading recent activities..." : "No recent activities. Start an AI scan!"}
                            </div>
                        )}
                    </div>
                </div>
            </main>

            <BottomNav />
        </div>
    );
}
