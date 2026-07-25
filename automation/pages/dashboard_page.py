from selenium.webdriver.common.by import By
from automation.pages.base_page import BasePage

class DashboardPage(BasePage):
    WELCOME_TITLE = (By.ID, "welcome-user-name")
    TOTAL_PATIENTS_STAT = (By.ID, "total-patients-value")
    SCANS_TODAY_STAT = (By.ID, "scans-today-value")
    NEW_SCAN_BTN = (By.CSS_SELECTOR, ".new-scan-btn")

    def open(self):
        self.navigate_to("dashboard.html")

    def get_welcome_name(self):
        return self.get_text(self.WELCOME_TITLE)

    def get_total_patients(self):
        return self.get_text(self.TOTAL_PATIENTS_STAT)
