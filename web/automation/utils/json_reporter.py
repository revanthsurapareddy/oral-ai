import os
import json
from automation.config.config import Config
from automation.utils.logger import AutomationLogger

class JSONReporter:
    @staticmethod
    def generate_json_report(results, summary_stats):
        Config.init_dirs()
        logger = AutomationLogger.get_logger()
        
        json_path = os.path.join(Config.JSON_REPORTS_DIR, "execution-results.json")
        payload = {
            "summary": summary_stats,
            "test_cases": results
        }

        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(payload, f, indent=2)

        logger.info("JSON Execution Report generated at %s", json_path)
