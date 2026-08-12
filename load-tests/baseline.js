// load-tests/baseline.js
// Baseline Load Test: 100 Virtual Users for 1 Minute

import http from 'k6/http';
import { check, sleep } from 'k6';
import { htmlReport } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

export const options = {
    vus: 100,             // 100 Virtual Users
    duration: '1m',       // Test duration: 1 minute
    thresholds: {
        http_req_failed: ['rate<0.01'],    // Error rate under 1%
        http_req_duration: ['p(95)<300'],  // 95% of requests under 300ms
    },
};

export default function () {
    const url = __ENV.BASE_URL || 'http://localhost:8000/api/patients';
    
    // Perform GET request
    const res = http.get(url);

    // Validate response status and content
    check(res, {
        'Status is 200': (r) => r.status === 200,
        'Response time < 300ms': (r) => r.timings.duration < 300,
    });

    // Pacing / Think Time: 1 second sleep between iterations
    sleep(1);
}

// Generate HTML Summary Report
export function handleSummary(data) {
    return {
        'load-test-report.html': htmlReport(data),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
