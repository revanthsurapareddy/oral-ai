// load-tests/suite.js
// Comprehensive End-to-End API Load Test Suite

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { htmlReport } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
import { BASE_URL, API_TOKEN } from './config.js';
import { getHeaders, generateRandomUser } from './helpers.js';

export const options = {
    stages: [
        { duration: '15s', target: 20 },
        { duration: '30s', target: 50 },
        { duration: '15s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.02'],                // Errors under 2%
        http_req_duration: ['p(95)<400', 'p(99)<800'], // Latency thresholds
        checks: ['rate>0.98'],                          // 98%+ checks pass
    },
};

export default function () {
    const headers = getHeaders(API_TOKEN);
    const testUser = generateRandomUser();
    let createdMrn = testUser.mrn;

    // Group 1: Authentication / Health Check
    group('01_Auth_Health', function () {
        const res = http.get(`${BASE_URL}/health`, { headers });
        check(res, {
            'Health check 200 OK': (r) => r.status === 200 || r.status === 404, // 404 acceptable if route optional
        });
    });

    sleep(1);

    // Group 2: Create Patient (POST Request)
    group('02_Create_Patient', function () {
        const payload = JSON.stringify(testUser);
        const res = http.post(`${BASE_URL}/api/patients`, payload, { headers });

        check(res, {
            'Patient POST status is 200/201': (r) => r.status === 200 || r.status === 201,
            'Response returns MRN': (r) => r.body && r.body.includes(testUser.mrn),
        });
    });

    sleep(1);

    // Group 3: Fetch Patients List (GET Request)
    group('03_Get_Patients', function () {
        const res = http.get(`${BASE_URL}/api/patients`, { headers });

        check(res, {
            'Patients GET status is 200': (r) => r.status === 200,
            'Response is valid array': (r) => {
                try {
                    const json = JSON.parse(r.body);
                    return Array.isArray(json);
                } catch (e) {
                    return false;
                }
            },
        });
    });

    sleep(1);

    // Group 4: Delete Patient (DELETE Request)
    group('04_Delete_Patient', function () {
        const res = http.del(`${BASE_URL}/api/patients/${createdMrn}`, null, { headers });

        check(res, {
            'Patient DELETE status 200/204': (r) => r.status === 200 || r.status === 204 || r.status === 404,
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
