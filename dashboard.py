#!/usr/bin/env python3
"""
XR R&D Dashboard — チケット管理＋パターン閲覧
Usage: python3 dashboard.py [--port 5000] [--project-root /path/to/repo]
"""

import os
import sys
import json
import glob
import yaml
import markdown
from datetime import datetime
from pathlib import Path
from http.server import HTTPServer, SimpleHTTPRequestHandler
from urllib.parse import parse_qs, urlparse
import argparse

# --- Config ---
PROJECT_ROOT = Path(".")
PORT = 5000

# --- Data Layer ---

def load_tickets():
    """Load all ticket YAML files."""
    tickets = []
    for f in sorted(glob.glob(str(PROJECT_ROOT / "tickets" / "*.yaml"))):
        if ".templates" in f:
            continue
        try:
            with open(f) as fh:
                t = yaml.safe_load(fh)
                t["_file"] = os.path.basename(f)
                tickets.append(t)
        except Exception as e:
            tickets.append({"id": "???", "title": f"Error loading {f}: {e}", "_file": os.path.basename(f)})
    return tickets

def save_ticket(filename, data):
    """Save ticket YAML."""
    path = PROJECT_ROOT / "tickets" / filename
    with open(path, "w") as f:
        yaml.dump(data, f, allow_unicode=True, default_flow_style=False, sort_keys=False)

def load_registry():
    """Load REGISTRY.md content."""
    path = PROJECT_ROOT / "REGISTRY.md"
    if path.exists():
        return path.read_text(encoding="utf-8")
    return "(REGISTRY.md not found)"

def load_failure_analysis():
    """Load failure-analysis.md content."""
    path = PROJECT_ROOT / "archive" / "failure-analysis.md"
    if path.exists():
        return path.read_text(encoding="utf-8")
    return "(failure-analysis.md not found)"

def load_patterns():
    """Load all pattern files by category."""
    patterns = {}
    for f in sorted(glob.glob(str(PROJECT_ROOT / "patterns" / "*.md"))):
        name = Path(f).stem  # e.g. "ui-patterns"
        category = name.replace("-patterns", "")
        with open(f, encoding="utf-8") as fh:
            content = fh.read()
        patterns[category] = {
            "file": os.path.basename(f),
            "content": content,
            "html": markdown.markdown(content, extensions=["fenced_code", "tables"]),
        }
    return patterns

def load_experiments():
    """Load experiment READMEs grouped by category."""
    experiments = {}
    base = PROJECT_ROOT / "experiments"
    for cat_dir in sorted(base.iterdir()):
        if not cat_dir.is_dir() or cat_dir.name.startswith("."):
            continue
        category = cat_dir.name
        exps = []
        for exp_dir in sorted(cat_dir.iterdir()):
            if not exp_dir.is_dir() or exp_dir.name.startswith("."):
                continue
            readme = exp_dir / "README.md"
            test_result = exp_dir / "test-result.yaml"
            review_result = exp_dir / "review-result.yaml"

            exp = {"name": exp_dir.name, "path": str(exp_dir.relative_to(PROJECT_ROOT))}

            if readme.exists():
                exp["readme_html"] = markdown.markdown(
                    readme.read_text(encoding="utf-8"),
                    extensions=["fenced_code", "tables"]
                )
            if test_result.exists():
                with open(test_result) as fh:
                    exp["test_result"] = yaml.safe_load(fh)
            if review_result.exists():
                with open(review_result) as fh:
                    exp["review_result"] = yaml.safe_load(fh)

            screenshot = exp_dir / "screenshot.png"
            if screenshot.exists():
                exp["has_screenshot"] = True

            exps.append(exp)
        if exps:
            experiments[category] = exps
    return experiments

def get_next_ticket_id():
    """Get next available ticket ID."""
    tickets = load_tickets()
    if not tickets:
        return "001"
    max_id = max(int(t.get("id", "0")) for t in tickets)
    return f"{max_id + 1:03d}"

# --- HTML Templates ---

def render_page(title, body, active_tab=""):
    status_colors = {
        "queued": "#6366f1",
        "in-progress": "#f59e0b",
        "testing": "#ef4444",
        "review": "#ec4899",
        "passed": "#22c55e",
        "failed": "#dc2626",
        "archived": "#6b7280",
    }
    return f"""<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title} — XR R&D</title>
<style>
* {{ margin:0; padding:0; box-sizing:border-box; }}
body {{ font-family: 'SF Mono', 'Cascadia Code', 'JetBrains Mono', monospace; background:#0a0a0a; color:#e0e0e0; font-size:13px; line-height:1.6; }}
a {{ color:#9BBFFF; text-decoration:none; }}
a:hover {{ text-decoration:underline; }}

nav {{ display:flex; gap:0; border-bottom:1px solid #222; background:#111; position:sticky; top:0; z-index:100; }}
nav a {{ padding:10px 20px; color:#888; border-bottom:2px solid transparent; }}
nav a:hover {{ color:#e0e0e0; text-decoration:none; background:#1a1a1a; }}
nav a.active {{ color:#9BBFFF; border-bottom-color:#9BBFFF; }}

.container {{ max-width:1100px; margin:0 auto; padding:20px; }}
h1 {{ font-size:16px; font-weight:600; margin-bottom:16px; color:#9BBFFF; }}
h2 {{ font-size:14px; font-weight:600; margin:20px 0 8px; color:#ccc; }}
h3 {{ font-size:13px; font-weight:600; margin:12px 0 4px; color:#aaa; }}

table {{ width:100%; border-collapse:collapse; margin:12px 0; }}
th {{ text-align:left; padding:8px 12px; background:#161616; color:#888; font-weight:500; font-size:11px; text-transform:uppercase; letter-spacing:0.5px; border-bottom:1px solid #222; }}
td {{ padding:8px 12px; border-bottom:1px solid #1a1a1a; vertical-align:top; }}
tr:hover td {{ background:#111; }}

.badge {{ display:inline-block; padding:2px 8px; border-radius:3px; font-size:11px; font-weight:500; }}
.badge-queued {{ background:#6366f120; color:#818cf8; border:1px solid #6366f140; }}
.badge-in-progress {{ background:#f59e0b20; color:#fbbf24; border:1px solid #f59e0b40; }}
.badge-testing {{ background:#ef444420; color:#f87171; border:1px solid #ef444440; }}
.badge-review {{ background:#ec489920; color:#f472b6; border:1px solid #ec489940; }}
.badge-passed {{ background:#22c55e20; color:#4ade80; border:1px solid #22c55e40; }}
.badge-failed {{ background:#dc262620; color:#f87171; border:1px solid #dc262640; }}
.badge-archived {{ background:#6b728020; color:#9ca3af; border:1px solid #6b728040; }}

.badge-high {{ background:#dc262620; color:#f87171; }}
.badge-medium {{ background:#f59e0b20; color:#fbbf24; }}
.badge-low {{ background:#22c55e20; color:#4ade80; }}

.card {{ background:#111; border:1px solid #222; border-radius:6px; padding:16px; margin:12px 0; }}
.card-header {{ display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; }}

.md-content {{ line-height:1.7; }}
.md-content h1 {{ font-size:15px; color:#9BBFFF; margin:16px 0 8px; }}
.md-content h2 {{ font-size:14px; color:#ccc; margin:14px 0 6px; }}
.md-content code {{ background:#1a1a1a; padding:1px 5px; border-radius:3px; font-size:12px; }}
.md-content pre {{ background:#0d0d0d; border:1px solid #222; border-radius:4px; padding:12px; overflow-x:auto; margin:8px 0; }}
.md-content pre code {{ background:none; padding:0; }}
.md-content table {{ border:1px solid #222; }}
.md-content td, .md-content th {{ border:1px solid #222; }}
.md-content ul, .md-content ol {{ padding-left:20px; margin:6px 0; }}

.score {{ font-size:20px; font-weight:700; }}
.score-pass {{ color:#4ade80; }}
.score-conditional {{ color:#fbbf24; }}
.score-fail {{ color:#f87171; }}

.form-group {{ margin:10px 0; }}
.form-group label {{ display:block; color:#888; font-size:11px; text-transform:uppercase; margin-bottom:4px; }}
.form-group input, .form-group select, .form-group textarea {{
  width:100%; background:#0d0d0d; border:1px solid #333; border-radius:4px;
  padding:8px 10px; color:#e0e0e0; font-family:inherit; font-size:13px;
}}
.form-group textarea {{ min-height:80px; resize:vertical; }}
button {{ background:#9BBFFF; color:#000; border:none; padding:8px 20px; border-radius:4px; cursor:pointer; font-family:inherit; font-weight:600; font-size:13px; }}
button:hover {{ background:#7da8f0; }}

.stats {{ display:grid; grid-template-columns:repeat(auto-fit, minmax(120px, 1fr)); gap:12px; margin:16px 0; }}
.stat {{ background:#111; border:1px solid #222; border-radius:6px; padding:12px; text-align:center; }}
.stat-value {{ font-size:24px; font-weight:700; color:#9BBFFF; }}
.stat-label {{ font-size:11px; color:#666; text-transform:uppercase; margin-top:2px; }}

.tab-group {{ display:flex; gap:0; margin:16px 0 0; border-bottom:1px solid #222; }}
.tab {{ padding:8px 16px; cursor:pointer; color:#666; border-bottom:2px solid transparent; font-size:12px; }}
.tab:hover {{ color:#ccc; }}
.tab.active {{ color:#9BBFFF; border-bottom-color:#9BBFFF; }}
.tab-content {{ display:none; }}
.tab-content.active {{ display:block; }}

.empty {{ color:#555; font-style:italic; padding:20px; text-align:center; }}
</style>
</head>
<body>
<nav>
  <a href="/" class="{'active' if active_tab=='dashboard' else ''}">Dashboard</a>
  <a href="/tickets" class="{'active' if active_tab=='tickets' else ''}">Tickets</a>
  <a href="/tickets/new" class="{'active' if active_tab=='new-ticket' else ''}">+ New</a>
  <a href="/experiments" class="{'active' if active_tab=='experiments' else ''}">Experiments</a>
  <a href="/patterns" class="{'active' if active_tab=='patterns' else ''}">Patterns</a>
  <a href="/registry" class="{'active' if active_tab=='registry' else ''}">Registry</a>
</nav>
<div class="container">
{body}
</div>
</body>
</html>"""

def status_badge(status):
    s = str(status).strip().lower()
    return f'<span class="badge badge-{s}">{status}</span>'

def priority_badge(priority):
    p = str(priority).strip().lower()
    return f'<span class="badge badge-{p}">{priority}</span>'

# --- Request Handler ---

class DashboardHandler(SimpleHTTPRequestHandler):
    def log_message(self, format, *args):
        pass  # Suppress default logging

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"

        if path == "/":
            self.serve_dashboard()
        elif path == "/tickets":
            self.serve_tickets()
        elif path == "/tickets/new":
            self.serve_new_ticket_form()
        elif path.startswith("/tickets/"):
            self.serve_ticket_detail(path.split("/")[-1])
        elif path == "/experiments":
            self.serve_experiments()
        elif path == "/patterns":
            self.serve_patterns()
        elif path == "/registry":
            self.serve_registry()
        elif path == "/api/tickets":
            self.serve_json(load_tickets())
        else:
            self.send_error(404)

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path == "/tickets/new":
            content_length = int(self.headers["Content-Length"])
            body = self.rfile.read(content_length).decode("utf-8")
            params = parse_qs(body)

            ticket_id = params.get("id", [get_next_ticket_id()])[0]
            category = params.get("category", ["ui"])[0]
            ticket = {
                "id": ticket_id,
                "title": params.get("title", [""])[0],
                "category": category,
                "priority": params.get("priority", ["medium"])[0],
                "hypothesis": params.get("hypothesis", [""])[0],
                "scope": [s.strip() for s in params.get("scope", [""])[0].split("\n") if s.strip()],
                "skills_needed": [s.strip() for s in params.get("skills_needed", [""])[0].split(",") if s.strip()],
                "success_criteria": [s.strip() for s in params.get("success_criteria", [""])[0].split("\n") if s.strip()],
                "verification_level": params.get("verification_level", ["emulator"])[0],
                "estimated_complexity": params.get("estimated_complexity", ["small"])[0],
                "status": "queued",
                "retry_count": 0,
            }
            filename = f"{ticket_id}-{params.get('slug', [ticket['title']])[0].lower().replace(' ', '-')[:40]}.yaml"
            save_ticket(filename, ticket)

            self.send_response(303)
            self.send_header("Location", "/tickets")
            self.end_headers()
        elif self.path.startswith("/tickets/") and self.path.endswith("/status"):
            content_length = int(self.headers["Content-Length"])
            body = self.rfile.read(content_length).decode("utf-8")
            params = parse_qs(body)
            filename = self.path.split("/")[2]
            new_status = params.get("status", ["queued"])[0]

            path = PROJECT_ROOT / "tickets" / filename
            if path.exists():
                with open(path) as f:
                    ticket = yaml.safe_load(f)
                ticket["status"] = new_status
                save_ticket(filename, ticket)

            self.send_response(303)
            self.send_header("Location", "/tickets")
            self.end_headers()
        else:
            self.send_error(404)

    def serve_html(self, title, body, tab=""):
        html = render_page(title, body, tab)
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(html.encode("utf-8"))

    def serve_json(self, data):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(data, ensure_ascii=False).encode("utf-8"))

    def serve_dashboard(self):
        tickets = load_tickets()
        by_status = {}
        for t in tickets:
            s = t.get("status", "unknown")
            by_status[s] = by_status.get(s, 0) + 1

        experiments = load_experiments()
        total_exp = sum(len(v) for v in experiments.values())

        stats = f"""
        <h1>XR R&D Dashboard</h1>
        <div class="stats">
          <div class="stat"><div class="stat-value">{len(tickets)}</div><div class="stat-label">Total Tickets</div></div>
          <div class="stat"><div class="stat-value">{by_status.get('queued', 0)}</div><div class="stat-label">Queued</div></div>
          <div class="stat"><div class="stat-value">{by_status.get('in-progress', 0)}</div><div class="stat-label">In Progress</div></div>
          <div class="stat"><div class="stat-value">{by_status.get('passed', 0)}</div><div class="stat-label">Passed</div></div>
          <div class="stat"><div class="stat-value">{by_status.get('archived', 0)}</div><div class="stat-label">Archived</div></div>
          <div class="stat"><div class="stat-value">{total_exp}</div><div class="stat-label">Experiments</div></div>
        </div>
        """

        # Recent tickets table
        recent = tickets[:10]
        rows = ""
        for t in recent:
            rows += f"""<tr>
              <td>{t.get('id','?')}</td>
              <td>{t.get('title','')}</td>
              <td><span class="badge badge-{t.get('category','')}">{t.get('category','')}</span></td>
              <td>{priority_badge(t.get('priority',''))}</td>
              <td>{status_badge(t.get('status',''))}</td>
            </tr>"""

        body = stats + f"""
        <h2>Recent Tickets</h2>
        <table>
          <tr><th>ID</th><th>Title</th><th>Category</th><th>Priority</th><th>Status</th></tr>
          {rows}
        </table>
        """
        self.serve_html("Dashboard", body, "dashboard")

    def serve_tickets(self):
        tickets = load_tickets()
        rows = ""
        for t in tickets:
            vl = t.get("verification_level", "static")
            rows += f"""<tr>
              <td>{t.get('id','?')}</td>
              <td><a href="/tickets/{t.get('_file','')}">{t.get('title','')}</a></td>
              <td><span class="badge badge-{t.get('category','')}">{t.get('category','')}</span></td>
              <td>{priority_badge(t.get('priority',''))}</td>
              <td>{vl}</td>
              <td>{status_badge(t.get('status',''))}</td>
              <td>
                <form method="POST" action="/tickets/{t.get('_file','')}/status" style="display:inline">
                  <select name="status" onchange="this.form.submit()" style="background:#0d0d0d;border:1px solid #333;color:#e0e0e0;padding:2px;font-size:11px;border-radius:3px;">
                    {''.join(f'<option value="{s}" {"selected" if s==t.get("status","") else ""}>{s}</option>' for s in ["queued","in-progress","testing","review","passed","failed","archived"])}
                  </select>
                </form>
              </td>
            </tr>"""

        body = f"""
        <h1>Tickets</h1>
        <table>
          <tr><th>ID</th><th>Title</th><th>Cat</th><th>Pri</th><th>Verify</th><th>Status</th><th>Change</th></tr>
          {rows if rows else '<tr><td colspan="7" class="empty">No tickets yet</td></tr>'}
        </table>
        """
        self.serve_html("Tickets", body, "tickets")

    def serve_ticket_detail(self, filename):
        path = PROJECT_ROOT / "tickets" / filename
        if not path.exists():
            self.send_error(404)
            return
        with open(path) as f:
            t = yaml.safe_load(f)

        scope_list = "".join(f"<li>{s}</li>" for s in t.get("scope", []))
        criteria_list = "".join(f"<li>{s}</li>" for s in t.get("success_criteria", []))
        skills_list = ", ".join(f"<code>{s}</code>" for s in t.get("skills_needed", []))

        body = f"""
        <h1>#{t.get('id','')} {t.get('title','')}</h1>
        <div class="card">
          <div style="display:grid;grid-template-columns:1fr 1fr 1fr 1fr;gap:12px;margin-bottom:12px;">
            <div><span style="color:#666">Category:</span> <span class="badge badge-{t.get('category','')}">{t.get('category','')}</span></div>
            <div><span style="color:#666">Priority:</span> {priority_badge(t.get('priority',''))}</div>
            <div><span style="color:#666">Status:</span> {status_badge(t.get('status',''))}</div>
            <div><span style="color:#666">Verify:</span> {t.get('verification_level','static')}</div>
          </div>
          <h3>Hypothesis</h3>
          <p>{t.get('hypothesis','')}</p>
          <h3>Scope</h3>
          <ul>{scope_list}</ul>
          <h3>Skills Needed</h3>
          <p>{skills_list}</p>
          <h3>Success Criteria</h3>
          <ul>{criteria_list}</ul>
          <p style="margin-top:12px;color:#666">Complexity: {t.get('estimated_complexity','')} / Retries: {t.get('retry_count',0)}</p>
        </div>
        <a href="/tickets">← Back to tickets</a>
        """
        self.serve_html(f"Ticket #{t.get('id','')}", body, "tickets")

    def serve_new_ticket_form(self):
        next_id = get_next_ticket_id()
        body = f"""
        <h1>New Ticket</h1>
        <form method="POST" action="/tickets/new">
          <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:12px;">
            <div class="form-group"><label>ID</label><input name="id" value="{next_id}" readonly></div>
            <div class="form-group"><label>Category</label>
              <select name="category">
                <option value="ui">ui</option><option value="input">input</option>
                <option value="voice">voice</option><option value="camera">camera</option>
                <option value="ar">ar</option><option value="architecture">architecture</option>
                <option value="integration">integration</option>
              </select>
            </div>
            <div class="form-group"><label>Priority</label>
              <select name="priority"><option value="high">high</option><option value="medium" selected>medium</option><option value="low">low</option></select>
            </div>
          </div>
          <div class="form-group"><label>Title</label><input name="title" required placeholder="実験タイトル"></div>
          <div class="form-group"><label>Slug (filename)</label><input name="slug" placeholder="auto-generated-from-title"></div>
          <div class="form-group"><label>Hypothesis</label><textarea name="hypothesis" placeholder="この実験で検証したい仮説"></textarea></div>
          <div class="form-group"><label>Scope (1行ずつ)</label><textarea name="scope" placeholder="実装すべき機能1&#10;実装すべき機能2"></textarea></div>
          <div class="form-group"><label>Skills Needed (カンマ区切り)</label><input name="skills_needed" value="glimmer-api, glasses-arch" placeholder="glimmer-api, projected-api"></div>
          <div class="form-group"><label>Success Criteria (1行ずつ)</label><textarea name="success_criteria" placeholder="成功条件1&#10;成功条件2"></textarea></div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
            <div class="form-group"><label>Verification Level</label>
              <select name="verification_level"><option value="static">static</option><option value="emulator" selected>emulator</option><option value="device">device</option></select>
            </div>
            <div class="form-group"><label>Complexity</label>
              <select name="estimated_complexity"><option value="small" selected>small</option><option value="medium">medium</option><option value="large">large</option></select>
            </div>
          </div>
          <button type="submit" style="margin-top:12px;">Create Ticket</button>
        </form>
        """
        self.serve_html("New Ticket", body, "new-ticket")

    def serve_experiments(self):
        experiments = load_experiments()
        if not experiments:
            body = '<h1>Experiments</h1><p class="empty">No experiments yet. Run the pipeline to generate some.</p>'
            self.serve_html("Experiments", body, "experiments")
            return

        tabs = ""
        contents = ""
        first = True
        for cat, exps in experiments.items():
            active = "active" if first else ""
            tabs += f'<div class="tab {active}" onclick="switchTab(\'{cat}\')">{cat} ({len(exps)})</div>'

            cards = ""
            for exp in exps:
                score_html = ""
                if "review_result" in exp:
                    r = exp["review_result"]
                    total = r.get("total", 0)
                    verdict = r.get("verdict", "")
                    cls = "score-pass" if verdict == "PASS" else "score-conditional" if verdict == "CONDITIONAL" else "score-fail"
                    score_html = f'<span class="score {cls}">{total}/10 {verdict}</span>'

                test_html = ""
                if "test_result" in exp:
                    overall = exp["test_result"].get("overall", "?")
                    test_html = f'{status_badge(overall.lower() if overall == "PASS" else "failed")}'

                readme_html = exp.get("readme_html", "<p class='empty'>No README</p>")
                cards += f"""
                <div class="card">
                  <div class="card-header">
                    <div><strong>{exp['name']}</strong> <span style="color:#555;font-size:11px">{exp['path']}</span></div>
                    <div>{test_html} {score_html}</div>
                  </div>
                  <details><summary style="color:#666;cursor:pointer;font-size:12px;">README.md</summary>
                    <div class="md-content" style="margin-top:8px;">{readme_html}</div>
                  </details>
                </div>"""

            contents += f'<div class="tab-content {active}" id="tab-{cat}">{cards}</div>'
            first = False

        body = f"""
        <h1>Experiments</h1>
        <div class="tab-group">{tabs}</div>
        {contents}
        <script>
        function switchTab(cat) {{
          document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
          document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
          event.target.classList.add('active');
          document.getElementById('tab-' + cat).classList.add('active');
        }}
        </script>
        """
        self.serve_html("Experiments", body, "experiments")

    def serve_patterns(self):
        patterns = load_patterns()
        if not patterns:
            body = '<h1>Patterns</h1><p class="empty">No patterns yet. Pass experiments to generate some.</p>'
            self.serve_html("Patterns", body, "patterns")
            return

        tabs = ""
        contents = ""
        first = True
        for cat, data in patterns.items():
            active = "active" if first else ""
            tabs += f'<div class="tab {active}" onclick="switchPatternTab(\'{cat}\')">{cat}</div>'
            contents += f'<div class="tab-content {active}" id="ptab-{cat}"><div class="card md-content">{data["html"]}</div></div>'
            first = False

        body = f"""
        <h1>Implementation Patterns</h1>
        <div class="tab-group">{tabs}</div>
        {contents}
        <script>
        function switchPatternTab(cat) {{
          document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
          document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
          event.target.classList.add('active');
          document.getElementById('ptab-' + cat).classList.add('active');
        }}
        </script>
        """
        self.serve_html("Patterns", body, "patterns")

    def serve_registry(self):
        reg_content = load_registry()
        reg_html = markdown.markdown(reg_content, extensions=["fenced_code", "tables"])
        fail_content = load_failure_analysis()
        fail_html = markdown.markdown(fail_content, extensions=["fenced_code", "tables"])

        body = f"""
        <h1>Registry & Analysis</h1>
        <div class="tab-group">
          <div class="tab active" onclick="switchRegTab('reg')">Completed (REGISTRY.md)</div>
          <div class="tab" onclick="switchRegTab('fail')">Failures (failure-analysis.md)</div>
        </div>
        <div class="tab-content active" id="rtab-reg"><div class="card md-content">{reg_html}</div></div>
        <div class="tab-content" id="rtab-fail"><div class="card md-content">{fail_html}</div></div>
        <script>
        function switchRegTab(id) {{
          document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
          document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
          event.target.classList.add('active');
          document.getElementById('rtab-' + id).classList.add('active');
        }}
        </script>
        """
        self.serve_html("Registry", body, "registry")


# --- Main ---

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="XR R&D Dashboard")
    parser.add_argument("--port", type=int, default=5000)
    parser.add_argument("--project-root", type=str, default=".")
    args = parser.parse_args()

    PROJECT_ROOT = Path(args.project_root).resolve()
    PORT = args.port

    server = HTTPServer(("0.0.0.0", PORT), DashboardHandler)
    print(f"XR R&D Dashboard running at http://localhost:{PORT}")
    print(f"Project root: {PROJECT_ROOT}")
    print("Press Ctrl+C to stop")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")
        server.server_close()
