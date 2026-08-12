"use client";

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { useDataStore, supabaseClient } from '@/context/DataStoreContext';
import { BarChart2, Activity, Shield, AlertTriangle, CheckCircle, ArrowLeft, Scan, Users } from 'lucide-react';
import '@/styles/profile.css';

export default function ProfilePage() {
    const router = useRouter();
    const { stats, activities, resetStore } = useDataStore();

    const [userName, setUserName] = useState('Doctor');
    const [userEmail, setUserEmail] = useState('doctor@hospital.com');
    const [profileActivities, setProfileActivities] = useState([]);

    useEffect(() => {
        async function fetchProfileData() {
            try {
                let doctorName = localStorage.getItem('oralai_user_name');
                let doctorEmail = localStorage.getItem('oralai_user_email');
                
                const { data: { session } } = await supabaseClient.auth.getSession();
                if (session && session.user) {
                    doctorName = session.user.user_metadata?.full_name || session.user.email?.split('@')[0] || doctorName;
                    doctorEmail = session.user.email || doctorEmail;
                }
                
                if (doctorName) {
                    const displayName = doctorName.toLowerCase().startsWith('dr') ? doctorName : `Dr. ${doctorName}`;
                    setUserName(displayName);
                }
                if (doctorEmail) {
                    setUserEmail(doctorEmail);
                }
            } catch (error) {
                console.error("Error fetching user profile:", error);
            }
        }
        fetchProfileData();

        // Load activities from datastore / localStorage reports
        const localReports = JSON.parse(localStorage.getItem('oralai_local_reports') || '[]');
        const combined = [...activities];

        if (combined.length === 0 && localReports.length > 0) {
            localReports.forEach(rep => {
                combined.push({
                    id: rep.id,
                    title: `AI Scan: ${rep.patient_name || rep.patient_id}`,
                    description: rep.has_cancer ? `High Risk (${rep.risk_percentage}%) - Malignant signs` : `Low Risk (${rep.risk_percentage}%) - Normal`,
                    timestamp: new Date(rep.analysis_date || Date.now()).toLocaleString(),
                    type: rep.has_cancer ? 'high_risk' : 'low_risk'
                });
            });
        }
        setProfileActivities(combined);
    }, [activities]);

    const handleClearLog = () => {
        if (confirm("Clear local activity log?")) {
            localStorage.removeItem('oralai_activity_log');
            setProfileActivities([]);
        }
    };

    const handleDeleteAccountData = async () => {
        if (confirm("Are you sure you want to clear your local data and sign out?")) {
            resetStore();
            try {
                await supabaseClient.auth.signOut();
            } catch (e) {}
            router.push('/login');
        }
    };

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <div className="header" style={{ display: 'flex', alignItems: 'center', padding: '20px', fontSize: '20px', fontWeight: '600', color: '#ffffff', borderBottom: '1px solid #1f2c3b' }}>
                <ArrowLeft size={24} style={{ marginRight: '15px', cursor: 'pointer' }} onClick={() => router.back()} />
                Doctor Profile & Stats
            </div>

            <main className="container" style={{ flex: 1, paddingBottom: '90px' }}>
                <div className="profile-card">
                    <div className="profile-avatar-large">
                        👨‍⚕️
                    </div>
                    <div className="profile-name">{userName}</div>
                    <div className="profile-email">{userEmail}</div>
                    <div className="badge-active">
                        <span className="dot-green"></span> Localhost & Online Active
                    </div>
                </div>

                <div>
                    <div className="stats-section-title">
                        <BarChart2 size={18} /> Clinical Activity Summary
                    </div>
                    <div className="profile-stats-grid">
                        <div className="stat-box">
                            <div className="icon icon-blue"><Scan size={18} /></div>
                            <div className="stat-val">{stats.totalScans}</div>
                            <div className="stat-lbl">Total AI Scans</div>
                        </div>

                        <div className="stat-box">
                            <div className="icon icon-purple"><Users size={18} /></div>
                            <div className="stat-val">{stats.totalPatients}</div>
                            <div className="stat-lbl">Total Patients</div>
                        </div>

                        <div className="stat-box">
                            <div className="icon icon-red"><AlertTriangle size={18} /></div>
                            <div className="stat-val">{stats.highRiskCount}</div>
                            <div className="stat-lbl">High Risk Cases</div>
                        </div>

                        <div className="stat-box">
                            <div className="icon icon-green"><CheckCircle size={18} /></div>
                            <div className="stat-val">{Math.max(0, stats.normalCount)}</div>
                            <div className="stat-lbl">Normal Scans</div>
                        </div>
                    </div>
                </div>

                <div className="activity-card">
                    <div className="activity-header">
                        <div className="stats-section-title" style={{ marginBottom: 0 }}>
                            <Activity size={18} /> Recent Local Activities
                        </div>
                        <button onClick={handleClearLog} style={{ background: 'none', border: 'none', color: '#7b8e9f', fontSize: '12px', cursor: 'pointer', textDecoration: 'underline' }}>Clear Log</button>
                    </div>

                    <div className="activity-list">
                        {profileActivities.length === 0 ? (
                            <div style={{ color: '#7b8e9f', fontSize: '13px', textAlign: 'center', padding: '20px' }}>
                                No activity recorded yet.<br />Perform an AI scan to see live activity updates here!
                            </div>
                        ) : (
                            profileActivities.slice(0, 15).map((act) => {
                                const isHigh = act.type === 'high_risk' || (act.description && act.description.includes('High Risk'));
                                return (
                                    <div key={act.id} className={`activity-item ${isHigh ? 'high_risk' : 'low_risk'}`}>
                                        <div className="activity-icon">
                                            {isHigh ? <AlertTriangle size={16} style={{ color: '#ff4b4b' }} /> : <CheckCircle size={16} style={{ color: '#10b981' }} />}
                                        </div>
                                        <div className="activity-details">
                                            <div className="activity-title">{act.title}</div>
                                            <div className="activity-desc">{act.description}</div>
                                            <div className="activity-time">{act.timestamp || 'Recently'}</div>
                                        </div>
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>

                <button className="btn-delete-profile" onClick={handleDeleteAccountData}>Delete Account Data</button>
                <div className="warning-text">
                    Deleting account data will remove your local cached reports and session credentials.
                </div>
            </main>

            <BottomNav />
        </div>
    );
}
