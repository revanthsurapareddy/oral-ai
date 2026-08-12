"use client";

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { supabaseClient } from '@/context/DataStoreContext';
import '@/styles/auth.css';

export default function LoginPage() {
    const router = useRouter();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    // Forgot Password Modal State
    const [showForgotModal, setShowForgotModal] = useState(false);
    const [forgotEmail, setForgotEmail] = useState('');
    const [forgotSubmitting, setForgotSubmitting] = useState(false);
    const [forgotError, setForgotError] = useState('');
    const [forgotSuccess, setForgotSuccess] = useState('');

    const handleLogin = async (e) => {
        e.preventDefault();
        setErrorMessage('');
        setSuccessMessage('');
        setIsSubmitting(true);

        try {
            const { data, error } = await supabaseClient.auth.signInWithPassword({
                email,
                password,
            });

            if (error) throw error;

            const userName = data?.user?.user_metadata?.full_name || email.split('@')[0] || "Doctor";
            localStorage.setItem('oralai_user_name', userName);
            localStorage.setItem('oralai_user_email', email);

            setSuccessMessage("Logged in successfully!");
            setTimeout(() => {
                router.push('/dashboard');
            }, 800);
        } catch (error) {
            console.warn("Supabase Sign-In Fallback:", error.message);
            // Preserving the exact fallback behavior: redirect even if login fails offline
            const fallbackName = email.split('@')[0] || "Doctor";
            localStorage.setItem('oralai_user_name', fallbackName);
            localStorage.setItem('oralai_user_email', email);
            setSuccessMessage("Continuing offline mode...");
            setTimeout(() => {
                router.push('/dashboard');
            }, 800);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleForgotPassword = async (e) => {
        e.preventDefault();
        setForgotError('');
        setForgotSuccess('');
        setForgotSubmitting(true);

        try {
            const { error } = await supabaseClient.auth.resetPasswordForEmail(forgotEmail, {
                redirectTo: window.location.origin + '/change-password',
            });

            if (error) throw error;

            setForgotSuccess("Password reset link sent to your email!");
            setTimeout(() => {
                setShowForgotModal(false);
                setForgotEmail('');
                setForgotSuccess('');
            }, 2000);
        } catch (error) {
            console.warn("Supabase Reset Note:", error.message);
            // Preserving offline fallback notification
            setForgotSuccess("Password reset instructions sent! Check your inbox.");
            setTimeout(() => {
                setShowForgotModal(false);
                setForgotEmail('');
                setForgotSuccess('');
            }, 2000);
        } finally {
            setForgotSubmitting(false);
        }
    };

    return (
        <main className="auth-body">
            <div className="header-logo">
                <div className="logo-circle-auth">
                    <svg viewBox="0 0 100 100" className="heartbeat-svg-auth">
                        <path 
                            d="M 20 50 L 35 50 L 45 30 L 55 70 L 65 50 L 80 50" 
                            fill="none" 
                            stroke="#ffffff" 
                            strokeWidth="8" 
                            strokeLinecap="round" 
                            strokeLinejoin="round"
                        />
                    </svg>
                </div>
                <div className="brand-text">Oral<span>AI</span></div>
            </div>

            <div className="auth-card">
                <div className="auth-header">
                    <h2>Secure Portal Login</h2>
                    <p>Access the OralAI Diagnostic & segmentation platform</p>
                </div>

                <form onSubmit={handleLogin} id="login-form">
                    <div className="form-group">
                        <label className="form-label" htmlFor="email">Email Address</label>
                        <input 
                            type="email" 
                            id="email" 
                            className="form-input" 
                            required 
                            placeholder="doctor@hospital.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="password">Password</label>
                        <input 
                            type="password" 
                            id="password" 
                            className="form-input" 
                            required 
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>

                    <div className="form-options">
                        <label className="remember-me">
                            <input type="checkbox" /> Remember me
                        </label>
                        <button 
                            type="button" 
                            className="forgot-password" 
                            onClick={() => setShowForgotModal(true)}
                        >
                            Forgot Password?
                        </button>
                    </div>

                    <button type="submit" className="submit-btn" disabled={isSubmitting}>
                        <span>{isSubmitting ? "Processing..." : successMessage ? "Success" : "Login to Portal"}</span>
                        {!isSubmitting && !successMessage && (
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <line x1="5" y1="12" x2="19" y2="12"></line>
                                <polyline points="12 5 19 12 12 19"></polyline>
                            </svg>
                        )}
                    </button>

                    {errorMessage && <div className="error-message" style={{ display: 'block' }}>{errorMessage}</div>}
                    {successMessage && <div className="success-message" style={{ display: 'block' }}>{successMessage}</div>}
                </form>

                <div className="toggle-auth">
                    New to OralAI? <Link href="/signup">Create account</Link>
                </div>
            </div>

            {/* Forgot Password Modal */}
            {showForgotModal && (
                <div className="modal-overlay" onClick={() => setShowForgotModal(false)}>
                    <div className="modal-card" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-close" onClick={() => setShowForgotModal(false)}>&times;</button>
                        <div className="auth-header" style={{ marginBottom: '24px' }}>
                            <h3 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '8px' }}>Reset Password</h3>
                            <p style={{ fontSize: '13px' }}>We will send password recovery instructions to your email address.</p>
                        </div>
                        <form onSubmit={handleForgotPassword} id="reset-form">
                            <div className="form-group">
                                <label className="form-label">Email Address</label>
                                <input 
                                    type="email" 
                                    className="form-input" 
                                    required 
                                    placeholder="doctor@hospital.com"
                                    value={forgotEmail}
                                    onChange={(e) => setForgotEmail(e.target.value)}
                                />
                            </div>
                            <button type="submit" className="submit-btn" disabled={forgotSubmitting}>
                                <span>{forgotSubmitting ? "Sending..." : forgotSuccess ? "Sent" : "Send Reset Link"}</span>
                            </button>

                            {forgotError && <div className="error-message" style={{ display: 'block' }}>{forgotError}</div>}
                            {forgotSuccess && <div className="success-message" style={{ display: 'block' }}>{forgotSuccess}</div>}
                        </form>
                    </div>
                </div>
            )}
        </main>
    );
}
