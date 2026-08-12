"use client";

import React from 'react';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { Info, Mail, ArrowLeft } from 'lucide-react';

export default function HelpSupportPage() {
    const router = useRouter();

    const faqs = [
        {
            title: "1. General Navigation",
            desc: "Use the bottom bar to switch between Home, Upload, Patients, and Settings easily."
        },
        {
            title: "2. Uploading a Scan",
            desc: "Click 'Upload', select an image, fill in patient details, and hit Analyze."
        },
        {
            title: "3. AI Confidence Score",
            desc: "The percentage shows how confident the AI is in detecting anomalies. It is a diagnostic aid, not a definitive conclusion."
        },
        {
            title: "4. Managing Patients",
            desc: "In the Patients tab, you can view past reports. Click any report to view or permanently delete it."
        },
        {
            title: "5. Updating Profile",
            desc: "Navigate to Settings > Profile to view your details. You can change your password via the Settings menu."
        }
    ];

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <div style={{ display: 'flex', alignItems: 'center', padding: '20px', fontSize: '20px', fontWeight: '600', color: '#ffffff', borderBottom: '1px solid #1f2c3b' }}>
                <ArrowLeft size={24} style={{ marginRight: '15px', cursor: 'pointer' }} onClick={() => router.back()} />
                Help and Support
            </div>

            <main style={{ padding: '20px', flex: 1, paddingBottom: '90px' }}>
                <div style={{ backgroundColor: '#151e2b', borderRadius: '12px', padding: '25px 20px', border: '1px solid #1f2c3b', marginBottom: '20px' }}>
                    <h3 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '25px', color: '#ffffff' }}>How can we assist you?</h3>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '25px' }}>
                        {faqs.map((f, idx) => (
                            <div key={idx} style={{ display: 'flex', alignItems: 'flex-start' }}>
                                <div style={{ marginRight: '15px', color: '#a855f7', marginTop: '2px' }}>
                                    <Info size={20} />
                                </div>
                                <div style={{ flex: 1 }}>
                                    <div style={{ fontSize: '16px', fontWeight: '600', marginBottom: '6px', color: '#ffffff' }}>{f.title}</div>
                                    <div style={{ fontSize: '14px', color: '#7b8e9f', lineHeight: 1.6 }}>{f.desc}</div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                <div style={{ backgroundColor: '#151e2b', borderRadius: '12px', padding: '25px 20px', border: '1px solid #1f2c3b' }}>
                    <h3 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '10px', color: '#ffffff' }}>Still need help?</h3>
                    <p style={{ fontSize: '14px', color: '#7b8e9f', lineHeight: 1.6, marginBottom: '20px' }}>Contact our support team directly via email.</p>
                    
                    <a 
                        href="mailto:support@oralai.com?subject=OralAI Support Request" 
                        style={{ 
                            display: 'flex', 
                            alignItems: 'center', 
                            justifyContent: 'center', 
                            width: '100%', 
                            backgroundColor: '#3b82f6', 
                            color: 'white', 
                            borderRadius: '8px', 
                            padding: '16px', 
                            fontSize: '15px', 
                            fontWeight: '600',
                            transition: 'background-color 0.2s',
                            gap: '10px'
                        }}
                    >
                        <Mail size={18} />
                        Email Support
                    </a>
                </div>
            </main>

            <BottomNav />
        </div>
    );
}
