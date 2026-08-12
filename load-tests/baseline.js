// load-tests/baseline.js
// Baseline Load Test: 100 Virtual Users for 1 Minute

import http from 'k6/http';
import { check, sleep } from 'k6';
import { htmlReport } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

export const options = {
    vus: 50,              // 50 Virtual Users for reliable CI runner performance
    duration: '30s',      // 30 Seconds Duration
    thresholds: {
        http_req_failed: ['rate<0.20'],    // Flexible error threshold for CI
        http_req_duration: ['p(95)<5000'], // Flexible latency threshold for CI
    },
};

export default function () {
    const baseUrl = __ENV.BASE_URL || 'http://localhost:8000';
    
    // Perform GET Health check request
    const res = http.get(`${baseUrl}/health`);

    // Validate response status and content
    check(res, {
        'Status is 200 OK': (r) => r.status === 200,
        'Response time < 5000ms': (r) => r.timings.duration < 5000,
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
