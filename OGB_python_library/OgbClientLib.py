# @author Bonvoyage/ICN2020 Univ. Tor Vergata Roma Project team

import json
import traceback
import datetime


import requests


class OgbClientLib:
    def __init__(self, server_url):
        self.FrontEndServerURL = server_url

    def is_valid_json(self, json_obj):
        try:
            json.loads(json_obj)
            return True
        except ValueError:
            return False
        except Exception:
            return False

    def login(self, user_id, tenant, password):
        try:
            headers = {"Content-Type" : "application/json"}
            login_params = {"tenantName": tenant, "userName": user_id, "password": password}
            url = self.FrontEndServerURL + "/OGB/user/login"
            r = requests.post(url, data=json.dumps(login_params), headers=headers)
            if r is not None:
                if 199 < r.status_code < 300:
                    return r.json()["token"]
        except Exception:
            print(traceback.format_exc())
        return None

    def insert_geoJSON(self, token, cid, geoJSON):
        try:
            url = self.FrontEndServerURL+"/OGB/content/insert/"+cid
            headers = {"Content-Type": "application/json", "Authorization": token}
            r = requests.post(url, data=geoJSON, headers=headers)
            if r is not None:
                if r.status_code == 200:
                    return r.json()["oid"]
                else:
                    print("Insertion failed")
                    print("SERVER REPLY, code: " + str(r.status_code) + " response: " + r.json()["response"])
                    return None
        except Exception:
            print(traceback.format_exc())
        return None

    def delete_object(self, token, oid):
        try:
            url = self.FrontEndServerURL + "/OGB/content/delete/"
            headers = {"Content-Type": "application/json", "Authorization": token}
            body = {"oid"+oid}
            r = requests.post(url, data=body, headers=headers)
            if r is not None:
                if r.status_code == 200:
                    return True
                else:
                    print("Delete failed")
                    print("SERVER REPLY, code: " + str(r.status_code) + " response: " + r.json()["response"])
            return False
        except Exception:
            print(traceback.format_exc())
            return False

    def query_object(self, token, cid, oid):
        try:
            url = self.FrontEndServerURL + "/OGB/query-service/element/" + cid
            headers = {"Content-Type": "application/json", "Authorization": token}
            body = {"oid" + oid}
            r = requests.post(url, data=body, headers=headers)
            if r is not None:
                if r.status_code == 200:
                    return r.content
                else:
                    print("Query failed")
                    print("SERVER REPLY, code: " + str(r.status_code) + " response: " + r.json()["response"])
            return None
        except Exception:
            print(traceback.format_exc())
            return None

    def query_geoJSON(self, token, cid, geoJSON):
        try:
            if not self.is_valid_json(geoJSON):
                print("Not valid JSON")
                return None
            url = self.FrontEndServerURL + "/OGB/query-service/" + cid
            headers = {"Content-Type": "application/json", "Authorization": token}
            r = requests.post(url, data=geoJSON, headers=headers)
            if r is not None:
                if r.status_code == 200:
                    return r.content
                else:
                    print("Query failed")
                    print("SERVER REPLY, code: " + str(r.status_code) + " response: " + r.json()["response"])
            return None
        except Exception:
            print(traceback.format_exc())
            return None

    def add_geoJSON(self, token, cid, geoJSON):
        try:
            if not self.is_valid_json(geoJSON):
                print("Not valid JSON")
                return None
            oid = self.insert_geoJSON(token, cid, geoJSON)
            return oid
        except Exception:
            print(traceback.format_exc())
            return None

    def add_point(self, token, cid, properties_map, location, start_date, stop_date):
        lat = location[1]
        lon = location[2]
        first = True
        json_obj = {"geometry": {"type": "Point", "coordinates": [str(lon), str(lat)]},
                "type": "Feature"}
        properties = {}
        for key in properties_map:
            properties{"key" : properties_map[key]}
        if start_date is not None and stop_date is not None:
            start = datetime.datetime.strptime(start_date, "yyyy-MM-dd'T'HH:mm:ss'Z'")
            stop = datetime.datetime.strptime(stop_date, "yyyy-MM-dd'T'HH:mm:ss'Z'")
            json_obj["properties"].