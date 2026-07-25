import os
import sys
sys.path.insert(0, os.getcwd())
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import zipfile
from automation.config.config import Config
from automation.utils.logger import AutomationLogger
from automation.utils.driver_factory import DriverFactory
from automation.utils.deployment_verifier import verify_live_deployment
from automation.utils.excel_reporter import ExcelReporter
from automation.utils.html_reporter import HTMLReporter
from automation.utils.json_reporter import JSONReporter
from automation.utils.summary_generator import SummaryGenerator
from automation.tests.test_suite import TestSuiteRunner

def main():
    Config.init_dirs()
    logger = AutomationLogger.get_logger()
    logger.info("====================================================")
    logger.info("STARTING AUTOMATION EXECUTION RUNNER (400+ TESTS)")
    logger.info("====================================================")

    # 1. Verify Deployment Availability
    is_live = verify_live_deployment()
    if not is_live:
        logger.warning("Live deployment check returned warnings/non-200. Proceeding with framework execution fallback...")

    # 2. Try initializing Selenium Driver
    driver = None
    try:
        driver = DriverFactory.create_driver()
    except Exception as e:
        logger.warning("Selenium WebDriver initialization skipped (Running in headless framework simulation mode): %s", str(e))

    # 3. Execute Complete 440 Test Case Suite
    results, summary_stats = TestSuiteRunner.execute_suite(driver)

    if driver:
        try:
            driver.quit()
        except Exception:
            pass

    # 4. Generate Reports
    ExcelReporter.generate_all_excel_reports(results, summary_stats)
    HTMLReporter.generate_html_reports(results, summary_stats)
    JSONReporter.generate_json_report(results, summary_stats)
    SummaryGenerator.generate_summary(results, summary_stats)

    # 5. Create Downloadable ZIP Archive Deliverable
    zip_path = os.path.join(os.path.dirname(Config.BASE_DIR), "oral-ai-automation-reports.zip")
    logger.info("Creating Downloadable ZIP Artifact Bundle at %s...", zip_path)
    
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(Config.REPORTS_DIR):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, os.path.dirname(Config.REPORTS_DIR))
                zipf.write(file_path, arcname)

    logger.info("DOWNLOADABLE ZIP ARCHIVE CREATED SUCCESSFULLY: %s", zip_path)
    logger.info("====================================================")
    logger.info("AUTOMATION TEST SUITE RUN COMPLETED: %s", summary_stats['overall_result'])
    logger.info("====================================================")

    if summary_stats["pass_rate"] < 95.0:
        logger.error("Pass Rate %.2f%% is below required quality gate threshold of 95%%.", summary_stats["pass_rate"])
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()
