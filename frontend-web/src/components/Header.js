"use client";

import React from 'react';
import Link from 'next/link';
import { useRouter, usePathname } from 'next/navigation';
import { LayoutGrid, CloudUpload, Users, Settings } from 'lucide-react';

export default function Header() {
    const router = useRouter();
    const pathname = usePathname();

    return (
        <header className="top-bar" style={{ width: '100%', padding: '16px 48px' }}>
            <div className="top-bar-inner" style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div className="brand" style={{ cursor: 'pointer' }} onClick={() => router.push('/dashboard')}>
                    <div className="logo-circle">
                        <svg viewBox="0 0 100 100" style={{ width: '60%', height: '60%' }}>
                            <path d="M 20 50 L 35 50 L 45 30 L 55 70 L 65 50 L 80 50" fill="none" stroke="#ffffff" strokeWidth="8" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                    </div>
                    <div className="brand-text">OralAI <span>Portal</span></div>
                </div>

                {/* Desktop Header Navigation */}
                <nav className="desktop-header-nav">
                    <Link href="/dashboard" className={`desktop-nav-link ${pathname === '/dashboard' ? 'active' : ''}`}>
                        <LayoutGrid size={18} />
                        <span>Dashboard</span>
                    </Link>
                    <Link href="/patients" className={`desktop-nav-link ${pathname?.startsWith('/patients') ? 'active' : ''}`}>
                        <Users size={18} />
                        <span>Patients</span>
                    </Link>
                    <Link href="/upload" className={`desktop-nav-link ${pathname === '/upload' || pathname === '/result' ? 'active' : ''}`}>
                        <CloudUpload size={18} />
                        <span>Upload Scan</span>
                    </Link>
                    <Link href="/settings" className={`desktop-nav-link ${pathname === '/settings' || pathname === '/change-password' ? 'active' : ''}`}>
                        <Settings size={18} />
                        <span>Settings</span>
                    </Link>
                </nav>

                <div className="user-actions">
                    <div className="profile-avatar" style={{ cursor: 'pointer' }} onClick={() => router.push('/profile')} title="Doctor Profile">
                        👨‍⚕️
                    </div>
                </div>
            </div>
        </header>
    );
}
