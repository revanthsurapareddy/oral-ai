// load-tests/helpers.js
// Helper functions for random data generation, request headers, and validation

import { check } from 'k6';

export function getHeaders(token = null) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
}

export function generateRandomUser() {
    const id = Math.floor(Math.random() * 100000);
    return {
        mrn: `MRN-${id}`,
        full_name: `Test Patient ${id}`,
        age: Math.floor(Math.random() * 50) + 20,
        gender: id % 2 === 0 ? 'Male' : 'Female'
    };
}

export function validateJsonResponse(response, expectedStatus = 200) {
    const success = check(response, {
        [`Status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
        'Response header is JSON': (r) => r.headers['Content-Type'] && r.headers['Content-Type'].includes('application/json'),
        'Response body is not empty': (r) => r.body && r.body.length > 0,
    });
    return success;
}
