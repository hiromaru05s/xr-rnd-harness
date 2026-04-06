#!/usr/bin/env python3
"""
XR R&D Dashboard — チケット管理＋パターン閲覧＋フィードバックループ
Usage: python3 dashboard.py [--port 5000] [--project-root /path/to/repo]
"""

import os
import re
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
            with open(f, encoding="utf-8") as fh:
                t = yaml.safe_load(fh)
                t["_file"] = os.path.basename(f)
                tickets.append(t)
        except Exception as e:
            tickets.append({"id": "???", "title": f"Error loading {f}: {e}", "_file": os.path.basename(f)})
    return tickets

def save_ticket(filename, data):
    """Save ticket YAML."""
    path = PROJECT_ROOT / "tickets" / filename
    with open(path, "w", encoding="utf-8") as f:
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

def load_human_rejections():
    """Load human-rejections.md content."""
    path = PROJECT_ROOT / "archive" / "human-rejections.md"
    if path.exists():
        return path.read_text(encoding="utf-8")
    return "(human-rejections.md not found)"

def load_patterns():
    """Load all pattern files by category."""
    patterns = {}
    for f in sorted(glob.glob(str(PROJECT_ROOT / "patterns" / "*.md"))):
        name = Path(f).stem
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
    if not base.exists():
        return experiments
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
            human_feedback = exp_dir / "human-feedback.yaml"

            exp = {"name": exp_dir.name, "path": str(exp_dir.relative_to(PROJECT_ROOT)), "category": category}

            if readme.exists():
                exp["readme_html"] = markdown.markdown(
                    readme.read_text(encoding="utf-8"),
                    extensions=["fenced_code", "tables"]
                )
            if test_result.exists():
                with open(test_result, encoding="utf-8") as fh:
                    exp["test_result"] = yaml.safe_load(fh)
            if review_result.exists():
                with open(review_result, encoding="utf-8") as fh:
                    exp["review_result"] = yaml.safe_load(fh)
            if human_feedback.exists():
                with open(human_feedback, encoding="utf-8") as fh:
                    exp["human_feedback"] = yaml.safe_load(fh)

            screenshot = exp_dir / "screenshot.png"
            if screenshot.exists():
                exp["has_screenshot"] = True

            exps.append(exp)
        if exps:
            experiments[category] = exps
    return experiments

def load_all_feedback():
    """Load all human-feedback.yaml files across all experiments."""
    feedbacks = []
    base = PROJECT_ROOT / "experiments"
    if not base.exists():
        return feedbacks
    for cat_dir in sorted(base.iterdir()):
        if not cat_dir.is_dir() or cat_dir.name.startswith("."):
            continue
        for exp_dir in sorted(cat_dir.iterdir()):
            if not exp_dir.is_dir():
                continue
            fb_file = exp_dir / "human-feedback.yaml"
            if fb_file.exists():
                try:
                    with open(fb_file, encoding="utf-8") as fh:
                        fb = yaml.safe_load(fh)
                        if fb:
                            fb["_experiment"] = exp_dir.name
                            fb["_category"] = cat_dir.name
                            fb["_path"] = str(exp_dir.relative_to(PROJECT_ROOT))
                            feedbacks.append(fb)
                except Exception:
                    pass
    # Sort by timestamp descending
    feedbacks.sort(key=lambda x: x.get("timestamp", ""), reverse=True)
    return feedbacks

def find_ticket_for_experiment(exp_name):
    """Find the ticket YAML file associated with an experiment."""
    tickets = load_tickets()
    exp_id = _extract_exp_id(exp_name)
    if not exp_id:
        return None
    for t in tickets:
        if str(t.get("id", "")).zfill(3) == exp_id.zfill(3):
            return t
    return None

def get_next_ticket_id():
    """Get next available ticket ID."""
    tickets = load_tickets()
    if not tickets:
        return "001"
    max_id = max(int(t.get("id", "0")) for t in tickets)
    return f"{max_id + 1:03d}"

def _extract_exp_id(exp_name):
    """Extract numeric experiment ID from experiment folder name (e.g., '001-glimmer-basic' → '001')."""
    match = re.match(r"^(\d+)", exp_name)
    return match.group(1) if match else None

def _validate_experiment_path(experiment_path):
    """Validate that experiment_path resolves within experiments/ directory."""
    try:
        exp_full_path = (PROJECT_ROOT / experiment_path).resolve()
        allowed_base = (PROJECT_ROOT / "experiments").resolve()
        if not str(exp_full_path).startswith(str(allowed_base) + os.sep) and exp_full_path != allowed_base:
            return None
        return exp_full_path
    except Exception:
        return None

def _remove_registry_entry(exp_id):
    """Remove an experiment entry from REGISTRY.md by its numeric ID."""
    registry_path = PROJECT_ROOT / "REGISTRY.md"
    if not registry_path.exists():
        return
    reg_content = registry_path.read_text(encoding="utf-8")
    # Check if entry exists at all
    if exp_id not in reg_content:
        return

    new_lines = []
    skip_block = False
    for line in reg_content.split("\n"):
        stripped = line.strip()
        # Detect heading that starts with the exact exp_id (e.g., "### 001:" or "## 001 ")
        if not skip_block and stripped.startswith("#"):
            heading_text = stripped.lstrip("#").strip()
            if re.match(rf"^0*{int(exp_id)}\b", heading_text):
                skip_block = True
                continue
        # Stop skipping at next same-or-higher-level heading
        if skip_block and stripped.startswith("#") and not re.match(rf"^#*\s*0*{int(exp_id)}\b", stripped):
            skip_block = False
        # Skip separator lines that belong to the deleted block
        if skip_block and stripped == "---":
            continue
        if not skip_block:
            new_lines.append(line)

    registry_path.write_text("\n".join(new_lines), encoding="utf-8")

def _remove_pattern_entries(exp_id, exp_name):
    """Remove pattern blocks referencing this experiment from patterns/ files."""
    patterns_dir = PROJECT_ROOT / "patterns"
    if not patterns_dir.exists():
        return
    for pf in patterns_dir.glob("*.md"):
        p_content = pf.read_text(encoding="utf-8")
        if exp_id not in p_content and exp_name not in p_content:
            continue
        new_lines = []
        skip_block = False
        skip_heading_level = 0
        for line in p_content.split("\n"):
            stripped = line.strip()
            if not skip_block and stripped.startswith("#") and (exp_id in stripped or exp_name in stripped):
                skip_heading_level = len(stripped) - len(stripped.lstrip("#"))
                skip_block = True
                continue
            if skip_block and stripped.startswith("#"):
                current_level = len(stripped) - len(stripped.lstrip("#"))
                if current_level <= skip_heading_level:
                    skip_block = False
            if not skip_block:
                new_lines.append(line)
        cleaned = "\n".join(new_lines).strip()
        if cleaned:
            pf.write_text(cleaned + "\n", encoding="utf-8")

def submit_feedback(experiment_path, reason, issues, improvement_direction, severity="medium"):
    """
    Submit human feedback for an experiment.
    - Validates experiment path (path traversal防止)
    - Creates/updates human-feedback.yaml in the experiment directory
    - Updates ticket status to queued with human_feedback flag (チケットが存在しない場合はエラー)
    - Removes entry from REGISTRY.md and patterns/
    - Appends to archive/human-rejections.md
    """
    # Path traversal防止: experiments/ 配下のみ許可
    exp_full_path = _validate_experiment_path(experiment_path)
    if exp_full_path is None or not exp_full_path.exists():
        return False, f"Invalid or non-existent experiment path: {experiment_path}"

    exp_name = exp_full_path.name
    category = exp_full_path.parent.name
    exp_id = _extract_exp_id(exp_name)
    if not exp_id:
        return False, f"Cannot extract experiment ID from: {exp_name}"

    # チケットの存在確認（先にやる — チケットがなければフィードバックを受け付けない）
    ticket = find_ticket_for_experiment(exp_name)
    if not ticket or "_file" not in ticket:
        return False, f"No ticket found for experiment: {exp_name}. Cannot submit feedback without a ticket."

    # Build feedback entry
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    issues_list = [i.strip() for i in issues.split("\n") if i.strip()]

    # Load existing feedback history or create new
    fb_file = exp_full_path / "human-feedback.yaml"
    existing_history = []
    if fb_file.exists():
        with open(fb_file, encoding="utf-8") as fh:
            existing = yaml.safe_load(fh) or {}
            if "history" in existing:
                existing_history = existing["history"]
            existing_entry = {k: v for k, v in existing.items() if k not in ("history", "_experiment", "_category", "_path")}
            if existing_entry.get("timestamp"):
                existing_history.append(existing_entry)

    feedback_data = {
        "reviewer": "human (Hiromaru)",
        "verdict": "REJECT",
        "timestamp": timestamp,
        "reason": reason,
        "severity": severity,
        "specific_issues": issues_list,
        "improvement_direction": improvement_direction,
    }
    if existing_history:
        feedback_data["history"] = existing_history

    with open(fb_file, "w", encoding="utf-8") as fh:
        yaml.dump(feedback_data, fh, allow_unicode=True, default_flow_style=False, sort_keys=False)

    # Update ticket status
    ticket["status"] = "queued"
    ticket["human_feedback"] = True
    ticket["retry_count"] = ticket.get("retry_count", 0) + 1
    ticket["last_feedback"] = timestamp
    ticket["last_feedback_reason"] = reason
    save_ticket(ticket["_file"], {k: v for k, v in ticket.items() if not k.startswith("_")})

    # Clean up REGISTRY.md and patterns/
    _remove_registry_entry(exp_id)
    _remove_pattern_entries(exp_id, exp_name)

    # Append to human-rejections.md
    rejections_path = PROJECT_ROOT / "archive" / "human-rejections.md"
    rejections_path.parent.mkdir(parents=True, exist_ok=True)

    issues_formatted = "\n".join(f"  - {i}" for i in issues_list)
    entry = f"""
### {exp_name} ({category}) — {timestamp}

- **理由**: {reason}
- **深刻度**: {severity}
- **具体的な問題**:
{issues_formatted}
- **改善方向**: {improvement_direction}
- **差し戻し回数**: {ticket.get('retry_count', 1)}

---
"""
    with open(rejections_path, "a", encoding="utf-8") as fh:
        fh.write(entry)

    return True, f"Feedback submitted for {exp_name}"

# --- HTML Templates ---

def render_page(title, body, active_tab=""):
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
nav a {{ padding:10px 20px; color:#888; border-bottom:2px solid transparent; white-space:nowrap; }}
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
.badge-rejected {{ background:#f9731620; color:#fb923c; border:1px solid #f9731640; }}

.badge-high {{ background:#dc262620; color:#f87171; }}
.badge-medium {{ background:#f59e0b20; color:#fbbf24; }}
.badge-low {{ background:#22c55e20; color:#4ade80; }}

.badge-critical {{ background:#dc262620; color:#f87171; border:1px solid #dc262640; }}

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
.btn-reject {{ background:#f97316; color:#000; }}
.btn-reject:hover {{ background:#ea580c; }}
.btn-secondary {{ background:#333; color:#e0e0e0; }}
.btn-secondary:hover {{ background:#444; }}

.stats {{ display:grid; grid-template-columns:repeat(auto-fit, minmax(120px, 1fr)); gap:12px; margin:16px 0; }}
.stat {{ background:#111; border:1px solid #222; border-radius:6px; padding:12px; text-align:center; }}
.stat-value {{ font-size:24px; font-weight:700; color:#9BBFFF; }}
.stat-label {{ font-size:11px; color:#666; text-transform:uppercase; margin-top:2px; }}
.stat-feedback .stat-value {{ color:#fb923c; }}

.tab-group {{ display:flex; gap:0; margin:16px 0 0; border-bottom:1px solid #222; }}
.tab {{ padding:8px 16px; cursor:pointer; color:#666; border-bottom:2px solid transparent; font-size:12px; }}
.tab:hover {{ color:#ccc; }}
.tab.active {{ color:#9BBFFF; border-bottom-color:#9BBFFF; }}
.tab-content {{ display:none; }}
.tab-content.active {{ display:block; }}

.empty {{ color:#555; font-style:italic; padding:20px; text-align:center; }}

/* Feedback form modal */
.fb-overlay {{ display:none; position:fixed; top:0; left:0; right:0; bottom:0; background:rgba(0,0,0,0.7); z-index:200; align-items:center; justify-content:center; }}
.fb-overlay.open {{ display:flex; }}
.fb-modal {{ background:#111; border:1px solid #333; border-radius:8px; padding:24px; width:560px; max-width:90vw; max-height:90vh; overflow-y:auto; }}
.fb-modal h2 {{ color:#fb923c; margin-bottom:16px; }}

/* Feedback history card */
.fb-entry {{ background:#0d0d0d; border:1px solid #222; border-radius:4px; padding:12px; margin:8px 0; }}
.fb-entry-header {{ display:flex; justify-content:space-between; align-items:center; margin-bottom:6px; }}
.fb-timestamp {{ color:#555; font-size:11px; }}
.fb-reason {{ color:#fb923c; font-weight:600; }}
.fb-issues {{ color:#aaa; font-size:12px; margin:4px 0; padding-left:16px; }}
.fb-direction {{ color:#9BBFFF; font-size:12px; margin-top:4px; }}

/* Human feedback indicator */
.has-feedback {{ border-left:3px solid #f97316; }}
</style>
</head>
<body>
<nav>
  <a href="/" class="{'active' if active_tab=='dashboard' else ''}">Dashboard</a>
  <a href="/tickets" class="{'active' if active_tab=='tickets' else ''}">Tickets</a>
  <a href="/tickets/new" class="{'active' if active_tab=='new-ticket' else ''}">+ New</a>
  <a href="/experiments" class="{'active' if active_tab=='experiments' else ''}">Experiments</a>
  <a href="/feedback" class="{'active' if active_tab=='feedback' else ''}">Feedback</a>
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

def severity_badge(severity):
    s = str(severity).strip().lower()
    return f'<span class="badge badge-{s}">{severity}</span>'

def feedback_indicator(ticket):
    """Show feedback-related indicators on a ticket."""
    parts = []
    if ticket.get("human_feedback"):
        parts.append('<span class="badge badge-rejected">FB</span>')
    retry = ticket.get("retry_count", 0)
    if retry > 0:
        parts.append(f'<span style="color:#666;font-size:11px;">retry:{retry}</span>')
    return " ".join(parts)

# --- Request Handler ---

class DashboardHandler(SimpleHTTPRequestHandler):
    def log_message(self, format, *args):
        pass

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
        elif path == "/feedback":
            qs = parse_qs(parsed.query)
            just_submitted = "submitted" in qs
            self.serve_feedback(just_submitted=just_submitted)
        elif path.startswith("/feedback/new/"):
            # /feedback/new/experiments/ui/001-xxx → show feedback form
            exp_path = "/".join(path.split("/")[3:])
            self.serve_feedback_form(exp_path)
        elif path == "/patterns":
            self.serve_patterns()
        elif path == "/registry":
            self.serve_registry()
        elif path == "/api/tickets":
            self.serve_json(load_tickets())
        elif path == "/api/feedback":
            self.serve_json(load_all_feedback())
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

        elif parsed.path == "/feedback/submit":
            content_length = int(self.headers["Content-Length"])
            body = self.rfile.read(content_length).decode("utf-8")
            params = parse_qs(body)

            exp_path = params.get("experiment_path", [""])[0]
            reason = params.get("reason", [""])[0]
            issues = params.get("issues", [""])[0]
            improvement = params.get("improvement_direction", [""])[0]
            severity = params.get("severity", ["medium"])[0]

            success, msg = submit_feedback(exp_path, reason, issues, improvement, severity)

            if success:
                self.send_response(303)
                self.send_header("Location", "/feedback?submitted=1")
                self.end_headers()
            else:
                # Show error page instead of raw text
                error_body = f"""
                <h1 style="color:#f87171;">Feedback Submission Error</h1>
                <div class="card">
                  <p>{msg}</p>
                  <p style="margin-top:12px;"><a href="/experiments">← Experiments に戻る</a></p>
                </div>
                """
                self.serve_html("Error", error_body, "feedback")

        elif self.path.startswith("/tickets/") and self.path.endswith("/status"):
            content_length = int(self.headers["Content-Length"])
            body = self.rfile.read(content_length).decode("utf-8")
            params = parse_qs(body)
            filename = self.path.split("/")[2]
            new_status = params.get("status", ["queued"])[0]

            path = PROJECT_ROOT / "tickets" / filename
            if path.exists():
                with open(path, encoding="utf-8") as f:
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

    # --- Dashboard ---
    def serve_dashboard(self):
        tickets = load_tickets()
        by_status = {}
        for t in tickets:
            s = t.get("status", "unknown")
            by_status[s] = by_status.get(s, 0) + 1

        experiments = load_experiments()
        total_exp = sum(len(v) for v in experiments.values())
        all_feedback = load_all_feedback()
        fb_count = len(all_feedback)
        tickets_with_fb = sum(1 for t in tickets if t.get("human_feedback"))

        stats = f"""
        <h1>XR R&D Dashboard</h1>
        <div class="stats">
          <div class="stat"><div class="stat-value">{len(tickets)}</div><div class="stat-label">Total Tickets</div></div>
          <div class="stat"><div class="stat-value">{by_status.get('queued', 0)}</div><div class="stat-label">Queued</div></div>
          <div class="stat"><div class="stat-value">{by_status.get('in-progress', 0)}</div><div class="stat-label">In Progress</div></div>
          <div class="stat"><div class="stat-value">{by_status.get('passed', 0)}</div><div class="stat-label">Passed</div></div>
          <div class="stat"><div class="stat-value">{by_status.get('archived', 0)}</div><div class="stat-label">Archived</div></div>
          <div class="stat"><div class="stat-value">{total_exp}</div><div class="stat-label">Experiments</div></div>
          <div class="stat stat-feedback"><div class="stat-value">{fb_count}</div><div class="stat-label">Feedback</div></div>
          <div class="stat stat-feedback"><div class="stat-value">{tickets_with_fb}</div><div class="stat-label">FB Pending</div></div>
        </div>
        """

        # Recent tickets
        recent = tickets[:10]
        rows = ""
        for t in recent:
            fb_ind = feedback_indicator(t)
            rows += f"""<tr>
              <td>{t.get('id','?')}</td>
              <td>{t.get('title','')}</td>
              <td><span class="badge badge-{t.get('category','')}">{t.get('category','')}</span></td>
              <td>{priority_badge(t.get('priority',''))}</td>
              <td>{status_badge(t.get('status',''))}</td>
              <td>{fb_ind}</td>
            </tr>"""

        # Recent feedback
        fb_rows = ""
        for fb in all_feedback[:5]:
            fb_rows += f"""<tr>
              <td>{fb.get('_experiment','')}</td>
              <td>{fb.get('_category','')}</td>
              <td class="fb-reason">{fb.get('reason','')[:60]}</td>
              <td>{severity_badge(fb.get('severity','medium'))}</td>
              <td class="fb-timestamp">{fb.get('timestamp','')}</td>
            </tr>"""

        fb_section = ""
        if fb_rows:
            fb_section = f"""
            <h2>Recent Feedback</h2>
            <table>
              <tr><th>Experiment</th><th>Cat</th><th>Reason</th><th>Severity</th><th>Date</th></tr>
              {fb_rows}
            </table>
            <a href="/feedback" style="font-size:12px;">View all feedback →</a>
            """

        body = stats + f"""
        <h2>Recent Tickets</h2>
        <table>
          <tr><th>ID</th><th>Title</th><th>Category</th><th>Priority</th><th>Status</th><th>FB</th></tr>
          {rows}
        </table>
        {fb_section}
        """
        self.serve_html("Dashboard", body, "dashboard")

    # --- Tickets ---
    def serve_tickets(self):
        tickets = load_tickets()
        rows = ""
        for t in tickets:
            vl = t.get("verification_level", "static")
            fb_ind = feedback_indicator(t)
            row_class = "has-feedback" if t.get("human_feedback") else ""
            rows += f"""<tr class="{row_class}">
              <td>{t.get('id','?')}</td>
              <td><a href="/tickets/{t.get('_file','')}">{t.get('title','')}</a></td>
              <td><span class="badge badge-{t.get('category','')}">{t.get('category','')}</span></td>
              <td>{priority_badge(t.get('priority',''))}</td>
              <td>{vl}</td>
              <td>{status_badge(t.get('status',''))}</td>
              <td>{fb_ind}</td>
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
          <tr><th>ID</th><th>Title</th><th>Cat</th><th>Pri</th><th>Verify</th><th>Status</th><th>FB</th><th>Change</th></tr>
          {rows if rows else '<tr><td colspan="8" class="empty">No tickets yet</td></tr>'}
        </table>
        """
        self.serve_html("Tickets", body, "tickets")

    # --- Ticket Detail ---
    def serve_ticket_detail(self, filename):
        path = PROJECT_ROOT / "tickets" / filename
        if not path.exists():
            self.send_error(404)
            return
        with open(path, encoding="utf-8") as f:
            t = yaml.safe_load(f)

        scope_list = "".join(f"<li>{s}</li>" for s in t.get("scope", []))
        criteria_list = "".join(f"<li>{s}</li>" for s in t.get("success_criteria", []))
        skills_list = ", ".join(f"<code>{s}</code>" for s in t.get("skills_needed", []))

        # Feedback section
        fb_html = ""
        ticket_id = str(t.get("id", "")).zfill(3)
        experiments = load_experiments()
        related_feedbacks = []
        for cat, exps in experiments.items():
            for exp in exps:
                if exp["name"].startswith(ticket_id):
                    if "human_feedback" in exp:
                        fb = exp["human_feedback"]
                        fb["_experiment"] = exp["name"]
                        fb["_path"] = exp["path"]
                        related_feedbacks.append(fb)

        if related_feedbacks or t.get("human_feedback"):
            fb_entries = ""
            for fb in related_feedbacks:
                issues_html = "".join(f"<li>{i}</li>" for i in fb.get("specific_issues", []))
                history_count = len(fb.get("history", []))
                history_note = f' <span style="color:#555;font-size:11px;">(過去{history_count}件)</span>' if history_count else ""

                fb_entries += f"""
                <div class="fb-entry">
                  <div class="fb-entry-header">
                    <span class="fb-reason">{fb.get('reason','')}</span>
                    <span>{severity_badge(fb.get('severity','medium'))} <span class="fb-timestamp">{fb.get('timestamp','')}</span></span>
                  </div>
                  <div class="fb-issues"><ul>{issues_html}</ul></div>
                  <div class="fb-direction">改善方向: {fb.get('improvement_direction','')}</div>
                  {f'<div style="color:#555;font-size:11px;margin-top:4px;">実験: {fb.get("_experiment","")}{history_note}</div>' if fb.get('_experiment') else ''}
                </div>"""

                # Show history entries
                for hist in fb.get("history", []):
                    hist_issues = "".join(f"<li>{i}</li>" for i in hist.get("specific_issues", []))
                    fb_entries += f"""
                    <div class="fb-entry" style="opacity:0.6;margin-left:20px;">
                      <div class="fb-entry-header">
                        <span class="fb-reason">{hist.get('reason','')}</span>
                        <span class="fb-timestamp">{hist.get('timestamp','')}</span>
                      </div>
                      <div class="fb-issues"><ul>{hist_issues}</ul></div>
                      <div class="fb-direction">改善方向: {hist.get('improvement_direction','')}</div>
                    </div>"""

            status_note = ""
            if t.get("human_feedback"):
                status_note = '<div style="background:#f9731620;border:1px solid #f9731640;border-radius:4px;padding:8px 12px;margin-bottom:12px;color:#fb923c;font-size:12px;">このチケットはフィードバックにより再キューされています。次回Builder実行時にフィードバックを読み込んで改修します。</div>'

            fb_html = f"""
            <h2 style="color:#fb923c;">Human Feedback</h2>
            {status_note}
            {fb_entries if fb_entries else '<p class="empty">フィードバックなし</p>'}
            """

        body = f"""
        <h1>#{t.get('id','')} {t.get('title','')}</h1>
        <div class="card">
          <div style="display:grid;grid-template-columns:1fr 1fr 1fr 1fr;gap:12px;margin-bottom:12px;">
            <div><span style="color:#666">Category:</span> <span class="badge badge-{t.get('category','')}">{t.get('category','')}</span></div>
            <div><span style="color:#666">Priority:</span> {priority_badge(t.get('priority',''))}</div>
            <div><span style="color:#666">Status:</span> {status_badge(t.get('status',''))}</div>
            <div><span style="color:#666">Verify:</span> {t.get('verification_level','static')}</div>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr 1fr 1fr;gap:12px;margin-bottom:12px;">
            <div><span style="color:#666">Complexity:</span> {t.get('estimated_complexity','')}</div>
            <div><span style="color:#666">Retries:</span> {t.get('retry_count',0)}</div>
            <div><span style="color:#666">Human FB:</span> {'<span class="badge badge-rejected">Yes</span>' if t.get('human_feedback') else '<span style="color:#555">No</span>'}</div>
            <div><span style="color:#666">Last FB:</span> <span style="color:#555;font-size:11px">{t.get('last_feedback','—')}</span></div>
          </div>
          <h3>Hypothesis</h3>
          <p>{t.get('hypothesis','')}</p>
          <h3>Scope</h3>
          <ul>{scope_list}</ul>
          <h3>Skills Needed</h3>
          <p>{skills_list}</p>
          <h3>Success Criteria</h3>
          <ul>{criteria_list}</ul>
        </div>
        {fb_html}
        <a href="/tickets">← Back to tickets</a>
        """
        self.serve_html(f"Ticket #{t.get('id','')}", body, "tickets")

    # --- New Ticket ---
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

    # --- Experiments ---
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

                # Feedback indicator
                fb_badge = ""
                if "human_feedback" in exp:
                    fb = exp["human_feedback"]
                    fb_badge = f'<span class="badge badge-rejected" title="{fb.get("reason","")[:80]}">Rejected</span>'

                readme_html = exp.get("readme_html", "<p class='empty'>No README</p>")

                # Reject button (only for experiments with PASS verdict)
                reject_btn = ""
                has_pass = False
                if "review_result" in exp:
                    has_pass = exp["review_result"].get("verdict") == "PASS"
                if has_pass or "test_result" in exp:
                    reject_btn = f'<a href="/feedback/new/{exp["path"]}" class="btn-reject" style="padding:4px 12px;border-radius:3px;font-size:11px;font-weight:600;text-decoration:none;color:#000;">差し戻し</a>'

                card_class = "card has-feedback" if "human_feedback" in exp else "card"
                cards += f"""
                <div class="{card_class}">
                  <div class="card-header">
                    <div><strong>{exp['name']}</strong> <span style="color:#555;font-size:11px">{exp['path']}</span></div>
                    <div style="display:flex;gap:8px;align-items:center;">{fb_badge} {test_html} {score_html} {reject_btn}</div>
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

    # --- Feedback Form ---
    def serve_feedback_form(self, experiment_path):
        exp_full_path = PROJECT_ROOT / experiment_path
        if not exp_full_path.exists():
            self.send_error(404, f"Experiment not found: {experiment_path}")
            return

        exp_name = exp_full_path.name
        category = exp_full_path.parent.name

        # Load existing feedback if any
        existing_fb = ""
        fb_file = exp_full_path / "human-feedback.yaml"
        if fb_file.exists():
            with open(fb_file, encoding="utf-8") as fh:
                fb = yaml.safe_load(fh)
                if fb:
                    existing_fb = f"""
                    <div class="fb-entry" style="margin-bottom:16px;">
                      <div style="color:#888;font-size:11px;margin-bottom:4px;">前回のフィードバック ({fb.get('timestamp','')}) — 新規送信すると履歴として保存されます</div>
                      <div class="fb-reason">{fb.get('reason','')}</div>
                      <div class="fb-issues"><ul>{''.join(f"<li>{i}</li>" for i in fb.get('specific_issues', []))}</ul></div>
                      <div class="fb-direction">改善方向: {fb.get('improvement_direction','')}</div>
                    </div>"""

        body = f"""
        <h1 style="color:#fb923c;">差し戻し: {exp_name}</h1>
        <p style="color:#666;margin-bottom:16px;">{experiment_path}</p>
        {existing_fb}
        <form method="POST" action="/feedback/submit" class="card" onsubmit="return confirm('この実験を差し戻しますか？\\n\\nチケットが queued に戻り、REGISTRY・patternsからエントリが削除されます。')">
          <input type="hidden" name="experiment_path" value="{experiment_path}">
          <div class="form-group">
            <label>理由（概要）</label>
            <input name="reason" required placeholder="例: フォーカスのアウトラインが見えない">
          </div>
          <div class="form-group">
            <label>深刻度</label>
            <select name="severity">
              <option value="low">low — 軽微（あると良い改善）</option>
              <option value="medium" selected>medium — 中程度（使用に支障あり）</option>
              <option value="high">high — 深刻（基本機能が動かない）</option>
              <option value="critical">critical — 致命的（クラッシュ/データ損失）</option>
            </select>
          </div>
          <div class="form-group">
            <label>具体的な問題点（1行ずつ）</label>
            <textarea name="issues" rows="5" required placeholder="スワイプの反応が3回に1回しかない&#10;フォーカスインジケータの色が暗すぎる&#10;テキストが小さすぎて読めない"></textarea>
          </div>
          <div class="form-group">
            <label>改善方向（Builderへの指示）</label>
            <textarea name="improvement_direction" rows="3" required placeholder="例: フォーカスインジケータの色をもっと明るくする。最低限#FFFFFFで2pxのボーダーが必要"></textarea>
          </div>
          <div style="display:flex;gap:12px;margin-top:16px;">
            <button type="submit" class="btn-reject">差し戻し送信</button>
            <a href="/experiments" class="btn-secondary" style="padding:8px 20px;border-radius:4px;font-weight:600;text-decoration:none;">キャンセル</a>
          </div>
        </form>
        """
        self.serve_html(f"Feedback: {exp_name}", body, "feedback")

    # --- Feedback History ---
    def serve_feedback(self, just_submitted=False):
        all_feedback = load_all_feedback()
        rejections_md = load_human_rejections()
        rejections_html = markdown.markdown(rejections_md, extensions=["fenced_code", "tables"])

        # Summary stats
        severity_counts = {}
        for fb in all_feedback:
            sev = fb.get("severity", "medium")
            severity_counts[sev] = severity_counts.get(sev, 0) + 1

        stats_html = ""
        if all_feedback:
            stats_html = f"""
            <div class="stats">
              <div class="stat stat-feedback"><div class="stat-value">{len(all_feedback)}</div><div class="stat-label">Total Feedback</div></div>
              <div class="stat"><div class="stat-value" style="color:#f87171;">{severity_counts.get('critical',0) + severity_counts.get('high',0)}</div><div class="stat-label">High/Critical</div></div>
              <div class="stat"><div class="stat-value" style="color:#fbbf24;">{severity_counts.get('medium',0)}</div><div class="stat-label">Medium</div></div>
              <div class="stat"><div class="stat-value" style="color:#4ade80;">{severity_counts.get('low',0)}</div><div class="stat-label">Low</div></div>
            </div>"""

        # Feedback cards
        fb_cards = ""
        for fb in all_feedback:
            issues_html = "".join(f"<li>{i}</li>" for i in fb.get("specific_issues", []))
            history_count = len(fb.get("history", []))
            retry_note = f'<span class="badge badge-medium">retry x{history_count + 1}</span>' if history_count else ""

            fb_cards += f"""
            <div class="fb-entry">
              <div class="fb-entry-header">
                <div>
                  <strong>{fb.get('_experiment','')}</strong>
                  <span class="badge badge-{fb.get('_category','')}" style="margin-left:8px;">{fb.get('_category','')}</span>
                  {retry_note}
                </div>
                <div>{severity_badge(fb.get('severity','medium'))} <span class="fb-timestamp">{fb.get('timestamp','')}</span></div>
              </div>
              <div class="fb-reason" style="margin:6px 0;">{fb.get('reason','')}</div>
              <div class="fb-issues"><ul>{issues_html}</ul></div>
              <div class="fb-direction">改善方向: {fb.get('improvement_direction','')}</div>
            </div>"""

        success_banner = ""
        if just_submitted:
            success_banner = '<div style="background:#22c55e20;border:1px solid #22c55e40;border-radius:4px;padding:10px 16px;margin-bottom:16px;color:#4ade80;">フィードバックを送信しました。チケットは queued に戻され、次回のOrchestrator実行時に優先処理されます。</div>'

        body = f"""
        <h1 style="color:#fb923c;">Feedback History</h1>
        {success_banner}
        {stats_html}
        <div class="tab-group">
          <div class="tab active" onclick="switchFbTab('active')">Active Feedback ({len(all_feedback)})</div>
          <div class="tab" onclick="switchFbTab('archive')">Archive (human-rejections.md)</div>
        </div>
        <div class="tab-content active" id="fbtab-active">
          {fb_cards if fb_cards else '<p class="empty">フィードバックなし。Experiments タブから差し戻しできます。</p>'}
        </div>
        <div class="tab-content" id="fbtab-archive">
          <div class="card md-content">{rejections_html}</div>
        </div>
        <script>
        function switchFbTab(id) {{
          document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
          document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
          event.target.classList.add('active');
          document.getElementById('fbtab-' + id).classList.add('active');
        }}
        </script>
        """
        self.serve_html("Feedback", body, "feedback")

    # --- Patterns ---
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

    # --- Registry ---
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
