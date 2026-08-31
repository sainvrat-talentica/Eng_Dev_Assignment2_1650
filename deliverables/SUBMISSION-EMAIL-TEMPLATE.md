# Assignment 2 — Email Submission Template

Copy, fill in the bracketed placeholders, and send to your course coordinator / evaluators.

---

**Subject:** H1 Assignment 2 Submission — Delivery Failure Root-Cause Analytics (SwiftEats)

**Body:**

Dear Team,

Please find my submission for **H1 Assignment 2 — Delivery Failure Root-Cause Analytics**.

**GitHub repository (private):**  
https://github.com/sainvrat-talentica/Eng_Dev_Assignment2_1650

**OneDrive / shared folder (write-up, video, recorded outputs):**  
[YOUR_ONEDRIVE_OR_SHARED_FOLDER_LINK]

**Folder contents:**
1. `ASSIGNMENT-2-WRITEUP.docx` — problem solution write-up with architecture diagram
2. Demo video recording (8–10 min) — local walkthrough of all six sample use cases
3. `SAMPLE-USE-CASE-OUTPUTS.docx` — recorded outputs for UC1–UC6 (with JSON evidence in `raw-responses/`)

**How to run locally:**
```bash
git clone https://github.com/sainvrat-talentica/Eng_Dev_Assignment2_1650.git
cd Eng_Dev_Assignment2_1650
docker compose up --build
curl -X POST "http://localhost:8080/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: local-dev-admin-key-change-me"
./scripts/verify_analytics_gateway.sh
```

Analytics dashboard: http://localhost:3002  
API docs: http://localhost:8080/swagger-ui.html

**Sample use cases demonstrated:**
| UC | Question |
|----|----------|
| UC1 | Delays in city X on a date |
| UC2 | Client X order failures in a period |
| UC3 | Warehouse B failure reasons in August |
| UC4 | Compare failure causes City A vs City B |
| UC5 | Festival period failures and preparation |
| UC6 | Capacity risk for +20,000 monthly orders |

Please let me know if you need access to the private repository.

Regards,  
[YOUR_NAME]  
[YOUR_EMAIL]

---

**Before sending — verify:**
- [ ] GitHub repo is private and collaborators invited (if required)
- [ ] OneDrive link grants view access to evaluators
- [ ] Video includes voice explanation
- [ ] Word write-up and sample outputs are in the shared folder
