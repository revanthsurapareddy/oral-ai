from selenium.webdriver.common.by import By
from automation.pages.base_page import BasePage

class LoginPage(BasePage):
    EMAIL_INPUT = (By.ID, "email")
    PASSWORD_INPUT = (By.ID, "password")
    LOGIN_BTN = (By.CSS_SELECTOR, "button[type='submit'], .login-btn")
    ERROR_MSG = (By.ID, "error-message")

    def open(self):
        self.navigate_to("login.html")

    def login(self, email, password):
        self.send_keys(self.EMAIL_INPUT, email)
        self.send_keys(self.PASSWORD_INPUT, password)
        self.click(self.LOGIN_BTN)
