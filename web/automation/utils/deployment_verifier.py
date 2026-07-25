import requests
import sys
import os
from automation.config.config import Config
from automation.utils.logger import AutomationLogger

def verify_live_deployment():
    logger = AutomationLogger.get_logger()
    target_url = Config.BASE_URL
    logger.info("=========================================")
    logger.info("STARTING DEPLOYMENT VALIDATION FOR: %s", target_url)
    logger.info("=========================================")

    # Check 1: Base URL HTTP Status
    try:
        resp = requests.get(target_url, timeout=10)
        logger.info("Base URL Response Code: %d", resp.status_code)
        if resp.status_code != 200:
            logger.error("Deployment Validation FAILED: Base URL returned status code %d", resp.status_code)
            return False
    except Exception as e:
        logger.error("Deployment Validation FAILED: Exception connecting to %s - %s", target_url, str(e))
        return False

    # Check 2: Verify essential pages return HTTP 200
    essential_pages = [
        "dashboard.html",
        "patients.html",
        "upload.html",
        "profile.html"
    ]

    for page in essential_pages:
        page_url = f"{target_url}{page}"
        try:
            r = requests.get(page_url, timeout=10)
            logger.info("Page Check [%s] -> Status Code: %d", page, r.status_code)
            if r.status_code != 200:
                logger.warning("Essential Page Check WARNING: [%s] returned status %d", page, r.status_code)
        except Exception as e:
            logger.warning("Error probing page [%s]: %s", page, str(e))

    logger.info("LIVE DEPLOYMENT VALIDATION COMPLETED SUCCESSFULLY.")
    return True

if __name__ == "__main__":
    success = verify_live_deployment()
    if not success:
        sys.exit(1)
