"use client";

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LayoutGrid, CloudUpload, Users, Settings } from 'lucide-react';

export default function BottomNav() {
    const pathname = usePathname();

    return (
        <nav className="bottom-nav">
            <Link href="/dashboard" className={`nav-item ${pathname === '/dashboard' ? 'active' : ''}`}>
                <LayoutGrid size={20} />
                <span>Home</span>
            </Link>
            <Link href="/upload" className={`nav-item ${pathname === '/upload' || pathname === '/result' ? 'active' : ''}`}>
                <CloudUpload size={20} />
                <span>Upload</span>
            </Link>
            <Link href="/patients" className={`nav-item ${pathname?.startsWith('/patients') ? 'active' : ''}`}>
                <Users size={20} />
                <span>Patients</span>
            </Link>
            <Link href="/settings" className={`nav-item ${pathname === '/settings' || pathname === '/change-password' ? 'active' : ''}`}>
                <Settings size={20} />
                <span>Settings</span>
            </Link>
        </nav>
    );
}
