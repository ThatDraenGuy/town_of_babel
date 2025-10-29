import requests
import json
from dataclasses import dataclass


class GithubApiException(Exception):
    def __init__(self, status_code):
        self.status_code = status_code

@dataclass
class GithubRepoDescription:
    owner : str
    name : str

    def fetch_languages(self):
        url = f"https://api.github.com/repos/{self.owner}/{self.name}/languages"
        headers = {"Accept": "application/vnd.github.v3+json"}
        response = requests.get(url, headers=headers)

        if (statuc_code := response.status_code) == 200:
            languages = response.json()
            return languages
        else:
            raise GithubApiException(statuc_code)
