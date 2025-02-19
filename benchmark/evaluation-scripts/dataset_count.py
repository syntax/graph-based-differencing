import json

with open('dataset_info.json', 'r') as file:
    data = json.load(file)

total_entries = len(data)
print("Total entries:", total_entries)

commit_ids = {entry["commit_id"] for entry in data}
commit_ids = set(commit_ids)
print("Unique commit_ids:", len(commit_ids))
