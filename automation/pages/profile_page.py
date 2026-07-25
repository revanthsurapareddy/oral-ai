from selenium.webdriver.common.by import By
from automation.pages.base_page import BasePage

class ProfilePage(BasePage):
    USER_NAME = (By.ID, "user-name")
    USER_EMAIL = (By.ID, "user-email")
    TOTAL_SCANS = (By.ID, "stat-total-scans")

    def open(self):
        self.navigate_to("profile.html")
