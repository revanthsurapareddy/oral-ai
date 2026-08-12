"use client";

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { supabaseClient } from '@/context/DataStoreContext';
import '@/styles/auth.css';

export default function SignupPage() {
    const router = useRouter();
    const [fullName, setFullName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const handleSignup = async (e) => {
        e.preventDefault();
        setErrorMessage('');
        setSuccessMessage('');
        setIsSubmitting(true);

        try {
            const { data, error } = await supabaseClient.auth.signUp({
                email: email,
                password: password,
                options: {
                    data: {
                        full_name: fullName,
                    }
                }
            });

            if (error) throw error;

            localStorage.setItem('oralai_user_name', fullName);
            localStorage.setItem('oralai_user_email', email);

            setSuccessMessage("Account created successfully!");
            setTimeout(() => {
                router.push('/login');
            }, 800);
        } catch (error) {
            console.warn("Supabase Sign-Up Fallback:", error.message);
            // Preserving the exact fallback behavior: redirect to login even if signup fails offline
            localStorage.setItem('oralai_user_name', fullName || email.split('@')[0] || "Doctor");
            localStorage.setItem('oralai_user_email', email);
            setSuccessMessage("Continuing offline mode...");
            setTimeout(() => {
                router.push('/login');
            }, 800);
        } finally {
            setIsSubmitting(false);
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
                    <h2>Create Account</h2>
                    <p>Register as a medical provider to get started</p>
                </div>

                <form onSubmit={handleSignup} id="signup-form">
                    <div className="form-group">
                        <label className="form-label" htmlFor="full-name">Full Name</label>
                        <input 
                            type="text" 
                            id="full-name" 
                            className="form-input" 
                            required 
                            placeholder="Dr. John Doe"
                            value={fullName}
                            onChange={(e) => setFullName(e.target.value)}
                        />
                    </div>

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
                            placeholder="Min. 8 characters"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>

                    <button type="submit" className="submit-btn" disabled={isSubmitting}>
                        <span>{isSubmitting ? "Creating..." : successMessage ? "Success" : "Sign Up"}</span>
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
                    Already registered? <Link href="/login">Login here</Link>
                </div>
            </div>
        </main>
    );
}
