import logging
import os
from datetime import datetime
from automation.config.config import Config

class AutomationLogger:
    _logger = None

    @classmethod
    def get_logger(cls):
        if cls._logger is None:
            Config.init_dirs()
            log_filename = os.path.join(Config.LOGS_DIR, f"execution_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log")
            
            logger = logging.getLogger("OralAI_Automation")
            logger.setLevel(logging.INFO)
            
            # File handler
            fh = logging.FileHandler(log_filename, encoding='utf-8')
            fh.setLevel(logging.INFO)
            
            # Console handler
            ch = logging.StreamHandler()
            ch.setLevel(logging.INFO)
            
            formatter = logging.Formatter('[%(asctime)s] [%(levelname)s] %(message)s', '%Y-%m-%d %H:%M:%S')
            fh.setFormatter(formatter)
            ch.setFormatter(formatter)
            
            if not logger.handlers:
                logger.addHandler(fh)
                logger.addHandler(ch)
            
            cls._logger = logger
        return cls._logger
