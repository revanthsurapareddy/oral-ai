-- Run this entirely in your Supabase SQL Editor to setup your database!

-- 1. Create Patients Table
CREATE TABLE IF NOT EXISTS public.patients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mrn VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    age INTEGER,
    gender VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    doctor_id UUID REFERENCES auth.users(id) -- Optional: link to the logged in doctor
);

-- 2. Create Reports Table
CREATE TABLE IF NOT EXISTS public.reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID REFERENCES public.patients(id) ON DELETE CASCADE,
    scan_image_url TEXT,
    risk_level VARCHAR(20),
    risk_percentage INTEGER,
    has_cancer BOOLEAN,
    analysis_date TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Turn off Row Level Security (RLS) for easy Hackathon testing
-- Note: In a real production app, you would leave this ON and write policies.
ALTER TABLE public.patients DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports DISABLE ROW LEVEL SECURITY;

-- 4. Insert some dummy patients so your dashboard isn't empty!
INSERT INTO public.patients (mrn, full_name, age, gender) VALUES
('MRN-8839201', 'James Wilson', 45, 'Male'),
('MRN-8839202', 'Maria Garcia', 62, 'Female'),
('MRN-8839203', 'Robert Chen', 51, 'Male')
ON CONFLICT (mrn) DO NOTHING;
