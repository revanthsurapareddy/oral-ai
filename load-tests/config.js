// load-tests/config.js
// Centralized Configuration and Thresholds for k6 Load Testing

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8000';
export const API_TOKEN = __ENV.TOKEN || 'sample-bearer-token-12345';

export const defaultOptions = {
    stages: [
        { duration: '30s', target: 20 },  // Ramp-up to 20 users
        { duration: '1m', target: 100 },  // Stay at 100 users for 1 minute (Peak Load)
        { duration: '30s', target: 0 },   // Ramp-down to 0 users
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],             // Less than 1% failed requests
        http_req_duration: ['p(95)<300', 'p(99)<500'], // 95% of requests under 300ms, 99% under 500ms
        checks: ['rate>0.99'],                      // 99%+ of functional checks must pass
    },
};
