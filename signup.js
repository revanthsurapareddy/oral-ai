// Initialize Supabase Client
const SUPABASE_URL = 'https://gduqgsxwcnrzdjqkextl.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo';

const supabase = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const form = document.getElementById('signup-form');
const btnText = document.getElementById('btn-text');
const errorMsg = document.getElementById('error-message');
const successMsg = document.getElementById('success-message');

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const fullname = document.getElementById('fullname').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    // Reset messages
    errorMsg.style.display = 'none';
    successMsg.style.display = 'none';
    btnText.innerText = "Processing...";

    try {
        const { data, error } = await supabase.auth.signUp({
            email: email,
            password: password,
            options: {
                data: {
                    full_name: fullname
                }
            }
        });

        if (error) throw error;

        localStorage.setItem('oralai_user_name', fullname);
        localStorage.setItem('oralai_user_email', email);

        successMsg.innerText = "Account created! Redirecting to login...";
        successMsg.style.display = 'block';
        btnText.innerText = "Success";
        
        // Wait a short moment then redirect to login screen
        setTimeout(() => {
            window.location.href = 'login.html';
        }, 1500);

    } catch (error) {
        btnText.innerText = "Sign Up";
        errorMsg.innerText = error.message;
        errorMsg.style.display = 'block';
    }
});
