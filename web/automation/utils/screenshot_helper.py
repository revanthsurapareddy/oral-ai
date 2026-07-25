import os
from datetime import datetime
from automation.config.config import Config
from automation.utils.logger import AutomationLogger

class ScreenshotHelper:
    @staticmethod
    def capture_screenshot(driver, test_id):
        Config.init_dirs()
        logger = AutomationLogger.get_logger()
        filename = f"{test_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.png"
        filepath = os.path.join(Config.SCREENSHOTS_DIR, filename)
        
        try:
            if driver:
                driver.save_screenshot(filepath)
                logger.info("Captured screenshot for [%s] -> %s", test_id, filepath)
                return filepath
        except Exception as e:
            logger.warning("Failed to capture screenshot for [%s]: %s", test_id, str(e))
        return None
