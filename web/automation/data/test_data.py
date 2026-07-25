import random

class TestData:
    VALID_USERS = [
        {"email": "doctor@oralai.com", "password": "Password123!", "name": "Dr. Sarah Jenkins"},
        {"email": "admin@oralai.com", "password": "AdminPassword123!", "name": "Dr. Alex Rivera"},
        {"email": "clinician@hospital.org", "password": "SecurePassword456!", "name": "Dr. Michael Chen"}
    ]
    
    INVALID_USERS = [
        {"email": "invalid@fake.com", "password": "WrongPassword"},
        {"email": "notanemail", "password": "123"},
        {"email": "", "password": ""}
    ]

    @staticmethod
    def generate_patient_data():
        random_id = random.randint(10000, 99999)
        names = ["Aarav Sharma", "Bhavna Patel", "Chetan Reddy", "Deepika Padukone", "Eshwar Rao"]
        genders = ["Male", "Female", "Other"]
        return {
            "mrn": f"PT-{random_id}",
            "name": random.choice(names),
            "age": str(random.randint(18, 85)),
            "gender": random.choice(genders)
        }
