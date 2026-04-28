import sys
import json
import re
import spacy
from PyPDF2 import PdfReader

# Predefined lists for simple matching
SKILLS_DB = [
    "java", "python", "sql", "spring boot", "react", "angular", "javascript",
    "c++", "c#", "html", "css", "docker", "kubernetes", "aws", "azure", "gcp",
    "git", "linux", "mongodb", "mysql", "postgresql", "node.js", "typescript",
    "machine learning", "data science", "nlp"
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

def extract_experience(text):
    # Match patterns like "3 years of experience", "5+ years", "2 yrs", "Experience: 3 years"
    patterns = [
        r'(\d+)\+?\s*(?:years?|yrs?)(?:\s*of)?\s*experience', # 3 years of experience
        r'experience(?:.*?)(\d+)\+?\s*(?:years?|yrs?)',        # Experience: 3 years
        r'(\d+)\+?\s*(?:years?|yrs?)'                          # fallback: 3 years (if near experience, but let's just match any "X years")
    ]
    
    # First try strict patterns
    for pattern in patterns[:2]:
        matches = re.findall(pattern, text, re.IGNORECASE)
        if matches:
            years = [int(m) for m in matches]
            return max(years)
            
    # If not found, try finding the word "experience" and looking for years nearby
    text_lower = text.lower()
    if "experience" in text_lower:
        # Just find any digit followed by years
        matches = re.findall(r'(\d+)\+?\s*(?:years?|yrs?)', text_lower)
        if matches:
            # Filter out ridiculously high numbers (like 2020 years)
            years = [int(m) for m in matches if int(m) < 40]
            if years:
                return max(years)
                
    return 0

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
