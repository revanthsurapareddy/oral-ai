"use client";

import React from 'react';
import { useRouter } from 'next/navigation';

export default function Header() {
    const router = useRouter();
    return (
        <header className="top-bar">
            <div className="brand" style={{ cursor: 'pointer' }} onClick={() => router.push('/dashboard')}>
                <div className="logo-circle">
                    <svg viewBox="0 0 100 100" style={{ width: '60%', height: '60%' }}>
                        <path d="M 20 50 L 35 50 L 45 30 L 55 70 L 65 50 L 80 50" fill="none" stroke="#ffffff" strokeWidth="8" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                </div>
                <div className="brand-text">OralAI</div>
            </div>
            <div className="user-actions">
                <div className="profile-avatar" style={{ cursor: 'pointer' }} onClick={() => router.push('/profile')}>
                    👨‍⚕️
                </div>
            </div>
        </header>
    );
}
