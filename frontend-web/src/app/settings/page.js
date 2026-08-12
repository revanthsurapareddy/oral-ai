"use client";

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { useDataStore, supabaseClient } from '@/context/DataStoreContext';
import { User, Shield, Key, HelpCircle, LogOut, RefreshCw, Database, ChevronRight } from 'lucide-react';
import '@/styles/settings.css';

export default function SettingsPage() {
    const router = useRouter();
    const { syncWithBackend, resetStore } = useDataStore();
    const [userName, setUserName] = useState('Doctor');
    const [userEmail, setUserEmail] = useState('doctor@hospital.com');
    const [syncing, setSyncing] = useState(false);

    useEffect(() => {
        async function fetchUserProfile() {
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
        fetchUserProfile();
    }, []);

    const handleSync = async () => {
        setSyncing(true);
        try {
            await syncWithBackend();
            alert("Database synchronized successfully!");
        } catch (e) {
            console.error(e);
            alert("Failed to sync database. Check connection.");
        } finally {
            setSyncing(false);
        }
    };

    const handleResetCache = () => {
        if (confirm("Are you sure you want to clear your local database cache? This will reset all offline diagnostic scans and patients.")) {
            resetStore();
            alert("Cache cleared successfully.");
            window.location.reload();
        }
    };

    const handleLogout = async () => {
        try {
            localStorage.removeItem('oralai_user_name');
            localStorage.removeItem('oralai_user_email');
            await supabaseClient.auth.signOut();
            router.push('/login');
        } catch (error) {
            console.error("Logout failed:", error);
            router.push('/login');
        }
    };

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <main className="content-area" style={{ flex: 1, paddingBottom: '90px' }}>
                <div className="settings-card">
                    <h2 className="settings-header">Settings</h2>

                    <div className="settings-profile-section" onClick={() => router.push('/profile')} style={{ cursor: 'pointer' }}>
                        <div className="settings-avatar">
                            👨‍⚕️
                        </div>
                        <div className="settings-user-info">
                            <div className="settings-user-name">{userName}</div>
                            <div className="settings-user-email">{userEmail}</div>
                        </div>
                    </div>

                    <div className="settings-divider"></div>

                    <div className="settings-list">
                        <Link href="/profile" className="settings-item">
                            <div className="settings-icon-wrapper">
                                <User className="icon-blue" size={18} />
                            </div>
                            <div className="settings-item-text">View Profile</div>
                            <ChevronRight className="settings-item-chevron" size={16} />
                        </Link>

                        <Link href="/privacy-policy" className="settings-item">
                            <div className="settings-icon-wrapper">
                                <Shield className="icon-green" size={18} />
                            </div>
                            <div className="settings-item-text">Privacy Policy</div>
                            <ChevronRight className="settings-item-chevron" size={16} />
                        </Link>

                        <Link href="/change-password" className="settings-item">
                            <div className="settings-icon-wrapper">
                                <Key className="icon-orange" size={18} />
                            </div>
                            <div className="settings-item-text">Change Password</div>
                            <ChevronRight className="settings-item-chevron" size={16} />
                        </Link>

                        <Link href="/help-support" className="settings-item">
                            <div className="settings-icon-wrapper">
                                <HelpCircle className="icon-purple" size={18} />
                            </div>
                            <div className="settings-item-text">Help and Support</div>
                            <ChevronRight className="settings-item-chevron" size={16} />
                        </Link>

                        <div className="settings-divider" style={{ margin: '15px 0' }}></div>

                        {/* Database synchronization and reset utility */}
                        <div className="settings-item" onClick={handleSync} style={{ cursor: 'pointer' }}>
                            <div className="settings-icon-wrapper">
                                <RefreshCw className={syncing ? "icon-blue spin" : "icon-blue"} style={{ animation: syncing ? 'spin 1.5s linear infinite' : 'none' }} size={18} />
                            </div>
                            <div className="settings-item-text">{syncing ? "Synchronizing..." : "Synchronize Database"}</div>
                            <ChevronRight className="settings-item-chevron" size={16} />
                        </div>

                        <div className="settings-item" onClick={handleResetCache} style={{ cursor: 'pointer' }}>
                            <div className="settings-icon-wrapper">
                                <Database className="icon-orange" size={18} />
                            </div>
                            <div className="settings-item-text" style={{ color: '#ff9800' }}>Reset Local Cache</div>
                            <ChevronRight className="settings-item-chevron" size={16} />
                        </div>

                        <div className="settings-divider" style={{ margin: '15px 0' }}></div>

                        <div className="settings-item settings-item-logout" onClick={handleLogout} style={{ cursor: 'pointer' }}>
                            <div className="settings-icon-wrapper">
                                <LogOut className="icon-red" size={18} />
                            </div>
                            <div className="settings-item-text">Logout</div>
                        </div>
                    </div>
                </div>
            </main>

            <style jsx global>{`
                @keyframes spin {
                    from { transform: rotate(0deg); }
                    to { transform: rotate(360deg); }
                }
            `}</style>

            <BottomNav />
        </div>
    );
}
