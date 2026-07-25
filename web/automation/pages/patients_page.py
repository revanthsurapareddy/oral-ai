from selenium.webdriver.common.by import By
from automation.pages.base_page import BasePage

class PatientsPage(BasePage):
    SEARCH_INPUT = (By.ID, "patient-search-input")
    PATIENTS_CONTAINER = (By.ID, "patients-container")
    FILTER_BTN = (By.ID, "filter-btn")

    def open(self):
        self.navigate_to("patients.html")

    def search_patient(self, keyword):
        self.send_keys(self.SEARCH_INPUT, keyword)
