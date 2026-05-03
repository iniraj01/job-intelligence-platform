import sys
import json
import re
import spacy
from PyPDF2 import PdfReader
from datetime import datetime

# Predefined lists for simple matching
SKILLS_DB = [
    "java", "python", "sql", "spring boot", "react", "angular", "javascript", "js",
    "c++", "c#", "html", "css", "docker", "kubernetes", "aws", "azure", "gcp",
    "git", "linux", "mongodb", "mysql", "postgresql", "node.js", "typescript",
    "machine learning", "data science", "nlp", "cybersecurity", "iot", "web developer"
]

COMMON_ROLES = [
    "software engineer", "developer", "intern", "data scientist",
    "product manager", "project manager", "qa engineer", "devops engineer",
    "system administrator", "analyst", "architect"
]

def extract_text_from_pdf(pdf_path):
    try:
        reader = PdfReader(pdf_path)
        text = ""
        for page in reader.pages:
            page_text = page.extract_text()
            if page_text:
                text += page_text + " "
        return text
    except Exception as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(1)

def parse_date(date_str):
    date_str = date_str.strip().lower()
    if date_str in ['present', 'current', 'now', 'today']:
        return datetime.now()
    # Try different formats
    for fmt in ('%b %Y', '%B %Y', '%m/%Y', '%m-%Y', '%Y', '%b-%Y'):
        try:
            return datetime.strptime(date_str, fmt)
        except ValueError:
            pass
    # If it's just a year like '2020'
    match = re.search(r'\b(20[0-2]\d|199\d)\b', date_str)
    if match:
        try:
            return datetime.strptime(match.group(1), '%Y')
        except ValueError:
            pass
    return None

def extract_experience(text):
    # 1. Try direct values first
    patterns = [
        r'(\d+(?:\.\d+)?)\+?\s*(?:years?|yrs?)(?:\s*of)?\s*experience',
        r'experience(?:.*?)(\d+(?:\.\d+)?)\+?\s*(?:years?|yrs?)',
        r'(?<!\d)(\d+(?:\.\d+)?)\+?\s*(?:years?|yrs?)(?!\s*old)'
    ]
    direct_years = []
    for pattern in patterns:
        matches = re.findall(pattern, text, re.IGNORECASE)
        if matches:
            direct_years.extend([float(m) for m in matches if float(m) < 40])
    
    if direct_years:
        return int(max(direct_years))

    # 2. If no direct value, look for date ranges
    month_regex = r'(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?|0?[1-9]|1[0-2])'
    year_regex = r'(?:199\d|20[0-2]\d)'
    date_part = rf'(?:{month_regex}[\s/-]+)?{year_regex}'
    range_regex = rf'({date_part})\s*(?:-|–|to|until)\s*({date_part}|present|current|now|today)'
    
    ranges = re.findall(range_regex, text, re.IGNORECASE)
    parsed_ranges = []
    for start_str, end_str in ranges:
        start_date = parse_date(start_str)
        end_date = parse_date(end_str)
        if start_date and end_date and start_date <= end_date:
            parsed_ranges.append((start_date, end_date))
            
    if not parsed_ranges:
        return 0
        
    # Merge overlapping ranges
    parsed_ranges.sort(key=lambda x: x[0])
    merged = []
    for current in parsed_ranges:
        if not merged:
            merged.append(current)
        else:
            last = merged[-1]
            if current[0] <= last[1]: # Overlap
                merged[-1] = (last[0], max(last[1], current[1]))
            else:
                merged.append(current)
                
    # Calculate total duration in days
    total_days = 0
    for start, end in merged:
        total_days += (end - start).days
        
    return int(max(1, total_days / 365))

def extract_information(text):
    try:
        nlp = spacy.load("en_core_web_sm")
        doc = nlp(text)
        
        extracted_skills = set()
        extracted_roles = set()
        extracted_companies = set()
        
        text_lower = text.lower()
        
        # 1. Extract Skills
        for skill in SKILLS_DB:
            if skill in text_lower:
                extracted_skills.add(skill.title() if len(skill) > 3 else skill.upper())
                
        # 2. Extract Roles (Keyword matching + some basic NLP)
        for role in COMMON_ROLES:
            if role in text_lower:
                extracted_roles.add(role.title())
                
        # 3. Extract Companies (Using spaCy ORG entities)
        for ent in doc.ents:
            if ent.label_ == "ORG":
                # Filter out some common false positives from ORG
                cleaned_org = ent.text.strip().replace('\n', ' ')
                if len(cleaned_org) > 2 and cleaned_org.lower() not in SKILLS_DB and cleaned_org.lower() not in COMMON_ROLES:
                    extracted_companies.add(cleaned_org)

        # 4. Extract Experience
        experience_years = extract_experience(text)
        
        return {
            "skills": list(extracted_skills),
            "experience_years": experience_years,
            "roles": list(extracted_roles),
            "companies": list(extracted_companies)[:5], # Limit companies
            "raw_text": text
        }
    except Exception as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"error": "No PDF path provided"}))
        sys.exit(1)

    pdf_path = sys.argv[1]
    
    # 1. Extract text
    text = extract_text_from_pdf(pdf_path)
    
    # 2. Extract structured data
    parsed_data = extract_information(text)
    
    # 3. Print output as JSON
    print(json.dumps(parsed_data))
