"use client";

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { supabaseClient } from '@/context/DataStoreContext';
import { Lock, Eye, EyeOff, ArrowLeft } from 'lucide-react';
import '@/styles/settings.css';

export default function ChangePasswordPage() {
    const router = useRouter();

    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showNew, setShowNew] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);

    const [isSaving, setIsSaving] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');
    const [successMsg, setSuccessMsg] = useState('');

    const handleSave = async (e) => {
        e.preventDefault();
        setErrorMsg('');
        setSuccessMsg('');

        if (!newPassword || !confirmPassword) {
            setErrorMsg('Please fill in both fields.');
            return;
        }

        if (newPassword !== confirmPassword) {
            setErrorMsg('Passwords do not match.');
            return;
        }

        if (newPassword.length < 6) {
            setErrorMsg('Password must be at least 6 characters.');
            return;
        }

        setIsSaving(true);

        try {
            const { error } = await supabaseClient.auth.updateUser({
                password: newPassword
            });

            if (error) throw error;

            setSuccessMsg('Password updated successfully!');
            setNewPassword('');
            setConfirmPassword('');
        } catch (error) {
            console.error("Error changing password:", error);
            setErrorMsg(error.message || 'Failed to update password.');
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <div className="header" style={{ display: 'flex', alignItems: 'center', padding: '20px', fontSize: '20px', fontWeight: '600', color: '#ffffff', borderBottom: '1px solid #1f2c3b' }}>
                <ArrowLeft size={24} style={{ marginRight: '15px', cursor: 'pointer' }} onClick={() => router.back()} />
                Change Password
            </div>

            <main className="content-area" style={{ flex: 1, padding: '20px', paddingBottom: '90px' }}>
                <form onSubmit={handleSave}>
                    <div className="input-group">
                        <label className="input-label">New Password</label>
                        <div className="input-wrapper">
                            <Lock className="left-icon" size={18} />
                            <input 
                                type={showNew ? "text" : "password"} 
                                className="input-field" 
                                placeholder="Enter new password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                disabled={isSaving}
                            />
                            {showNew ? (
                                <EyeOff className="right-icon" size={18} onClick={() => setShowNew(false)} />
                            ) : (
                                <Eye className="right-icon" size={18} onClick={() => setShowNew(true)} />
                            )}
                        </div>
                    </div>

                    <div className="input-group">
                        <label className="input-label">Re-enter Password</label>
                        <div className="input-wrapper">
                            <Lock className="left-icon" size={18} />
                            <input 
                                type={showConfirm ? "text" : "password"} 
                                className="input-field" 
                                placeholder="Re-enter new password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                disabled={isSaving}
                            />
                            {showConfirm ? (
                                <EyeOff className="right-icon" size={18} onClick={() => setShowConfirm(false)} />
                            ) : (
                                <Eye className="right-icon" size={18} onClick={() => setShowConfirm(true)} />
                            )}
                        </div>
                    </div>

                    {errorMsg && <div className="error-message">{errorMsg}</div>}
                    {successMsg && <div className="success-message">{successMsg}</div>}

                    <button type="submit" className="btn-save" disabled={isSaving}>
                        {isSaving ? "Saving..." : "Save Changes"}
                    </button>
                </form>
            </main>

            <BottomNav />
        </div>
    );
}
