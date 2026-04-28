import requests
import json
import sys

def fetch_jsearch_jobs():
    url = "https://jsearch.p.rapidapi.com/search"
    querystring = {"query": "Java Developer in India", "page": "1", "num_pages": "4"}
    
    headers = {
        "X-RapidAPI-Key": "57e320e8c7msh85519efa429050dp1f1065jsn4bcc6ae91dc7",
        "X-RapidAPI-Host": "jsearch.p.rapidapi.com"
    }
    
    jobs = []
    
    print("USING JSEARCH API", file=sys.stderr)
    
    try:
        response = requests.get(url, headers=headers, params=querystring, timeout=15)
        
        # Debug logs
        print("Status:", response.status_code, file=sys.stderr)
        
        if response.status_code != 200:
            raise Exception("API FAILED")
            
        data = response.json()
        results = data.get("data", [])
        
        for item in results:
            title = item.get("job_title", "")
            company = item.get("employer_name", "")
            location = item.get("job_city", "")
            link = item.get("job_apply_link", "")
            
            if title and company:
                jobs.append({
                    "title": title,
                    "company": company,
                    "location": location,
                    "link": link
                })
                
                # Limit to 40 jobs to prevent excessive memory/processing
                if len(jobs) >= 40:
                    break
                    
    except Exception as e:
        raise Exception("API FAILED") from e
        
    return jobs

if __name__ == "__main__":
    jobs = fetch_jsearch_jobs()
    
    # Output clean JSON only
    print(json.dumps(jobs))
