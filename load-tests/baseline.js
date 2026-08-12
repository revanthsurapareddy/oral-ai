// load-tests/baseline.js
// Baseline Load Test: 50 Virtual Users for 30 Seconds

import http from 'k6/http';
import { check, sleep } from 'k6';
import { htmlReport } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

export const options = {
    vus: 50,              // 50 Virtual Users
    duration: '30s',      // 30 Seconds Duration
};

export default function () {
    const baseUrl = __ENV.BASE_URL || 'http://localhost:8000';
    
    // Perform GET Health check request
    const res = http.get(`${baseUrl}/health`);

    // Validate response status and content
    check(res, {
        'Status is 200 OK': (r) => r.status === 200,
        'Response received': (r) => r.body && r.body.length > 0,
    });

    sleep(1);
}

// Generate HTML Summary Report
export function handleSummary(data) {
    return {
        'load-test-report.html': htmlReport(data),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
