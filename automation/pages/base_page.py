from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from automation.config.config import Config
from automation.utils.logger import AutomationLogger

class BasePage:
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, Config.EXPLICIT_WAIT)
        self.logger = AutomationLogger.get_logger()

    def navigate_to(self, relative_path=""):
        url = f"{Config.BASE_URL}{relative_path.lstrip('/')}"
        self.logger.info("Navigating to: %s", url)
        self.driver.get(url)

    def find_element(self, by_locator):
        return self.wait.until(EC.presence_of_element_located(by_locator))

    def click(self, by_locator):
        el = self.wait.until(EC.element_to_be_clickable(by_locator))
        el.click()

    def send_keys(self, by_locator, text):
        el = self.find_element(by_locator)
        el.clear()
        el.send_keys(text)

    def get_text(self, by_locator):
        el = self.find_element(by_locator)
        return el.text.strip()

    def is_displayed(self, by_locator):
        try:
            return self.find_element(by_locator).is_displayed()
        except Exception:
            return False
