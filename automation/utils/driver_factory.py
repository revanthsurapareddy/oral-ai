from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from automation.config.config import Config
from automation.utils.logger import AutomationLogger

class DriverFactory:
    @staticmethod
    def create_driver():
        logger = AutomationLogger.get_logger()
        options = Options()
        
        if Config.HEADLESS:
            options.add_argument("--headless=new")
        
        options.add_argument("--disable-gpu")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--window-size=1920,1080")
        options.add_argument("--allow-insecure-localhost")
        options.add_argument("--ignore-certificate-errors")
        
        try:
            driver = webdriver.Chrome(options=options)
            driver.implicitly_wait(Config.IMPLICIT_WAIT)
            driver.set_page_load_timeout(Config.PAGE_LOAD_TIMEOUT)
            logger.info("Chrome WebDriver initialized successfully (Headless Mode: %s)", Config.HEADLESS)
            return driver
        except Exception as e:
            logger.error("Failed to initialize Chrome WebDriver: %s", str(e))
            raise e
