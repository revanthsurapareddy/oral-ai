// load-tests/suite.js
// Comprehensive End-to-End API Load Test Suite

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { htmlReport } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

export const options = {
    stages: [
        { duration: '10s', target: 10 },
        { duration: '20s', target: 30 },
        { duration: '10s', target: 0 },
    ],
};

export default function () {
    const baseUrl = __ENV.BASE_URL || 'http://localhost:8000';
    const headers = { 'Content-Type': 'application/json' };

    // Group 1: Health Check / Root Endpoint
    group('01_Health_Check', function () {
        const res = http.get(`${baseUrl}/health`, { headers });
        check(res, {
            'Health status 200': (r) => r.status === 200,
        });
    });

    sleep(1);

    // Group 2: Ping Endpoint
    group('02_Ping', function () {
        const res = http.get(`${baseUrl}/ping`, { headers });
        check(res, {
            'Ping status 200': (r) => r.status === 200,
        });
    });

    sleep(1);
}

export function handleSummary(data) {
    return {
        'load-test-report.html': htmlReport(data),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
