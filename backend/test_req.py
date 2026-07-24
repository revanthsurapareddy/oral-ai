import requests
with open('c:/Users/vavil/OneDrive/Desktop/test/1input.jpeg', 'rb') as f:
    resp = requests.post('http://localhost:8000/analyze', files={'file': f})
    
print(resp.status_code)
try:
    print(resp.json()["has_cancer"])
except:
    print(resp.text)
