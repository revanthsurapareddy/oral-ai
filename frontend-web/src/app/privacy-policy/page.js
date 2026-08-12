"use client";

import React from 'react';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { CheckCircle, ArrowLeft } from 'lucide-react';

export default function PrivacyPolicyPage() {
    const router = useRouter();

    const policies = [
        {
            title: "1. Data Collection",
            desc: "We only collect necessary patient information and scan images required for accurate AI analysis."
        },
        {
            title: "2. Secure Storage",
            desc: "All medical data is securely encrypted and stored following strict compliance guidelines."
        },
        {
            title: "3. No Third-Party Sharing",
            desc: "We do not sell, share, or distribute any patient or diagnostic data to third-party marketing companies."
        },
        {
            title: "4. Data Deletion",
            desc: "You have the right to request permanent deletion of any saved reports or your entire account at any time."
        },
        {
            title: "5. AI Model Training",
            desc: "Uploaded scans may be anonymized and used to improve the diagnostic accuracy of our AI models, unless you opt-out."
        },
        {
            title: "6. Transparency",
            desc: "Any changes to our privacy practices will be communicated clearly and require your explicit consent."
        }
    ];

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <div style={{ display: 'flex', alignItems: 'center', padding: '20px', fontSize: '20px', fontWeight: '600', color: '#ffffff', borderBottom: '1px solid #1f2c3b' }}>
                <ArrowLeft size={24} style={{ marginRight: '15px', cursor: 'pointer' }} onClick={() => router.back()} />
                Privacy and Policy
            </div>

            <main style={{ padding: '20px', flex: 1, paddingBottom: '90px' }}>
                <div style={{ backgroundColor: '#151e2b', borderRadius: '12px', padding: '25px 20px', border: '1px solid #1f2c3b' }}>
                    <h3 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '12px', color: '#ffffff' }}>Your Privacy is our Priority</h3>
                    <p style={{ fontSize: '14px', color: '#7b8e9f', lineHeight: 1.5, marginBottom: '30px' }}>Please read the following carefully to understand how your data is handled.</p>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '25px' }}>
                        {policies.map((p, idx) => (
                            <div key={idx} style={{ display: 'flex', alignItems: 'flex-start' }}>
                                <div style={{ marginRight: '15px', color: '#10b981', marginTop: '2px' }}>
                                    <CheckCircle size={20} />
                                </div>
                                <div style={{ flex: 1 }}>
                                    <div style={{ fontSize: '16px', fontWeight: '600', marginBottom: '6px', color: '#ffffff' }}>{p.title}</div>
                                    <div style={{ fontSize: '14px', color: '#7b8e9f', lineHeight: 1.6 }}>{p.desc}</div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </main>

            <BottomNav />
        </div>
    );
}
