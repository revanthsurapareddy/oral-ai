import os

class Config:
    # Mandatory requirement: BASE_URL must be configurable via environment variable
    # Default is the live deployment URL
    BASE_URL = os.environ.get("BASE_URL", "https://revanthsurapareddy.github.io/oral-ai/").rstrip('/') + '/'
    
    # Headless Chrome configuration
    HEADLESS = os.environ.get("HEADLESS", "true").lower() == "true"
    IMPLICIT_WAIT = 10
    EXPLICIT_WAIT = 15
    PAGE_LOAD_TIMEOUT = 30
    
    # Path configuration
    BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    REPORTS_DIR = os.path.join(BASE_DIR, "reports")
    EXCEL_REPORTS_DIR = os.path.join(REPORTS_DIR, "Excel")
    HTML_REPORTS_DIR = os.path.join(REPORTS_DIR, "HTML")
    JSON_REPORTS_DIR = os.path.join(REPORTS_DIR, "JSON")
    SUMMARY_REPORTS_DIR = os.path.join(REPORTS_DIR, "Summary")
    SCREENSHOTS_DIR = os.path.join(REPORTS_DIR, "Screenshots")
    LOGS_DIR = os.path.join(REPORTS_DIR, "Logs")

    @classmethod
    def init_dirs(cls):
        for path in [
            cls.REPORTS_DIR, cls.EXCEL_REPORTS_DIR, cls.HTML_REPORTS_DIR,
            cls.JSON_REPORTS_DIR, cls.SUMMARY_REPORTS_DIR,
            cls.SCREENSHOTS_DIR, cls.LOGS_DIR
        ]:
            os.makedirs(path, exist_ok=True)
