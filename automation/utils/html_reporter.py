import os
from automation.config.config import Config
from automation.utils.logger import AutomationLogger

class HTMLReporter:
    @staticmethod
    def generate_html_reports(results, summary_stats):
        Config.init_dirs()
        logger = AutomationLogger.get_logger()
        logger.info("Generating Professional HTML Reports...")

        exec_report_path = os.path.join(Config.HTML_REPORTS_DIR, "execution-report.html")
        dash_report_path = os.path.join(Config.HTML_REPORTS_DIR, "dashboard.html")

        # HTML Execution Report Template
        html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>OralAI E2E Test Execution Report</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        body {{ font-family: 'Inter', sans-serif; background-color: #0b111a; color: #ffffff; margin: 0; padding: 20px; }}
        .header {{ background: #151e2b; padding: 20px; border-radius: 12px; border: 1px solid #1f2c3b; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }}
        .title {{ font-size: 24px; font-weight: 700; color: #00c6ff; }}
        .subtitle {{ font-size: 13px; color: #7b8e9f; margin-top: 4px; }}
        .stats-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 15px; margin-bottom: 25px; }}
        .stat-card {{ background: #151e2b; padding: 18px; border-radius: 10px; border: 1px solid #1f2c3b; }}
        .stat-val {{ font-size: 28px; font-weight: 700; margin-bottom: 4px; }}
        .val-pass {{ color: #10b981; }} .val-fail {{ color: #ff4b4b; }} .val-skip {{ color: #f59e0b; }} .val-rate {{ color: #00c6ff; }}
        .stat-lbl {{ font-size: 12px; color: #7b8e9f; font-weight: 500; }}
        .table-container {{ background: #151e2b; border-radius: 12px; border: 1px solid #1f2c3b; padding: 20px; overflow-x: auto; }}
        table {{ width: 100%; border-collapse: collapse; text-align: left; font-size: 13px; }}
        th {{ background: #0b111a; color: #7b8e9f; padding: 12px; font-weight: 600; border-bottom: 1px solid #1f2c3b; }}
        td {{ padding: 12px; border-bottom: 1px solid #1f2c3b; color: #e2e8f0; }}
        .badge {{ padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; display: inline-block; }}
        .badge-pass {{ background: rgba(16,185,129,0.15); color: #10b981; }}
        .badge-fail {{ background: rgba(255,75,75,0.15); color: #ff4b4b; }}
        .badge-skip {{ background: rgba(245,158,11,0.15); color: #f59e0b; }}
        .search-box {{ width: 100%; max-width: 400px; padding: 10px 14px; background: #0b111a; border: 1px solid #1f2c3b; border-radius: 8px; color: #fff; margin-bottom: 15px; font-size: 13px; }}
    </style>
</head>
<body>
    <div class="header">
        <div>
            <div class="title">OralAI Automation Execution Report</div>
            <div class="subtitle">Target Base URL: {summary_stats['base_url']} | Executed at: {summary_stats['timestamp']}</div>
        </div>
        <div style="text-align: right;">
            <div style="font-size: 14px; font-weight: 600; color: #10b981;">{summary_stats['status_badge']}</div>
            <div style="font-size: 12px; color: #7b8e9f;">Duration: {summary_stats['duration']}s</div>
        </div>
    </div>

    <div class="stats-grid">
        <div class="stat-card"><div class="stat-val">{summary_stats['total']}</div><div class="stat-lbl">TOTAL EXECUTED</div></div>
        <div class="stat-card"><div class="stat-val val-pass">{summary_stats['passed']}</div><div class="stat-lbl">PASSED TESTS</div></div>
        <div class="stat-card"><div class="stat-val val-fail">{summary_stats['failed']}</div><div class="stat-lbl">FAILED TESTS</div></div>
        <div class="stat-card"><div class="stat-val val-skip">{summary_stats['skipped']}</div><div class="stat-lbl">SKIPPED TESTS</div></div>
        <div class="stat-card"><div class="stat-val val-rate">{summary_stats['pass_rate']}%</div><div class="stat-lbl">PASS RATE</div></div>
    </div>

    <div class="table-container">
        <input type="text" id="search" class="search-box" placeholder="Filter test cases..." onkeyup="filterTable()">
        <table id="testTable">
            <thead>
                <tr>
                    <th>Test ID</th>
                    <th>Module</th>
                    <th>Test Name</th>
                    <th>Priority</th>
                    <th>Status</th>
                    <th>Duration</th>
                    <th>Details</th>
                </tr>
            </thead>
            <tbody>
"""

        for r in results:
            st = r["status"]
            badge_cls = "badge-pass" if st == "PASS" else ("badge-fail" if st == "FAIL" else "badge-skip")
            html_content += f"""
                <tr>
                    <td><b>{r['test_id']}</b></td>
                    <td>{r['module']}</td>
                    <td>{r['test_name']}</td>
                    <td>{r.get('priority', 'P2')}</td>
                    <td><span class="badge {badge_cls}">{st}</span></td>
                    <td>{round(r.get('execution_time', 0.05), 3)}s</td>
                    <td style="color:#7b8e9f;">{r.get('failure_reason', '')}</td>
                </tr>
"""

        html_content += """
            </tbody>
        </table>
    </div>

    <script>
        function filterTable() {
            let input = document.getElementById("search").value.toLowerCase();
            let rows = document.querySelectorAll("#testTable tbody tr");
            rows.forEach(row => {
                let text = row.innerText.toLowerCase();
                row.style.display = text.includes(input) ? "" : "none";
            });
        }
    </script>
</body>
</html>
"""

        with open(exec_report_path, "w", encoding="utf-8") as f:
            f.write(html_content)

        # Dashboard HTML
        with open(dash_report_path, "w", encoding="utf-8") as f:
            f.write(html_content.replace("Execution Report", "Execution Dashboard"))

        logger.info("HTML Reports generated at %s", Config.HTML_REPORTS_DIR)
