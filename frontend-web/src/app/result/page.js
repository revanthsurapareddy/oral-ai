"use client";

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import BottomNav from '@/components/BottomNav';
import { AlertTriangle, CheckCircle, ArrowLeft, RefreshCw, Upload } from 'lucide-react';
import '@/styles/result.css';

export default function ResultPage() {
    const router = useRouter();
    const [originalScan, setOriginalScan] = useState('');
    const [analyzedScan, setAnalyzedScan] = useState('');
    const [patientInfo, setPatientInfo] = useState({ name: 'Anonymous', id: 'N/A' });
    const [apiResult, setApiResult] = useState(null);

    useEffect(() => {
        if (typeof window === 'undefined') return;

        const scannedImage = sessionStorage.getItem('originalImageForMD5') || sessionStorage.getItem('scannedImage') || localStorage.getItem('oralai_scanned_image');
        const patientInfoStr = sessionStorage.getItem('patientInfo') || localStorage.getItem('oralai_current_patient');
        const apiResultStr = sessionStorage.getItem('apiResult') || localStorage.getItem('oralai_api_result');

        if (scannedImage) {
            setOriginalScan(scannedImage);
        }

        if (patientInfoStr) {
            try {
                const pat = JSON.parse(patientInfoStr);
                setPatientInfo({
                    name: pat.name || pat.full_name || 'Anonymous',
                    id: pat.id || pat.mrn || 'N/A'
                });
            } catch (e) {
                console.warn(e);
            }
        }

        if (apiResultStr) {
            try {
                const result = JSON.parse(apiResultStr);
                setApiResult(result);
                const rawImage = result.overlay_image || result.image_base64 || result.scan_image_url || scannedImage;

                if (rawImage) {
                    setAnalyzedScan(rawImage);
                }
            } catch (e) {
                console.warn(e);
            }
        } else if (scannedImage) {
            setAnalyzedScan(scannedImage);
        }
    }, []);

    function createAnnotatedScanOverlay(imgSrc, result, callback) {
        if (!imgSrc) return callback('');
        const img = new Image();
        img.crossOrigin = "Anonymous";
        img.onload = function() {
            const canvas = document.createElement('canvas');
            canvas.width = img.width;
            canvas.height = img.height;
            const ctx = canvas.getContext('2d');

            // Draw Base Image
            ctx.drawImage(img, 0, 0);

            const w = img.width;
            const h = img.height;

            // Only draw outlines if we have actual AI result data
            if (!result || result.has_cancer === false) {
                return callback(canvas.toDataURL('image/jpeg', 0.85));
            }

            const innerLesionPts = (result.inner_lesion_pts && result.inner_lesion_pts.length >= 3)
                ? result.inner_lesion_pts : [];
            const outerSafetyPts = (result.outer_safety_pts && result.outer_safety_pts.length >= 3)
                ? result.outer_safety_pts : [];

            function drawPolygon(pts, strokeColor, fillColor, lineWidth, dashArray) {
                if (!pts || pts.length < 3) return;
                ctx.save();

                ctx.beginPath();
                ctx.moveTo(pts[0][0] * w, pts[0][1] * h);
                for (let i = 1; i < pts.length; i++) {
                    ctx.lineTo(pts[i][0] * w, pts[i][1] * h);
                }
                ctx.closePath();

                // Semi-transparent fill
                if (fillColor) {
                    ctx.fillStyle = fillColor;
                    ctx.fill();
                }

                // Stroke outline
                ctx.strokeStyle = strokeColor;
                ctx.lineWidth = lineWidth;
                ctx.setLineDash(dashArray || []);
                ctx.lineCap = 'round';
                ctx.lineJoin = 'round';
                ctx.stroke();

                ctx.restore();
            }

            // 1. Outer Safety Margin — bright lime green, dashed
            if (outerSafetyPts.length >= 3) {
                drawPolygon(
                    outerSafetyPts,
                    '#00FF88',          // Bright lime green stroke
                    'rgba(0,255,136,0.08)', // Very light green fill
                    Math.max(3, w * 0.012),
                    [Math.max(14, w * 0.025), Math.max(7, w * 0.012)]
                );
            }

            // 2. Inner Lesion Boundary — bright yellow screening outline (matching 034.jpeg)
            if (innerLesionPts.length >= 3) {
                drawPolygon(
                    innerLesionPts,
                    '#FFFF00',          // Bright yellow stroke matching Output with Screening/034.jpeg
                    'rgba(255,255,0,0.12)', // Light yellow tinted fill
                    Math.max(3.5, w * 0.016),
                    []                  // Solid line for inner lesion
                );
            }

            callback(canvas.toDataURL('image/jpeg', 0.85));
        };
        img.onerror = function() {
            callback(imgSrc);
        };
        img.src = imgSrc;
    }

    const handlePrint = () => {
        window.print();
    };

    const handleShare = async () => {
        if (typeof window === 'undefined') return;

        if (navigator.share) {
            try {
                const title = apiResult ? (apiResult.has_cancer ? 'Cancer Detected' : 'No Anomalies Detected') : 'No Result';
                const details = apiResult?.message || "OralAI Scan assessment details.";
                await navigator.share({
                    title: 'OralAI Scan Result',
                    text: `OralAI Diagnostic Result: ${title}\n${details}`,
                    url: window.location.href
                });
            } catch (error) {
                console.log('Error sharing:', error);
            }
        } else {
            navigator.clipboard.writeText(window.location.href);
            alert("Result link copied to clipboard!");
        }
    };

    const hasCancer = apiResult?.has_cancer;

    return (
        <div style={{ backgroundColor: '#0b111a', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Header />

            <div className="header-result" style={{ color: '#ffffff' }}>
                <ArrowLeft size={24} onClick={() => router.push('/dashboard')} />
                Analysis Result
            </div>

            <div className="container-result" style={{ paddingBottom: '90px' }}>
                <div className="scan-section">
                    <div className="scan-label">Original Scan</div>
                    {originalScan ? (
                        <img className="scan-image" src={originalScan} alt="Original" />
                    ) : (
                        <div style={{ padding: '30px', color: '#7b8e9f', background: '#151e2b', borderRadius: '12px', textAlign: 'center' }}>No scan loaded</div>
                    )}
                </div>

                <div className="scan-section">
                    <div className="scan-label">Analyzed Scan (Contours Margin)</div>
                    {analyzedScan ? (
                        <img className="scan-image" src={analyzedScan} alt="Analyzed" />
                    ) : (
                        <div style={{ padding: '30px', color: '#7b8e9f', background: '#151e2b', borderRadius: '12px', textAlign: 'center' }}>Generating overlay contours...</div>
                    )}
                </div>

                <div className="result-card">
                    {apiResult === null ? (
                        <>
                            <div className="risk-header">
                                <div className="risk-icon" style={{ background: 'rgba(245, 158, 11, 0.15)', color: '#f59e0b' }}>
                                    <AlertTriangle size={20} />
                                </div>
                                <div>
                                    <div className="risk-title" style={{ color: '#f59e0b' }}>No AI Analysis Result Found</div>
                                    <div className="risk-subtitle" style={{ color: '#7b8e9f' }}>Result Expired or Not Processed</div>
                                </div>
                            </div>

                            <div style={{ marginBottom: '12px', fontSize: '13px', color: '#00c6ff', fontWeight: '600' }}>
                                Patient: {patientInfo.name} (MRN: {patientInfo.id})
                            </div>

                            <div className="details-title" style={{ color: '#ffffff' }}>Details</div>
                            <div className="details-text" style={{ color: '#94a3b8' }}>
                                We could not retrieve the diagnostic analysis result for this scan. The session may have expired or the image analysis was interrupted. Please re-upload the scan image to run the AI screening model again.
                            </div>

                            <button 
                                className="action-btn" 
                                style={{ background: '#f59e0b', color: '#ffffff', border: 'none', marginTop: '14px', width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }} 
                                onClick={() => router.push('/upload')}
                            >
                                <Upload size={18} /> Re-upload Image & Run AI Analysis
                            </button>
                        </>
                    ) : (
                        <>
                            <div className="risk-header">
                                {hasCancer ? (
                                    <>
                                        <div className="risk-icon high">
                                            <AlertTriangle size={20} />
                                        </div>
                                        <div>
                                            <div className="risk-title high">Cancer Detected</div>
                                            <div className="risk-subtitle">High Risk ({apiResult?.risk_percentage || 96}%)</div>
                                        </div>
                                    </>
                                ) : (
                                    <>
                                        <div className="risk-icon low">
                                            <CheckCircle size={20} />
                                        </div>
                                        <div>
                                            <div className="risk-title low">No Anomalies Detected</div>
                                            <div className="risk-subtitle">Low Risk ({apiResult?.risk_percentage || 5}%)</div>
                                        </div>
                                    </>
                                )}
                            </div>

                            <div style={{ marginBottom: '12px', fontSize: '13px', color: '#00c6ff', fontWeight: '600' }}>
                                Patient: {patientInfo.name} (MRN: {patientInfo.id})
                            </div>

                            <div className="details-title" style={{ color: '#ffffff' }}>Details</div>
                            <div className="details-text">
                                {apiResult?.message || (hasCancer 
                                    ? "The AI model has detected anomalous tissue structures in the highlighted region. Based on the confidence score, this represents a High risk. Immediate clinical evaluation and biopsy are recommended."
                                    : "The AI model did not detect any immediate signs of oral cancer in the provided scan. Routine checkups are recommended to maintain oral health."
                                )}
                            </div>
                        </>
                    )}
                </div>

                <button className="action-btn btn-dashboard" onClick={() => router.push('/dashboard')}>Return to Dashboard</button>
                <button className="action-btn" style={{ background: '#00c6ff', color: '#ffffff', border: 'none', marginTop: '10px' }} onClick={() => router.push('/patients')}>View Patients List</button>
                
                <div className="btn-row">
                    <button className="action-btn btn-save" onClick={handlePrint}>Save PDF / Print</button>
                    <button className="action-btn btn-share" onClick={handleShare}>Share Link</button>
                </div>
            </div>

            <BottomNav />
        </div>
    );
}
