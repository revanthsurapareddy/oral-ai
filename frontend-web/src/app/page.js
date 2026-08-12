"use client";

import React, { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import '@/styles/styles.css';

export default function SplashPage() {
    const router = useRouter();

    useEffect(() => {
        const timer = setTimeout(() => {
            router.push('/login');
        }, 3000);
        return () => clearTimeout(timer);
    }, [router]);

    return (
        <main className="splash-body" style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            width: '100%',
            height: '100vh',
            overflow: 'hidden',
            background: 'radial-gradient(circle at center, #10263f 0%, #06111e 100%)'
        }}>
            <div className="splash-container">
                <div className="logo-container">
                    <div className="logo-circle">
                        <svg viewBox="0 0 100 100" className="heartbeat-svg">
                            <path 
                                d="M 20 50 L 35 50 L 45 30 L 55 70 L 65 50 L 80 50" 
                                fill="none" 
                                stroke="#ffffff" 
                                strokeWidth="6" 
                                strokeLinecap="round" 
                                strokeLinejoin="round"
                            />
                        </svg>
                    </div>
                    <div className="glow"></div>
                </div>
                
                <h1 className="title">Oral<span className="ai-text">AI</span> Scan</h1>
                <p className="subtitle">REAL-TIME AI ORAL CANCER MARGIN DETECTION</p>
            </div>
        </main>
    );
}
