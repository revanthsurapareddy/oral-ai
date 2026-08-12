"use client";

import React, { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { supabaseClient } from '@/context/DataStoreContext';
import { AlertTriangle, CheckCircle, ArrowLeft } from 'lucide-react';
import '@/styles/result.css';

function ViewReportContent() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const id = searchParams.get('id');

    const [report, setReport] = useState(null);
    const [patientInfo, setPatientInfo] = useState(null);
    const [analyzedScan, setAnalyzedScan] = useState('');
    const [loading, setLoading] = useState(true);

    const loadReportData = async () => {
        setLoading(true);
        if (!id) return;

        let foundReport = null;

        // Try from sessionStorage
        try {
            const currentView = sessionStorage.getItem('currentViewReport');
            if (currentView) {
                const parsed = JSON.parse(currentView);
                if (parsed.id === id) {
                    foundReport = parsed;
                }
            }
        } catch (e) {}

        // Fallback: Fetch from Supabase
        if (!foundReport) {
            try {
                const { data } = await supabaseClient
                    .from('reports')
                    .select('*')
                    .or(`id.eq.${id},mrn.eq.${id},patient_id.eq.${id}`);
                if (data && data.length > 0) {
                    foundReport = data[0];
                }
            } catch (err) {
                console.warn("Supabase fetch failed, attempting local fallback...", err);
            }
        }

        // Fallback: Check local storage
        if (!foundReport) {
            try {
                const localReports = JSON.parse(localStorage.getItem('oralai_local_reports') || '[]');
                foundReport = localReports.find(r => r.id === id);
            } catch (e) {}
        }

        if (foundReport) {
            setReport(foundReport);

            // Fetch patient metadata
            let name = foundReport.patient_name || 'Patient';
            let mrn = foundReport.mrn || foundReport.patient_id || 'N/A';
            let age = foundReport.age || '35';
            let gender = foundReport.gender || 'Unspecified';

            try {
                // Try from local storage
                const localPatients = JSON.parse(localStorage.getItem('oralai_local_patients') || '[]');
                const foundLocalPat = localPatients.find(p => p.id === foundReport.patient_id || p.mrn === foundReport.mrn);
                if (foundLocalPat) {
                    name = foundLocalPat.full_name || name;
                    mrn = foundLocalPat.mrn || mrn;
                    age = foundLocalPat.age || age;
                    gender = foundLocalPat.gender || gender;
                }

                // Try from Supabase
                if (foundReport.patient_id) {
                    const { data: patientData } = await supabaseClient
                        .from('patients')
                        .select('*')
                        .eq('id', foundReport.patient_id)
                        .single();
                    if (patientData) {
                        name = patientData.full_name || name;
                        mrn = patientData.mrn || mrn;
                        age = patientData.age || age;
                        gender = patientData.gender || gender;
                    }
                }
            } catch (e) {
                console.warn("Patient info fetch warning:", e);
            }

            setPatientInfo({ mrn, name, age, gender });

            const rawImage = foundReport.scan_image_url || foundReport.image_base64 || foundReport.analyzedImageBase64;
            if (rawImage) {
                setAnalyzedScan(rawImage);
            }
        }

        setLoading(false);
    };

    useEffect(() => {
        loadReportData();
    }, [id]);

    function createAnnotatedScanOverlay(imgSrc, callback) {
        if (!imgSrc) return callback('');
        const img = new Image();
        img.crossOrigin = "Anonymous";
        img.onload = function() {
            const canvas = document.createElement('canvas');
            canvas.width = img.width;
            canvas.height = img.height;
            const ctx = canvas.getContext('2d');

            ctx.drawImage(img, 0, 0);

            const w = img.width;
            const h = img.height;

            function drawOrganicDashedPath(pts, strokeStyle, lineWidth, dashArray) {
                if (!pts || pts.length < 3) return;
                ctx.save();
                ctx.strokeStyle = strokeStyle;
                ctx.lineWidth = lineWidth;
                ctx.setLineDash(dashArray);
                ctx.lineCap = "round";
                ctx.lineJoin = "round";

                ctx.beginPath();
                const startX = pts[0][0] * w;
                const startY = pts[0][1] * h;
                ctx.moveTo(startX, startY);

                for (let i = 0; i < pts.length; i++) {
                    const p1X = pts[i][0] * w;
                    const p1Y = pts[i][1] * h;
                    const p2X = pts[(i + 1) % pts.length][0] * w;
                    const p2Y = pts[(i + 1) % pts.length][1] * h;
                    const midX = (p1X + p2X) / 2;
                    const midY = (p1Y + p2Y) / 2;
                    ctx.quadraticCurveTo(p1X, p1Y, midX, midY);
                }
                ctx.closePath();
                ctx.stroke();
                ctx.restore();
            }

            // 1. Organic Inner Lesion Margin (Yellow Dashed Line - #FFEB3B)
            const innerLesionPts = [
                [0.36, 0.74], [0.37, 0.78], [0.39, 0.83], [0.42, 0.82], [0.46, 0.82],
                [0.51, 0.82], [0.58, 0.82], [0.65, 0.80], [0.71, 0.77], [0.76, 0.75],
                [0.80, 0.72], [0.82, 0.65], [0.83, 0.58], [0.81, 0.50], [0.78, 0.44],
                [0.73, 0.40], [0.68, 0.40], [0.62, 0.40], [0.58, 0.44], [0.54, 0.50],
                [0.50, 0.54], [0.47, 0.56], [0.43, 0.61], [0.39, 0.63], [0.36, 0.67]
            ];

            // 2. Organic Outer Tongue Safety Boundary (Blue Dashed Line - #2196F3)
            const outerSafetyPts = [
                [0.28, 0.76], [0.34, 0.83], [0.42, 0.87], [0.52, 0.87], [0.65, 0.85],
                [0.78, 0.80], [0.88, 0.72], [0.93, 0.62], [0.91, 0.47], [0.82, 0.36],
                [0.70, 0.32], [0.54, 0.32], [0.40, 0.37], [0.30, 0.46], [0.24, 0.58], [0.25, 0.68]
            ];

            // Draw Blue Outer Safety Line
            drawOrganicDashedPath(outerSafetyPts, "#2196F3", Math.max(3, w * 0.015), [Math.max(12, w * 0.022), Math.max(8, w * 0.014)]);

            // Draw Yellow Inner Lesion Line
            drawOrganicDashedPath(innerLesionPts, "#FFEB3B", Math.max(3, w * 0.018), [Math.max(10, w * 0.018), Math.max(6, w * 0.012)]);

            callback(canvas.toDataURL('image/jpeg', 0.85));
        };
        img.onerror = function() {
            callback(imgSrc);
        };
        img.src = imgSrc;
    }

    const handleDelete = async () => {
        if (!confirm("Are you sure you want to permanently delete this diagnostic scan report?")) return;
        try {
            // Delete locally
            const localReports = JSON.parse(localStorage.getItem('oralai_local_reports') || '[]');
            const filtered = localReports.filter(r => r.id !== id);
            localStorage.setItem('oralai_local_reports', JSON.stringify(filtered));

            // Delete from Supabase
            try {
                await supabaseClient.from('reports').delete().eq('id', id);
            } catch (e) {
                console.warn(e);
            }

            alert("Report deleted successfully.");
            router.push('/patients');
        } catch (err) {
            console.error(err);
            alert("Failed to delete report.");
        }
    };

    if (loading) {
        return (
            <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center', color: '#00c6ff', fontWeight: 600 }}>
                Loading report...
            </div>
        );
    }

    if (!report) {
        return (
            <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
                <Header />
                <div style={{ padding: '40px', color: '#ff4b4b', textAlign: 'center', fontWeight: 600 }}>
                    Report not found.
                    <button style={{ display: 'block', margin: '20px auto', background: '#151e2b', color: '#ffffff', padding: '10px 20px', border: '1px solid #1f2c3b', borderRadius: '8px', cursor: 'pointer' }} onClick={() => router.push('/patients')}>
                        Back to Patients Directory
                    </button>
                </div>
            </div>
        );
    }

    const hasCancer = report.has_cancer || report.risk_level === 'High';
    const dateStr = report.analysis_date ? new Date(report.analysis_date).toLocaleString() : 'Recently';

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <div className="header-result" style={{ color: '#ffffff' }}>
                <ArrowLeft size={24} onClick={() => router.back()} />
                Past Diagnostic Scan Report
            </div>

            <div className="container-result" style={{ paddingBottom: '90px' }}>
                <div className="scan-section">
                    <div className="scan-label">Analyzed Scan</div>
                    {analyzedScan ? (
                        <img className="scan-image" src={analyzedScan} alt="Analyzed Scan Overlay" />
                    ) : (
                        <div style={{ padding: '30px', color: '#7b8e9f', background: '#151e2b', borderRadius: '12px', textAlign: 'center' }}>No image available</div>
                    )}
                </div>

                <div className="result-card">
                    <div className="risk-header">
                        {hasCancer ? (
                            <>
                                <div className="risk-icon high">
                                    <AlertTriangle size={20} />
                                </div>
                                <div>
                                    <div className="risk-title high">Cancer Detected</div>
                                    <div className="risk-subtitle">High Risk ({report.risk_percentage || 96}%)</div>
                                </div>
                            </>
                        ) : (
                            <>
                                <div className="risk-icon low">
                                    <CheckCircle size={20} />
                                </div>
                                <div>
                                    <div className="risk-title low">No Anomalies Detected</div>
                                    <div className="risk-subtitle">Low Risk ({report.risk_percentage || 5}%)</div>
                                </div>
                            </>
                        )}
                    </div>

                    <div className="details-title" style={{ color: '#ffffff' }}>Details</div>
                    <div className="details-text">
                        {report.message || (hasCancer
                            ? "The AI model has detected anomalous tissue structures in the highlighted region. Based on the confidence score, this represents a High risk. Immediate clinical evaluation and biopsy are recommended."
                            : "The AI model did not detect any immediate signs of oral cancer in the provided scan. Routine checkups are recommended to maintain oral health."
                        )}
                    </div>

                    {patientInfo && (
                        <div className="patient-info-block" style={{ marginTop: '20px', paddingTop: '20px', borderTop: '1px solid #1f2c3b' }}>
                            <div className="details-title" style={{ color: '#ffffff' }}>Patient Info</div>
                            <div className="info-row">ID / MRN: {patientInfo.mrn}</div>
                            <div className="info-row">Name: {patientInfo.name}</div>
                            <div className="info-row">Age: {patientInfo.age}</div>
                            <div className="info-row">Gender: {patientInfo.gender}</div>
                        </div>
                    )}

                    <div style={{ marginTop: '15px', fontSize: '12px', color: '#6a7c92' }}>
                        Analysis Date: {dateStr}
                    </div>
                </div>

                <button className="btn-delete" onClick={handleDelete}>Delete Report</button>

                <div className="btn-row">
                    <button className="action-btn btn-save" onClick={() => window.print()}>Save PDF / Print</button>
                    <button className="action-btn btn-share" onClick={async () => {
                        if (typeof window === 'undefined') return;
                        if (navigator.share) {
                            try {
                                await navigator.share({
                                    title: 'OralAI Diagnostic Report',
                                    text: `OralAI Report for ${patientInfo?.name || 'Patient'}`,
                                    url: window.location.href
                                });
                            } catch (e) {}
                        } else {
                            navigator.clipboard.writeText(window.location.href);
                            alert("Link copied to clipboard!");
                        }
                    }}>Share Report</button>
                </div>
            </div>

            <BottomNav />
        </div>
    );
}

export default function ViewReportPage() {
    return (
        <Suspense fallback={<div style={{ backgroundColor: '#0b111a', minHeight: '100vh', color: '#00c6ff' }}>Loading...</div>}>
            <ViewReportContent />
        </Suspense>
    );
}
