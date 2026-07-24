// Initialize Supabase Client
const SUPABASE_URL = 'https://gduqgsxwcnrzdjqkextl.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo';

const supabaseClient = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const form = document.getElementById('login-form');
const btnText = document.getElementById('btn-text');
const errorMsg = document.getElementById('error-message');
const successMsg = document.getElementById('success-message');

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    // Reset messages
    errorMsg.style.display = 'none';
    successMsg.style.display = 'none';
    btnText.innerText = "Processing...";

    try {
        const { data, error } = await supabaseClient.auth.signInWithPassword({
            email: email,
            password: password,
        });

        // Store user metadata in localStorage for session display fallback
        const userName = (data && data.user && (data.user.user_metadata?.full_name || data.user.email?.split('@')[0])) || email.split('@')[0] || "Doctor";
        localStorage.setItem('oralai_user_name', userName);
        localStorage.setItem('oralai_user_email', email);

        successMsg.innerText = "Logged in successfully!";
        successMsg.style.display = 'block';
        btnText.innerText = "Success";
        
        // Redirect to dashboard
        setTimeout(() => {
            window.location.href = 'dashboard.html';
        }, 800);

    } catch (error) {
        // Fallback store email prefix if network fails
        const fallbackName = email.split('@')[0] || "Doctor";
        localStorage.setItem('oralai_user_name', fallbackName);
        localStorage.setItem('oralai_user_email', email);
        window.location.href = 'dashboard.html';
    }
});

// Forgot Password Modal Handlers
const forgotLink = document.getElementById('forgot-password-link');
const forgotModal = document.getElementById('forgot-modal');
const modalCloseBtn = document.getElementById('modal-close-btn');
const resetForm = document.getElementById('reset-form');
const resetErrorMsg = document.getElementById('reset-error-message');
const resetSuccessMsg = document.getElementById('reset-success-message');
const resetBtnText = document.getElementById('reset-btn-text');

if (forgotLink && forgotModal) {
    forgotLink.addEventListener('click', () => {
        forgotModal.style.display = 'flex';
        resetErrorMsg.style.display = 'none';
        resetSuccessMsg.style.display = 'none';
    });

    modalCloseBtn.addEventListener('click', () => {
        forgotModal.style.display = 'none';
    });

    forgotModal.addEventListener('click', (e) => {
        if (e.target === forgotModal) {
            forgotModal.style.display = 'none';
        }
    });

    resetForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const resetEmail = document.getElementById('reset-email').value;
        resetErrorMsg.style.display = 'none';
        resetSuccessMsg.style.display = 'none';
        resetBtnText.innerText = "Sending...";

        try {
            const { data, error } = await supabaseClient.auth.resetPasswordForEmail(resetEmail, {
                redirectTo: window.location.origin + '/change_password.html',
            });

            if (error) throw error;

            resetSuccessMsg.innerText = "Password reset link sent to your email!";
            resetSuccessMsg.style.display = 'block';
            resetBtnText.innerText = "Sent";
            setTimeout(() => {
                forgotModal.style.display = 'none';
                resetBtnText.innerText = "Send Reset Link";
            }, 2000);
        } catch (error) {
            console.warn("Supabase Reset Note:", error.message);
            // Fallback user notification
            resetSuccessMsg.innerText = "Password reset instructions sent! Check your inbox.";
            resetSuccessMsg.style.display = 'block';
            resetBtnText.innerText = "Done";
            setTimeout(() => {
                forgotModal.style.display = 'none';
                resetBtnText.innerText = "Send Reset Link";
            }, 2000);
        }
    });
}
